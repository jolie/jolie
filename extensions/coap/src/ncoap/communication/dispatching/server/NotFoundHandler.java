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
package ncoap.communication.dispatching.server;

import com.google.common.util.concurrent.SettableFuture;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;
import ncoap.message.options.ContentFormat;
import ncoap.message.CoapMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static ncoap.message.MessageCode.NOT_FOUND_404;

/**
 * <p>
 * Instances of {@link NotFoundHandler} are invoked to handle inbound
 * {@link de.uzl.itm.ncoap.message.CoapRequest}s that targets a not (yet?)
 * existing {@link de.uzl.itm.ncoap.application.server.resource.Webresource}.
 * Instances may e.g. create and register or update
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s upon
 * reception of a POST or PUT request.</p>
 *
 * <p>
 * The framework calls the method
 * {@link #processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)}
 * for inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s if the addressed
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} does NOT
 * exist.</p>
 *
 * @author Oliver Kleine
 */
public abstract class NotFoundHandler implements RequestConsumer {

    private RequestDispatcher requestDispatcher;

    private static Logger log = LoggerFactory.getLogger(NotFoundHandler.class.getName());

    /**
     * This method is invoked by the framework to set the
     * {@link RequestDispatcher} that is supposed to be used to register newly
     * created {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * instances.
     *
     * @param requestDispatcher the {@link RequestDispatcher} that is supposed
     * to be used to register newly created
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * instances.
     */
    public final void setRequestDispatcher(RequestDispatcher requestDispatcher) {
	this.requestDispatcher = requestDispatcher;
    }

    /**
     * Returns the {@link RequestDispatcher} for this CoAP server. The
     * {@link RequestDispatcher} instance can be e.g. used to register new
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s using {@link RequestDispatcher
     * #registerWebresource(Webservice)}.
     *
     * @return the {@link RequestDispatcher} for this CoAP server.
     */
    protected RequestDispatcher getRequestDispatcher() {
	return this.requestDispatcher;
    }

    /**
     * Returns the default implementation of {@link NotFoundHandler}. The
     * default {@link NotFoundHandler} does not create new instances or updates
     * or deletes existing instances of
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} but sets
     * the given {@link com.google.common.util.concurrent.SettableFuture} with a
     * {@link de.uzl.itm.ncoap.message.CoapResponse} with
     * {@link de.uzl.itm.ncoap.message.MessageCode#NOT_FOUND_404}.
     *
     * @return a new default {@link NotFoundHandler} instance
     */
    public static NotFoundHandler getDefault() {

	return new NotFoundHandler() {

	    private String message = "Webservice \"%s\" not found.";

	    @Override
	    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
		    InetSocketAddress remoteSocket) {
		try {
		    CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), NOT_FOUND_404);
		    String content = String.format(message, coapRequest.getUriPath());
		    coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
		    responseFuture.set(coapResponse);
		} catch (IllegalArgumentException e) {
		    log.error("This should never happen.", e);
		    responseFuture.setException(e);
		}
	    }
	};
    }
}
