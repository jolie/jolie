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

package ncoap.application;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import ncoap.communication.codec.CoapMessageDecoder;
import ncoap.communication.codec.CoapMessageEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract base class for pipeline factories for clients, servers and peers.
 *
 * @author Oliver Kleine
 */
public abstract class CoapChannelPipelineFactory {

    private static Logger LOG = LoggerFactory.getLogger(CoapChannelPipelineFactory.class.getName());

    private Set<ChannelHandler> channelHandlers;
    private final EventExecutorGroup executor;

    protected CoapChannelPipelineFactory(EventExecutorGroup executor) {
	this.channelHandlers = new LinkedHashSet<>();
	this.executor = executor;

	addChannelHandler(new CoapMessageEncoder());
	addChannelHandler(new CoapMessageDecoder());
    }

    protected void addChannelHandler(ChannelHandler channelHandler) {
	this.channelHandlers.add(channelHandler);
    }

    public Set<ChannelHandler> getChannelHandlers() {
	return this.channelHandlers;
    }

    public ChannelPipeline getPipeline() throws Exception {
	ChannelPipeline pipeline = ChannelPipeline.class.newInstance();

	for (ChannelHandler handler : this.channelHandlers) {
	    String handlerName = handler.getClass().getSimpleName();
	    pipeline.addLast(executor, handler.getClass().getSimpleName(), handler);
	    LOG.debug("Added Handler to Pipeline: {}.", handlerName);
	}

	return pipeline;
    }
}
