package jolie.net.coap;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Iterator;

public class CoapResponse extends CoapMessage {

    private static final String NO_ERRROR_CODE = "Code no. %s "
	    + "is no error code!";

    public CoapResponse(int messageType, int messageCode)
	    throws IllegalArgumentException {

	super(messageType, messageCode);
	if (!MessageCode.isResponse(messageCode)) {
	    throw new IllegalArgumentException("Message code no."
		    + messageCode + " is no response code.");
	}
    }

    public static CoapResponse createErrorResponse(int messageType,
	    int messageCode, String content) throws IllegalArgumentException {

	if (!MessageCode.isErrorMessage(messageCode)) {
	    throw new IllegalArgumentException(String.format(
		    NO_ERRROR_CODE, MessageCode.asString(messageCode)));
	}

	CoapResponse errorResponse = new CoapResponse(messageType, messageCode);
	errorResponse.setContent(content.getBytes(CoapMessage.CHARSET),
		ContentFormat.TEXT_PLAIN_UTF8);

	return errorResponse;
    }

    public static CoapResponse createErrorResponse(int messageType,
	    int messageCode, Throwable throwable)
	    throws IllegalArgumentException {

	StringWriter stringWriter = new StringWriter();
	throwable.printStackTrace(new PrintWriter(stringWriter));
	return createErrorResponse(messageType, messageCode,
		stringWriter.toString());
    }

    public boolean isErrorResponse() {
	return MessageCode.isErrorMessage(this.getMessageCode());
    }

    public void setEtag(byte[] etag) throws IllegalArgumentException {
	this.addOpaqueOption(Option.ETAG, etag);
    }

    public byte[] getEtag() {
	if (options.containsKey(Option.ETAG)) {
	    return ((OpaqueOptionValue) options.get(Option.ETAG).iterator()
		    .next()).getDecodedValue();
	} else {
	    return null;
	}
    }

    public void setObserve() {
	this.setObserve(System.currentTimeMillis() % ResourceStatusAge.MODULUS);
    }

    public void setPreferredBlock2Size(BlockSize block2Size) {
	if (BlockSize.UNBOUND == block2Size || block2Size == null) {
	    this.removeOptions(Option.BLOCK_2);
	} else {
	    this.setBlock2(0, false, block2Size.getSzx());
	}
    }

    public void setBlock2(long number, boolean more, long szx)
	    throws IllegalArgumentException {
	try {
	    this.removeOptions(Option.BLOCK_2);
	    if (number > 1048575) {
		throw new IllegalArgumentException("Max. BLOCK2NUM is 1048575");
	    }
	    //long more = ((more) ? 1 : 0) << 3;
	    this.addUintOption(Option.BLOCK_2, ((number & 0xFFFFF) << 4)
		    + ((more ? 1 : 0) << 3) + szx);
	} catch (IllegalArgumentException e) {
	    this.removeOptions(Option.BLOCK_2);
	}
    }

    public void setBlock1(long number, long szx)
	    throws IllegalArgumentException {
	try {
	    this.removeOptions(Option.BLOCK_1);
	    if (number > 1048575) {
		throw new IllegalArgumentException("Max. BLOCK1NUM is 1048575");
	    }
	    //long more = ((more) ? 1 : 0) << 3;
	    this.addUintOption(Option.BLOCK_1, ((number & 0xFFFFF) << 4)
		    + (1 << 3) + szx);
	} catch (IllegalArgumentException e) {
	    this.removeOptions(Option.BLOCK_1);
	}
    }

    public boolean isUpdateNotification() {
	return this.getObserve() != UintOptionValue.UNDEFINED;
    }

    public void setLocationURI(URI locationURI)
	    throws IllegalArgumentException {

	options.removeAll(Option.LOCATION_PATH);
	options.removeAll(Option.LOCATION_QUERY);

	String locationPath = locationURI.getRawPath();
	String locationQuery = locationURI.getRawQuery();

	try {
	    if (locationPath != null) {
		//Path must not start with "/" to be further processed
		if (locationPath.startsWith("/")) {
		    locationPath = locationPath.substring(1);
		}

		for (String pathComponent : locationPath.split("/")) {
		    this.addStringOption(Option.LOCATION_PATH, pathComponent);
		}
	    }

	    if (locationQuery != null) {
		for (String queryComponent : locationQuery.split("&")) {
		    this.addStringOption(Option.LOCATION_QUERY, queryComponent);
		}
	    }
	} catch (IllegalArgumentException ex) {
	    options.removeAll(Option.LOCATION_PATH);
	    options.removeAll(Option.LOCATION_QUERY);
	    throw ex;
	}
    }

    public URI getLocationURI() throws URISyntaxException {

	//Reconstruct path
	StringBuilder locationPath = new StringBuilder();

	if (options.containsKey(Option.LOCATION_PATH)) {
	    for (OptionValue optionValue : options.get(Option.LOCATION_PATH)) {
		locationPath.append("/")
			.append(((StringOptionValue) optionValue)
				.getDecodedValue());
	    }
	}

	//Reconstruct query
	StringBuilder locationQuery = new StringBuilder();

	if (options.containsKey(Option.LOCATION_QUERY)) {
	    Iterator<OptionValue> queryComponentIterator
		    = options.get(Option.LOCATION_QUERY).iterator();
	    locationQuery.append(((StringOptionValue) queryComponentIterator
		    .next()).getDecodedValue());
	    while (queryComponentIterator.hasNext()) {
		locationQuery.append("&")
			.append(((StringOptionValue) queryComponentIterator
				.next()).getDecodedValue());
	    }
	}

	if (locationPath.length() == 0 && locationQuery.length() == 0) {
	    return null;
	}

	return new URI(null, null, null, (int) UintOptionValue.UNDEFINED,
		locationPath.toString(), locationQuery.toString(), null);
    }

    public void setMaxAge(long maxAge) {
	try {
	    this.options.removeAll(Option.MAX_AGE);
	    this.addUintOption(Option.MAX_AGE, maxAge);
	} catch (IllegalArgumentException e) {
	}
    }

    public long getMaxAge() {
	if (options.containsKey(Option.MAX_AGE)) {
	    return ((UintOptionValue) options.get(Option.MAX_AGE).iterator()
		    .next()).getDecodedValue();
	} else {
	    return OptionValue.MAX_AGE_DEFAULT;
	}
    }
}
