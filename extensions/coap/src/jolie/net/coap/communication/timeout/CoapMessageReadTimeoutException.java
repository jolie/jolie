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

import io.netty.channel.ChannelException;

public final class CoapMessageReadTimeoutException extends ChannelException
{

	private static final long serialVersionUID = 169287984113283421L;

	public static final CoapMessageReadTimeoutException INSTANCE = new CoapMessageReadTimeoutException();
	private long id;
	private int timeout;

	private CoapMessageReadTimeoutException()
	{
	}

	CoapMessageReadTimeoutException( long id, int timeout )
	{
		this.id = id;
		this.timeout = timeout;
	}

	@Override
	public Throwable fillInStackTrace()
	{
		return this;
	}

	public long getId()
	{
		return id;
	}

	public int getTimeout()
	{
		return timeout;
	}

}
