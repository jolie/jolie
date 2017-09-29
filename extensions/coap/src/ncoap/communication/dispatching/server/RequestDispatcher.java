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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import ncoap.application.linkformat.LinkValue;
import ncoap.application.linkformat.LinkValueList;
import ncoap.application.server.resource.ObservableWebresource;
import ncoap.application.server.resource.Webresource;
import ncoap.application.server.resource.WellKnownCoreResource;
import ncoap.communication.AbstractCoapChannelHandler;
import ncoap.communication.blockwise.BlockSize;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.server.ObserverAcceptedEvent;
import ncoap.communication.observing.ServerObservationHandler;
import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;
import ncoap.message.options.ContentFormat;
import ncoap.message.options.Option;
import ncoap.message.options.UintOptionValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static ncoap.message.MessageCode.INTERNAL_SERVER_ERROR_500;
import static ncoap.message.MessageCode.PRECONDITION_FAILED_412;

/**
 * The {@link RequestDispatcher} is the topmost {@link ChannelHandler} of the
 * {@link ChannelPipeline} returned by
 * {@link de.uzl.itm.ncoap.application.server.CoapServerChannelPipelineFactory}.
 * It is responsible to dispatch inbound
 * {@link de.uzl.itm.ncoap.message.CoapRequest}s, i.e.
 *
 * <ul>
 * <li>invoke the method
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource#processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)}
 * of the addressed
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance (if
 * it exists) or</li>
 * <li>invoke the method
 * {@link NotFoundHandler#processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)}
 * if the inbound {@link de.uzl.itm.ncoap.message.CoapRequest} addresses a
 * service that does not (yet) exist.
 * </li>
 * </ul>
 *
 * Upon invocation of the method it awaits a proper
 * {@link de.uzl.itm.ncoap.message.CoapResponse} and sends that response
 * downstream, i.e. in the direction of the local socket, i.e. to the client
 * that sent the {@link de.uzl.itm.ncoap.message.CoapRequest}.
 *
 * However, the {@link RequestDispatcher} is aware of all registered
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instances
 * and is thus to be used to register new
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instances,
 * e.g. while processing an inbound {@link de.uzl.itm.ncoap.message.CoapRequest}
 * with {@link de.uzl.itm.ncoap.message.MessageCode#POST}. That is why all
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instances
 * can reference their {@link RequestDispatcher} via
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource#getRequestDispatcher()}.
 *
 * Last but not least it checks whether the
 * {@link de.uzl.itm.ncoap.message.options.Option#IF_NONE_MATCH} is set on
 * inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s and sends a
 * {@link de.uzl.itm.ncoap.message.CoapResponse} with
 * {@link de.uzl.itm.ncoap.message.MessageCode#PRECONDITION_FAILED_412} if the
 * option was set but the addressed
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} already
 * exists.
 *
 * @author Oliver Kleine
 */
public class RequestDispatcher extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(RequestDispatcher.class.getName());

    //This map holds all registered webresources (key: URI path, value: Webservice instance)
    private Map<String, Webresource> registeredServices;

    private NotFoundHandler notFoundHandler;
    //private Channel channel;
    private boolean shutdown;

    /**
     * @param notFoundHandler Instance of {@link NotFoundHandler} to deal with
     * inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s with
     * {@link de.uzl.itm.ncoap.message.MessageCode#PUT} if the addresses
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} does not
     * exist.
     *
     * @param executor the {@link ScheduledExecutorService} to process the task
     * to send a {@link de.uzl.itm.ncoap.message.CoapResponse} and
     */
    public RequestDispatcher(NotFoundHandler notFoundHandler, ScheduledExecutorService executor) {
	super(executor);
	this.registeredServices = Collections.synchronizedMap(new HashMap<>());
	this.notFoundHandler = notFoundHandler;
	this.shutdown = false;
    }

    public void registerWellKnownCoreResource() {
	//LinkValueList linkValueList = new LinkValueList();
//        for(Webresource webresource : this.registeredServices.values()) {
//            Collection<LinkParam> linkParams = webresource.getLinkParams();
//            linkValueList.addLinkValue(new LinkValue(webresource.getUriPath(), linkParams));
//        }
	registerWebresource(new WellKnownCoreResource(new LinkValueList(), this.getExecutor()));
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, final InetSocketAddress remoteSocket) {

	if (!(coapMessage instanceof CoapRequest)) {
	    return true;
	}

	final CoapRequest coapRequest = (CoapRequest) coapMessage;

	//Create settable future to wait for response
	final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

	//Look up web service instance to handle the request
	final Webresource webresource = this.registeredServices.get(coapRequest.getUriPath());
	if (webresource == null) {
	    // the requested Webservice DOES NOT exist
	    try {
		this.notFoundHandler.processCoapRequest(responseFuture, coapRequest, remoteSocket);
	    } catch (Exception ex) {
		responseFuture.setException(ex);
	    }
	} else if (coapRequest.isIfNonMatchSet()) {
	    createPreconditionFailed(coapRequest.getMessageType(), coapRequest.getUriPath(), responseFuture);
	} else {
	    // the requested Webservice DOES exist
	    try {
		webresource.processCoapRequest(responseFuture, coapRequest, remoteSocket);
	    } catch (Exception ex) {
		responseFuture.setException(ex);
	    }
	}

	Futures.addCallback(responseFuture, new ResponseCallback(
		getContext().channel(), coapRequest, webresource, remoteSocket), getExecutor());

	return true;
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
	// nothing to do ...
	return true;
    }

