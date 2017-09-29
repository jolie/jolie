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
package ncoap.communication.identification;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLongs;
import ncoap.communication.dispatching.Token;

import java.util.Arrays;

/**
 * Created by olli on 31.08.15.
 */
public class EndpointID implements Comparable<EndpointID>{

    public static final int MAX_LENGTH = 4;

    private byte[] bytes;

    /**
     * Creates a new {@link ncoap.communication.identification.EndpointID}
     * instance.
     *
     * @param bytes the byte array this
     * {@link ncoap.communication.identification.EndpointID} is     * supposed to consist of
     * @throws IllegalArgumentException if the length of the given byte array is larger than 8
     */
    public EndpointID(byte[] bytes) {
        if (bytes.length > MAX_LENGTH) {
            throw new IllegalArgumentException("Maximum endpoint ID length is 4 (but given length was " +
                    bytes.length + ")");
        }
       this.bytes = bytes;
    }

    /**
     * Returns the byte array this {@link Token} instance wraps
     * @return the byte array this {@link Token} instance wraps
     */
    public byte[] getBytes() {
        return this.bytes;
    }

    /**
     * Returns a representation of the token in form of a HEX string or "<EMPTY>" for tokens of length 0
     * @return a representation of the token in form of a HEX string or "<EMPTY>" for tokens of length 0
     */
    @Override
    public String toString() {
        String tmp = Token.bytesToHex(getBytes());

        if (tmp.length() == 0)
            return "<EMPTY>";
        else
            return "0x" + tmp;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || (!(object instanceof EndpointID)))
            return false;

        EndpointID other = (EndpointID) object;
        return Arrays.equals(this.getBytes(), other.getBytes());
    }

    @Override
    public int compareTo(EndpointID other) {

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
