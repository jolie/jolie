package ncoap.message.options;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import ncoap.message.CoapMessage;
import static ncoap.message.options.Option.*;
import static ncoap.message.options.OptionValue.Type.*;

public abstract class OptionValue<T>{

    private static final String UNKNOWN_OPTION = "Unknown option no. %d";
    private static final String VALUE_IS_DEFAULT_VALUE = "Given value is default value for option no. %d.";
    private static final String OUT_OF_ALLOWED_RANGE = "Given value length (%d) is out of allowed range " +
            "for option no. %d (min: %d, max; %d).";

    public static enum Type {
        EMPTY, STRING, UINT, OPAQUE
    }
    

    /**
     * Corresponds to 60, i.e. 60 seconds
     */
    public static final long MAX_AGE_DEFAULT    = 60;

    /**
     * Corresponds to the maximum value of the max-age option (app. 136 years)
     */
    public static final long MAX_AGE_MAX        = 0xFFFFFFFFL;

    /**
     * Corresponds to the encoded value of {@link #MAX_AGE_DEFAULT}
     */
    public static final byte[] ENCODED_MAX_AGE_DEFAULT =
            new BigInteger(1, Longs.toByteArray(MAX_AGE_DEFAULT)).toByteArray();

    /**
     * Corresponds to 5683
     */
    public static final long URI_PORT_DEFAULT   = 5683;

    /**
     * Corresponds to the encoded value of {@link #URI_PORT_DEFAULT}
     */
    public static final byte[] ENCODED_URI_PORT_DEFAULT =
            new BigInteger(1, Longs.toByteArray(URI_PORT_DEFAULT)).toByteArray();


    private static class Characteristics {
        private Type type;
        private int minLength;
        private int maxLength;

        private Characteristics(Type type, int minLength, int maxLength) {
            this.type = type;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public Type getType() {
            return type;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }
    }

    private static HashMap<Integer, Characteristics> CHARACTERISTICS = new HashMap<>();
    static {
        CHARACTERISTICS.put( IF_MATCH,        new Characteristics( OPAQUE,  0,    8 ));
        CHARACTERISTICS.put( URI_HOST,        new Characteristics( STRING,  1,  255 ));
        CHARACTERISTICS.put( ETAG,            new Characteristics( OPAQUE,  1,    8 ));
        CHARACTERISTICS.put( IF_NONE_MATCH,   new Characteristics( EMPTY,   0,    0 ));
        CHARACTERISTICS.put( URI_PORT,        new Characteristics( UINT,    0,    2 ));
        CHARACTERISTICS.put( LOCATION_PATH,   new Characteristics( STRING,  0,  255 ));
        CHARACTERISTICS.put( OBSERVE,         new Characteristics( UINT,    0,    3 ));
        CHARACTERISTICS.put( URI_PATH,        new Characteristics( STRING,  0,  255 ));
        CHARACTERISTICS.put( CONTENT_FORMAT,  new Characteristics( UINT,    0,    2 ));
        CHARACTERISTICS.put( MAX_AGE,         new Characteristics( UINT,    0,    4 ));
        CHARACTERISTICS.put( URI_QUERY,       new Characteristics( STRING,  0,  255 ));
        CHARACTERISTICS.put( ACCEPT,          new Characteristics( UINT,    0,    2 ));
        CHARACTERISTICS.put( LOCATION_QUERY,  new Characteristics( STRING,  0,  255 ));
        CHARACTERISTICS.put( BLOCK_2,         new Characteristics( UINT,    0,    3 ));
        CHARACTERISTICS.put( BLOCK_1,         new Characteristics( UINT,    0,    3 ));
        CHARACTERISTICS.put( SIZE_2,          new Characteristics( UINT,    0,    4 ));
        CHARACTERISTICS.put( PROXY_URI,       new Characteristics( STRING,  1, 1034 ));
        CHARACTERISTICS.put( PROXY_SCHEME,    new Characteristics( STRING,  1,  255 ));
        CHARACTERISTICS.put( SIZE_1,          new Characteristics( UINT,    0,    4 ));
        CHARACTERISTICS.put( ENDPOINT_ID_1,   new Characteristics( OPAQUE,  0,    8 ));
        CHARACTERISTICS.put( ENDPOINT_ID_2,   new Characteristics( OPAQUE,  0,    8 ));
    }

