/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck                     *
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
