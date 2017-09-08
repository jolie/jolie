/*
 * The MIT License
 *
 * Copyright 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ncoap.communication.events;

import java.net.InetSocketAddress;
import ncoap.communication.dispatching.Token;

public abstract class AbstractMessageTransferEvent extends AbstractMessageExchangeEvent {

    private int messageID;

    /**
     * Creates a new instance of {@link AbstractMessageTransferEvent}
     *
     * @param remoteSocket the remote socket of the transfer that caused this
     * event
     * @param messageID the message ID of the transfer that caused this event
     * @param token the {@link Token} of the of the transfer that caused this
     * event
     */
    public AbstractMessageTransferEvent(InetSocketAddress remoteSocket, int messageID, Token token) {
	super(remoteSocket, token);
	this.messageID = messageID;
    }

    /**
     * Returns the message ID of the message that caused this events
     *
     * @return the message ID of the message that caused this events
     */
    public int getMessageID() {
	return messageID;
    }

//    /**
//     * Returns <code>true</code> if this events causes the related message exchange to stop and <code>false</code>
//     * otherwise
//     * @return <code>true</code> if this events causes the related message exchange to stop and <code>false</code>
//     * otherwise
//     */
//    public abstract boolean stopsMessageExchange();
}
