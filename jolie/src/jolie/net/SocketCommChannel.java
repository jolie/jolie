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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import jolie.net.protocols.CommProtocol;


/**
 * A CommChannel using a socket to implement communications.
 * @author Fabrizio Montesi
 */
public class SocketCommChannel extends SelectableStreamingCommChannel
{
	final private SocketChannel socketChannel;
	final private InputStream istream;
	final private OutputStream ostream;
	
	/** Constructor.
	 * 
	 * @param socketChannel the SocketChannel underlying this SocketCommChannel
	 * @param protocol the CommProtocol to use to send and receive messages
	 * @see CommProtocol
	 * @see SocketChannel
	 */
	public SocketCommChannel( SocketChannel socketChannel, URI location, CommProtocol protocol )
		throws IOException
	{
		super( location, protocol );
		this.socketChannel = socketChannel;
		this.istream = new BufferedInputStream( socketChannel.socket().getInputStream() );
		this.ostream = socketChannel.socket().getOutputStream();

		setToBeClosed( false ); // Socket connections are kept open by default
	}
	
	/**
	 * Returns the SocketChannel underlying this SocketCommChannel
	 * @return the SocketChannel underlying this SocketCommChannel
	 */
	public SelectableChannel selectableChannel()
	{
		return socketChannel;
	}
	
	public InputStream inputStream()
	{
		return istream;
	}
	
	/**
	 * Receives a message from the channel.
	 * @return the received CommMessage
	 * @see CommMessage
	 */
	protected CommMessage recvImpl()
		throws IOException
	{
		return protocol().recv( istream, ostream );
	}
	
	/**
	 * Sends a message through the channel.
	 * @param message the CommMessage to send
	 * @see CommMessage
	 * @throws IOException if an error sending the message occurs
	 */
	protected void sendImpl( CommMessage message )
		throws IOException
	{
		protocol().send( ostream, message, istream );
		ostream.flush();
	}

	protected void closeImpl()
		throws IOException
	{
		socketChannel.close();
	}

	@Override
	protected boolean isOpenImpl()
	{
		return socketChannel.isOpen();
	}
}