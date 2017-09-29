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
package ncoap.application.server;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import ncoap.application.AbstractCoapApplication;
import ncoap.application.server.resource.Webresource;
import ncoap.communication.blockwise.BlockSize;
import ncoap.communication.dispatching.server.NotFoundHandler;
import ncoap.communication.dispatching.server.RequestDispatcher;
import ncoap.message.CoapRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * An instance of {@link CoapServer} is the component to enable instances of
 * {@link Webresource} to communicate with the outside world, i.e. the Internet.
 * Once a {@link CoapServer} was instanciated one can register
 * {@link Webresource} instances and by this means make them available at their
 * specified path.
 *
 * Each instance of {@link CoapServer} is automatically bound to a local port to
 * listen for inbound requests.
 *
 * @author Oliver Kleine
 */
public class CoapServer extends AbstractCoapApplication {

    /**
     * 5683
     */
    public static final int DEFAULT_PORT_NUMBER = 5683;

    /**
     * 'nCoAP Server'
     */
    public static final String DEFAULT_NAME = "nCoAP Server";

    private static Logger LOG = LoggerFactory.getLogger(CoapServer.class.getName());

    private RequestDispatcher requestDispatcher;

    /**
     * Creates a new instance of {@link CoapServer} with default parameters,
     * i.e.
     * <ul>
     * <li>{@link CoapServer#DEFAULT_NAME} as name,</li>
     * <li>{@link NotFoundHandler#getDefault()} as {@link NotFoundHandler},</li>
     * <li>{@link CoapServer#DEFAULT_PORT_NUMBER} as port number (listening at
     * all local IP-addresses), and</li>
     * <li>{@link BlockSize#UNBOUND} for maximum size of both, inbound requests
     * and outbound responses.</li>
     * </ul>
     */
    public CoapServer() {
	this(DEFAULT_NAME, NotFoundHandler.getDefault(), getDefaultSocket(), BlockSize.UNBOUND, BlockSize.UNBOUND);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param serverPort the port number for the server to listen at (holds for
     * all IP addresses of the server)
     */
    public CoapServer(int serverPort) {
	this(DEFAULT_NAME, NotFoundHandler.getDefault(), getSocket(serverPort), BlockSize.UNBOUND, BlockSize.UNBOUND);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param serverSocket the socket address for the server to listen at
     */
    public CoapServer(InetSocketAddress serverSocket) {
	this(DEFAULT_NAME, NotFoundHandler.getDefault(), serverSocket, BlockSize.UNBOUND, BlockSize.UNBOUND);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param notFoundHandler to handle inbound {@link CoapRequest}s for unknown
     * resources
     */
    public CoapServer(NotFoundHandler notFoundHandler) {
	this(DEFAULT_NAME, notFoundHandler, getDefaultSocket(), BlockSize.UNBOUND, BlockSize.UNBOUND);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param notFoundHandler to handle inbound {@link CoapRequest}s for unknown
     * resources
     * @param serverPort the port number for the server to listen at (holds for
     * all IP addresses of the server)
     */
    public CoapServer(NotFoundHandler notFoundHandler, int serverPort) {
	this(DEFAULT_NAME, notFoundHandler, getSocket(serverPort), BlockSize.UNBOUND, BlockSize.UNBOUND);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param notFoundHandler to handle inbound {@link CoapRequest}s for unknown
     * resources
     * @param maxBlock1Size the maximum blocksize for inbound requests
     * @param maxBlock2Size the maximum blocksize for outbound responses
     */
    public CoapServer(NotFoundHandler notFoundHandler, BlockSize maxBlock1Size, BlockSize maxBlock2Size) {
	this(DEFAULT_NAME, notFoundHandler, getDefaultSocket(), maxBlock1Size, maxBlock2Size);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}. See {@link #CoapServer()}
     * for default values of unspecified parameters).</p>
     *
     * @param maxBlock1Size the maximum blocksize for inbound requests
     * @param maxBlock2Size the maximum blocksize for outbound responses
     */
    public CoapServer(BlockSize maxBlock1Size, BlockSize maxBlock2Size) {
	this(DEFAULT_NAME, NotFoundHandler.getDefault(), getDefaultSocket(), maxBlock1Size, maxBlock2Size);
    }

    /**
     * Creates a new instance of {@link CoapServer}
     *
     * @param name the name of this {@link CoapServer} (for logging only)
     * @param notFoundHandler the {@link NotFoundHandler} to handle inbound
     * requests for unknown resources
     * @param serverPort the port number of the socket to receive messages (all
     * local IP-addresses)
     * @param maxBlock1Size the maximum blocksize for inbound requests
     * @param maxBlock2Size the maximum blocksize for outbound responses
     */
    public CoapServer(String name, NotFoundHandler notFoundHandler, int serverPort,
	    BlockSize maxBlock1Size, BlockSize maxBlock2Size) {

	this(name, notFoundHandler, getSocket(serverPort), maxBlock1Size, maxBlock2Size);
    }

    /**
     * <p>
     * Creates a new instance of {@link CoapServer}</p>
     *
     * @param name the name of this {@link CoapServer} (for logging only)
     * @param notFoundHandler the {@link NotFoundHandler} to handle inbound
     * requests for unknown resources
     * @param serverSocket the socket address for the server to listen at
     * @param maxBlock1Size the maximum blocksize for inbound requests
     * @param maxBlock2Size the maximum blocksize for outbound responses
     */
    public CoapServer(String name, NotFoundHandler notFoundHandler, InetSocketAddress serverSocket,
	    BlockSize maxBlock1Size, BlockSize maxBlock2Size) {

	super(name);

//        CoapServerChannelPipelineFactory pipelineFactory =
	//               new CoapServerChannelPipelineFactory(this.getExecutor(), notFoundHandler, maxBlock1Size, maxBlock2Size);
	//startApplication(pipelineFactory, serverSocket);
	// set the request dispatcher and register .well-known/core
	this.requestDispatcher = getChannel().pipeline().get(RequestDispatcher.class);
	this.requestDispatcher.registerWellKnownCoreResource();
    }

    /**
     * <p>
     * Returns the socket address of a socket including the given port and all
     * local IP addresses.</p>
     *
     * <p>
     * Note: This method is intended as a shortcut for internal use.</p>
     *
     * @return the socket address of a socket including the given port and all
     * local IP addresses.
     */
    public static InetSocketAddress getSocket(int portNumber) {
	return new InetSocketAddress(portNumber);
    }

    /**
     * <p>
     * Returns the default socket address for CoAP servers, i.e. all local IP
     * addresses and port number {@link CoapServer#DEFAULT_PORT_NUMBER}.</p>
     *
     * <p>
     * Note: This method is intended as a shortcut for internal use.</p>
     *
     * @return the default socket address for CoAP servers, i.e. all local IP
     * addresses and port number {@link CoapServer#DEFAULT_PORT_NUMBER}.
     */
    public static InetSocketAddress getDefaultSocket() {
	return getSocket(DEFAULT_PORT_NUMBER);
    }

    /**
     * Registers a new {@link Webresource} at this {@link CoapServer}.
     *
     * @param webresource the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * to be registered
     *
     * @throws java.lang.IllegalArgumentException if there was already a
     * {@link Webresource} registered with the same path
     */
    public void registerWebresource(Webresource webresource) throws IllegalArgumentException {
	this.getRequestDispatcher().registerWebresource(webresource);
    }

    private RequestDispatcher getRequestDispatcher() {
	return getChannel().pipeline().get(RequestDispatcher.class);
    }

    /**
     * <p>
     * Gracefully shuts down the {@link Webresource} that was registered at the
     * given path (if any).</p>
     *
     * That includes e.g. notifications to all observers in case we deal with a
     * {@link de.uzl.itm.ncoap.application.server.resource.ObservableWebresource}
     *
     * @param uriPath the path of the {@link Webresource} to be shut down.
     */
    public void shutdownWebresource(String uriPath) {
	this.getRequestDispatcher().shutdownWebresource(uriPath);
    }

    /**
     * Gracefully shuts down the server by sequentially shutting down all its
     * components, i.e. the registered
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} to write and
     * receive messages.
     */
    public ListenableFuture<Void> shutdown() {
	LOG.warn("Shutdown server...");
	final SettableFuture<Void> shutdownFuture = SettableFuture.create();
	Futures.addCallback(this.requestDispatcher.shutdown(), new FutureCallback<Void>() {
	    @Override
	    public void onSuccess(Void aVoid) {
		ChannelFuture channelClosedFuture = getChannel().close();

		//Await the closure and let the factory release its external resource to finalize the shutdown
		channelClosedFuture.addListener(new ChannelFutureListener() {
		    @Override
		    public void operationComplete(ChannelFuture future) throws Exception {
			LOG.warn("Server channel closed. Release external resources...");

			//getChannel().getFactory().releaseExternalResources();
		    }
		});

		channelClosedFuture.awaitUninterruptibly().addListener(new ChannelFutureListener() {
		    @Override
		    public void operationComplete(ChannelFuture future) throws Exception {
			LOG.warn("Server shutdown completed!");
			shutdownFuture.set(null);
		    }
		});
	    }

	    @Override
	    public void onFailure(Throwable throwable) {
		onSuccess(null);
	    }
	});
	return shutdownFuture;
    }
}
