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

import jolie.net.ports.OutputPort;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.net.ext.CommChannelFactory;
import jolie.runtime.AndJarDeps;

@AndJarDeps( { "cx.ath.matthew.jar" } )
public class LocalSocketCommChannelFactory extends CommChannelFactory {
	public LocalSocketCommChannelFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port )
		throws IOException {
		String path = location.getPath();
		if( path == null || path.isEmpty() ) {
			throw new FileNotFoundException( "Local socket path not specified!" );
		}
		UnixSocket socket = new UnixSocket(
			new UnixSocketAddress( path, location.getHost() != null && location.getHost().equals( "abs" ) ) );
		CommChannel ret = null;
		try {
			ret = new LocalSocketCommChannel( socket, location, port.getProtocol() );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
		return ret;
	}
}
