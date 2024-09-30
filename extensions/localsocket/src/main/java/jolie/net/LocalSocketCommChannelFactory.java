/*
 * Copyright (C) 2007-2024 Fabrizio Montesi <famontesi@gmail.com>
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
import java.net.URISyntaxException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.InvalidPathException;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;

/**
 * A <code>CommChannelFactory</code> using local Unix sockets as backend.
 *
 * @author Fabrizio Montesi
 */
public class LocalSocketCommChannelFactory extends CommChannelFactory {
	public LocalSocketCommChannelFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port )
		throws IOException {
		try {
			SocketChannel channel =
				SocketChannel.open( UnixDomainSocketAddress.of( LocalSocketUtils.pathFromLocation( location ) ) );
			return new SocketCommChannel( channel, location, port.getProtocol() );
		} catch( InvalidPathException | URISyntaxException e ) {
			throw new IOException( e );
		}
	}
}