    /**
     * Returns the {@link de.uzl.itm.ncoap.message.options.OptionValue.Type} the given option number refers to
     *
     * @param optionNumber the option number to return the type of
     *
     * @return the {@link de.uzl.itm.ncoap.message.options.OptionValue.Type} the given option number refers to
     *
     * @throws java.lang.IllegalArgumentException if the given option number refers to an unknown option
     */
    public static Type getType(int optionNumber) throws IllegalArgumentException {
        Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
        if (characteristics == null) {
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNumber));
        } else {
            return characteristics.getType();
        }
    }

    /**
     * Returns the minimum length for the given option number in bytes.
     *
     * @param optionNumber the option number to check the minimum length of
     *
     * @return the minimum length for the given option number in bytes
     *
     * @throws java.lang.IllegalArgumentException if the given option number refers to an unknown option
     */
    public static int getMinLength(int optionNumber) throws IllegalArgumentException {
        Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
        if (characteristics == null) {
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNumber));
        } else {
            return characteristics.getMinLength();
        }
    }


    /**
     * Returns the maximum length for the given option number in bytes.
     *
     * @param optionNumber the option number to check the maximum length of
     *
     * @return the maximum length for the given option number in bytes
     *
     * @throws java.lang.IllegalArgumentException if the given option number refers to an unknown option
     */
    public static int getMaxLength(int optionNumber) throws IllegalArgumentException {
        Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
        if (characteristics == null) {
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNumber));
        } else {
            return characteristics.getMaxLength();
        }
    }


    /**
     * Returns <code>true</code> if the given value is the default value for the given option number and
     * <code>false</code> if it is not the default value. Options with default value cannot be created.
     *
     * @param optionNumber the option number
     * @param value the value to check if it is the default value for the option number
     *
     * @return <code>true</code> if the given value is the default value for the given option number
     */
    public static boolean isDefaultValue(int optionNumber, byte[] value) {

        if (optionNumber == URI_PORT && Arrays.equals(value, ENCODED_URI_PORT_DEFAULT)) {
            return true;
        } else if (optionNumber == MAX_AGE && Arrays.equals(value, ENCODED_MAX_AGE_DEFAULT)) {
            return true;
        } else  if (optionNumber == URI_HOST) {
            String hostName = new String(value, CoapMessage.CHARSET);
            if (hostName.startsWith("[") && hostName.endsWith("]")) {
                hostName = hostName.substring(1, hostName.length() - 1);
            }

            if (InetAddresses.isInetAddress(hostName)) {
                return true;
            }
        }

        return false;
    }


    protected byte[] value;

    /**
     * @param optionNumber the number of the {@link OptionValue} to be created.
     * @param value the encoded value of the option to be created.
     * @param allowDefault
     *
     * @throws java.lang.IllegalArgumentException if the {@link OptionValue} instance could not be created because
     * either the
     * given value is the default value or the length of the given value exceeds the defined limits.
     */
    protected OptionValue(int optionNumber, byte[] value, boolean allowDefault) throws IllegalArgumentException {

        if (!allowDefault && OptionValue.isDefaultValue(optionNumber, value)) {
            throw new IllegalArgumentException(String.format(VALUE_IS_DEFAULT_VALUE, optionNumber));
        }

        if (getMinLength(optionNumber) > value.length || getMaxLength(optionNumber) < value.length) {
            throw new IllegalArgumentException(String.format(OUT_OF_ALLOWED_RANGE, value.length, optionNumber,
                    getMinLength(optionNumber), getMaxLength(optionNumber)));
        }

        this.value = value;
    }

    /**
     * Returns the encoded value of this {@link OptionValue} as byte array. The way how to interpret the returned value
     * depends on the {@link Type}. Usually it is more convenient to use {@link #getDecodedValue()} instead.
     *
     * @return the encoded value of this option as byte array
     */
    public byte[] getValue() {
        return this.value;
    }

    /**
     * Returns the decoded value of this {@link OptionValue} as an instance of <code>T</code>.
     *
     * @return the decoded value of this {@link OptionValue} as an instance of <code>T</code>
     */
    public abstract T getDecodedValue();


    @Override
    public abstract int hashCode();

    /**
     * Returns <code>true</code> if the given object is an instance of {@link OptionValue} and both byte arrays returned by
     * respective {@link #getValue()} method contain the same bytes, i.e. the same array length and the same byte
     * values in the same order.
     *
     * @param object the object to check for equality with this instance of {@link OptionValue}
     *
     * @return <code>true</code> if the given object is an instance of {@link OptionValue} and both byte arrays returned by
     * respective {@link #getValue()} method contain the same bytes, i.e. the same array length and the same byte
     * values in the same order.
     */
    @Override
    public abstract boolean equals(Object object);

    /**
     * Returns a {@link String} representation of this option.
     *
     * @return a {@link String} representation of this option.
     */
    @Override
    public String toString() {
        return "" + this.getDecodedValue();
    }

   
}
