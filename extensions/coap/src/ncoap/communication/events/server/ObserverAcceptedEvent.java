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
package ncoap.communication.events.server;

import ncoap.application.server.resource.ObservableWebresource;
import ncoap.communication.blockwise.BlockSize;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.AbstractMessageExchangeEvent;
import ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;

/**
 * Created by olli on 04.09.15.
 */
public class ObserverAcceptedEvent extends AbstractMessageExchangeEvent {

    private final ObservableWebresource webresource;
    private final long contentFormat;
    private final BlockSize block2Size;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.events.server.ObserverAcceptedEvent}
     *
     * @param remoteSocket the socket of the newly accepted observer
     * @param token the {@link Token} to be used for this observation
     */
    public ObserverAcceptedEvent(InetSocketAddress remoteSocket, Token token,
	    ObservableWebresource webresource, long contentFormat, BlockSize block2Size) {

	super(remoteSocket, token);
	this.webresource = webresource;
	this.contentFormat = contentFormat;
	this.block2Size = block2Size;
    }

    /**
     * Returns the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * that is observed
     *
     * @return the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * that is observed
     */
    public ObservableWebresource getWebresource() {
	return webresource;
    }

    /**
     * Returns the number representing the content format to be used for this
     * observation
     *
     * @see ContentFormat
     *
     * @return the number representing the content format to be used for this
     * observation
     */
    public long getContentFormat() {
	return contentFormat;
    }

    public BlockSize getBlock2Size() {
	return block2Size;
    }

    public interface Handler {

	public void handleEvent(ObserverAcceptedEvent event);
    }
}
