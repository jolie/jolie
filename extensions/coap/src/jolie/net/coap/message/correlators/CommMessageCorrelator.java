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
	public void sendRequest( CommMessage in )
	{
		requests.putIfAbsent( in.id(), in );
	}

	/**
	As soon as a response is received by the { @link AsyncCommProtocol } 
	the { @link CommMessage } request is retrieved from the { @link ConcurrentHashMap }.
	@param id
	@return 
	 */
	public CommMessage receiveResponse( long key )
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
