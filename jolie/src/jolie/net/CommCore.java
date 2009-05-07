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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.process.InputProcessExecution;
import jolie.process.Process;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.VariablePath;

/** 
 * Handles the communications mechanisms for an Interpreter instance.
 * 
 * Each CommCore is related to an Interpreter, and each Interpreter owns one and only CommCore instance.
 */
public class CommCore
{
	final private Map< String, CommListener > listenersMap = new HashMap< String, CommListener >();

	final private ThreadGroup threadGroup;

	final private Logger logger = Logger.getLogger( "JOLIE" );

	final private int connectionsLimit;
	final private Interpreter interpreter;

	final private Map< URI, Map< String, CommChannel > > persistentChannels =
			new HashMap< URI, Map< String, CommChannel > >();

	public CommChannel getPersistentChannel( URI location, String protocol )
	{
		CommChannel ret = null;
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if ( protocolChannels != null ) {
				ret = protocolChannels.get( protocol );
				if ( ret != null ) {
					if ( ret.isOpen() ) {
						/*
						 * We are going to return this channel, but first
						 * check if it supports concurrent use.
						 * If not, then others should not access this until
						 * the caller is finished using it.
						 */
						if ( ret.isThreadSafe() == false ) {
							protocolChannels.remove( protocol );
							if ( protocolChannels.isEmpty() ) {
								persistentChannels.remove( location );
							}
						}
					} else {
						// If the channel is closed, remove it and return null
						protocolChannels.remove( protocol );
						if ( protocolChannels.isEmpty() ) {
							persistentChannels.remove( location );
						}
						return null;
					}
				}
			}
		}
		return ret;
	}

	public void putPersistentChannel( URI location, String protocol, CommChannel channel )
	{
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if ( protocolChannels == null ) {
				protocolChannels = new HashMap< String, CommChannel >();
				persistentChannels.put( location, protocolChannels );
			}
			if ( protocolChannels.containsKey( protocol ) == false ) {
				protocolChannels.put( protocol, channel );
				// TODO implement channel garbage collection
			}
		}
	}

	/**
	 * Returns the Interpreter instance this CommCore refers to.
	 * @return the Interpreter instance this CommCore refers to
	 */
	public Interpreter interpreter()
	{
		return interpreter;
	}

	/**
	 * Constructor.
	 * @param interpreter the Interpreter to refer to for this CommCore operations
	 * @param connectionsLimit if more than zero, specifies an upper bound to the connections handled in parallel.
	 * @throws java.io.IOException
	 */
	public CommCore( Interpreter interpreter, int connectionsLimit )
		throws IOException
	{
		this.interpreter = interpreter;
		this.localListener = new LocalListener( interpreter );
		this.connectionsLimit = connectionsLimit;
		this.threadGroup = new ThreadGroup( "CommCore-" + interpreter.hashCode() );
		if ( connectionsLimit > 0 ) {
			executorService = Executors.newFixedThreadPool( connectionsLimit, new CommThreadFactory() );
		} else {
			executorService = Executors.newCachedThreadPool( new CommThreadFactory() );
		}
				
		//TODO make socket an extension, too?
		CommListenerFactory listenerFactory = new SocketListenerFactory( this );
		listenerFactories.put( "socket", listenerFactory );
		CommChannelFactory channelFactory = new SocketCommChannelFactory( this );
		channelFactories.put( "socket", channelFactory );
	}
	
	/**
	 * Returns the Logger used by this CommCore.
	 * @return the Logger used by this CommCore
	 */
	public Logger logger()
	{
		return logger;
	}
	
	/**
	 * Returns the connectionsLimit of this CommCore.
	 * @return the connectionsLimit of this CommCore
	 */
	public int connectionsLimit()
	{
		return connectionsLimit;
	}

	public ThreadGroup threadGroup()
	{
		return threadGroup;
	}
	
	final private Collection< Process > protocolConfigurations = new Vector< Process > ();
	
	public Collection< Process > protocolConfigurations()
	{
		return protocolConfigurations;
	}
	
	public CommListener getListenerByInputPortName( String serviceName )
	{
		return listenersMap.get( serviceName );
	}
	
	final private Map< String, CommChannelFactory > channelFactories = 
						new HashMap< String, CommChannelFactory > ();

	private CommChannelFactory getCommChannelFactory( String name )
		throws IOException
	{
		CommChannelFactory factory = channelFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommChannelFactory( name, this );
			if ( factory != null ) {
				channelFactories.put( name, factory );
			}
		}
		return factory;
	}

	public CommChannel createCommChannel( URI uri, OutputPort port )
		throws IOException
	{
		String medium = uri.getScheme();
		CommChannelFactory factory = getCommChannelFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}
		
		return factory.createChannel( uri, port );
	}
	
	final private Map< String, CommProtocolFactory > protocolFactories = 
						new HashMap< String, CommProtocolFactory > ();
	
	public CommProtocolFactory getCommProtocolFactory( String name )
		throws IOException
	{
		CommProtocolFactory factory = protocolFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommProtocolFactory( name, this );
			if ( factory != null ) {
				protocolFactories.put( name, factory );
			}
		}
		return factory;
	}
	
	public CommProtocol createCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException
	{
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if ( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}
		
		return factory.createProtocol( configurationPath, uri );
	}
	
	final private Map< String, CommListenerFactory > listenerFactories = 
						new HashMap< String, CommListenerFactory > ();

	final private LocalListener localListener;

	private CommListenerFactory getCommListenerFactory( String name )
		throws IOException
	{
		CommListenerFactory factory = listenerFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommListenerFactory( name, this );
			if ( factory != null ) {
				listenerFactories.put( name, factory );
			}
		}
		return factory;
	}
	
	public LocalListener localListener()
	{
		return localListener;
	}
	
	public void addLocalInputPort(
				String inputPortName,
				Collection< String > operationNames,
				Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		localListener.addOperationNames( operationNames );
		localListener.addRedirections( redirectionMap );
		listenersMap.put( inputPortName, localListener );
	}
	
	public void addInputPort(
				String inputPortName,
				URI uri,
				CommProtocolFactory protocolFactory,
				VariablePath protocolConfigurationPath,
				Process protocolConfigurationProcess,
				Collection< String > operationNames,
				Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		if ( protocolConfigurationProcess != null ) {
			protocolConfigurations.add( protocolConfigurationProcess );
		}

		CommListener listener = null;
		String medium = uri.getScheme();
		CommListenerFactory factory = getCommListenerFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}

		listener = factory.createListener(
			interpreter,
			uri,
			protocolFactory,
			protocolConfigurationPath,
			operationNames,
			redirectionMap
		);
		listenersMap.put( inputPortName, listener );
	}
	
	final private ExecutorService executorService;
	
	private class CommThreadFactory implements ThreadFactory {
		public Thread newThread( Runnable r )
		{
			return new CommChannelHandler( interpreter, r );
		}
	}

	private static Pattern pathSplitPattern = Pattern.compile( "/" );

	private class CommChannelHandlerRunnable implements Runnable {
		final private CommChannel channel;
		final private CommListener listener;
		
		public CommChannelHandlerRunnable( CommChannel channel, CommListener listener )
		{
			this.channel = channel;
			this.listener = listener;
		}
		
		private void redirectMessage( CommMessage message )
			throws IOException
		{
			channel.redirectionChannel().send( message );
			channel.redirectionChannel().disposeForInput();
			channel.setRedirectionChannel( null );
			channel.closeImpl();
		}

		private void handleMessage( CommMessage message )
			throws IOException
		{
			try {
				String[] ss = pathSplitPattern.split( message.resourcePath() );
				if ( ss.length > 1 && listener != null ) {
					// Redirection
					String rPath = new String();
					if ( ss.length <= 2 ) {
						rPath = "/";
					} else {
						for( int i = 2; i < ss.length; i++ ) {
							rPath += "/" + ss[ i ];
						}
					}
					OutputPort port = listener.redirectionMap().get( ss[1] );
					if ( port == null ) {
						String error = "Discarded a message for resource " + ss[1] +
								", not specified in the appropriate redirection table.";
						interpreter.logWarning( error );
						throw new IOException( error );
					}
					CommChannel oChannel = port.getNewCommChannel();
					CommMessage rMessage =
								new CommMessage(
										message.id(),
										message.operationName(),
										rPath,
										message.value(),
										message.fault()
								);
					oChannel.setRedirectionChannel( channel );
					try {
						oChannel.send( rMessage );
						oChannel.setToBeClosed( false );
						oChannel.disposeForInput();
					} catch( IOException e ) {
						channel.send( CommMessage.createFaultResponse( message, new FaultException( e ) ) );
					}
				} else {
					InputOperation operation =
						interpreter.getInputOperation( message.operationName() );
					if ( listener == null || listener.canHandleInputOperation( operation ) ) {
						operation.recvMessage( channel, message );
					} else {
						interpreter.logWarning(
								"Discarded a message for operation " + operation.id() +
								", not specified in an input port at the receiving service."
							);
					}
					channel.disposeForInput();
				}
			} catch( InvalidIdException e ) {
				interpreter.logWarning( e );
			} catch( URISyntaxException e ) {
				interpreter.logSevere( e );
			}
		}
		
		public void run()
		{
			try {
				CommChannelHandler.currentThread().setExecutionThread( interpreter().mainThread() );
				CommMessage message = channel.recv();
				channel.setScheduled( false );
				if ( channel.redirectionChannel() == null ) {
					handleMessage( message );
				} else {
					redirectMessage( message );
				}
			} catch( IOException e ) {
				interpreter.logSevere( e );
			}
		}
	}
	
	public void scheduleReceive( CommChannel channel, CommListener listener )
	{
		if ( channel.isScheduled() == false ) {
			channel.setScheduled( true );
			executorService.execute( new CommChannelHandlerRunnable( channel, listener ) );
		}
	}

	protected void startCommChannelHandler( Runnable r )
	{
		executorService.execute( r );
	}
	
	/**
	 * Initializes the communication core, starting its communication listeners.
	 * 
	 * @see CommListener
	 */
	public void init()
		throws IOException
	{		
		for( Entry< String, CommListener > entry : listenersMap.entrySet() ) {
			entry.getValue().start();
		}
		active = true;
	}
	
	private PollingThread pollingThread = null;
	
	private PollingThread pollingThread()
	{
		if ( pollingThread == null ) {
			pollingThread = new PollingThread();
			pollingThread.start();
		}
		return pollingThread;
	}
	
	private class PollingThread extends Thread {
		final private Set< CommChannel > channels = new HashSet< CommChannel >();

		public PollingThread()
		{
			super( threadGroup, interpreter.programFile().getName() + "-PollingThread" );
		}

		@Override
		public void run()
		{
			Iterator< CommChannel > it;
			CommChannel channel;
			while( active ) {
				synchronized( this ) {
					if ( channels.isEmpty() ) {
						// Do not busy-wait for no reason
						try {
							this.wait();
						} catch( InterruptedException e ) {}
					}
					it = channels.iterator();
					while( it.hasNext() ) {
						channel = it.next();
						try {
							if ( ((PollableCommChannel)channel).isReady() ) {
								it.remove();
								scheduleReceive( channel, channel.parentListener() );
							}
						} catch( IOException e ) {
							e.printStackTrace();
						}
					}
				}
				try {
					Thread.sleep( 50 ); // msecs
				} catch( InterruptedException e ) {}
			}
		}
		
		public void register( CommChannel channel )
			throws IOException
		{
			if ( !(channel instanceof PollableCommChannel) ) {
				throw new IOException( "Channels registering for polling must implement PollableCommChannel interface");
			}
			
			synchronized( this ) {
				channels.add( channel );
				if ( channels.size() == 1 ) { // set was empty
					this.notify();
				}
			}
		}
	}
	
	public void registerForPolling( CommChannel channel )
		throws IOException
	{
		pollingThread().register( channel );
	}
	
	private SelectorThread selectorThread = null;
	
	private SelectorThread selectorThread()
		throws IOException
	{
		synchronized( this ) {
			if ( selectorThread == null ) {
				selectorThread = new SelectorThread();
				selectorThread.start();
			}
		}
		return selectorThread;
	}
	
	private class SelectorThread extends Thread {
		final private Selector selector;
		public SelectorThread()
			throws IOException
		{
			super( threadGroup, interpreter.programFile().getName() + "-SelectorThread" );
			this.selector = Selector.open();
		}
		
		@Override
		public void run()
		{
			int buffer;
			SelectableStreamingCommChannel channel;
			InputStream stream;
			while( active ) {
				try {
					selector.select();
					synchronized( this ) {
						for( SelectionKey key : selector.selectedKeys() ) {
							key.cancel();
							channel = (SelectableStreamingCommChannel)key.attachment();
							channel.setSelectionKey( null );
							try {
								key.channel().configureBlocking( true );
								stream = channel.inputStream();
								stream.mark( 1 );
								// It could just be a closing read. If not, receive it.
								try {
									buffer = stream.read();
								} catch( IOException e ) {
									buffer = -1;
									if ( interpreter.verbose() ) {
										interpreter.logSevere( e );
									}
								}
								if ( buffer != -1 ) {
									stream.reset();
									scheduleReceive( channel, channel.parentListener() );
								} else {
									channel.closeImpl();
								}
							} catch( IOException e ) {
								interpreter.logSevere( e );
								//channel.selectionKey().cancel();
								channel.setSelectionKey( null );
							}
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		}
		
		public void register( SelectableStreamingCommChannel channel )
		{
			try {
				synchronized( channel ) {
					if ( channel.inputStream().available() > 0 ) {
						scheduleReceive( channel, channel.parentListener() );
						return;
					}
				}
				synchronized( this ) {
					if ( channel.selectionKey() == null ) {
						SelectableChannel c = channel.selectableChannel();
						c.configureBlocking( false );
						selector.wakeup();
						selector.selectNow();
						channel.setSelectionKey( c.register( selector, SelectionKey.OP_READ, channel ) );
					}
				}
			} catch( ClosedChannelException e ) {
				interpreter.logWarning( e );
			} catch( IOException e ) {
				interpreter.logSevere( e );
			}
		}

		public void unregister( SelectableStreamingCommChannel channel )
			throws IOException
		{
			synchronized( this ) {
				if ( channel.selectionKey() != null ) {
					channel.selectionKey().cancel();
					channel.selectableChannel().configureBlocking( true );
					channel.setSelectionKey( null );
					selector.wakeup();
					selector.selectNow();
				}
			}
		}
	}

	public void unregisterForSelection( SelectableStreamingCommChannel channel )
		throws IOException
	{
		selectorThread().unregister( channel );
	}
	
	public void registerForSelection( SelectableStreamingCommChannel channel )
		throws IOException
	{
		selectorThread().register( channel );
	}

	final private Vector< InputProcessExecution<?> > waitingCorrelatedInputs = new Vector< InputProcessExecution<?> >();

	public synchronized void addWaitingCorrelatedInput( InputProcessExecution<?> input )
	{
		if ( active ) {
			waitingCorrelatedInputs.add( input );
		}
	}

	public synchronized void removeWaitingCorrelatedInput( InputProcessExecution<?> input )
	{
		waitingCorrelatedInputs.remove( input );
	}

	/** Shutdowns the communication core, interrupting every communication-related thread. */
	public synchronized void shutdown()
	{
		if ( active ) {
			for( Entry< String, CommListener > entry : listenersMap.entrySet() ) {
				entry.getValue().shutdown();
			}
			if ( selectorThread != null ) {
				selectorThread.selector.wakeup();
			}
			executorService.shutdown();
			threadGroup.interrupt();
			for( InputProcessExecution input : waitingCorrelatedInputs ) {
				input.interpreterExit();
			}
			active = false;
		}
	}

	private boolean active = false;
}
