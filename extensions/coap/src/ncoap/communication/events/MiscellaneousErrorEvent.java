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

public class MiscellaneousErrorEvent extends AbstractMessageTransferEvent {

    private final String description;

    /**
     * Creates a new instance of {@link MiscellaneousErrorEvent}
     *
     * @param remoteSocket the remote socket of the transfer that caused this
     * event
     * @param messageID the message ID of the message that caused this event
     * @param token the {@link Token} of the message that caused this event
     * @param description a human readable description of the error that caused
     * this event
     */
    public MiscellaneousErrorEvent(InetSocketAddress remoteSocket, int messageID, Token token, String description) {
	super(remoteSocket, messageID, token);
	this.description = description;
    }

    /**
     * Returns a human readable description of the error that caused this event
     *
     * @return a human readable description of the error that caused this event
     */
    public String getDescription() {
	return this.description;
    }

//    @Override
//    public boolean stopsMessageExchange() {
//        return true;
//    }
    @Override
    public String toString() {
	return "MISCELLANEOUS MESSAGE EXCHANGE ERROR (remote endpoint: " + this.getRemoteSocket()
		+ ", message ID: " + this.getMessageID() + ", token: " + this.getToken() + ")";
    }

    public interface Handler {

	public void handleEvent(MiscellaneousErrorEvent event);
    }
}
