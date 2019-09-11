/*
 * Copyright (C) 2015 Martin Wolf <mw@martinwolf.eu>
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

import java.io.IOException;
import java.net.URI;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;

public class LocalCommChannelFactory extends CommChannelFactory
{
	public LocalCommChannelFactory( CommCore commCore )
	{
		super( commCore );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port )
		throws IOException
	{
		LocalListener localListener = LocalListenerFactory.getListener( location.getHost() );
		if ( localListener == null ) {
			throw new IOException( "Channel does not exist." );
		}
		return new LocalCommChannel( localListener.interpreter(), localListener );
	}
}
