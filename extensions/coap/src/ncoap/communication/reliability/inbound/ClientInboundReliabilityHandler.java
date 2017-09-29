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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import ncoap.communication.events.client.TokenReleasedEvent;
import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ncoap.message.MessageType.CON;
import static ncoap.message.MessageType.NON;

/**
 * Created by olli on 09.09.15.
 */
public class ClientInboundReliabilityHandler extends AbstractCoapChannelHandler
        implements RemoteServerSocketChangedEvent.Handler, TokenReleasedEvent.Handler{

    /**
     * Minimum delay in milliseconds (1500) between the reception of a confirmable request and an empty ACK
     */
    private static Logger LOG = LoggerFactory.getLogger(ClientInboundReliabilityHandler.class.getName());

    private Multimap<InetSocketAddress, Token> awaitedResponses;
    private ReentrantReadWriteLock lock;


    /**
     * Creates a new instance of
     * {@link ncoap.communication.reliability.inbound.ClientInboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to provide the threads to execute the
     *                 tasks for reliability.
     */
    public ClientInboundReliabilityHandler(ScheduledExecutorService executor) {
        super(executor);
        this.awaitedResponses = HashMultimap.create();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, final InetSocketAddress remoteSocket) {

        LOG.debug("HANDLE INBOUND MESSAGE: {}", coapMessage);

        if (coapMessage instanceof CoapResponse) {
            return handleInboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
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

        if (coapMessage instanceof CoapRequest) {
            addToAwaitedResponses(remoteSocket, coapMessage.getToken());
            LOG.debug("Exchanges: {}", this.awaitedResponses.size());
        }

        return true;
    }


    @Override
    public void handleEvent(RemoteServerSocketChangedEvent event) {
        removeFromAwaitedResponses(event.getPreviousRemoteSocket(), event.getToken());
        addToAwaitedResponses(event.getRemoteSocket(), event.getToken());
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeFromAwaitedResponses(event.getRemoteSocket(), event.getToken());
    }


    private boolean handleInboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

        final int messageType = coapResponse.getMessageType();
        Token token = coapResponse.getToken();

        if (!this.isResponseAwaited(remoteSocket, token)) {
            if (messageType == CON || (messageType == NON && coapResponse.isUpdateNotification())) {
                // response was unexpected (send RST)
                sendReset(coapResponse.getMessageID(), remoteSocket);
            }
            LOG.debug("Received unexpected response from \"{}\" (token: {})", remoteSocket, token);
            return false;
        } else if ((messageType == CON)) {
            // send empty ACK
            sendEmptyACK(coapResponse.getMessageID(), remoteSocket);
        }
        return true;
    }


    private boolean isResponseAwaited(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            return awaitedResponses.get(remoteSocket).contains(token);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private void addToAwaitedResponses(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            this.awaitedResponses.put(remoteSocket, token);
            LOG.debug("Added message exchange with \"{}\" and token {} (Now: {})",
                    new Object[]{remoteSocket, token, this.awaitedResponses.size()});
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private boolean removeFromAwaitedResponses(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            if (!awaitedResponses.get(remoteSocket).contains(token)) {
                return false;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            if (this.awaitedResponses.remove(remoteSocket, token)) {
                LOG.debug("Removed message exchange with \"{}\" and token {} (Remaining: {})",
                        new Object[]{remoteSocket, token, this.awaitedResponses.size()});
                return true;
            } else {
                return false;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }


}
