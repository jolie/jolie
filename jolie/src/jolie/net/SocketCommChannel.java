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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

import jolie.Interpreter;

public class SocketCommChannel extends StreamingCommChannel
{
	final private SocketChannel socketChannel;
	final private InputStream istream;
	final private OutputStream ostream;
	
	/** Constructor.
	 * 
	 * @param istream the channel input stream.
	 * @param ostream the channel output stream.
	 * @param protocol the protocol to use to send and receive messages.
	 */
	public SocketCommChannel( SocketChannel socketChannel, CommProtocol protocol )
		throws IOException
	{
		super( protocol );
		this.socketChannel = socketChannel;
		this.istream = new BufferedInputStream( socketChannel.socket().getInputStream() );
		this.ostream = socketChannel.socket().getOutputStream();

		toBeClosed = false; // Socket connections are kept open by default
	}
	
	public SocketChannel socketChannel()
	{
		return socketChannel;
	}
	
	protected InputStream inputStream()
	{
		return istream;
	}
	
	/** Receives a message from the channel. */
	public CommMessage recv()
		throws IOException
	{
		return protocol.recv( istream );
	}
	
	/** Sends a message through the channel. */
	public void send( CommMessage message )
		throws IOException
	{
		protocol.send( ostream, message );
	}
	
	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForSelection( this );
	}

	/** Closes the communication channel */
	protected void closeImpl()
		throws IOException
	{
		socketChannel.close();
	}
}