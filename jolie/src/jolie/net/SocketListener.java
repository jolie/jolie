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
import java.net.ServerSocket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import jolie.Interpreter;
import jolie.deploy.InputPort;

public class SocketListener extends CommListener
{
	private ServerSocketChannel serverChannel;

	public SocketListener( Interpreter interpreter, CommProtocol protocol, int port, Collection< InputPort > inputPorts )
		throws IOException
	{
		super( interpreter, protocol, inputPorts );
		
		serverChannel = ServerSocketChannel.open();
		ServerSocket socket = serverChannel.socket();
		socket.bind( new InetSocketAddress( port ) );
	}
	
	public void run()
	{
		try {
			SocketChannel socketChannel;
			CommChannel channel;
			while ( (socketChannel = serverChannel.accept()) != null ) {
				channel = new SocketCommChannel(
							socketChannel,
							createProtocol() );
				
				interpreter().commCore().scheduleReceive( channel, this );
				channel = null; // Dispose for garbage collection
			}
			serverChannel.close();
		} catch( ClosedByInterruptException ce ) {
			try {
				serverChannel.close();
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}
