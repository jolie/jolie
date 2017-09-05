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
package jolie.net.coap.communication.observing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The {@link ClientObservationHandler} deals with
 * running observations. It e.g. ensures that inbound update notifications answered with a RST if the
 * observation was canceled by the {@link de.uzl.itm.ncoap.application.client.CoapClient}.
 *
 * @author Oliver Kleine
 */
public class ClientObservationHandler extends AbstractCoapChannelHandler implements
        TokenReleasedEvent.Handler, RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientObservationHandler.class.getName());

    private Table<InetSocketAddress, Token, ResourceStatusAge> observations;
    private ReentrantReadWriteLock lock;


    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.communication.observing.ClientObservationHandler}
     */
    public ClientObservationHandler(ScheduledExecutorService executor) {
        super(executor);
        this.observations = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, final InetSocketAddress remoteSocket) {

        if (!(coapMessage instanceof CoapResponse)) {
            return true;
        }

        final CoapResponse coapResponse = (CoapResponse) coapMessage;
        Token token = coapResponse.getToken();

        if (coapResponse.isUpdateNotification() && !coapResponse.isErrorResponse()) {
            //Current response is (non-error) update notification and there is a suitable observation
            ResourceStatusAge latestStatusAge = observations.get(remoteSocket, token);

            //Get status age from newly received update notification
            long receivedSequenceNo = coapResponse.getObserve();
            ResourceStatusAge receivedStatusAge = new ResourceStatusAge(receivedSequenceNo, System.currentTimeMillis());

            if (ResourceStatusAge.isReceivedStatusNewer(latestStatusAge, receivedStatusAge)) {
                updateStatusAge(remoteSocket, token, receivedStatusAge);
            } else {
                LOG.warn("Received update notification ({}) is older than latest ({}). IGNORE!",
                        receivedStatusAge, latestStatusAge);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        if (coapMessage instanceof CoapRequest && coapMessage.getObserve() != UintOptionValue.UNDEFINED) {
            Token token = coapMessage.getToken();
            if (coapMessage.getObserve() == 0) {
                LOG.debug("Add observation (remote endpoint: {}, token: {})", remoteSocket, token);
                startObservation(remoteSocket, token);
            } else {
                LOG.debug("Stop observation due to \"observe != 0\" (remote endpoint: {}, token: {})",
                        remoteSocket, token);
                stopObservation(remoteSocket, token);
            }
        }

        return true;
    }


    @Override
    public void handleEvent(TokenReleasedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        stopObservation(remoteSocket, token);
        LOG.debug("Next update notification from \"{}\" with token {} will cause a RST.", remoteSocket, token);
    }


    @Override
    public void handleEvent(RemoteServerSocketChangedEvent event) {
        InetSocketAddress previousSocket = event.getPreviousRemoteSocket();
        Token token = event.getToken();
        try{
            lock.readLock().lock();
            ResourceStatusAge statusAge = this.observations.get(previousSocket, token);
            if (statusAge == null) {
                LOG.info("No observation found for updated socket (token: {}, old socket: {}).", token, previousSocket);
                return;
            }
        } finally {
            lock.readLock().unlock();
        }

        InetSocketAddress remoteSocket = event.getRemoteSocket();
        try{
            lock.writeLock().lock();
            ResourceStatusAge statusAge = this.observations.remove(previousSocket, token);
            if (statusAge == null) {
                LOG.info("No observation found with token {} for updated socket (old: {}, new: {}).",
                        new Object[]{token, previousSocket, remoteSocket});
            } else {
                this.observations.put(remoteSocket, token, statusAge);
                LOG.info("Observation (Token: {}) updated with new remote socket (old: {}, new: {})!",
                        new Object[]{token, previousSocket, remoteSocket});
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


//    @Override
//    public void handleEvent(TransmissionTimeoutEvent event) {
//        stopObservation(event.getRemoteSocket(), event.getToken());
//    }


    private void startObservation(InetSocketAddress remoteSocket, Token token) {
        try{
            this.lock.readLock().lock();
            if (this.observations.contains(remoteSocket, token)) {
                LOG.error("Tried to override existing observation (remote endpoint: {}, token: {}).",
                        remoteSocket, token);
                return;
            }
        }
        finally{
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            if (this.observations.contains(remoteSocket, token)) {
                LOG.error("Tried to override existing observation (remote endpoint: {}, token: {}).",
                        remoteSocket, token);
            }

            else{
                this.observations.put(remoteSocket, token, new ResourceStatusAge(0, 0));
                LOG.info("New observation added (remote endpoint: {}, token: {})", remoteSocket, token);
            }
        }
        finally{
            this.lock.writeLock().unlock();
        }
    }


    private void updateStatusAge(InetSocketAddress remoteSocket, Token token, ResourceStatusAge age) {
        try{
            this.lock.writeLock().lock();
            this.observations.put(remoteSocket, token, age);
            LOG.info("Updated observation (remote endpoint: {}, token: {}): {}",
                    new Object[]{remoteSocket, token, age});
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    private ResourceStatusAge stopObservation(InetSocketAddress remoteSocket, Token token) {
        try{
            this.lock.readLock().lock();
            if (!this.observations.contains(remoteSocket, token)) {
                LOG.debug("No observation found to be stopped (remote endpoint: {}, token: {})", remoteSocket, token);
                return null;
            }
        }
        finally{
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            ResourceStatusAge age = this.observations.remove(remoteSocket, token);
            if (age == null) {
                LOG.warn("No observation found to be stopped (remote endpoint: {}, token: {})", remoteSocket, token);
            }
            else{
                LOG.info("Observation stopped (remote endpoint: {}, token: {})!", remoteSocket, token);
            }
            return age;
        }
        finally{
            this.lock.writeLock().unlock();
        }
    }
}
