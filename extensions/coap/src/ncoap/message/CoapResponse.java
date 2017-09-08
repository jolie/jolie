package ncoap.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Iterator;

import ncoap.communication.blockwise.BlockSize;
import ncoap.communication.observing.ResourceStatusAge;
import ncoap.message.options.ContentFormat;
import ncoap.message.options.OpaqueOptionValue;
import ncoap.message.options.OptionValue;
import ncoap.message.options.StringOptionValue;
import ncoap.message.options.UintOptionValue;
import static ncoap.message.options.Option.BLOCK_1;
import static ncoap.message.options.Option.BLOCK_2;
import static ncoap.message.options.Option.ETAG;
import static ncoap.message.options.Option.LOCATION_PATH;
import static ncoap.message.options.Option.LOCATION_QUERY;
import static ncoap.message.options.Option.MAX_AGE;

/**
 * <p>
 * Instances of {@link CoapResponse} are created by an instance of
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource} to answer
 * requests.</p>
 *
 * <p>
 * <b>Note:</b> The given {@link MessageType} (one of
 * {@link de.uzl.itm.ncoap.message.MessageType#CON} or
 * {@link de.uzl.itm.ncoap.message.MessageType#NON}) may be changed by the
 * framework before it is sent to the other CoAP endpoints. Such a change might
 * e.g. happen if this {@link CoapResponse} was created with
 * {@link de.uzl.itm.ncoap.message.MessageType#CON} to answer a
 * {@link CoapRequest} with {@link de.uzl.itm.ncoap.message.MessageType#CON} and
 * the framework did not yet send an empty {@link CoapMessage} with
 * {@link MessageType#ACK}. Then the framework will ensure the
 * {@link MessageType} of this {@link CoapResponse} to be set to
 * {@link de.uzl.itm.ncoap.message.MessageType#ACK} to make it a piggy-backed
 * response.</p>
 *
 * @author Oliver Kleine
 */
