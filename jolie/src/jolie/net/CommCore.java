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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import jolie.Constants;
import jolie.Interpreter;
import jolie.process.Process;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;

/** Handles networking communications.
 * The CommCore class represent the communication core of JOLIE.
 */
public class CommCore
{
	private Vector< CommListener > listeners = new Vector< CommListener >();

	private ThreadGroup threadGroup;

	private Logger logger = Logger.getLogger( "JOLIE" );

	private int connectionsLimit = -1;
	private Interpreter interpreter;

	public CommCore( Interpreter interpreter, int connectionsLimit )
	{
		this.interpreter = interpreter;
		this.connectionsLimit = connectionsLimit;
		this.threadGroup = new ThreadGroup( "CommCore-" + interpreter.hashCode() );
	}
	
	public Logger logger()
	{
		return logger;
	}
	
	public int connectionsLimit()
	{
		return connectionsLimit;
	}

	public ThreadGroup threadGroup()
	{
		return threadGroup;
	}
	
	private Collection< Process > protocolConfigurations = new Vector< Process > ();
	
	public Collection< Process > protocolConfigurations()
	{
		return protocolConfigurations;
	}
	
	/** Adds an input service.
	 * @param uri
	 * @param protocol
	 */
	public void addService(
				URI uri,
				Collection< InputPort > inputPorts,
				CommProtocol protocol,
				Process protocolConfigurationProcess,
				Map< String, OutputPort > redirectionMap
			)
		throws UnsupportedCommMediumException, IOException
	{
		if ( protocolConfigurationProcess != null )
			protocolConfigurations.add( protocolConfigurationProcess );

		CommListener listener = null;
		Constants.MediumId medium = Constants.stringToMediumId( uri.getScheme() );
		if ( medium.equals( Constants.MediumId.SOCKET ) ) {
			listener = new SocketListener( interpreter, protocol, uri.getPort(), inputPorts, redirectionMap );
		} else if ( medium.equals( Constants.MediumId.PIPE ) ) {
			listener = new PipeListener( interpreter, protocol, inputPorts, redirectionMap );
			Interpreter.registerPipeListener( uri.getSchemeSpecificPart(), (PipeListener)listener );
		} else
			throw new UnsupportedCommMediumException( uri.getScheme() );

		assert listener != null;
		listeners.add( listener );
	}
	
	private ExecutorService executorService;
	
	private class CommThreadFactory implements ThreadFactory {
		public Thread newThread( Runnable r )
		{
			return new CommChannelHandler( interpreter, r );
		}
	}

	private class CommChannelHandlerRunnable implements Runnable {
		private CommChannel channel;
		private CommListener listener;
		
		public CommChannelHandlerRunnable( CommChannel channel, CommListener listener )
		{
			this.channel = channel;
			this.listener = listener;
		}
		
		private void redirectMessage( CommMessage message )
			throws IOException
		{
			channel.redirectionChannel().send( message );
		}
		
		private void handleMessage( CommMessage message )
			throws IOException
		{
			try {
				String[] ss = message.resourcePath().split( "/" );
				if ( listener != null && ss.length > 1 ) {
					CommChannelHandler.currentThread().setExecutionThread( interpreter.mainThread() );
					// We should check for redirection
					OutputPort port = listener.redirectionMap().get( ss[1] );
					if ( port == null ) {
						Interpreter.getInstance().logger().warning(
								"Discarded a message for resource " + ss[1] +
								", not specified in the appropriate redirection table."
							);
					}
					CommChannel oChannel = port.getCommChannel( interpreter.mainThread().state().root() );
					String rPath = new String();
					if ( ss.length <= 2 )
						rPath = "/";
					else {
						for( int i = 2; i < ss.length; i++ ) {
							rPath += "/" + ss[ i ];
						}
					}
					CommMessage rMessage =
								new CommMessage(
										message.operationName(),
										rPath,
										message.value(),
										message.fault()
								);
					oChannel.send( rMessage );
					oChannel.setRedirectionChannel( channel );
					oChannel.disposeForInput();
				} else {
					InputOperation operation =
						interpreter.getInputOperation( message.operationName() );
					if ( listener != null && !listener.canHandleInputOperation( operation ) )
						Interpreter.getInstance().logger().warning(
								"Discarded a message for operation " + operation +
								", not specified in an input port at the receiving service."
							);
					else
						operation.recvMessage( channel, message );
				}
			} catch( InvalidIdException iie ) {
				iie.printStackTrace();
			} catch( URISyntaxException e ) {
				e.printStackTrace();
			}
		}
		
		public void run()
		{
			try {
				CommMessage message = channel.recv();
				if ( channel.redirectionChannel() == null )
					handleMessage( message );
				else
					redirectMessage( message );
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	public void scheduleReceive( CommChannel channel, CommListener listener )
	{
		executorService.execute( new CommChannelHandlerRunnable( channel, listener ) );
	}
	
	/** Initializes the communication core. */
	public void init()
		throws IOException
	{
		if ( connectionsLimit > 0 )
			executorService = Executors.newFixedThreadPool( connectionsLimit, new CommThreadFactory() );
		else
			executorService = Executors.newCachedThreadPool( new CommThreadFactory() );
		
		selectorThread = new SelectorThread();
		selectorThread.start();
		
		for( CommListener listener : listeners )
			listener.start();
	}
	
	private SelectorThread selectorThread;
	
	private class SelectorThread extends Thread {
		private Selector selector;
		public SelectorThread()
			throws IOException
		{
			this.selector = Selector.open();
		}
		
		public void run()
		{
			SocketCommChannel channel;
			InputStream stream;
			while( true ) {
				try {
					selector.select();
					synchronized( this ) {
						for( SelectionKey key : selector.selectedKeys() ) {
							channel = (SocketCommChannel)key.attachment();
							key.cancel();
							key.channel().configureBlocking( true );
							stream = channel.inputStream();
							stream.mark( 1 );
							// It could just be a closing read. If not, receive it.
							if ( stream.read() != -1 ) {
								stream.reset();
								scheduleReceive( channel, channel.parentListener() );
							}
						}
					}
				} catch( IOException ioe ) {
					// TODO Handle this properly
					//ioe.printStackTrace();
				}
			}
		}
		
		public void register( SocketCommChannel channel )
		{
			try {
				synchronized( this ) {
					selector.wakeup();
					SocketChannel socketChannel = channel.socketChannel();
					socketChannel.configureBlocking( false );
					socketChannel.register( selector, SelectionKey.OP_READ, channel );
				}
			} catch( ClosedChannelException cce ) {}
			catch( IOException ioe ) {}
		}
	}
	
	public void addToSelectionPool( SocketCommChannel channel )
	{
		selectorThread.register( channel );
	}

	/** Shutdowns the communication core, interrupting every communication-related thread. */
	public void shutdown()
	{
		threadGroup.interrupt();
	}
}
