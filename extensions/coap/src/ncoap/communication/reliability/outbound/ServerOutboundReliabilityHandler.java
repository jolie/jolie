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
package ncoap.communication.reliability.outbound;

import com.google.common.collect.HashBasedTable;

import ncoap.communication.dispatching.Token;
import ncoap.message.CoapMessage;
import ncoap.message.CoapResponse;
import ncoap.message.MessageCode;
import ncoap.message.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is the handler to deal with reliability message transfers1 (e.g.
 * retransmissions of confirmable messages) for CoAP Endpoints.
 *
 * @author Oliver Kleine
 */
public class ServerOutboundReliabilityHandler extends AbstractOutboundReliabilityHandler implements Observer {

    private static Logger LOG = LoggerFactory.getLogger(ServerOutboundReliabilityHandler.class.getName());

    private HashBasedTable<InetSocketAddress, Integer, Token> transfers1;
    private HashBasedTable<InetSocketAddress, Token, CoapResponse> transfers2;

    private ReentrantReadWriteLock lock;
    private final MessageIDFactory messageIDFactory;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.reliability.outbound.ServerOutboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService}
     * to process the tasks to ensure reliable message transfer
     */
    public ServerOutboundReliabilityHandler(ScheduledExecutorService executor, MessageIDFactory factory) {
	super(executor, factory);
	this.transfers1 = HashBasedTable.create();
	this.transfers2 = HashBasedTable.create();

	this.messageIDFactory = new MessageIDFactory(executor);
	this.messageIDFactory.addObserver(this);
	this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
	if (coapMessage instanceof CoapResponse) {
	    return handleOutboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
	} else {
	    return true;
	}
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
	if (coapMessage.getMessageCode() == MessageCode.EMPTY) {
	    return handleInboundEmptyMessage(coapMessage, remoteSocket);
	} else {
	    return true;
	}
    }

    @Override
    public void update(Observable o, Object arg) {
	if (arg instanceof MessageIDFactory.MessageIDRelease) {
	    InetSocketAddress remoteSocket = ((MessageIDFactory.MessageIDRelease) arg).getRemoteSocket();
	    Token token = ((MessageIDFactory.MessageIDRelease) arg).getToken();
	    CoapResponse coapResponse = removeTransfer(remoteSocket, token);
	    if (coapResponse != null && coapResponse.getMessageType() == MessageType.CON) {
		// there was an ongoing outbound transfer
		int messageID = coapResponse.getMessageID();
		LOG.warn("Transmission timed out (remote socket: \"{}\", token: {}, message ID: {})",
			new Object[]{remoteSocket, token, messageID});
		//triggerEvent(new TransmissionTimeoutEvent(remoteSocket, messageID, token), false);
	    } else {
		LOG.info("Message ID retirement does not lead to a transmission timeout (GOOD!)");
	    }
	} else {
	    LOG.error("This should never happen...");
	}
    }

    private boolean handleInboundEmptyMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
	int messageType = coapMessage.getMessageType();
	if (messageType == MessageType.CON) {
	    // incoming PINGs are handled by the inbound reliability handler
	    return true;
	} else {
	    int messageID = coapMessage.getMessageID();
	    Token token = removeTransfer(remoteSocket, messageID);
	    if (token != null && messageType == MessageType.RST) {
		LOG.info("Received RST from \"{}\" for token {} (Message ID: {}).",
			new Object[]{remoteSocket, messageID, token});
		//triggerEvent(new ResetReceivedEvent(remoteSocket, messageID, token), false);
	    } else {
		LOG.warn("No open transfer found for RST from \"{}\" (Message ID: {}).", remoteSocket, messageID);
	    }
	    return false;
	}
    }

    private boolean handleOutboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

	// update update notifications (i.e. send as next retransmission)
	if (coapResponse.isUpdateNotification() && coapResponse.getMessageType() != MessageType.ACK) {
	    if (updateRetransmission(remoteSocket, coapResponse)) {
		return false;
	    } else {
		coapResponse.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
	    }
	}

	int messageID = coapResponse.getMessageID();

	// set a new message ID if necessary
	if (messageID == CoapMessage.UNDEFINED_MESSAGE_ID) {
	    messageID = this.assignMessageID(coapResponse, remoteSocket);
	    if (messageID == CoapMessage.UNDEFINED_MESSAGE_ID) {
		return false;
	    }
	}

	int messageType = coapResponse.getMessageType();
	if (messageType == MessageType.CON) {
	    addTransfer(remoteSocket, coapResponse);
	    scheduleRetransmission(remoteSocket, coapResponse.getToken(), 1);
	} else if (messageType == MessageType.NON && coapResponse.isUpdateNotification()) {
	    addTransfer(remoteSocket, coapResponse);
	    scheduleTransferRemoval(remoteSocket, coapResponse.getMessageID());
	}
	return true;
    }

    private void addTransfer(InetSocketAddress remoteSocket, CoapResponse coapResponse) {
	try {
	    this.lock.writeLock().lock();
	    this.transfers1.put(remoteSocket, coapResponse.getMessageID(), coapResponse.getToken());
	    this.transfers2.put(remoteSocket, coapResponse.getToken(), coapResponse);
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private void scheduleTransferRemoval(final InetSocketAddress remoteSocket, final int messageID) {
	getExecutor().schedule(new Runnable() {

	    @Override
	    public void run() {
		if (removeTransfer(remoteSocket, messageID) != null) {
		    LOG.debug("Removed non-confirmable transfer for update notification (Remote Socket: {}). "
			    + "Cancellation with RST is not possible anymore.", remoteSocket);
		}
	    }

	}, 3, TimeUnit.SECONDS);
    }

    private void scheduleRetransmission(InetSocketAddress remoteSocket, Token token, int retransmissionNo) {
	long delay = provideRetransmissionDelay(retransmissionNo);
	ResponseRetransmissionTask task = new ResponseRetransmissionTask(remoteSocket, token, retransmissionNo);
	getExecutor().schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    private Token removeTransfer(InetSocketAddress remoteSocket, int messageID) {
	try {
	    this.lock.readLock().lock();
	    if (this.transfers1.get(remoteSocket, messageID) == null) {
		return null;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    Token token = this.transfers1.remove(remoteSocket, messageID);
	    if (token != null) {
		this.transfers2.remove(remoteSocket, token);
	    }
	    return token;
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private CoapResponse removeTransfer(InetSocketAddress remoteSocket, Token token) {
	try {
	    this.lock.readLock().lock();
	    if (!this.transfers2.contains(remoteSocket, token)) {
		return null;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    CoapResponse coapResponse = this.transfers2.remove(remoteSocket, token);
	    if (coapResponse != null) {
		this.transfers1.remove(remoteSocket, coapResponse.getMessageID());
	    }
	    return coapResponse;
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private boolean updateRetransmission(InetSocketAddress remoteSocket, CoapResponse updatedResponse) {
	Token token = updatedResponse.getToken();
	try {
	    //update the update notification to be retransmitted
	    lock.readLock().lock();
	    if (this.transfers2.get(remoteSocket, token) == null) {
		return false;
	    }
	} finally {
	    lock.readLock().unlock();
	}

	try {
	    lock.writeLock().lock();
	    CoapResponse previousResponse = this.transfers2.remove(remoteSocket, token);
	    if (previousResponse == null || previousResponse.getMessageType() == MessageType.NON) {
		return false;
	    } else {
		int messageID = previousResponse.getMessageID();
		updatedResponse.setMessageID(messageID);
		this.transfers2.put(remoteSocket, token, updatedResponse);
		LOG.info("Retransmission with updated response...");
		return true;
	    }
	} finally {
	    lock.writeLock().unlock();
	}
    }

    private CoapResponse getCoapResponse(InetSocketAddress remoteSocket, Token token) {
	try {
	    this.lock.readLock().lock();
	    return this.transfers2.get(remoteSocket, token);
	} finally {
	    this.lock.readLock().unlock();
	}
    }

    class ResponseRetransmissionTask implements Runnable {

	private InetSocketAddress remoteSocket;
	private Token token;
	private int retransmissionNo;

	private ResponseRetransmissionTask(InetSocketAddress remoteSocket, Token token, int retransmissionNo) {
	    this.remoteSocket = remoteSocket;
	    this.token = token;
	    this.retransmissionNo = retransmissionNo;
	}

	@Override
	public synchronized void run() {

	    final CoapResponse coapResponse = getCoapResponse(this.remoteSocket, this.token);

	    if (!(coapResponse == null)) {
		if (coapResponse.isUpdateNotification()) {
		    coapResponse.setObserve();
		}

		/*
		// retransmit message
		ChannelFuture future = Channels.future(getContext().getChannel());
		Channels.write(getContext(), future, coapResponse, remoteSocket);
		future.addListener(new ChannelFutureListener() {
		    @Override
		    public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
			    LOG.info("Retransmitted...");
			} else {
			    LOG.error("Retransmission failed...");
			}
		    }
		});
		 */
		if (retransmissionNo < MAX_RETRANSMISSIONS) {
		    scheduleRetransmission(remoteSocket, coapResponse.getToken(), retransmissionNo + 1);
		    LOG.debug("Scheduled next retransmission to \"{}\" (Message ID: {})",
			    remoteSocket, coapResponse.getMessageID()
		    );
		} else {
		    LOG.warn("No more retransmissions (remote endpoint: {}, message ID: {})!",
			    remoteSocket, coapResponse.getMessageID()
		    );
		}
	    }
	}
    }
}
