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

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import ncoap.communication.dispatching.Token;
import ncoap.message.CoapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An instances of {@link MessageIDFactory} creates and manages message IDs for outgoing messages. On creation of
 * new message IDs the factory ensures that the same message ID is not used twice for different messages to the
 * same remote CoAP endpoints within {@link #EXCHANGE_LIFETIME} seconds.
 *
 * @author Oliver Kleine
*/
public class MessageIDFactory extends Observable {

    /**
     * The number of seconds (247) a message ID is allocated by the nCoAP framework to avoid duplicate
     * usage of the same message ID in communications with the same remote CoAP endpoints.
     */
    public static final int EXCHANGE_LIFETIME = 247;

    /**
     * The number of different message IDs per remote CoAP endpoint (65536), i.e. there are at most 65536
     * communications with the same endpoints possible within {@link #EXCHANGE_LIFETIME} milliseconds.
     */
    public static final int MODULUS = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Random random;

    private TreeMultimap<InetSocketAddress, Integer> allocations;
    private ReentrantReadWriteLock lock;
    private ScheduledExecutorService executor;


    /**
     * @param executor the {@link ScheduledExecutorService} to provide the thread for operations to
     *                        provide available message IDs
     */
    public MessageIDFactory(ScheduledExecutorService executor) {
        this.executor = executor;
        this.allocations = TreeMultimap.create(Ordering.arbitrary(), Ordering.natural());
        this.lock = new ReentrantReadWriteLock();
        this.random = new Random(System.currentTimeMillis());
    }


    /**
     * Returns a message ID to be used for outgoing {@link de.uzl.itm.ncoap.message.CoapMessage}s and
     * allocates this message ID for {@link #EXCHANGE_LIFETIME} seconds, i.e. the returned message ID will not
     * be returned again within {@link #EXCHANGE_LIFETIME} seconds.
     *
     * If all message IDs available for the given remote endpoint are in use
     * {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} is returned.
     *
     * @param remoteSocket the recipient of the message the returned message ID is supposed to be used for
     *
     * @return the message ID to be used for outgoing messages or
     * {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} if all IDs are in use.
     */
    public int getNextMessageID(final InetSocketAddress remoteSocket, final Token token) {

        try{
            lock.readLock().lock();

            if (this.allocations.get(remoteSocket).size() == MODULUS) {
                log.warn("No more message IDs available for remote endpoint {}.", remoteSocket);
                return CoapMessage.UNDEFINED_MESSAGE_ID;
            }
        }
        finally{
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();

	    final SortedSet<Integer> allocations_1 = this.allocations.get(remoteSocket);
	    if (allocations_1.size() == MODULUS) {
                log.warn("No more message IDs available for remote endpoint {}.", remoteSocket);
                return CoapMessage.UNDEFINED_MESSAGE_ID;
            }

            final int messageID;
	    if (allocations_1.isEmpty()) {
                messageID = this.random.nextInt(MODULUS);
            } else {
		messageID = (allocations_1.last() + 1);
            }
            this.allocations.put(remoteSocket, messageID);

            this.executor.schedule(new Runnable() {
                @Override
                public void run() {
                    releaseMessageID(remoteSocket, messageID, token);
                }
            }, EXCHANGE_LIFETIME, TimeUnit.SECONDS);

            return messageID % MODULUS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void releaseMessageID(InetSocketAddress remoteSocket, int messageID, Token token) {
        try{
            lock.writeLock().lock();
            if (this.allocations.remove(remoteSocket, messageID)) {
                log.info("Released message ID \"{}\" (Remote Socket: \"{}\", Token: {}",
                        new Object[]{(messageID % MODULUS), remoteSocket, token});
            }
            setChanged();
            notifyObservers(new MessageIDRelease(remoteSocket, messageID % MODULUS, token));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void shutdown() {
        try{
            lock.writeLock().lock();
            this.allocations.clear();
        } finally {
            lock.writeLock().lock();
        }
    }

    class MessageIDRelease {

        private InetSocketAddress remoteSocket;
        private int messageID;
        private Token token;

        private MessageIDRelease(InetSocketAddress remoteSocket, int messageID, Token token) {
            this.remoteSocket = remoteSocket;
            this.messageID = messageID;
            this.token = token;
        }

        public InetSocketAddress getRemoteSocket() {
            return remoteSocket;
        }

        public int getMessageID() {
            return messageID;
        }

        public Token getToken() {
            return token;
        }
    }
}
