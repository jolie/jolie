/*
 * The MIT License
 *
 * Copyright 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ncoap.application.server;

import io.netty.util.concurrent.EventExecutorGroup;
import ncoap.application.CoapChannelPipelineFactory;
import ncoap.communication.dispatching.server.NotFoundHandler;
import ncoap.communication.dispatching.server.RequestDispatcher;
import ncoap.communication.identification.ServerIdentificationHandler;
import ncoap.communication.observing.ServerObservationHandler;
import ncoap.communication.reliability.inbound.ServerInboundReliabilityHandler;
import ncoap.communication.reliability.outbound.MessageIDFactory;
import ncoap.communication.reliability.outbound.ServerOutboundReliabilityHandler;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory to provide the {@link ChannelPipeline} for newly created
 * {@link DatagramChannel}s.
 *
 * @author Oliver Kleine
 */
public class CoapServerChannelPipelineFactory extends CoapChannelPipelineFactory {

    /**
     * Creates a new instance of {@link CoapServerChannelPipelineFactory}.
     *
     * @param executor The {@link ScheduledExecutorService} to provide the
     * thread(s) for I/O operations
     * @param notFoundHandler the
     * {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler}
     * to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s targeting
     * unknown
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s.
     */
    public CoapServerChannelPipelineFactory(EventExecutorGroup executor, NotFoundHandler notFoundHandler) {

	super(executor);
	addChannelHandler(new ServerIdentificationHandler(executor));
	addChannelHandler(new ServerOutboundReliabilityHandler(executor, new MessageIDFactory(executor)));
	addChannelHandler(new ServerInboundReliabilityHandler(executor));
	addChannelHandler(new ServerObservationHandler(executor));
	addChannelHandler(new RequestDispatcher(notFoundHandler, executor));
    }
}
