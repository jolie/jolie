/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck											*
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/
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
