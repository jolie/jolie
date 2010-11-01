/***************************************************************************
 *   Copyright (C) 2006-2009 by Fabrizio Montesi <famontesi@gmail.com>     *
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
 *   You should have receicved a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.net;

import java.io.IOException;
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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.JolieThread;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.process.InputProcessExecution;
import jolie.process.Process;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.TimeoutHandler;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/** 
 * Handles the communications mechanisms for an Interpreter instance.
 * 
 * Each CommCore is related to an Interpreter, and each Interpreter owns one and only CommCore instance.
 *
 * @author Fabrizio Montesi
 */
public class CommCore
{
	private final Map< String, CommListener > listenersMap = new HashMap< String, CommListener >();

	private final ThreadGroup threadGroup;

	private final Logger logger = Logger.getLogger( "JOLIE" );

	private final int connectionsLimit;
	private final int connectionsCache;
	private final Interpreter interpreter;

	private final Map< URI, Map< String, CommChannel > > persistentChannels =
			new HashMap< URI, Map< String, CommChannel > >();

	private void removePersistentChannel( URI location, String protocol, Map< String, CommChannel > protocolChannels )
	{
		protocolChannels.remove( protocol );
		if ( protocolChannels.isEmpty() ) {
			persistentChannels.remove( location );
		}
	}

