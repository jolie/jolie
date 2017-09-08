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

import de.uzl.itm.ncoap.communication.dispatching.Token;

import java.net.InetSocketAddress;

import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.OptionValue;


/**
 * Instances of {@link OptionCodecException} are thrown if either encoding of decoding of a {@link de.uzl.itm.ncoap.message.CoapMessage}
 * failed because of an invalid option (for decoding only if the option was critical as non-critical options are
 * silently ignored).
 *
 * @author Oliver Kleine
 */
public class OptionCodecException extends Exception {

    private static final String message = "Unsupported or misplaced critical option %s";

    private int optionNumber;
    private int messageID;
    private Token token;
    private InetSocketAddress remoteSocket;
    private int messageType;

    /**
     * @param optionNumber the option number of the {@link OptionValue} that caused this exception
     */
    public OptionCodecException(int optionNumber) {
        super();
        this.optionNumber = optionNumber;
    }


    /**
     * Returns the number of the option that caused this exception
     *
     * @return the number of the option that caused this exception
     */
    public int getOptionNumber() {
        return optionNumber;
    }


    /**
     * Method to set the message ID of the message that caused this exception
     *
     * @param messageID the message ID of the message that caused this exception
     */
    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }


    /**
     * Returns the message ID of the message that caused this exception
     *
     * @return the message ID of the message that caused this exception
     */
    public int getMessageID() {
        return messageID;
    }


    /**
     * Method to set the {@link Token} of the message that caused this exception
     *
     * @param token the {@link Token} of the message that caused this exception
     */
    public void setToken(Token token) {
        this.token = token;
    }


    /**
     * Returns the {@link Token} of the message that caused this exception
     *
     * @return the {@link Token} of the message that caused this exception
     */
    public Token getToken() {
        return token;
    }


    /**
     * Method to set the remote CoAP endpoints of the message that caused this exception (the
     * remote CoAP endpoints is either the message origin if this exception was thrown by the
     * {@link CoapMessageDecoder} or he desired recipient if this exception was thrown by the
     * {@link CoapMessageEncoder}.
     *
     * @param remoteSocket the remote CoAP endpoints of the message that caused this exception
     */
    public void setremoteSocket(InetSocketAddress remoteSocket) {
        this.remoteSocket = remoteSocket;
    }


    /**
     * Returns the remote CoAP endpoints of the message that caused this exception (the
     * remote CoAP endpoints is either the message origin if this exception was thrown by the
     * {@link CoapMessageDecoder} or he desired recipient if this exception was thrown by the
     * {@link CoapMessageEncoder}.
     *
     * @return the remote CoAP endpoints of the message that caused this exception
     */
    public InetSocketAddress getremoteSocket() {
        return remoteSocket;
    }


    /**
     * Returns the number refering to the {@link de.uzl.itm.ncoap.message.MessageType} of the message that caused this exception
     *
     * @return the number refering to the {@link de.uzl.itm.ncoap.message.MessageType} of the message that caused this exception
     */
    public int getMessageType() {
        return messageType;
    }


    /**
     * Method to set the number refering to the {@link de.uzl.itm.ncoap.message.MessageType} of the message that caused this exception
     *
     * @param messageType the number refering to the {@link de.uzl.itm.ncoap.message.MessageType} of the message that caused this exception
     */
    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    @Override
    public String getMessage() {
        return String.format(message, Option.asString(this.optionNumber));
    }
}
