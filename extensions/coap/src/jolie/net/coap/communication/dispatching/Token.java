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
package jolie.net.coap.communication.dispatching;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLongs;

import java.util.Arrays;

/**
 * A {@link Token} is the identifier to relate {@link de.uzl.itm.ncoap.message.CoapRequest}s with {@link de.uzl.itm.ncoap.message.CoapResponse}s. It consists of a byte
 * array with a size between 0 and 8 (both inclusive). So, {@link Token} basically is a wrapper class for a byte array.
 *
 * The byte array content has no semantic meaning and thus, e.g. a {@link Token} instance backed by a byte
 * array containing a single zero byte (all bits set to 0) is different from a byte array backed by a byte array
 * containing two zero bytes.
 *
 * @author Oliver Kleine
 */
public class Token implements Comparable<Token>{

    public static int MAX_LENGTH = 8;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private byte[] token;

    /**
     * Creates a new {@link Token} instance.
     *
     * @param token the byte array this {@link Token} is supposed to consist of
     *
     * @throws java.lang.IllegalArgumentException if the length of the given byte array is larger than 8
     */
    public Token(byte[] token) {
        if (token.length > 8)
            throw new IllegalArgumentException("Maximum token length is 8 (but given length was " + token.length + ")");

        this.token = token;
    }

    /**
     * Returns the byte array this {@link Token} instance wraps
     * @return the byte array this {@link Token} instance wraps
     */
    public byte[] getBytes() {
        return this.token;
    }


    /**
     * Returns a representation of the token in form of a HEX string or "<EMPTY>" for tokens of length 0
     * @return a representation of the token in form of a HEX string or "<EMPTY>" for tokens of length 0
     */
    @Override
    public String toString() {
        String tmp = bytesToHex(getBytes());

        if (tmp.length() == 0)
            return "<EMPTY>";
        else
            return "0x" + tmp;
    }


    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || (!(object instanceof Token)))
            return false;

        Token other = (Token) object;
        return Arrays.equals(this.getBytes(), other.getBytes());
    }


    @Override
    public int hashCode() {
        return Arrays.hashCode(token);
    }


    @Override
    public int compareTo(Token other) {

        if (other.equals(this))
            return 0;

        if (this.getBytes().length < other.getBytes().length)
            return -1;

        if (this.getBytes().length > other.getBytes().length)
            return 1;

        return UnsignedLongs.compare(Longs.fromByteArray(Bytes.concat(this.getBytes(), new byte[8])),
                                     Longs.fromByteArray(Bytes.concat(other.getBytes(), new byte[8])));
    }
}
