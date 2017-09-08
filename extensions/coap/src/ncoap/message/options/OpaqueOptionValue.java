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
package ncoap.message.options;

import java.util.Arrays;

/**
 * This class contains all specific functionality for {@link OptionValue} instances of {@link OptionValue.Type#OPAQUE}.
 *
 * @author Oliver Kleine
 */
public class OpaqueOptionValue extends OptionValue<byte[]> {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public OpaqueOptionValue(int optionNumber, byte[] value) throws IllegalArgumentException {
        super(optionNumber, value, false);
    }

    /**
     * For {@link OpaqueOptionValue}s the returned value is the same as {@link #getValue()}.
     *
     * @return the byte array containing the actual value of this option
     */
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
        if (!(object instanceof OpaqueOptionValue))
            return false;

        OpaqueOptionValue other = (OpaqueOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }


    /**
     * Returns a {@link String} representation of this options value. Basically, this is a shortcut for
     * {@link #toHexString(byte[])} with {@link #getValue()} as given parameter.
     *
     * @return a {@link String} representation of this options value
     */
    @Override
    public String toString() {
        return toHexString(this.value);
    }

    /**
     * Returns a {@link String} representation of this options value, i.e. the string <code><empty></code> if the
     * length of the byte array returned by {@link #getValue()} is 0 or a hex-string representing the bytes contained
     * in that array.
     *
     * @param bytes the byte array to get the hex-string representation of
     *
     * @return a {@link String} representation of this options value
     */
    public static String toHexString(byte[] bytes) {
        if (bytes.length == 0)
            return "<empty>";
        else
            return "0x" + bytesToHex(bytes);
//            return "0x" + new BigInteger(1, bytes).toString(16).toUpperCase();
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
