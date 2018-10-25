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
package jolie.net.coap.correlator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jolie.net.CommMessage;

public class CommMessageCorrelator
{

	// Ensure that ONLY ONE ConcurrentHashMap is used for every thread
	private static final Map<Long, CommMessage> requests = new ConcurrentHashMap<>();

	/**
	To be called in case of receiving a { @link CommMessage } from 
	the { @link CommCore } to be sent as an { @link AsyncCommProtocol }
	request. It stores the { @link CommMessage } in a { @link ConcurrentHashMap }
	securely.
	@param in 
	 */
	public void sendProtocolRequest( CommMessage in )
	{
		requests.putIfAbsent( in.id(), in );
	}

	/**
	As soon as a response is received by the { @link AsyncCommProtocol } 
	the { @link CommMessage } request is retrieved from the { @link ConcurrentHashMap }.
	@param id
	@return 
	 */
	public CommMessage receiveProtocolResponse( long key )
	{
		return receiveProtocolResponse( key, true );
	}

	/**
	As soon as a response is received by the { @link AsyncCommProtocol } 
	the { @link CommMessage } request is retrieved from the { @link ConcurrentHashMap }
	and <b>removed</b> from it.
	@param id
	@return 
	 */
	public CommMessage receiveProtocolResponse( long key, boolean remove )
	{
		return remove ? requests.remove( key ) : requests.get( key );
	}

}
