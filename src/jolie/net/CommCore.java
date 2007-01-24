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

import java.net.ServerSocket;
import java.nio.channels.*;
import java.io.IOException;
import java.net.InetSocketAddress;


class SocketListener extends CommListener
{
	private ServerSocketChannel serverChannel;

	public SocketListener( ThreadGroup threadGroup, CommProtocol protocol )
		throws IOException
	{
		super( threadGroup, protocol );
		
		serverChannel = ServerSocketChannel.open();
		ServerSocket socket = serverChannel.socket();
		socket.bind( new InetSocketAddress( CommCore.port() ) );
	}
	
	public void run()
	{
		try {
			SocketChannel socketChannel;
			CommChannel channel;
			while ( (socketChannel = serverChannel.accept()) != null ) {
				channel = new CommChannel(
							socketChannel.socket().getInputStream(),
							socketChannel.socket().getOutputStream(),
							protocol() );
				(new CommChannelHandler( getThreadGroup(), channel )).start();
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

/** Handles the networking communications.
 * The CommCore class represent the communication core of JOLIE.
 */
public class CommCore
{
	private static int port = 2555;
	
	private static ThreadGroup threadGroup = new ThreadGroup( "CommCoreGroup" );
	
	private CommCore(){}
	
	/** Sets the TCP/IP port to listen for incoming network messages. */
	public static void setPort( int commPort )
	{
		port = commPort;
	}
	
	/** Returns the TCP/IP port the CommCore is listening to.
	 * 
	 * @return the TCP/IP port the CommCore is listening to.
	 */
	public static int port()
	{
		return port;
	}

	/** Initializes the communication core. */
	public static void init()
		throws IOException
	{
		SocketListener listener = new SocketListener( threadGroup, new SODEPProtocol() );
		//SocketListener listener = new SocketListener( threadGroup, new SOAPProtocol() );
		listener.start();
	}

	/** Returns the current communication channel, if any. 
	 * 
	 * @return the current communication channel, null otherwise.
	 */
	public static CommChannel currentCommChannel()
	{
		CommChannel channel = null;
		Thread th = Thread.currentThread();
		if ( th instanceof CommChannelHandler )
			channel = ((CommChannelHandler)th).commChannel();
		
		return channel;
	}
	
	/** Shutdowns the communication core, interrupting every communication-related thread. */
	public static void shutdown()
	{
		threadGroup.interrupt();
	}
}
