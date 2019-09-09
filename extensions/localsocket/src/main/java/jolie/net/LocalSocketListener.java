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

import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.channels.ClosedByInterruptException;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class LocalSocketListener extends CommListener
{
	final private UnixServerSocket serverSocket;
	final private UnixSocketAddress socketAddress;
	public LocalSocketListener(
				Interpreter interpreter,
				CommProtocolFactory protocolFactory,
				InputPort inputPort
			)
		throws IOException
	{
		super( interpreter, protocolFactory, inputPort );

		String path = inputPort.location().getPath();
		if ( path == null || path.isEmpty() ) {
			throw new FileNotFoundException( "Local socket path not specified!" );
		}
		socketAddress = new UnixSocketAddress( path, inputPort.location().getHost() != null ? inputPort.location().getHost().equals( "abs" ) : false );
		serverSocket = new UnixServerSocket( socketAddress );
	}
	
	@Override
	public void shutdown()
	{
		if ( !socketAddress.isAbstract() ) {
			new File( socketAddress.getPath() ).delete();
		}
	}

	@Override
	public void run()
	{
		try {
			UnixSocket socket;
			CommChannel channel;
			while ( (socket = serverSocket.accept()) != null ) {
				channel = new LocalSocketCommChannel(
								socket,
								inputPort().location(),
								createProtocol()
							);
				channel.setParentInputPort( inputPort() );
				interpreter().commCore().scheduleReceive( channel, inputPort() );
				channel = null; // Dispose for garbage collection
			}
			serverSocket.close();
		} catch( ClosedByInterruptException ce ) {
			try {
				serverSocket.close();
			} catch( IOException e ) {
				e.printStackTrace();
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}
