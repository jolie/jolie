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
