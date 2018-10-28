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
package jolie.net.coap.message.correlators;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jolie.net.coap.message.CoapMessage;

public class CoapMessageCorrelator
{

	// Ensure that ONLY ONE ConcurrentHashMap is used for every thread
	private static final Map<Integer, CoapMessage> requests = new ConcurrentHashMap<>();

	/**
	To be called in case of receiving a { @link CoapMessage } request.
	This has to be sent to { @link CommCore }, hence stored in a { @link ConcurrentHashMap }.
	@param in 
	 */
	public void receiveRequest( int key, CoapMessage in )
	{
		requests.putIfAbsent( key, in );
	}

	/**
	As soon as a response is received from the { @link CommCore },
	the { @link CoapMessage } request is retrieved from the { @link ConcurrentHashMap }
	and removed.
	@param id
	@return the { @link CoapMessage } from the { @link ConcurrentHashMap }
	 */
	public CoapMessage sendResponse( int key )
	{
		return sendResponse( key, true );
	}

	/**
	As soon as a response is received from the { @link CommCore },
	the { @link CoapMessage } request is retrieved from the { @link ConcurrentHashMap },
	removed or not depending on the boolean parameter.
	@param key
	@param remove
	@return the { @link CoapMessage } from the { @link ConcurrentHashMap }
	 */
	public CoapMessage sendResponse( int key, boolean remove )
	{
		return remove ? requests.remove( key ) : requests.get( key );
	}

}
