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

package ncoap.application.server.resource;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;

import ncoap.application.linkformat.LinkParam;
import ncoap.application.linkformat.LinkValueList;
import ncoap.message.CoapMessage;
import ncoap.message.CoapRequest;
import ncoap.message.CoapResponse;
import ncoap.message.MessageCode;
import ncoap.message.options.ContentFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import static ncoap.message.MessageCode.*;

/**
 * <p>
 * The .well-known/core resource is a standard webresource to be provided by
 * every CoAP webserver as defined in the CoAP protocol draft. It provides a
 * list of all available services on the server in CoRE Link Format.</p>
 *
 * <p>
 * Furthermore it provides the ability to filter resources based on query
 * parameters, e.g.
 * <ul>
 * <li><code>coap://example.org/.well-known/core?ct=0</code> to filter all
 * resources that support content format 0 (plain text) for responses</li>
 * <li><code>coap://example.org/.well-known/core?title="some title"</code></li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Note:</b> for string values (such as a title) the double quotes are part
 * of the value and thus must be part of the query parameter (percent encoded).
 * According to RFC 6690 double quotes that enclose numerical values separated
 * by blanks refer to a list of values (e.g. for link param ct="0 40"
 * resp.).</p>
 *
 *
 * @author Oliver Kleine
 */
public final class WellKnownCoreResource extends ObservableWebresource<LinkValueList> {

    public static final String URI_PATH = "/.well-known/core";

    private static Logger LOG = LoggerFactory.getLogger(WellKnownCoreResource.class.getName());

    private byte[] etag;

    /**
     * Creates the well-known/core resource at path /.well-known/core as defined
     * in the CoAP draft
     *
     * @param initialStatus the {@link java.util.Map} containing all available
     * path
     */
    public WellKnownCoreResource(LinkValueList initialStatus, ScheduledExecutorService executor) {
	super(URI_PATH, initialStatus, 0, executor);

	// set content format "40" as link param
	this.setLinkParam(LinkParam.createLinkParam(LinkParam.Key.CT, "40"));
    }

    /**
     * The .well-known/core resource only allows requests with
     * {@link MessageCode#GET}. Any other code returns a {@link CoapResponse}
     * with {@link MessageCode#METHOD_NOT_ALLOWED_405}.
     *
     * In case of a request with {@link MessageCode#GET} it returns a
     * {@link CoapResponse} with {@link MessageCode#CONTENT_205} and with a
     * payload listing all paths to the available resources (i.e.
     * {@link Webresource} instances}).
     *
     * <b>Note:</b> The payload is always formatted in
     * {@link de.uzl.itm.ncoap.message.options.ContentFormat#APP_LINK_FORMAT},
     * possibly contained {@link de.uzl.itm.ncoap.message.options.Option#ACCEPT}
     * options in inbound {@link CoapRequest}s are ignored!
     *
     * @param responseFuture The {@link SettableFuture} to be set with a
     * {@link CoapResponse} containing the list of available services in CoRE
     * link format.
     * @param coapRequest The {@link CoapRequest} to be processed by the
     * {@link Webresource} instance
     * @param remoteSocket The address of the sender of the request
     *
     * @throws Exception Implementing classes may throw any {@link Exception}.
     * Thrown {@link Exception}s cause the framework to send a
     * {@link CoapResponse} with {@link MessageCode#INTERNAL_SERVER_ERROR_500}
     * to the client.
     */
    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
	    InetSocketAddress remoteSocket) throws Exception {

	if (!(coapRequest.getMessageCode() == GET)) {
	    responseFuture.set(CoapResponse.createErrorResponse(
		    coapRequest.getMessageType(), METHOD_NOT_ALLOWED_405, "GET is the only allowed method!"
	    ));
	} else {
	    processCoapGetRequest(responseFuture, coapRequest);
	}
    }

    private void processCoapGetRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest) {
	try {
	    String query = coapRequest.getUriQuery();
	    LinkParam linkParam = "".equals(query) ? null : LinkParam.decode(query);

	    WrappedResourceStatus status = this.getWrappedResourceStatus(ContentFormat.APP_LINK_FORMAT);
	    byte[] content;
	    if (linkParam == null) {
		// the /.well-known/core will for sure cause no NullPointerException
		content = status.getContent();
	    } else {
		// the /.well-known/core will for sure cause no NullPointerException
		LinkValueList linkValueList = LinkValueList.decode(new String(status.getContent(), CoapMessage.CHARSET));
		content = getFilteredSerializedResourceStatus(linkValueList, linkParam);
	    }

	    CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
	    coapResponse.setContent(content, ContentFormat.APP_LINK_FORMAT);
	    coapResponse.setEtag(this.etag);
	    responseFuture.set(coapResponse);

	} catch (IllegalArgumentException ex) {
	    responseFuture.set(CoapResponse.createErrorResponse(
		    coapRequest.getMessageType(), BAD_REQUEST_400, ex.getMessage()
	    ));
	}
    }

    private static byte[] getFilteredSerializedResourceStatus(LinkValueList linkValueList, LinkParam filter) {
//        for (Webresource webresource : this.getResourceStatus()) {
//            LinkValue linkValue = new LinkValue(webresource.getUriPath(), webresource.getLinkParams());
//            linkValueList.addLinkValue(linkValue);
//        }

	StringBuilder buffer = new StringBuilder();
	if (filter == null) {
	    buffer.append(linkValueList.encode());
	} else {
	    buffer.append(linkValueList.filter(filter.getKey(), filter.getValue()).encode());
	}

	LOG.debug("Content: \n{}", buffer.toString());

	return buffer.toString().getBytes(CoapMessage.CHARSET);
    }

    /**
     * <p>
     * Returns the serialized resource status in
     * {@link ContentFormat#APP_LINK_FORMAT}</p>
     *
     * <p>
     * <b>Note:</b> The contentFormat parameter is ignored!</p>
     *
     * @param contentFormat the number indicating the desired format of the
     * returned content, see
     * {@link de.uzl.itm.ncoap.message.options.ContentFormat} for some
     * pre-defined constants.
     *
     * @return the serialized resource status in
     * {@link ContentFormat#APP_LINK_FORMAT}
     */
    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
	return this.getResourceStatus().encode().getBytes(CoapMessage.CHARSET);
    }

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket) {
	return true;
    }

    @Override
    public void removeObserver(InetSocketAddress remoteSocket) {
	// nothing to do
    }

    @Override
    public void shutdown() {
	//nothing to do here...
    }

    @Override
    public byte[] getEtag(long contentFormat) {
	return this.etag;
    }

    @Override
    public void updateEtag(LinkValueList resourceStatus) {
	this.etag = Ints.toByteArray(Arrays.hashCode(getSerializedResourceStatus(ContentFormat.APP_LINK_FORMAT)));
    }

}