	public CommChannel getPersistentChannel( URI location, String protocol )
	{
		CommChannel ret = null;
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if ( protocolChannels != null ) {
				ret = protocolChannels.get( protocol );
				if ( ret != null ) {
					if ( ret.lock.tryLock() ) {
						if ( ret.isOpen() ) {
							/*
							 * We are going to return this channel, but first
							 * check if it supports concurrent use.
							 * If not, then others should not access this until
							 * the caller is finished using it.
							 */
							if ( ret.isThreadSafe() == false ) {
								removePersistentChannel( location, protocol, protocolChannels );
							}
							ret.lock.unlock();
						} else { // Channel is closed
							removePersistentChannel( location, protocol, protocolChannels );
							ret.lock.unlock();
							ret = null;
						}
					} else { // Channel is busy
						removePersistentChannel( location, protocol, protocolChannels );
						ret = null;
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
			if ( protocolChannels.size() <= connectionsCache && protocolChannels.containsKey( protocol ) == false ) {
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
	 * @param connectionsCache specifies an upper bound to the persistent output connection cache.
	 * @throws java.io.IOException
	 */
	public CommCore( Interpreter interpreter, int connectionsLimit, int connectionsCache )
		throws IOException
	{
		this.interpreter = interpreter;
		this.localListener = new LocalListener( interpreter );
		this.connectionsLimit = connectionsLimit;
		this.connectionsCache = connectionsCache;
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
	
	private final Collection< Process > protocolConfigurations = new LinkedList< Process > ();
	
	public Collection< Process > protocolConfigurations()
	{
		return protocolConfigurations;
	}
	
	public CommListener getListenerByInputPortName( String serviceName )
	{
		return listenersMap.get( serviceName );
	}
	
	private final Map< String, CommChannelFactory > channelFactories =
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
	
	private final Map< String, CommProtocolFactory > protocolFactories =
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
	
	public CommProtocol createOutputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException
	{
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if ( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}
		
		return factory.createOutputProtocol( configurationPath, uri );
	}

	public CommProtocol createInputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException
	{
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if ( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}

		return factory.createInputProtocol( configurationPath, uri );
	}
	
	private final Map< String, CommListenerFactory > listenerFactories =
						new HashMap< String, CommListenerFactory > ();

	private final LocalListener localListener;

	public LocalCommChannel getLocalCommChannel()
	{
		return new LocalCommChannel( interpreter, localListener );
	}

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
				Map< String, AggregatedOperation > aggregationMap,
				Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		localListener.addOperationNames( operationNames );
		localListener.addAggregations( aggregationMap );
		localListener.addRedirections( redirectionMap );
		listenersMap.put( inputPortName, localListener );
	}
	
	/**
	 * Adds an input port to this <code>CommCore</code>.
	 * This method is not thread-safe.
	 * @param inputPortName the name of the input port to add
	 * @param uri the <code>URI</code> of the input port to add
	 * @param protocolFactory the <code>CommProtocolFactory</code> to use for the input port
	 * @param protocolConfigurationPath the protocol configuration variable path to use for the input port
	 * @param protocolConfigurationProcess the protocol configuration process to execute for configuring the created protocols
	 * @param operationNames the operation names the input port can handle
	 * @param aggregationMap the aggregation mapping of the input port
	 * @param redirectionMap the redirection mapping of the input port
	 * @throws java.io.IOException in case of some underlying implementation error
	 * @see URI
	 * @see CommProtocolFactory
	 */
	public void addInputPort(
				String inputPortName,
				URI uri,
				CommProtocolFactory protocolFactory,
				VariablePath protocolConfigurationPath,
				Process protocolConfigurationProcess,
				Collection< String > operationNames,
				Map< String, AggregatedOperation > aggregationMap,
				Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		protocolConfigurations.add( protocolConfigurationProcess );

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
			aggregationMap,
			redirectionMap
		);
		listenersMap.put( inputPortName, listener );
	}
	
	private final ExecutorService executorService;
	
	private class CommThreadFactory implements ThreadFactory {
		public Thread newThread( Runnable r )
		{
			return new CommChannelHandler( interpreter, r );
		}
	}

	private static Pattern pathSplitPattern = Pattern.compile( "/" );

	private class CommChannelHandlerRunnable implements Runnable {
		private final CommChannel channel;
		private final CommListener listener;
		
		public CommChannelHandlerRunnable( CommChannel channel, CommListener listener )
		{
			this.channel = channel;
			this.listener = listener;
		}
		
		private void forwardResponse( CommMessage message )
			throws IOException
		{
			message = new CommMessage(
				channel.redirectionMessageId(),
				message.operationName(),
				message.resourcePath(),
				message.value(),
				message.fault()
			);
			try {
				try {
					channel.redirectionChannel().send( message );
				} finally {
					try {
						if ( channel.redirectionChannel().toBeClosed() ) {
							channel.redirectionChannel().close();
						} else {
							channel.redirectionChannel().disposeForInput();
						}
					} finally {
						channel.setRedirectionChannel( null );
					}
				}
			} finally {
				channel.closeImpl();
			}
		}

		private void handleRedirectionInput( CommMessage message, String[] ss )
			throws IOException, URISyntaxException
		{
			// Redirection
			String rPath = "";
			if ( ss.length <= 2 ) {
				rPath = "/";
			} else {
				StringBuilder builder = new StringBuilder();
				for( int i = 2; i < ss.length; i++ ) {
					builder.append( '/' );
					builder.append( ss[ i ] );
				}
				rPath = builder.toString();
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
			oChannel.setRedirectionMessageId( rMessage.id() );
			try {
				oChannel.send( rMessage );
				oChannel.setToBeClosed( false );
				oChannel.disposeForInput();
			} catch( IOException e ) {
				channel.send( CommMessage.createFaultResponse( message, new FaultException( e ) ) );
				channel.disposeForInput();
			}
		}

		private void handleAggregatedInput( CommMessage message, AggregatedOperation operation )
			throws IOException, URISyntaxException
		{
			// Aggregation input
			if ( operation.isOneWay() ) {
				CommChannel oChannel = operation.port().getCommChannel();
				oChannel.send( message );
				oChannel.release();
			} else {
				CommChannel oChannel = operation.port().getNewCommChannel();
				oChannel.setRedirectionChannel( channel );
				oChannel.setRedirectionMessageId( message.id() );
				try {
					oChannel.send( message );
					oChannel.setToBeClosed( false );
					oChannel.disposeForInput();
				} catch( IOException e ) {
					channel.send( CommMessage.createFaultResponse( message, new FaultException( e ) ) );
					channel.disposeForInput();
				}
			}
		}

		private void handleDirectMessage( CommMessage message )
			throws IOException
		{
			try {
				InputOperation operation =
					interpreter.getInputOperation( message.operationName() );
				operation.recvMessage( channel, message );
				channel.disposeForInput();
			} catch( InvalidIdException e ) {
				interpreter.logWarning( "Received a message for undefined operation " + message.operationName() + ". Sending IOException to the caller." );
				channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
				channel.disposeForInput();
			}
		}

		private void handleMessage( CommMessage message )
			throws IOException
		{
			try {
				String[] ss = pathSplitPattern.split( message.resourcePath() );
				if ( ss.length > 1 ) {
					handleRedirectionInput( message, ss );
				} else {
					if ( listener.canHandleInputOperationDirectly( message.operationName() ) ) {
						handleDirectMessage( message );
					} else {
						AggregatedOperation operation = listener.getAggregatedOperation( message.operationName() );
						if ( operation == null ) {
							interpreter.logWarning(
								"Received a message for operation " + message.operationName() +
									", not specified in the input port at the receiving service. Sending IOException to the caller."
							);
							channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
							channel.disposeForInput();
						} else {
							handleAggregatedInput( message, operation );
						}
					}
				}
			} catch( URISyntaxException e ) {
				interpreter.logSevere( e );
			}
		}
		
		public void run()
		{
			CommChannelHandler thread = CommChannelHandler.currentThread();
			thread.setExecutionThread( interpreter().mainThread() );
			channel.lock.lock();
			try {
				if ( channel.redirectionChannel() == null ) {
					assert( listener != null );
					CommMessage message = channel.recv();
					if ( message != null ) {
						handleMessage( message );
					}
				} else {
					channel.lock.unlock();
					CommMessage response = channel.recvResponseFor( new CommMessage( channel.redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, null ) );
					if ( response != null ) {
						forwardResponse( response );
					}
				}
			} catch( IOException e ) {
				interpreter.logSevere( e );
			} finally {
				if ( channel.lock.isHeldByCurrentThread() ) {
					channel.lock.unlock();
				}
				thread.setExecutionThread( null );
			}
		}
	}

	/**
	 * Schedules the receiving of a message on this <code>CommCore</code> instance.
	 * @param channel the <code>CommChannel</code> to use for receiving the message
	 * @param listener the <code>CommListener</code> responsible for the message receiving
	 */
	public void scheduleReceive( CommChannel channel, CommListener listener )
	{
		executorService.execute( new CommChannelHandlerRunnable( channel, listener ) );
	}

	protected void startCommChannelHandler( Runnable r )
	{
		executorService.execute( r );
	}
	
	/**
	 * Initializes the communication core, starting its communication listeners.
	 * This method is asynchronous. When it returns, every communication listener has
	 * been issued to start, but they are not guaranteed to be ready to receive messages.
	 * This method throws an exception if some listener can not be issued to start;
	 * other errors will be logged by the listener through the interpreter logger.
	 *
	 * @throws IOException in case of some underlying <code>CommListener</code> initialization error
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
		synchronized( this ) {
			if ( pollingThread == null ) {
				pollingThread = new PollingThread();
				pollingThread.start();
			}
		}
		return pollingThread;
	}
	
	private class PollingThread extends Thread {
		private final Set< CommChannel > channels = new HashSet< CommChannel >();

		private PollingThread()
		{
			super( threadGroup, interpreter.programFilename() + "-PollingThread" );
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

			for( CommChannel c : channels ) {
				try {
					c.closeImpl();
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
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

	/**
	 * Registers a <code>CommChannel</code> for input polling.
	 * The registered channel must implement the {@link PollableCommChannel <code>PollableCommChannel</code>} interface.
	 * @param channel the channel to register for polling
	 * @throws java.io.IOException in case the channel could not be registered for polling
	 * @see CommChannel
	 * @see PollableCommChannel
	 */
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
				selectorThread = new SelectorThread( interpreter );
				selectorThread.start();
			}
		}
		return selectorThread;
	}

	private class SelectorThread extends JolieThread {
		private final Selector selector;
		private final Object selectingMutex = new Object();
		public SelectorThread( Interpreter interpreter )
			throws IOException
		{
			super( interpreter, threadGroup, interpreter.programFilename() + "-SelectorThread" );
			this.selector = Selector.open();
		}
		
		@Override
		public void run()
		{
			SelectableStreamingCommChannel channel;
			while( active ) {
				try {
					synchronized( selectingMutex ) {
						selector.select();
					}
					synchronized( this ) {
						for( SelectionKey key : selector.selectedKeys() ) {
							if ( key.isValid() ) {
								channel = (SelectableStreamingCommChannel)key.attachment();
								try {
									if ( channel.lock.tryLock() ) {
										try {
											key.cancel();
											if ( channel.isOpen() ) {
												key.channel().configureBlocking( true );
												if ( channel.selectionTimeoutHandler() != null ) {
													interpreter.removeTimeoutHandler( channel.selectionTimeoutHandler() );
												}
												scheduleReceive( channel, channel.parentListener() );
											} else {
												channel.closeImpl();
											}
											channel.lock.unlock();
										} catch( IOException e ) {
											channel.lock.unlock();
											throw e;
										}
									}
								} catch( IOException e ) {
									if ( channel.lock.isHeldByCurrentThread() ) {
										channel.lock.unlock();
									}
									if ( interpreter.verbose() ) {
										interpreter.logSevere( e );
									}
								}
							}
						}
						synchronized( selectingMutex ) {
							selector.selectNow(); // Clean up the cancelled keys
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}

			for( SelectionKey key : selector.keys() ) {
				try {
					((SelectableStreamingCommChannel)key.attachment()).closeImpl();
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			}
		}
		
		public boolean register( SelectableStreamingCommChannel channel )
		{
			try {
				if ( channel.inputStream().available() > 0 ) {
					scheduleReceive( channel, channel.parentListener() );
					return false;
				}

				synchronized( this ) {
					if ( isSelecting( channel ) == false ) {
						SelectableChannel c = channel.selectableChannel();
						c.configureBlocking( false );
						selector.wakeup();
						synchronized( selectingMutex ) {
							c.register( selector, SelectionKey.OP_READ, channel );
							selector.selectNow();
						}
					}
				}
				return true;
			} catch( ClosedChannelException e ) {
				interpreter.logWarning( e );
				return false;
			} catch( IOException e ) {
				interpreter.logSevere( e );
				return false;
			}
		}

		public void unregister( SelectableStreamingCommChannel channel )
			throws IOException
		{
			synchronized( this ) {
				if ( isSelecting( channel ) ) {
					selector.wakeup();
					synchronized( selectingMutex ) {
						channel.selectableChannel().keyFor( selector ).cancel();
						selector.selectNow();
					}
					channel.selectableChannel().configureBlocking( true );
				}
			}
		}

		private boolean isSelecting( SelectableStreamingCommChannel channel )
		{
			SelectableChannel c = channel.selectableChannel();
			if ( c == null ) {
				return false;
			}
			return c.keyFor( selector ) != null;
		}
	}

	protected boolean isSelecting( SelectableStreamingCommChannel channel )
	{
		synchronized( this ) {
			if ( selectorThread == null ) {
				return false;
			}
		}
		final SelectorThread t = selectorThread;
		synchronized( t ) {
			return t.isSelecting( channel );
		}
	}

	protected void unregisterForSelection( SelectableStreamingCommChannel channel )
		throws IOException
	{
		selectorThread().unregister( channel );
	}
	
	protected void registerForSelection( final SelectableStreamingCommChannel channel )
		throws IOException
	{
		final TimeoutHandler handler = new TimeoutHandler( interpreter.openInputConnectionTimeout() ) {
			@Override
			public void onTimeout()
			{
				try {
					selectorThread().unregister( channel );
					channel.close();
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		};
		channel.setSelectionTimeoutHandler( handler );
		if ( selectorThread().register( channel ) ) {
			interpreter.addTimeoutHandler( handler );
		} else {
			channel.setSelectionTimeoutHandler( null );
		}
	}

	private final Collection< InputProcessExecution<?> > waitingCorrelatedInputs = new LinkedList< InputProcessExecution<?> >();

	/**
	 * Registers a waiting session spawner. This is necessary for operating graceful shutdowns.
	 * @param input the session spawner to register
	 */
	public synchronized void addWaitingCorrelatedInput( InputProcessExecution<?> input )
	{
		if ( active ) {
			waitingCorrelatedInputs.add( input );
		}
	}

	/**
	 * Unregisters a waiting session spawner.
	 * @param input the session spawner to unregister
	 */
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
