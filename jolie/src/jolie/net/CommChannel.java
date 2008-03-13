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
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

import jolie.Constants;
import jolie.Interpreter;
import jolie.runtime.InvalidIdException;


/** A communication channel permits to send and receive messages.
 * 
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
abstract public class CommChannel implements Channel
{
	protected boolean toBeClosed = true;
	//private SelectableChannel selectableChannel = null;

	public static CommChannel createCommChannel( URI uri, CommProtocol protocol )
		throws IOException, URISyntaxException
	{
		CommChannel channel = null;
		Constants.MediumId medium = Constants.stringToMediumId( uri.getScheme() );
		
		if ( medium == Constants.MediumId.SOCKET ) {
			SocketChannel socketChannel = 
						SocketChannel.open( new InetSocketAddress( uri.getHost(), uri.getPort() ) );
			channel = new SocketCommChannel( socketChannel, protocol );
		} else if ( medium == Constants.MediumId.PIPE ) {
			String id = uri.getSchemeSpecificPart();
			try {
				channel = Interpreter.getNewPipeChannel( id );
			} catch( InvalidIdException e ) {
				throw new IOException( e );
			}
		} else
			throw new IOException( "Unsupported communication medium: " + uri.getScheme() );
		
		return channel;
	}
	
	/** Receives a message from the channel. */
	abstract public CommMessage recv()
		throws IOException;
	
	/** Sends a message through the channel. */
	abstract public void send( CommMessage message )
		throws IOException;
	
	
	/*
	public SelectableChannel getSelectableChannel()
	{
		return selectableChannel;
	}*/
	
	/** Closes the communication channel */
	public void close()
		throws IOException
	{
		if ( toBeClosed )
			closeImpl();
	}
	
	public void setToBeClosed( boolean toBeClosed )
	{
		this.toBeClosed = toBeClosed;
	}

	/** Implements the communication channel closing operation. */
	abstract protected void closeImpl()
		throws IOException;
}