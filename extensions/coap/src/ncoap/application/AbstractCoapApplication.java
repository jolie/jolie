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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;

import ncoap.communication.AbstractCoapChannelHandler;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.*;

/**
 * The abstract base class for all kinds of CoAP applications, i.e. clients,
 * servers, and endpoints (combining client and server functionality)
 *
 * @author Oliver Kleine
 */
public abstract class AbstractCoapApplication {

    /**
     * {@value #RECEIVE_BUFFER_SIZE}
     */
    public static final int RECEIVE_BUFFER_SIZE = 65536;

    /**
     * {@value #NOT_BOUND}
     */
    public static final int NOT_BOUND = -1;

    private ScheduledThreadPoolExecutor executor;
    private NioDatagramChannel channel;
    private String applicationName;

    /**
     * Creates a new instance of {@link AbstractCoapApplication}.
     *
     * @param applicationName the given name of this application (for logging
     * only)
     */
    protected AbstractCoapApplication(String applicationName) {

	this.applicationName = applicationName;

	ThreadFactory threadFactory
		= new ThreadFactoryBuilder().setNameFormat(applicationName + " I/O Worker #%d").build();

	/*
	ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer() {
	    @Override
	    public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
		return null;
	    }
	});
	 */
	// determine number of I/O threads and create thread pool executor of that size
	int ioThreads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 4);
	this.executor = new ScheduledThreadPoolExecutor(ioThreads, threadFactory);
//        this.executor = new SynchronizedExecutor(ioThreads, threadFactory);
    }

    /**
     * Returns the local port number the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClient} is bound to or
     * {@link #NOT_BOUND} if the application has not yet been started.
     *
     * @return the local port number the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClient} is bound to or
     * {@link #NOT_BOUND} if the application has not yet been started.
     */
    public int getPort() {
	return ((InetSocketAddress) this.channel.localAddress()).getPort();
    }

    /**
     * Returns the {@link java.util.concurrent.ScheduledExecutorService} which
     * is used by this
     * {@link de.uzl.itm.ncoap.application.AbstractCoapApplication} to handle
     * tasks, e.g. write and receive messages. The returned
     * {@link java.util.concurrent.ScheduledExecutorService} may also be used by
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s to
     * handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s
     *
     * @return the {@link java.util.concurrent.ScheduledExecutorService} which
     * is used by this
     * {@link de.uzl.itm.ncoap.application.AbstractCoapApplication} to handle
     * tasks, e.g. write and receive messages.
     */
    public ScheduledExecutorService getExecutor() {
	return this.executor;
    }

    /**
     * Returns the {@link DatagramChannel} instance this application uses to
     * communicate with other endpoints
     *
     * @return the {@link DatagramChannel} instance this application uses to
     * communicate with other endpoints
     */
    public NioDatagramChannel getChannel() {
	return this.channel;
    }

    /**
     * Returns the name this application was given
     *
     * @return the name this application was given
     */
    public String getApplicationName() {
	return applicationName;
    }

//    private class SynchronizedExecutor extends ScheduledThreadPoolExecutor {
//
//        public SynchronizedExecutor(int corePoolSize, ThreadFactory threadFactory) {
//            super(corePoolSize, threadFactory);
//        }
//
//        @Override
//        public synchronized ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit timeUnit) {
//            return super.schedule(task, delay, timeUnit);
//        }
//    }
}
