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

import ncoap.communication.dispatching.Token;
import ncoap.communication.events.server.RemoteClientSocketChangedEvent;
import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>
 * The {@link ncoap.communication.identification.ServerIdentificationHandler}
 * realizes a non-RFC * extension to the CoAP protocol (see:
 * <a href="http://tools.ietf.org/html/draft-kleine-core-coap-endpoint-id-01">CoAP-Endpoint-ID-Draft-01</a>).
 * </p>
 *
 * <p>
 * This extension to the raw protocol deals with the issue if one of the
 * endpoints (client or server) changes its socket (i.e. IP address, port
 * number, or both) during an ongoing conversation (e.g. an observation).</p>
 *
 * @author Oliver Kleine
 */
public class ServerIdentificationHandler extends AbstractIdentificationHandler {

    private static Logger LOG = LoggerFactory.getLogger(ServerIdentificationHandler.class.getName());

    public ServerIdentificationHandler(ScheduledExecutorService executor) {
	super(executor);
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

	if (coapMessage instanceof CoapRequest) {
	    handleInboundCoapRequest((CoapRequest) coapMessage, remoteSocket);
	}
	return true;
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

	if (coapMessage instanceof CoapResponse) {
	    handleOutboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
	}
	return true;
    }

    private void handleOutboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
	byte[] endpointID2 = getFromAssignedToMe(remoteSocket, coapResponse.getToken());
	if (endpointID2 != null) {
	    coapResponse.setEndpointID2(endpointID2);
	}

	if (!(coapResponse.getEndpointID1() == null)) {
	    // set the endpoint ID 1 option with a valid value
	    EndpointID endpointID1 = getFromAssignedByMe(remoteSocket, coapResponse.getToken());
	    if (endpointID1 == null) {
		endpointID1 = getFactory().getNextEndpointID();
		addToAssignedByMe(remoteSocket, coapResponse.getToken(), endpointID1);
	    }
	    coapResponse.setEndpointID1(endpointID1.getBytes());
	}

	if (!coapResponse.isUpdateNotification()) {
	    removeFromAssignedToMe(remoteSocket, coapResponse.getToken());
	    removeFromAssignedByMe(remoteSocket, coapResponse.getToken(), true);
	}
    }

    private void handleInboundCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket) {

	Token token = coapRequest.getToken();

	// handle endpoint ID 2 if any (assigned by me)
	byte[] value = coapRequest.getEndpointID2();
	if (value != null) {
	    EndpointID endpointID2 = new EndpointID(value);
	    InetSocketAddress previousSocket = getFromAssignedByMe(endpointID2, token);

	    if (updateAssignedByMe(endpointID2, token, remoteSocket)) {
		triggerEvent(new RemoteClientSocketChangedEvent(
			remoteSocket, previousSocket, coapRequest.getToken()
		), false);
		continueMessageProcessing(coapRequest, remoteSocket);
	    }
	}

	// handle endpoint ID 1 if present in message (assigned to me)
	byte[] endpointID1 = coapRequest.getEndpointID1();
	if (endpointID1 != null) {
	    addToAssignedToMe(remoteSocket, token, endpointID1);
	}
    }
}
