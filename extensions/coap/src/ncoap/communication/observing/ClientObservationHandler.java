package ncoap.communication.observing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import ncoap.communication.events.client.TokenReleasedEvent;

import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;
import ncoap.message.options.UintOptionValue;

/**
 * The {@link ClientObservationHandler} deals with running observations. It e.g.
 * ensures that inbound update notifications answered with a RST if the
 * observation was canceled by the
 * {@link de.uzl.itm.ncoap.application.client.CoapClient}.
 *
 * @author Oliver Kleine
 */
public class ClientObservationHandler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler, RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientObservationHandler.class.getName());

    private Table<InetSocketAddress, Token, ResourceStatusAge> observations;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of
     * {@link de.uzl.itm.ncoap.communication.observing.ClientObservationHandler}
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
	try {
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
	try {
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
	try {
	    this.lock.readLock().lock();
	    if (this.observations.contains(remoteSocket, token)) {
		LOG.error("Tried to override existing observation (remote endpoint: {}, token: {}).",
			remoteSocket, token);
		return;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    if (this.observations.contains(remoteSocket, token)) {
		LOG.error("Tried to override existing observation (remote endpoint: {}, token: {}).",
			remoteSocket, token);
	    } else {
		this.observations.put(remoteSocket, token, new ResourceStatusAge(0, 0));
		LOG.info("New observation added (remote endpoint: {}, token: {})", remoteSocket, token);
	    }
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private void updateStatusAge(InetSocketAddress remoteSocket, Token token, ResourceStatusAge age) {
	try {
	    this.lock.writeLock().lock();
	    this.observations.put(remoteSocket, token, age);
	    LOG.info("Updated observation (remote endpoint: {}, token: {}): {}",
		    new Object[]{remoteSocket, token, age});
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    private ResourceStatusAge stopObservation(InetSocketAddress remoteSocket, Token token) {
	try {
	    this.lock.readLock().lock();
	    if (!this.observations.contains(remoteSocket, token)) {
		LOG.debug("No observation found to be stopped (remote endpoint: {}, token: {})", remoteSocket, token);
		return null;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    this.lock.writeLock().lock();
	    ResourceStatusAge age = this.observations.remove(remoteSocket, token);
	    if (age == null) {
		LOG.warn("No observation found to be stopped (remote endpoint: {}, token: {})", remoteSocket, token);
	    } else {
		LOG.info("Observation stopped (remote endpoint: {}, token: {})!", remoteSocket, token);
	    }
	    return age;
	} finally {
	    this.lock.writeLock().unlock();
	}
    }
}
