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
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class LocalSocketListener extends CommListener {
	private final ServerSocketChannel serverChannel;
	private final Path socketPath;

	public LocalSocketListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException {
		super(
			interpreter,
			protocolFactory,
			inputPort );

		serverChannel = ServerSocketChannel.open( StandardProtocolFamily.UNIX );
		try {
			socketPath = Path.of( inputPort.location().getSchemeSpecificPart() );
			serverChannel.bind( UnixDomainSocketAddress.of( socketPath ) );
		} catch( InvalidPathException | IOException e ) {
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
		try {
			Files.deleteIfExists( socketPath );
		} catch( IOException e ) {
			interpreter().logWarning( e );
		}
	}

	// TODO (dedup): This is actually the same as in SocketListener.java.
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
