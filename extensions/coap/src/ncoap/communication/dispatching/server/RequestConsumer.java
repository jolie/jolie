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

import java.net.InetSocketAddress;

/**
 * Created by olli on 05.10.15.
 */
public interface RequestConsumer {

    /**
     * This method is invoked by the framework on inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s with
     * {@link de.uzl.itm.ncoap.message.MessageCode#PUT} if there is no
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} registered at the path given as
     * {@link de.uzl.itm.ncoap.message.CoapRequest#getUriPath()}.
     *
     * @param responseFuture the {@link com.google.common.util.concurrent.SettableFuture} to be set with a proper
     * {@link de.uzl.itm.ncoap.message.CoapResponse} to indicate whether there was a new
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} created or not.
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be processed
     *
     * @param remoteSocket the {@link java.net.InetSocketAddress} of the {@link de.uzl.itm.ncoap.message.CoapRequest}s origin.
     */
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                            InetSocketAddress remoteSocket) throws Exception;

}
