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

import com.google.common.collect.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.*;
import ncoap.message.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This is the handler to deal with message transmissions (e.g. transmissions of confirmable messages)
 * for CoAP Clients.
 *
 * @author Oliver Kleine
 */
public class ClientOutboundReliabilityHandler extends AbstractOutboundReliabilityHandler {

    private static Logger LOG = LoggerFactory.getLogger(ClientOutboundReliabilityHandler.class.getName());

    private Table<InetSocketAddress, Integer, TransmissionTask[]> transmissions;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.reliability.outbound.ClientOutboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to process the tasks to ensure
     *                 reliable message transfer
     */
    public ClientOutboundReliabilityHandler(ScheduledExecutorService executor, MessageIDFactory factory) {
        super(executor, factory);
        this.transmissions = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest || coapMessage.isPing()) {
            handleOutboundCoapMessage2(coapMessage, remoteSocket);
            return true;
        } else {
            return true;
        }
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse) {
            return handleInboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
        } else if (coapMessage.getMessageCode() == MessageCode.EMPTY) {
            return handleInboundEmptyMessage(coapMessage, remoteSocket);
        } else {
            return true;
        }
    }


    private void handleOutboundCoapMessage2(CoapMessage coapRequest, InetSocketAddress remoteSocket) {
        LOG.debug("HANDLE OUTBOUND MESSAGE: {}", coapRequest);

        int messageID = assignMessageID(coapRequest, remoteSocket);
        Token token = coapRequest.getToken();
        if (messageID == CoapMessage.UNDEFINED_MESSAGE_ID) {
            LOG.info("No message ID available for \"{}\" (ID pool exhausted).", remoteSocket);
            triggerEvent(new NoMessageIDAvailableEvent(remoteSocket, token), false);
        } else {
            LOG.info("Set message ID to {}", messageID);
            triggerEvent(new MessageIDAssignedEvent(remoteSocket, messageID, token), false);

            scheduleTransmissions(coapRequest, remoteSocket);
        }
    }


    private boolean handleInboundEmptyMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        int messageType = coapMessage.getMessageType();
        int messageID = coapMessage.getMessageID();

        if (messageType == MessageType.CON) {
            // incoming PINGs are handled by the inbound reliability handler
            return true;
        } else {
            Token token = stopRetransmissions(remoteSocket, messageID);
            if (token == null) {
                return true;
            } else if (messageType == MessageType.ACK) {
                LOG.info("Received empty ACK from \"{}\" for token {} (Message ID: {}).",
                    new Object[]{remoteSocket, messageID, token});
                triggerEvent(new EmptyAckReceivedEvent(remoteSocket, messageID, token), false);
                return false;
            } else if (messageType == MessageType.RST) {
                LOG.info("Received RST from \"{}\" for token {} (Message ID: {}).",
                        new Object[]{remoteSocket, messageID, token});
                triggerEvent(new ResetReceivedEvent(remoteSocket, messageID, token), false);
                return false;
            } else {
                LOG.error("Could not handle empty message from \"{}\": {}", remoteSocket, coapMessage);
            }
            return false;
        }
    }


    private boolean handleInboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

        int messageType = coapResponse.getMessageType();

        if (messageType == MessageType.ACK) {
            int messageID = coapResponse.getMessageID();
            if (stopRetransmissions(remoteSocket, messageID) != null) {
                return true;
            } else {
                LOG.warn("Received ACK from \"{}\" for unknown message ID {}", remoteSocket, messageID);
                return false;
            }
        } else {
            return true;
        }
    }


    private Token stopRetransmissions(InetSocketAddress remoteSocket, int messageID) {
        try {
            this.lock.writeLock().lock();
            TransmissionTask[] tasks = this.transmissions.remove(remoteSocket, messageID);
            if (tasks == null) {
                return null;
            } else {
                for (int i = 0; i < tasks.length; i++) {
                    if (tasks[i].cancel()) {
                        LOG.debug("Cancelled transmission #{} (Remote Socket: {}, Message ID: {})",
                                new Object[]{i + 1, remoteSocket, messageID});
                    } else {
                        LOG.debug("Could not cancel transmission #{} (Remote Socket: {}, Message ID: {})",
                                new Object[]{i + 1, remoteSocket, messageID});
                    }
                }
                return tasks[0].getToken();
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void scheduleTransmissions(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        int messageType = coapMessage.getMessageType();
        TransmissionTask[] tasks;
        if(messageType == MessageType.CON) {
            tasks = new TransmissionTask[5];
            long[] delays = provideTransmissionDelays();
            for (int i = 0; i < 5; i++) {
                tasks[i] = new TransmissionTask(coapMessage, remoteSocket, i);
                if(i > 0) {
                    tasks[i].setFuture(this.scheduleTask(tasks[i], delays[i], TimeUnit.MILLISECONDS));
                    LOG.debug("Scheduled transmission #{} with delay {} ms (Remote Socket: {}, message ID: {}).",
                            new Object[]{i + 1, delays[i], remoteSocket, coapMessage.getMessageID()}
                    );
                }
            }
        } else {
            tasks = new TransmissionTask[1];
            tasks[0] = new TransmissionTask(coapMessage, remoteSocket, 0);
            //tasks[0].setFuture(this.scheduleTask(tasks[0], 0, TimeUnit.MILLISECONDS));
        }

        try {
            this.lock.writeLock().lock();
            this.transmissions.put(remoteSocket, coapMessage.getMessageID(), tasks);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * This method is called by the {@link MessageIDFactory} when a message ID was released
     *
     * @param factory the {@link MessageIDFactory} that released the message ID
     * @param releasedID representaton of the message ID that was released
     */
    @Override
    public void update(Observable factory, Object releasedID) {
        if (releasedID instanceof MessageIDFactory.MessageIDRelease) {
            InetSocketAddress remoteSocket = ((MessageIDFactory.MessageIDRelease) releasedID).getRemoteSocket();
            int messageID = ((MessageIDFactory.MessageIDRelease) releasedID).getMessageID();
            Token token = ((MessageIDFactory.MessageIDRelease) releasedID).getToken();

            if (stopRetransmissions(remoteSocket, messageID) != null) {
                // there was an ongoing outbound transfer (i.e. CON with no ACK or NON with no response)
                LOG.warn("Transmission timed out (remote socket: \"{}\", token: {}, message ID: {})",
                        new Object[]{remoteSocket, token, messageID});
                triggerEvent(new TransmissionTimeoutEvent(remoteSocket, messageID, token), true);
            } else {
                triggerEvent(new MessageIDReleasedEvent(remoteSocket, messageID, token), true);
                LOG.info("Message ID was released (remote socket: \"{}\", token: {}, message ID: {})");
            }
        } else {
            LOG.error("This should never happen...");
        }
    }

    private class TransmissionTask implements Runnable {

        private CoapMessage coapMessage;
        private InetSocketAddress remoteSocket;
        private int transmissionNumber;
        private ScheduledFuture future;

        public TransmissionTask(CoapMessage coapMessage, InetSocketAddress remoteSocket, int transmissionNumber) {
            this.coapMessage = coapMessage;
            this.remoteSocket = remoteSocket;
            this.transmissionNumber = transmissionNumber;
        }

        @Override
        public void run() {

            ChannelFuture channelFuture = sendCoapMessage(coapMessage, remoteSocket);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    int messageID = coapMessage.getMessageID();
                    Token token = coapMessage.getToken();
                    if (future.isSuccess()) {
                        triggerEvent(new MessageRetransmittedEvent(remoteSocket, messageID, token), false);
                        LOG.debug("Finished transmission #{}: {}", transmissionNumber, coapMessage);
                    } else {
			String desc = "Transmission failed (\"" + future.cause().getMessage() + "\"";
                        triggerEvent(new MiscellaneousErrorEvent(remoteSocket, messageID, token, desc), false);
                    }
                }
            });
        }

        public void setFuture(ScheduledFuture future) {
            this.future = future;
        }

        public Token getToken() {
            return this.coapMessage.getToken();
        }

        public boolean cancel() {
            return this.future != null && this.future.cancel(true);
        }

    }
}
