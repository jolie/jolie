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
package de.uzl.itm.ncoap.communication.codec;

import java.net.InetSocketAddress;

/**
 * An {@link HeaderDecodingException} indicates that the header, i.e. the first 4 bytes of an inbound serialized
 * {@link de.uzl.itm.ncoap.message.CoapMessage} are malformed. This exception is thrown during the decoding process and causes an RST message
 * to be sent to the inbound message origin.
 *
 * @author Oliver Kleine
 */
public class HeaderDecodingException extends Exception{

    private int messageID;
    private InetSocketAddress remoteSocket;

    /**
     * Creates a new instance of {@link HeaderDecodingException}.
     *
     * @param messageID the message ID of the message that caused
     * @param remoteSocket the malformed message origin
     */
    public HeaderDecodingException(int messageID, InetSocketAddress remoteSocket, String message) {
        super(message);
        this.messageID = messageID;
        this.remoteSocket = remoteSocket;
    }

    /**
     * Returns the message ID of the inbound malformed message
     *
     * @return the message ID of the inbound malformed message
     */
    public int getMessageID() {
        return messageID;
    }

    /**
     * Returns the malformed inbound messages origin CoAP endpoints
     *
     * @return the malformed inbound messages origin CoAP endpoints
     */
    public InetSocketAddress getremoteSocket() {
        return remoteSocket;
    }
}
