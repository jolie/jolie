/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ncoap.communication.reliability.inbound;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.communication.dispatching.Token;
import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;
import ncoap.message.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is the {@link org.jboss.netty.channel.ChannelUpstreamHandler} to
 * deal with inbound decoded {@link de.uzl.itm.ncoap.message.CoapMessage}s at
 * {@link de.uzl.itm.ncoap.application.server.CoapServer}s. If the inbound
 * message is a confirmable {@link de.uzl.itm.ncoap.message.CoapRequest} it
 * schedules the sending of an empty acknowledgement to the sender if there
 * wasn't a response from the addressed webresource within a period of 1.5
 * seconds.
 *
 * @author Oliver Kleine
 */
public class ServerInboundReliabilityHandler extends AbstractCoapChannelHandler {

    /**
     * Minimum delay in milliseconds (1500) between the reception of a
     * confirmable request and an empty ACK
     */
    public static final int EMPTY_ACK_DELAY = 1500;

    private static Logger LOG = LoggerFactory.getLogger(ServerInboundReliabilityHandler.class.getName());

    private Table<InetSocketAddress, Integer, Token> unprocessedRequests;
    private Table<InetSocketAddress, Integer, ScheduledFuture> scheduledEmptyAcknowledgements;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.reliability.inbound.ServerInboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService}
     * to provide the threads to execute the tasks for reliability.
     */
    public ServerInboundReliabilityHandler(ScheduledExecutorService executor) {
	super(executor);
	this.unprocessedRequests = HashBasedTable.create();
	this.scheduledEmptyAcknowledgements = HashBasedTable.create();

	this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, final InetSocketAddress remoteSocket) {

	LOG.debug("HANDLE INBOUND MESSAGE: {}", coapMessage);

	if (coapMessage instanceof CoapRequest) {
	    return handleInboundCoapRequest((CoapRequest) coapMessage, remoteSocket);
	} else if (coapMessage.isPing()) {
	    LOG.debug("Received PING from \"{}\" (Message ID: {}).", remoteSocket, coapMessage.getMessageID());
	    sendReset(coapMessage.getMessageID(), remoteSocket);
	    return false;
	} else {
	    return true;
	}
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

	LOG.debug("HANDLE OUTBOUND MESSAGE: {}", coapMessage);
	if (coapMessage instanceof CoapResponse) {
	    Token token = coapMessage.getToken();
	    int messageID = coapMessage.getMessageID();
	    removeUnprocessedRequest(remoteSocket, messageID, token);
	    if (!cancelEmptyAcknowledgement(remoteSocket, coapMessage.getMessageID())) {
		// will be set by the next handler
		coapMessage.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
	    } else {
		coapMessage.setMessageType(MessageType.ACK);
		LOG.info("Changed message type to ACK!");
	    }
	}
	return true;
    }

    private boolean handleInboundCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket) {

	int messageType = coapRequest.getMessageType();
	int messageID = coapRequest.getMessageID();

	if (!addUnprocessedRequest(remoteSocket, messageID, coapRequest.getToken())) {
	    LOG.info("Duplicate Request received from \"{}\" (message ID: {})", remoteSocket, messageID);
	    if (messageType == MessageType.CON) {
		ScheduledFuture future = getFromScheduledEmptyAcknowledgements(remoteSocket, messageID);
		if (future == null || future.isDone()) {
		    LOG.debug("Duplicate was CON. Send immediate empty ACK...");
		    sendEmptyACK(messageID, remoteSocket);
		} else {
		    LOG.debug("Duplicate was CON but first empty ACK was not yet sent. IGNORE duplicate!");
		}
	    } else {
		LOG.debug("Duplicate was NON! IGNORE!");
	    }
	    return false;
	} else {
	    if (messageType == MessageType.CON) {
		scheduleEmptyAcknowledgement(remoteSocket, messageID);
	    }
	    return true;
	}
    }

    private boolean addUnprocessedRequest(InetSocketAddress remoteSocket, int messageID, Token token) {
	try {
	    this.lock.readLock().lock();
	    if (this.unprocessedRequests.contains(remoteSocket, messageID)) {
		return false;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    if (this.unprocessedRequests.contains(remoteSocket, messageID)) {
		return false;
	    } else {
		this.unprocessedRequests.put(remoteSocket, messageID, token);
		return true;
	    }
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private void removeUnprocessedRequest(InetSocketAddress remoteSocket, int messageID, Token token) {
	try {
	    this.lock.readLock().lock();
	    if (!token.equals(this.unprocessedRequests.get(remoteSocket, messageID))) {
		return;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    if (token.equals(this.unprocessedRequests.get(remoteSocket, messageID))) {
		this.unprocessedRequests.remove(remoteSocket, messageID);
		LOG.debug("Removed request from \"{}\" from \"unprocessed\" (Message ID: {}, Token: {}).",
			new Object[]{remoteSocket, messageID, token});
	    }
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private void scheduleEmptyAcknowledgement(final InetSocketAddress remoteSocket, final int messageID) {

	try {
	    this.lock.readLock().lock();
	    if (this.scheduledEmptyAcknowledgements.contains(remoteSocket, messageID)) {
		LOG.debug("Empty ACK was already scheduled (RCPT: \"{}\", message ID: {}", remoteSocket, messageID);
		return;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    if (this.scheduledEmptyAcknowledgements.contains(remoteSocket, messageID)) {
		LOG.debug("Empty ACK was already scheduled (RCPT: \"{}\", message ID: {}", remoteSocket, messageID);
		return;
	    }
	    //RequestConfirmationTask confirmationTask = new RequestConfirmationTask(ctx, remoteSocket, messageID);
	    ScheduledFuture future = getExecutor().schedule(new Runnable() {
		@Override
		public void run() {
		    removeFromScheduledEmptyAcknowledgements(remoteSocket, messageID);
		    sendEmptyACK(messageID, remoteSocket);
		}
	    }, EMPTY_ACK_DELAY, TimeUnit.MILLISECONDS);
	    this.scheduledEmptyAcknowledgements.put(remoteSocket, messageID, future);
	    LOG.debug("Scheduled empty ACK (RCPT: \"{}\", message ID: {}", remoteSocket, messageID);
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private boolean cancelEmptyAcknowledgement(InetSocketAddress remoteSocket, int messageID) {
	try {
	    this.lock.readLock().lock();
	    ScheduledFuture future = this.scheduledEmptyAcknowledgements.get(remoteSocket, messageID);
	    if (future == null || future.isDone()) {
		return false;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	ScheduledFuture future = removeFromScheduledEmptyAcknowledgements(remoteSocket, messageID);
	if (future != null && !future.isDone()) {
	    if (future.cancel(false)) {
		LOG.info("Canceled empty ACK to \"{}\" (message ID: {})", remoteSocket, messageID);
		return true;
	    } else {
		LOG.warn("Could NOT cancel empty ACK to \"{}\" (message ID: {})", remoteSocket, messageID);
		return false;
	    }
	} else {
	    return false;
	}
    }

    private ScheduledFuture removeFromScheduledEmptyAcknowledgements(InetSocketAddress remoteSocket, int messageID) {
	try {
	    this.lock.writeLock().lock();
	    ScheduledFuture future = this.scheduledEmptyAcknowledgements.remove(remoteSocket, messageID);
	    if (LOG.isDebugEnabled() && future != null) {
		LOG.debug("Removed scheduled empty ACK (Remaining: {})", this.scheduledEmptyAcknowledgements.size());
	    }
	    return future;
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private ScheduledFuture getFromScheduledEmptyAcknowledgements(InetSocketAddress remoteSocket, int messageID) {
	try {
	    this.lock.readLock().lock();
	    return this.scheduledEmptyAcknowledgements.get(remoteSocket, messageID);
	} finally {
	    this.lock.readLock().unlock();
	}
    }
}
