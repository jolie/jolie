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

import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.message.CoapMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observer;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;


/**
  * This is the handler to deal with reliability message transfers (e.g. retransmissions of confirmable messages) for
  * CoAP Endpoints.
  *
  * @author Oliver Kleine
 */
public abstract class AbstractOutboundReliabilityHandler extends AbstractCoapChannelHandler implements Observer {

    private static Logger LOG = LoggerFactory.getLogger(AbstractOutboundReliabilityHandler.class.getName());

    public static final int MAX_RETRANSMISSIONS = 4;

    /**
     * The minimum number of milliseconds (2000) to wait for the first retransmit of an outgoing
     * {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType#CON}
     */
    public static final int ACK_TIMEOUT_MILLIS = 2000;

    /**
     * The factor (1.5) to be multiplied with {@link #ACK_TIMEOUT_MILLIS} to get the maximum number of milliseconds
     * (3000) to wait for the first retransmit of an outgoing {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType#CON}
     */
    public static final double ACK_RANDOM_FACTOR = 1.5;

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    /**
     * Provides a random(!) delay for the given retransmission number according to the CoAP specification
     * @param retransmission the retransmission number (e.g. 2 for the 2nd retransmission)
     * @return a random(!) delay for the given retransmission number according to the CoAP specification
     */
    public static long provideRetransmissionDelay(int retransmission) {
        return (long)(Math.pow(2, retransmission - 1) * ACK_TIMEOUT_MILLIS *
                (1 + RANDOM.nextDouble() * (ACK_RANDOM_FACTOR - 1)));
    }

    public static long[] provideTransmissionDelays() {
        long[] delays = new long[5];
        delays[0] = 0;
        for (int i = 1; i < 5; i++) {
            delays[i] = delays[i - 1] + provideRetransmissionDelay(i);
        }
        return delays;
    }

    private MessageIDFactory messageIDFactory;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.reliability.outbound.AbstractOutboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to process the tasks to ensure
     *                 reliable message transfer
     */
    public AbstractOutboundReliabilityHandler(ScheduledExecutorService executor, MessageIDFactory factory) {
        super(executor);
        this.messageIDFactory = factory;
        this.messageIDFactory.addObserver(this);
    }


    protected int assignMessageID(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        int messageID = this.messageIDFactory.getNextMessageID(remoteSocket, coapMessage.getToken());

        if (!(messageID == CoapMessage.UNDEFINED_MESSAGE_ID)) {
            coapMessage.setMessageID(messageID);
            LOG.debug("Message ID set to {}.", messageID);

        }
        return messageID;
    }
}
