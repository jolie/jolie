/*
 * Copyright (C) 2016 Martin M. Andersen
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net;

import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import jolie.Interpreter;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class NioSocketListenerFactory extends CommListenerFactory
{
	protected final EventLoopGroup bossGroup;
	protected final EventLoopGroup workerGroup;

	public NioSocketListenerFactory( CommCore commCore, EventLoopGroup bossGroup, EventLoopGroup workerGroup )
	{
		super( commCore );
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
	}

	@Override
	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort
	)
		throws IOException
	{ 
		return new NioSocketListener( interpreter, protocolFactory, inputPort, bossGroup, workerGroup );
	}
}
