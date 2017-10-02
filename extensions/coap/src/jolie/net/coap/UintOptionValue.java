package jolie.net.coap;

import java.math.BigInteger;
import java.util.Arrays;

public class UintOptionValue extends OptionValue<Long> {

    public static final long UNDEFINED = -1;

    public UintOptionValue(int optionNumber, byte[] value)
	    throws IllegalArgumentException {
	this(optionNumber, shortenValue(value), false);
    }

    public UintOptionValue(int optionNumber, byte[] value, boolean allowDefault)
	    throws IllegalArgumentException {
	super(optionNumber, shortenValue(value), allowDefault);
    }

    @Override
    public Long getDecodedValue() {
	return new BigInteger(1, value).longValue();
    }

    @Override
    public int hashCode() {
	return getDecodedValue().hashCode();
    }

    @Override
    public boolean equals(Object object) {
	if (!(object instanceof UintOptionValue)) {
	    return false;
	}

	UintOptionValue other = (UintOptionValue) object;
	return Arrays.equals(this.getValue(), other.getValue());
    }

    public static byte[] shortenValue(byte[] value) {
	int index = 0;
	while (index < value.length - 1 && value[index] == 0) {
	    index++;
	}

	return Arrays.copyOfRange(value, index, value.length);
    }
}
