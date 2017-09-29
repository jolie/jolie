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
package ncoap.communication.identification;

import com.google.common.collect.HashBasedTable;

import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.communication.dispatching.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract base class for {@link ClientIdentificationHandler} and
 * {@link ServerIdentificationHandler}.
 *
 * @author Oliver Kleine
 */
public abstract class AbstractIdentificationHandler extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(AbstractIdentificationHandler.class.getName());

    // the endpoint IDs assigned to (!) other endpoints
    private HashBasedTable<EndpointID, Token, InetSocketAddress> assignedByMe1;
    private HashBasedTable<InetSocketAddress, Token, EndpointID> assignedByMe2;

    // the endpoint IDs assigned by (!) other endpoints
    private HashBasedTable<InetSocketAddress, Token, byte[]> assignedToMe;

    private EndpointIDFactory factory;
    private ReentrantReadWriteLock lock;

    protected AbstractIdentificationHandler(ScheduledExecutorService executor) {
	super(executor);
	this.assignedByMe1 = HashBasedTable.create();
	this.assignedByMe2 = HashBasedTable.create();
	this.assignedToMe = HashBasedTable.create();

	this.factory = new EndpointIDFactory();
	this.lock = new ReentrantReadWriteLock();
    }

    protected EndpointIDFactory getFactory() {
	return this.factory;
    }

    protected void addToAssignedToMe(InetSocketAddress remoteSocket, Token token, byte[] endpointID) {
	try {
	    lock.writeLock().lock();
	    this.assignedToMe.put(remoteSocket, token, endpointID);
	    LOG.info("New ID to identify myself at remote endpoint {}: {}", remoteSocket, new EndpointID(endpointID));
	} finally {
	    lock.writeLock().unlock();
	}
    }

    protected byte[] getFromAssignedToMe(InetSocketAddress remoteSocket, Token token) {
	try {
	    lock.readLock().lock();
	    return this.assignedToMe.get(remoteSocket, token);
	} finally {
	    lock.readLock().unlock();
	}
    }

    protected void removeFromAssignedToMe(InetSocketAddress remoteSocket, Token token) {
	try {
	    lock.readLock().lock();
	    if (!this.assignedToMe.contains(remoteSocket, token)) {
		return;
	    }
	} finally {
	    lock.readLock().unlock();
	}
	try {
	    lock.writeLock().lock();
	    byte[] endpointID = this.assignedToMe.remove(remoteSocket, token);
	    LOG.info("Removed ID to identify myself at remote endpoint {}: {}", remoteSocket, new EndpointID(endpointID));
	} finally {
	    lock.writeLock().unlock();
	}
    }

    protected InetSocketAddress getFromAssignedByMe(EndpointID endpointID, Token token) {
	try {
	    lock.readLock().lock();
	    return this.assignedByMe1.get(endpointID, token);
	} finally {
	    lock.readLock().unlock();
	}
    }

    protected EndpointID getFromAssignedByMe(InetSocketAddress remoteSocket, Token token) {
	try {
	    lock.readLock().lock();
	    return this.assignedByMe2.get(remoteSocket, token);
	} finally {
	    lock.readLock().unlock();
	}
    }

    protected void removeFromAssignedByMe(InetSocketAddress remoteSocket, Token token, boolean releaseEndpointID) {
	try {
	    lock.readLock().lock();
	    if (!this.assignedByMe2.contains(remoteSocket, token)) {
		return;
	    }
	} finally {
	    lock.readLock().unlock();
	}
	try {
	    lock.writeLock().lock();
	    EndpointID endpointID = this.assignedByMe2.remove(remoteSocket, token);
	    if (endpointID != null) {
		this.assignedByMe1.remove(endpointID, token);
		if (releaseEndpointID) {
		    this.factory.passBackEndpointID(endpointID);
		}
		LOG.info("Removed ID to identify remote host {}: {}", remoteSocket, endpointID);
	    }
	} finally {
	    lock.writeLock().unlock();
	}
    }

    protected void addToAssignedByMe(InetSocketAddress remoteSocket, Token token, EndpointID endpointID) {
	try {
	    this.lock.writeLock().lock();
	    this.assignedByMe1.put(endpointID, token, remoteSocket);
	    this.assignedByMe2.put(remoteSocket, token, endpointID);
	    LOG.info("Added ID to identify remote host {}: {}", remoteSocket, endpointID);
	} finally {
	    this.lock.writeLock().unlock();
	}
    }

    protected boolean updateAssignedByMe(EndpointID endpointID, Token token, InetSocketAddress remoteSocket) {

	try {
	    this.lock.readLock().lock();
	    InetSocketAddress previousRemoteSocket = getFromAssignedByMe(endpointID, token);
	    if (remoteSocket.equals(previousRemoteSocket)) {
		return false;
	    }
	} finally {
	    this.lock.readLock().unlock();
	}

	try {
	    lock.writeLock().lock();
	    InetSocketAddress previousRemoteSocket = getFromAssignedByMe(endpointID, token);
	    if (remoteSocket.equals(previousRemoteSocket)) {
		return false;
	    }

	    removeFromAssignedByMe(previousRemoteSocket, token, false);
	    addToAssignedByMe(remoteSocket, token, endpointID);

	    LOG.info("Socket for remote Endpoint (EID: {}) updated: {} (Token: {}).",
		    new Object[]{endpointID, remoteSocket, token});

	    return true;
	} finally {
	    lock.writeLock().unlock();
	}
    }
}