//    /**
//     * This method is called by the framework to enable the {@link RequestDispatcher} to send messages
//     * to other handlers in the {@link ChannelPipeline}.
//     *
//     * @param channel the {@link Channel} to be used for messages to be sent to other handlers in that channel
//     */
//    public void setChannel(Channel channel) {
//        this.channel = channel;
//    }
    private void createPreconditionFailed(int messageType, String resourcePath,
	    SettableFuture<CoapResponse> responseFuture) {

	CoapResponse coapResponse = new CoapResponse(messageType, PRECONDITION_FAILED_412);
	String message = "IF-NONE-MATCH option was set but service \"" + resourcePath + "\" exists.";
	coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
	responseFuture.set(coapResponse);
    }

    /**
     * This method is invoked by the framework if an exception occured during
     * the reception or sending of a {@link CoapMessage}.
     *
     * Classes extending {@link de.uzl.itm.ncoap.application.server.CoapServer}
     * may override this method to, e.g. deal with different types of exceptions
     * differently. This implementation only logs an the error message (if
     * logging is enabled).
     *
     * @param ctx the {@link ChannelHandlerContext} relating the
     * {@link de.uzl.itm.ncoap.application.server.CoapServer} and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} on which the
     * exception occured
     *
     * @param exceptionEvent the {@link ExceptionEvent} containing, e.g. the
     * exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	super.exceptionCaught(ctx, cause);
	if (!shutdown) {
	    LOG.error("Unsupported exception caught! Don't know what to do...", cause);
	}
    }

    /**
     * Shuts the server down by closing the datagramChannel which includes to
     * unbind the datagramChannel from a listening port and by this means free
     * the port. All blocked or bound external resources are released.
     *
     * Prior to doing so this methods removes all registered
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * instances from the server, i.e. invokes the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource#shutdown()}
     * method of all registered services.
     */
    public ListenableFuture<Void> shutdown() {
	this.shutdown = true;
	String[] uriPaths = registeredServices.keySet().toArray(new String[registeredServices.size()]);
	for (String path : uriPaths) {
	    shutdownWebresource(path);
	}

	// some time to send possible update notifications (404_NOT_FOUND) to observers
	try {
	    Thread.sleep(5000);
	} catch (InterruptedException e) {
	    LOG.error("Interrupted while shutting down RequestDispatcher Manager!", e);
	}

	// TODO make this an asynchronous method
	SettableFuture<Void> future = SettableFuture.create();
	future.set(null);
	return future;
    }

    /**
     * Shut down the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * registered at the given path from the server
     *
     * @param uriPath the path of the
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * to be shut down
     */
    public void shutdownWebresource(final String uriPath) {
	Webresource webresource = registeredServices.remove(uriPath);

	if (webresource != null) {
	    LOG.info("Resource \"{}\" removed from server.", uriPath);
	    webresource.shutdown();
	    WellKnownCoreResource wkcResource
		    = ((WellKnownCoreResource) this.registeredServices.get(WellKnownCoreResource.URI_PATH));
	    if (wkcResource != null) {
		byte[] oldStatus = wkcResource.getWrappedResourceStatus(ContentFormat.APP_LINK_FORMAT).getContent();
		LinkValueList linkValueList = LinkValueList.decode(new String(oldStatus, CoapMessage.CHARSET));
		linkValueList.removeLinkValue(uriPath);
		wkcResource.setResourceStatus(linkValueList, 0);
	    }
	} else {
	    LOG.error("Resource \"{}\" could not be removed. Does not exist.", uriPath);
	}
    }

    /**
     * Registers a Webservice instance at the server. After registration the
     * service will be available at the path given as
     * <code>service.getUriPath()</code>.
     *
     * It is not possible to register multiple webServices at a single path. If
     * a new service is registered at the server with a path from another
     * already registered service, then the new service replaces the old one.
     *
     * @param webresource A
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} instance
     * to be registered at the server
     *
     * @throws java.lang.IllegalArgumentException if there was already a
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * registered with the same path
     */
    public final void registerWebresource(final Webresource webresource) throws IllegalArgumentException {
	if (registeredServices.containsKey(webresource.getUriPath())) {
	    throw new IllegalArgumentException("Resource " + webresource.getUriPath() + " is already registered");
	}

	webresource.setRequestDispatcher(this);
	registeredServices.put(webresource.getUriPath(), webresource);
	LOG.info("Registered new service at " + webresource.getUriPath());

	if (webresource instanceof ObservableWebresource) {
	    ChannelPipeline pipeline = getContext().channel().pipeline();
	    ServerObservationHandler handler = (ServerObservationHandler) getContext().handler();
	    handler.registerWebresource((ObservableWebresource) webresource);
	}

	// update /.well-known/core resource
	WellKnownCoreResource wkcResource
		= (WellKnownCoreResource) this.registeredServices.get(WellKnownCoreResource.URI_PATH);

	if (wkcResource != null) {
	    byte[] oldStatus = wkcResource.getWrappedResourceStatus(ContentFormat.APP_LINK_FORMAT).getContent();
	    LinkValueList linkValueList;
	    if (oldStatus.length == 0) {
		linkValueList = new LinkValueList();
	    } else {
		linkValueList = LinkValueList.decode(new String(oldStatus, CoapMessage.CHARSET));
	    }

	    linkValueList.addLinkValue(new LinkValue(webresource.getUriPath(), webresource.getLinkParams()));
	    wkcResource.setResourceStatus(linkValueList, 0);
	}
    }

    private class ResponseCallback implements FutureCallback<CoapResponse> {

	private final Channel channel;
	private final CoapRequest coapRequest;
	private final Webresource webresource;
	private final InetSocketAddress remoteSocket;

	public ResponseCallback(Channel channel, CoapRequest coapRequest, Webresource webresource,
		InetSocketAddress remoteSocket) {
	    this.channel = channel;
	    this.coapRequest = coapRequest;
	    this.webresource = webresource;
	    this.remoteSocket = remoteSocket;
	}

	@Override
	public void onSuccess(final CoapResponse coapResponse) {
	    coapResponse.setMessageID(coapRequest.getMessageID());
	    coapResponse.setToken(coapRequest.getToken());

	    if (this.coapRequest.getBlock2Szx() != UintOptionValue.UNDEFINED) {
		coapResponse.setPreferredBlock2Size(BlockSize.getBlockSize(this.coapRequest.getBlock2Szx()));
	    }

	    if (coapResponse.isUpdateNotification()) {
		if (webresource instanceof ObservableWebresource && coapRequest.getObserve() == 0) {
		    // trigger new observer accepted event
		    Token token = coapResponse.getToken();
		    long contentFormat = coapResponse.getContentFormat();
		    BlockSize block2Size = BlockSize.getBlockSize(coapRequest.getBlock2Szx());
		    /*
		    triggerEvent(new ObserverAcceptedEvent(
			    remoteSocket, token, (ObservableWebresource) webresource, contentFormat, block2Size
		    ), true);*/
		} else {
		    // the observe option is useless here (remove it)...
		    coapResponse.removeOptions(Option.OBSERVE);
		    LOG.warn("Removed observe option from response!");
		}
	    }
	    sendResponse(coapResponse);
	}

	@Override
	public void onFailure(Throwable throwable) {
	    LOG.error("Exception while processing inbound request", throwable);
	    CoapResponse coapResponse = CoapResponse.createErrorResponse(
		    coapRequest.getMessageType(), INTERNAL_SERVER_ERROR_500, throwable.getMessage()
	    );

	    coapResponse.setMessageID(coapRequest.getMessageID());
	    coapResponse.setToken(coapRequest.getToken());

	    sendResponse(coapResponse);
	}

	private void sendResponse(final CoapResponse coapResponse) {
	    /*
	    ChannelFuture future = Channels.write(this.channel, coapResponse, this.remoteSocket);
	    if (LOG.isDebugEnabled()) {
		future.addListener(new ChannelFutureListener() {
		    @Override
		    public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
			    LOG.debug("Response sent to \"{}\" (Token: {}).", remoteSocket, coapResponse.getToken());
			}
		    }
		});
	}*/
	}
    }
}
