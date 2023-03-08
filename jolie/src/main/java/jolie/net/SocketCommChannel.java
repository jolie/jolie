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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;
import jolie.util.Helpers;


/**
 * A CommChannel using a socket to implement communications.
 * 
 * @author Fabrizio Montesi
 */
public class SocketCommChannel extends SelectableStreamingCommChannel {
	private final SocketChannel socketChannel;
	private final PreBufferedInputStream istream;
	private final OutputStream ostream;

	private static final int SO_LINGER = 10000;

	/**
	 * Constructor.
	 * 
	 * @param socketChannel the SocketChannel underlying this SocketCommChannel
	 * @param location the location for this channel
	 * @param protocol the CommProtocol to use to send and receive messages
	 * @throws java.io.IOException
	 * @see CommProtocol
	 * @see SocketChannel
	 */
	public SocketCommChannel( SocketChannel socketChannel, URI location, CommProtocol protocol )
		throws IOException {
		super( location, protocol );
		this.socketChannel = socketChannel;
		socketChannel.socket().setSoLinger( true, SO_LINGER );
		// this.istream = new PreBufferedInputStream( new BufferedInputStream( Channels.newInputStream(
		// socketChannel ) ) );
		this.istream = new PreBufferedInputStream( Channels.newInputStream( socketChannel ) );
		this.ostream = new BufferedOutputStream( Channels.newOutputStream( socketChannel ) );
		setToBeClosed( false ); // Socket connections are kept open by default
	}

	/**
	 * Returns the SocketChannel underlying this SocketCommChannel
	 * 
	 * @return the SocketChannel underlying this SocketCommChannel
	 */
	@Override
	public SelectableChannel selectableChannel() {
		return socketChannel;
	}

	@Override
	public InputStream inputStream() {
		return istream;
	}

	/**
	 * Receives a message from the channel.
	 * 
	 * @return the received CommMessage
	 * @throws java.io.IOException
	 * @see CommMessage
	 */
	@Override
	protected CommMessage recvImpl()
		throws IOException {
		try {
			return protocol().recv( istream, ostream );
		} catch( IllegalBlockingModeException e ) {
			throw new IOException( e );
		}
	}

	/**
	 * Sends a message through the channel.
	 * 
	 * @param message the CommMessage to send
	 * @see CommMessage
	 * @throws IOException if an error sending the message occurs
	 */
	@Override
	protected void sendImpl( CommMessage message )
		throws IOException {
		try {
			protocol().send( ostream, message, istream );
			ostream.flush();
		} catch( IllegalBlockingModeException e ) {
			throw new IOException( e );
		}
	}

	@Override
	protected void closeImpl()
		throws IOException {
		final Interpreter interpreter = Interpreter.getInstance();
		if( interpreter != null && interpreter.commCore().isSelecting( this ) ) {
			interpreter.commCore().unregisterForSelection( this );
		}
		socketChannel.close();
	}

	private final ByteBuffer buffer = ByteBuffer.allocateDirect( 1024 );

	private boolean _isOpenImpl()
		throws IOException {
		buffer.clear();

		final boolean wasBlocking = socketChannel.isBlocking();

		if( wasBlocking ) {
			socketChannel.configureBlocking( false );
		}
		final int read;
		try {
			read = socketChannel.read( buffer );
		} catch( IOException e ) {
			/*
			 * This should never happen in non Windows systems. In Windows systems an IOException is thrown
			 * whenever a client has closed its output connection towards this channel and this method is called
			 * by CommCore.
			 */
			return false;
		}
		if( wasBlocking ) {
			socketChannel.configureBlocking( true );
		}
		if( read == -1 ) {
			return false;
		} else if( read > 0 ) {
			buffer.limit( read );
			buffer.rewind();
			istream.append( buffer );
		}
		return true;
	}

	@Override
	protected boolean isOpenImpl() {
		if( socketChannel.isConnected() == false || socketChannel.isOpen() == false ) {
			return false;
		}

		final boolean ret;
		try {
			ret = Helpers.lockAndThen( rwLock, this::_isOpenImpl );
		} catch( IOException e ) {
			Interpreter.getInstance().logWarning( e );
			return false;
		}
		return ret;
	}
}
