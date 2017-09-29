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
package ncoap.communication.events;

import ncoap.communication.dispatching.Token;

import java.net.InetSocketAddress;

/**
 * Instances of {@link MessageIDAssignedEvent} are sent upstream if a message ID was assigned to an outbound
 * message.
 *
 * @author Oliver Kleine
 */
public class MessageIDAssignedEvent extends AbstractMessageTransferEvent {

    /**
     * Creates a new instance of {@link MessageIDAssignedEvent}
     *
     * @param remoteSocket the socket of the recipient of the message that was assigned a message ID
     * @param messageID the message ID that was assigned
     * @param token the {@link Token} of the message that was assigned a message ID
     */
    public MessageIDAssignedEvent(InetSocketAddress remoteSocket, int messageID, Token token) {
        super(remoteSocket, messageID, token);
    }



    @Override
    public String toString() {
        return "MESSAGE ID " + this.getMessageID() + " ASSIGNED (remote endpoint: " + this.getRemoteSocket() +
                ", token: " + this.getToken() + ")";
    }

    public interface Handler {
        public void handleEvent(MessageIDAssignedEvent event);
    }
}
