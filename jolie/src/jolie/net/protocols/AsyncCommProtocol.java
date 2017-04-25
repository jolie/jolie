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

package jolie.net.protocols;

import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jolie.StatefulContext;
import jolie.net.CommMessage;
import jolie.runtime.VariablePath;

public abstract class AsyncCommProtocol extends CommProtocol
{

	public AsyncCommProtocol( VariablePath configurationPath )
	{
		super( configurationPath );
	}

	abstract public void setupPipeline( ChannelPipeline pipeline );
	
	public void setupWrapablePipeline( ChannelPipeline pipeline ) {
		setupPipeline( pipeline );
	};

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}
	
	public void initialize(StatefulContext ctx) {
		// Do nothing.
	}

}
