/*
 * The MIT License
 *
 * Copyright 2018 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
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
package jolie.net.coap.communication.timeout;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jolie.net.CommMessage;

public class CoapMessageReadTimeoutHandler extends ReadTimeoutHandler
{
	private boolean closed;
	private final CommMessage request;
	private final int timeout;

	public CoapMessageReadTimeoutHandler( int timeout, CommMessage in )
	{
		super( timeout );
		this.timeout = timeout;
		this.request = in;
	}

	public CommMessage getRequest()
	{
		return request;
	}

	public int getTimeout()
	{
		return timeout;
	}

	@Override
	protected void readTimedOut( ChannelHandlerContext ctx ) throws Exception
	{
		if ( !closed ) {
			ctx.fireExceptionCaught( new CoapMessageReadTimeoutException( request.id(), timeout ) );
			ctx.close();
			closed = true;
		}
	}

}