public class CoapResponse extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapResponse.class.getName());

    private static final String NO_ERRROR_CODE = "Code no. %s is no error code!";

    /**
     * Creates a new instance of {@link CoapResponse}.
     *
     * @param messageType
     * <p>
     * the number representing the {@link MessageType}</p>
     *
     * <p>
     * <b>Note:</b> the {@link MessageType} might be changed by the framework
     * (see class description).</p>
     *
     * @param messageCode the {@link MessageCode} for this {@link CoapResponse}
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given
     * arguments causes an error
     */
    public CoapResponse(int messageType, int messageCode) throws IllegalArgumentException {
	super(messageType, messageCode);

	if (!MessageCode.isResponse(messageCode)) {
	    throw new IllegalArgumentException("Message code no." + messageCode + " is no response code.");
	}
    }

    /**
     * Creates a new instance of {@link CoapResponse} with
     * {@link MessageCode#INTERNAL_SERVER_ERROR_500} and the stacktrace of the
     * given {@link Throwable} as payload (this is particularly useful for
     * debugging). Basically, this can be considered a shortcut to create error
     * responses.
     *
     * @param messageType
     * <p>
     * the {@link MessageType} (one of {@link MessageType#CON} or
     * {@link MessageType#NON}).</p>
     *
     * <p>
     * <b>Note:</b> the {@link MessageType} might be changed by the framework
     * (see class description).</p>
     * @param messageCode the {@link MessageCode} for this {@link CoapResponse}
     *
     * @return a new instance of {@link CoapResponse} with the
     * {@link Throwable#getMessage} as content (payload).
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given
     * arguments causes an error
     */
    public static CoapResponse createErrorResponse(int messageType, int messageCode, String content)
	    throws IllegalArgumentException {

	if (!MessageCode.isErrorMessage(messageCode)) {
	    throw new IllegalArgumentException(String.format(NO_ERRROR_CODE, MessageCode.asString(messageCode)));
	}

	CoapResponse errorResponse = new CoapResponse(messageType, messageCode);
	errorResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

	return errorResponse;
    }

    /**
     * Creates a new instance of {@link CoapResponse} with the stacktrace of the
     * given {@link Throwable} as payload (this is particularly useful for
     * debugging). Basically, this can be considered a shortcut to create error
     * responses.
     *
     * @param messageType
     * <p>
     * the {@link MessageType} (one of {@link MessageType#CON} or
     * {@link MessageType#NON}).</p>
     *
     * <p>
     * <b>Note:</b> the {@link MessageType} might be changed by the framework
     * (see class description).</p>
     * @param messageCode the {@link MessageCode} for this {@link CoapResponse}
     *
     * @return a new instance of {@link CoapResponse} with the
     * {@link Throwable#getMessage} as content (payload).
     *
     * @throws java.lang.IllegalArgumentException if the given message code does
     * not refer to an error
     */
    public static CoapResponse createErrorResponse(int messageType, int messageCode, Throwable throwable)
	    throws IllegalArgumentException {

	StringWriter stringWriter = new StringWriter();
	throwable.printStackTrace(new PrintWriter(stringWriter));
	return createErrorResponse(messageType, messageCode, stringWriter.toString());
    }

    public boolean isErrorResponse() {
	return MessageCode.isErrorMessage(this.getMessageCode());
    }

    /**
     * Sets the {@link de.uzl.itm.ncoap.message.options.Option#ETAG} of this
     * {@link CoapResponse}.
     *
     * @param etag the byte array that is supposed to represent the ETAG of the
     * content returned by {@link #getContent()}.
     *
     * @throws IllegalArgumentException if the given byte array is invalid to be
     * considered an ETAG
     */
    public void setEtag(byte[] etag) throws IllegalArgumentException {
	this.addOpaqueOption(ETAG, etag);
    }

    /**
     * Returns the byte array representing the ETAG of the content returned by
     * {@link #getContent()}
     *
     * @return the byte array representing the ETAG of the content returned by
     * {@link #getContent()}
     */
    public byte[] getEtag() {
	if (options.containsKey(ETAG)) {
	    return ((OpaqueOptionValue) options.get(ETAG).iterator().next()).getDecodedValue();
	} else {
	    return null;
	}
    }

    /**
     * Sets the observe option to a proper value automatically. This method is
     * to be invoked by instances of
     * {@link de.uzl.itm.ncoap.application.server.resource.ObservableWebresource}
     * if an inbound {@link CoapRequest} to start a new observation is accepted.
     */
    public void setObserve() {
	this.setObserve(System.currentTimeMillis() % ResourceStatusAge.MODULUS);
    }

    public void setPreferredBlock2Size(BlockSize block2Size) {
	if (BlockSize.UNBOUND == block2Size || block2Size == null) {
	    this.removeOptions(BLOCK_2);
	} else {
	    this.setBlock2(0, false, block2Size.getSzx());
	}
    }

    /**
     * Sets the BLOCK2 option in this {@link CoapRequest} and returns
     * <code>true</code> if the option is set after method returns (may already
     * have been set beforehand in a prior method invocation) or
     * <code>false</code> if the option is not set, e.g. because that option has
     * no meaning with the message code of this {@link CoapRequest}.
     *
     * @param number The relative number of the block sent or requested
     * @param more Whether more blocks are following;
     * @param szx The block size (can assume values between 0 and 6, the actual
     * block size is then 2^(szx + 4)).
     *
     * @throws IllegalArgumentException if the block number is greater than
     * 1048575 (2^20 - 1)
     */
    public void setBlock2(long number, boolean more, long szx) throws IllegalArgumentException {
	try {
	    this.removeOptions(BLOCK_2);
	    if (number > 1048575) {
		throw new IllegalArgumentException("Max. BLOCK2NUM is 1048575");
	    }
	    //long more = ((more) ? 1 : 0) << 3;
	    this.addUintOption(BLOCK_2, ((number & 0xFFFFF) << 4) + ((more ? 1 : 0) << 3) + szx);
	} catch (IllegalArgumentException e) {
	    this.removeOptions(BLOCK_2);
	    log.error("This should never happen.", e);
	}
    }

    public void setBlock1(long number, long szx) throws IllegalArgumentException {
	try {
	    this.removeOptions(BLOCK_1);
	    if (number > 1048575) {
		throw new IllegalArgumentException("Max. BLOCK1NUM is 1048575");
	    }
	    //long more = ((more) ? 1 : 0) << 3;
	    this.addUintOption(BLOCK_1, ((number & 0xFFFFF) << 4) + (1 << 3) + szx);
	} catch (IllegalArgumentException e) {
	    this.removeOptions(BLOCK_1);
	    log.error("This should never happen.", e);
	}
    }

    /**
     * Returns <code>true</code> if this {@link CoapResponse} is an update
     * notification and <code>false</code> otherwise. A {@link CoapResponse} is
     * considered an update notification if the invocation of
     * {@link #getObserve()} returns a value other than <code>null</code>.
     *
     * @return <code>true</code> if this {@link CoapResponse} is an update
     * notification and <code>false</code> otherwise.
     */
    public boolean isUpdateNotification() {
	return this.getObserve() != UintOptionValue.UNDEFINED;
    }

    /**
     * Adds all necessary location URI related options to the list. This causes
     * eventually already contained location URI related options to be removed
     * from the list even in case of an exception.
     *
     * @param locationURI The location URI of the newly created resource. The
     * parts scheme, host, and port are ignored anyway and thus may not be
     * included in the URI object
     *
     * @throws java.lang.IllegalArgumentException if at least one of the options
     * to be added is not valid. Previously to throwing the exception possibly
     * contained options of
     * {@link de.uzl.itm.ncoap.message.options.Option#LOCATION_PATH} and
     * {@link de.uzl.itm.ncoap.message.options.Option#LOCATION_QUERY} are
     * removed from this {@link CoapResponse}.
     */
    public void setLocationURI(URI locationURI) throws IllegalArgumentException {

	options.removeAll(LOCATION_PATH);
	options.removeAll(LOCATION_QUERY);

	String locationPath = locationURI.getRawPath();
	String locationQuery = locationURI.getRawQuery();

	try {
	    if (locationPath != null) {
		//Path must not start with "/" to be further processed
		if (locationPath.startsWith("/")) {
		    locationPath = locationPath.substring(1);
		}

		for (String pathComponent : locationPath.split("/")) {
		    this.addStringOption(LOCATION_PATH, pathComponent);
		}
	    }

	    if (locationQuery != null) {
		for (String queryComponent : locationQuery.split("&")) {
		    this.addStringOption(LOCATION_QUERY, queryComponent);
		}
	    }
	} catch (IllegalArgumentException ex) {
	    options.removeAll(LOCATION_PATH);
	    options.removeAll(LOCATION_QUERY);
	    throw ex;
	}
    }

    /**
     * Returns the URI reconstructed from the location URI related options
     * contained in the message
     *
     * @return the URI reconstructed from the location URI related options
     * contained in the message or null if there are no location URI related
     * options
     *
     * @throws java.net.URISyntaxException if the URI to be reconstructed from
     * options is invalid
     */
    public URI getLocationURI() throws URISyntaxException {

	//Reconstruct path
	StringBuilder locationPath = new StringBuilder();

	if (options.containsKey(LOCATION_PATH)) {
	    for (OptionValue optionValue : options.get(LOCATION_PATH)) {
		locationPath.append("/").append(((StringOptionValue) optionValue).getDecodedValue());
	    }
	}

	//Reconstruct query
	StringBuilder locationQuery = new StringBuilder();

	if (options.containsKey(LOCATION_QUERY)) {
	    Iterator<OptionValue> queryComponentIterator = options.get(LOCATION_QUERY).iterator();
	    locationQuery.append(((StringOptionValue) queryComponentIterator.next()).getDecodedValue());
	    while (queryComponentIterator.hasNext()) {
		locationQuery.append("&")
			.append(((StringOptionValue) queryComponentIterator.next()).getDecodedValue());
	    }
	}

	if (locationPath.length() == 0 && locationQuery.length() == 0) {
	    return null;
	}

	return new URI(null, null, null, (int) UintOptionValue.UNDEFINED, locationPath.toString(),
		locationQuery.toString(), null);
    }

    /**
     * Sets the Max-Age option of this
     * {@link de.uzl.itm.ncoap.message.CoapResponse}. If there was a Max-Age
     * option set prior to the invocation of this method, the previous value is
     * overwritten.
     *
     * @param maxAge the value for the Max-Age option to be set
     */
    public void setMaxAge(long maxAge) {
	try {
	    this.options.removeAll(MAX_AGE);
	    this.addUintOption(MAX_AGE, maxAge);
	} catch (IllegalArgumentException e) {
	    log.error("This should never happen.", e);
	}
    }

    /**
     * Returns the value of the Max-Age option of this
     * {@link de.uzl.itm.ncoap.message.CoapResponse}. If no such option exists,
     * this method returns
     * {@link de.uzl.itm.ncoap.message.options.OptionValue#MAX_AGE_DEFAULT}.
     *
     * @return the value of the Max-Age option of this
     * {@link de.uzl.itm.ncoap.message.CoapResponse}. If no such option exists,
     * this method returns
     * {@link de.uzl.itm.ncoap.message.options.OptionValue#MAX_AGE_DEFAULT}.
     */
    public long getMaxAge() {
	if (options.containsKey(MAX_AGE)) {
	    return ((UintOptionValue) options.get(MAX_AGE).iterator().next()).getDecodedValue();
	} else {
	    return OptionValue.MAX_AGE_DEFAULT;
	}
    }
}
