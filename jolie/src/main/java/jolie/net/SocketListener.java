/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.net.ServerSocket;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

/**
 * A communication listener using sockets as backend.
 *
 * @author Fabrizio Montesi
 */
public class SocketListener extends CommListener {
	private final ServerSocketChannel serverChannel;

	public SocketListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException {
		super(
			interpreter,
			protocolFactory,
			inputPort );

		serverChannel = ServerSocketChannel.open();
		final ServerSocket socket = serverChannel.socket();
		try {
			socket.bind( new InetSocketAddress( inputPort.location().getPort() ) );
		} catch( IOException e ) {
			final IOException exception =
				new IOException( e.getMessage() + " [with location: " + inputPort.location().toString() + "]" );
			exception.setStackTrace( e.getStackTrace() );
			throw exception;
		}
	}

	@Override
	public void onShutdown() {
		if( serverChannel.isOpen() ) {
			try {
				serverChannel.close();
			} catch( IOException e ) {
			}
		}
	}

	@Override
	public void run() {
		try {
			SocketChannel socketChannel;
			while( (socketChannel = serverChannel.accept()) != null ) {
				final CommChannel channel = new SocketCommChannel(
					socketChannel,
					inputPort().location(),
					createProtocol() );
				channel.setParentInputPort( inputPort() );
				interpreter().commCore().scheduleReceive( channel, inputPort() );
			}
		} catch( ClosedByInterruptException e ) {
			try {
				serverChannel.close();
			} catch( IOException ioe ) {
				interpreter().logWarning( ioe );
			}
		} catch( AsynchronousCloseException e ) {
			// Closed by CommCore shutdown
		} catch( IOException e ) {
			interpreter().logWarning( e );
		}
	}
}
