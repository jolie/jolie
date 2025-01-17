/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;

/**
 * A <code>CommChannelFactory</code> using TCP/IP sockets as backend.
 *
 * @author Fabrizio Montesi
 */
public class SocketCommChannelFactory extends CommChannelFactory {
	public SocketCommChannelFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port )
		throws IOException {

		SocketChannel channel = null;
		try {
			channel = SocketChannel.open( new InetSocketAddress( location.getHost(), location.getPort() ) );
		} catch( UnresolvedAddressException e ) {
			throw new IOException( "Unable to resolve address: " + location.toString(), e );
		}
		SocketCommChannel ret = null;
		try {
			ret = new SocketCommChannel( channel, location, port.getProtocol() );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
		return ret;
	}
}
