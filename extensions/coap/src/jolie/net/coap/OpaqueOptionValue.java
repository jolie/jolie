package jolie.net.coap;

import java.util.Arrays;

public class OpaqueOptionValue extends OptionValue<byte[]> {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public OpaqueOptionValue(int optionNumber, byte[] value)
	    throws IllegalArgumentException {
	super(optionNumber, value, false);
    }

    @Override
    public byte[] getDecodedValue() {
	return this.value;
    }

    @Override
    public int hashCode() {
	return Arrays.hashCode(getDecodedValue());
    }

    @Override
    public boolean equals(Object object) {
	if (!(object instanceof OpaqueOptionValue)) {
	    return false;
	}

	OpaqueOptionValue other = (OpaqueOptionValue) object;
	return Arrays.equals(this.getValue(), other.getValue());
    }

    @Override
    public String toString() {
	return toHexString(this.value);
    }

    public static String toHexString(byte[] bytes) {
	if (bytes.length == 0) {
	    return "<empty>";
	} else {
	    return "0x" + bytesToHex(bytes);
	}
    }

    private static String bytesToHex(byte[] bytes) {
	char[] hexChars = new char[bytes.length * 2];
	for (int j = 0; j < bytes.length; j++) {
	    int v = bytes[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }
}
