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
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.net;

import jolie.Interpreter;
import jolie.JolieThreadPoolExecutor;
import jolie.NativeJolieThread;
import jolie.lang.Constants;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.CommProtocol;
import jolie.process.Process;
import jolie.runtime.*;
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.typing.TypeCheckingException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles the communications mechanisms for an Interpreter instance.
 * 
 * Each CommCore is related to an Interpreter, and each Interpreter owns one and only CommCore
 * instance.
 *
 * @author Fabrizio Montesi
 */
public class CommCore {
	private final Map< String, CommListener > listenersMap = new HashMap<>();
	private final static int CHANNEL_HANDLER_TIMEOUT = 5;
	private final ThreadGroup threadGroup;

	private static final Logger LOGGER = Logger.getLogger( "JOLIE" );

	private final int connectionsLimit;
	private final Interpreter interpreter;
	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );
	private SelectorThread[] selectorThreads;

	/**
	 * Returns the Interpreter instance this CommCore refers to.
	 * 
	 * @return the Interpreter instance this CommCore refers to
	 */
	public Interpreter interpreter() {
		return interpreter;
	}

	/**
	 * Constructor.
	 * 
	 * @param interpreter the Interpreter to refer to for this CommCore operations
	 * @param connectionsLimit if more than zero, specifies an upper bound to the connections handled in
	 *        parallel.
	 * @throws java.io.IOException
	 */
	public CommCore( Interpreter interpreter, int connectionsLimit /* , int connectionsCacheSize */ )
		throws IOException {
		this.interpreter = interpreter;
		this.connectionsLimit = connectionsLimit;
		// this.connectionCacheSize = connectionsCacheSize;
		this.threadGroup = new ThreadGroup( "CommCore-" + interpreter.hashCode() );
		/*
		 * if ( connectionsLimit > 0 ) { executorService = Executors.newFixedThreadPool( connectionsLimit,
		 * new CommThreadFactory() ); } else { executorService = Executors.newCachedThreadPool( new
		 * CommThreadFactory() ); }
		 */
		executorService = new JolieThreadPoolExecutor( new CommThreadFactory() );

		// TODO make socket an extension, too?
		CommListenerFactory listenerFactory = new SocketListenerFactory( this );
		listenerFactories.put( "socket", listenerFactory );
		CommChannelFactory channelFactory = new SocketCommChannelFactory( this );
		channelFactories.put( "socket", channelFactory );
	}

	public ExecutorService executor() {
		return executorService;
	}

	private SelectorThread[] selectorThreads()
		throws IOException {
		if( selectorThreads == null ) {
			selectorThreads = new SelectorThread[ Runtime.getRuntime().availableProcessors() ];
			for( int i = 0; i < selectorThreads.length; i++ ) {
				selectorThreads[ i ] = new SelectorThread( interpreter );
			}
		}

		return selectorThreads;
	}



	/**
	 * Returns the Logger used by this CommCore.
	 * 
	 * @return the Logger used by this CommCore
	 */
	public Logger logger() {
		return LOGGER;
	}

	/**
	 * Returns the connectionsLimit of this CommCore.
	 * 
	 * @return the connectionsLimit of this CommCore
	 */
	public int connectionsLimit() {
		return connectionsLimit;
	}

	public ThreadGroup threadGroup() {
		return threadGroup;
	}

	private final Collection< Process > protocolConfigurations = new LinkedList<>();

	public Collection< Process > protocolConfigurations() {
		return protocolConfigurations;
	}

	public CommListener getListenerByInputPortName( String serviceName ) {
		return listenersMap.get( serviceName );
	}

	private final Map< String, CommChannelFactory > channelFactories = new HashMap<>();

	private CommChannelFactory getCommChannelFactory( String name )
		throws IOException {
		CommChannelFactory factory = channelFactories.get( name );
		if( factory == null ) {
			factory = interpreter.getClassLoader().createCommChannelFactory( name, this );
			if( factory != null ) {
				channelFactories.put( name, factory );
			}
		}
		return factory;
	}

	public CommChannel createCommChannel( URI uri, OutputPort port )
		throws IOException {
		String medium = uri.getScheme();
		CommChannelFactory factory = getCommChannelFactory( medium );
		if( factory == null ) {
			throw new UnsupportedCommMediumException(
				"medium = " + medium + ", location = " + uri.toString() + ", output port = "
					+ (port != null ? port.id() : "null") );
		}

		return factory.createChannel( uri, port );
	}

	private final Map< String, CommProtocolFactory > protocolFactories = new HashMap<>();

	public CommProtocolFactory getCommProtocolFactory( String name )
		throws IOException {
		CommProtocolFactory factory = protocolFactories.get( name );
		if( factory == null ) {
			factory = interpreter.getClassLoader().createCommProtocolFactory( name, this );
			if( factory != null ) {
				protocolFactories.put( name, factory );
			}
		}
		return factory;
	}

	public CommProtocol createOutputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException {
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}

		return factory.createOutputProtocol( configurationPath, uri );
	}

	public CommProtocol createInputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException {
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}

		return factory.createInputProtocol( configurationPath, uri );
	}

	private final Map< String, CommListenerFactory > listenerFactories = new HashMap<>();

	private LocalListener localListener;

	public LocalCommChannel getLocalCommChannel() {
		return new LocalCommChannel( interpreter, localListener() );
	}

	public LocalCommChannel getLocalCommChannel( CommListener listener ) {
		return new LocalCommChannel( interpreter, listener );
	}

	public CommListenerFactory getCommListenerFactory( String name )
		throws IOException {
		CommListenerFactory factory = listenerFactories.get( name );
		if( factory == null ) {
			factory = interpreter.getClassLoader().createCommListenerFactory( name, this );
			if( factory != null ) {
				listenerFactories.put( name, factory );
			}
		}
		return factory;
	}

	public LocalListener localListener() {
		if( localListener == null ) {
			localListener = LocalListener.create( interpreter );
		}
		return localListener;
	}

	public void addLocalInputPort( InputPort inputPort )
		throws IOException {
		final LocalListener l = localListener();
		l.mergeInterface( inputPort.getInterface() );
		l.addAggregations( inputPort.aggregationMap() );
		l.addRedirections( inputPort.redirectionMap() );
		listenersMap.put( inputPort.name(), l );
	}

	/**
	 * Adds an input port to this <code>CommCore</code>. This method is not thread-safe.
	 * 
	 * @param inputPort the {@link InputPort} to add
	 * @param protocolFactory the <code>CommProtocolFactory</code> to use for the input port
	 * @param protocolConfigurationProcess the protocol configuration process to execute for configuring
	 *        the created protocols
	 * @throws java.io.IOException in case of some underlying implementation error
	 * @see URI
	 * @see CommProtocolFactory
	 */
	public void addInputPort(
		InputPort inputPort,
		CommProtocolFactory protocolFactory,
		Process protocolConfigurationProcess )
		throws IOException {
		protocolConfigurations.add( protocolConfigurationProcess );

		String medium = inputPort.location().getScheme();
		CommListenerFactory factory = getCommListenerFactory( medium );
		if( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}

		CommListener listener = factory.createListener(
			interpreter,
			protocolFactory,
			inputPort );
		listenersMap.put( inputPort.name(), listener );
	}

	private final ExecutorService executorService;

	private final static class CommThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread( Runnable r ) {
			return new CommChannelHandler( r );
		}
	}

	private final static Pattern PATH_SPLIT_PATTERN = Pattern.compile( "/" );

	private class CommChannelHandlerRunnable implements Runnable {
		private final CommChannel channel;
		private final InputPort port;

		public CommChannelHandlerRunnable( CommChannel channel, InputPort port ) {
			this.channel = channel;
			this.port = port;
		}

		private void forwardResponse( CommMessage message )
			throws IOException {
			message = new CommMessage(
				channel.redirectionMessageId(),
				message.operationName(),
				message.resourcePath(),
				message.value(),
				message.fault() );
			try {
				try {
					channel.redirectionChannel().send( message );
				} finally {
					try {
						if( channel.redirectionChannel().toBeClosed() ) {
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
			throws IOException, URISyntaxException {
			// Redirection
			String rPath;
			if( ss.length <= 2 ) {
				rPath = "/";
			} else {
				StringBuilder builder = new StringBuilder();
				for( int i = 2; i < ss.length; i++ ) {
					builder.append( '/' );
					builder.append( ss[ i ] );
				}
				rPath = builder.toString();
			}
			OutputPort oPort = port.redirectionMap().get( ss[ 1 ] );
			if( oPort == null ) {
				String error = "Discarded a message for resource " + ss[ 1 ] +
					", not specified in the appropriate redirection table.";
				interpreter.logWarning( error );
				throw new IOException( error );
			}
			try {
				CommChannel oChannel = oPort.getNewCommChannel();
				CommMessage rMessage =
					new CommMessage(
						message.requestId(),
						message.operationName(),
						rPath,
						message.value(),
						message.fault() );
				oChannel.setRedirectionChannel( channel );
				oChannel.setRedirectionMessageId( rMessage.requestId() );
				oChannel.send( rMessage );
				oChannel.setToBeClosed( false );
				oChannel.disposeForInput();
			} catch( IOException e ) {
				channel.send( CommMessage.createFaultResponse( message,
					new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ) );
				channel.disposeForInput();
				throw e;
			}
		}

		private void handleAggregatedInput( CommMessage message, AggregatedOperation operation )
			throws IOException, URISyntaxException {
			operation.runAggregationBehaviour( message, channel );
		}

		private void handleDirectMessage( CommMessage message )
			throws IOException {
			try {
				InputOperation operation =
					interpreter.getInputOperation( message.operationName() );
				try {
					operation.requestType().check( message.value() );
					interpreter.correlationEngine().onMessageReceive( message, channel );
					if( operation instanceof OneWayOperation ) {
						// We need to send the acknowledgement
						channel.send( CommMessage.createEmptyResponse( message ) );
						// channel.release();
					}
				} catch( TypeCheckingException e ) {
					interpreter.logWarning(
						"Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
					try {
						channel.send( CommMessage.createFaultResponse( message,
							new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
					} catch( IOException ioe ) {
						Interpreter.getInstance().logSevere( ioe );
					}
				} catch( CorrelationError e ) {
					interpreter.logWarning( "Received a non correlating message for operation "
						+ message.operationName() + ". Sending CorrelationError to the caller." );
					channel.send( CommMessage.createFaultResponse( message, new FaultException( "CorrelationError",
						"The message you sent can not be correlated with any session and can not be used to start a new session." ) ) );
				}
			} catch( InvalidIdException e ) {
				interpreter.logWarning( "Received a message for undefined operation " + message.operationName()
					+ ". Sending IOException to the caller." );
				channel.send( CommMessage.createFaultResponse( message,
					new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
			} finally {
				channel.disposeForInput();
			}
		}

		private void handleMessage( CommMessage message )
			throws IOException {
			try {
				String[] ss = PATH_SPLIT_PATTERN.split( message.resourcePath() );
				if( ss.length > 1 ) {
					handleRedirectionInput( message, ss );
				} else {
					if( port.canHandleInputOperationDirectly( message.operationName() ) ) {
						handleDirectMessage( message );
					} else {
						AggregatedOperation operation = port.getAggregatedOperation( message.operationName() );
						if( operation == null ) {
							interpreter.logWarning(
								"Received a message for operation " + message.operationName() +
									", not specified in the input port " + port.name()
									+ " at the receiving service. Sending IOException to the caller." );
							try {
								channel
									.send( CommMessage.createFaultResponse( message, new FaultException( "IOException",
										"Invalid operation: " + message.operationName() ) ) );
							} finally {
								channel.disposeForInput();
							}
						} else {
							handleAggregatedInput( message, operation );
						}
					}
				}
			} catch( URISyntaxException e ) {
				interpreter.logSevere( e );
			}
		}

		@Override
		public void run() {
			final CommChannelHandler thread = CommChannelHandler.currentThread();
			thread.setExecutionThread( interpreter().initThread().getNewSessionThread() );
			channel.rwLock.lock();
			channelHandlersLock.readLock().lock();
			try {
				if( channel.redirectionChannel() == null ) {
					assert (port != null);
					final CommMessage message = channel.recv();
					if( message != null ) {
						handleMessage( message );
					} else {
						channel.disposeForInput();
					}
				} else {
					channel.rwLock.unlock();
					CommMessage response = null;
					try {
						response = channel.recvResponseFor(
							new CommMessage( channel.redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, null ) )
							.get();
					} catch( InterruptedException | ExecutionException e ) {
						interpreter.logFine( e );
					} finally {
						if( response == null ) {
							response = new CommMessage( channel.redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE,
								new FaultException( "IOException", "Internal server error" ) );
						}
						forwardResponse( response );
					}
				}
			} catch( ChannelClosingException e ) {
				interpreter.logFine( e );
			} catch( IOException e ) {
				interpreter.logSevere( e );
				try {
					channel.closeImpl();
				} catch( IOException e2 ) {
					interpreter.logSevere( e2 );
				}
			} finally {
				channelHandlersLock.readLock().unlock();
				if( channel.rwLock.isHeldByCurrentThread() ) {
					channel.rwLock.unlock();
				}
				thread.setExecutionThread( null );
			}
		}
	}

	/**
	 * Schedules the receiving of a message on this <code>CommCore</code> instance.
	 * 
	 * @param channel the <code>CommChannel</code> to use for receiving the message
	 * @param port the <code>Port</code> responsible for the message receiving
	 */
	public void scheduleReceive( CommChannel channel, InputPort port ) {
		executorService.execute( new CommChannelHandlerRunnable( channel, port ) );
	}

	protected void startCommChannelHandler( Runnable r ) {
		executorService.execute( r );
	}

	/**
	 * Initializes the communication core, starting its communication listeners. This method is
	 * asynchronous. When it returns, every communication listener has been issued to start, but they
	 * are not guaranteed to be ready to receive messages. This method throws an exception if some
	 * listener cannot be issued to start; other errors will be logged by the listener through the
	 * interpreter logger.
	 *
	 * @throws IOException in case of some underlying <code>CommListener</code> initialization error
	 * @see CommListener
	 */
	public void init()
		throws IOException {
		active = true;
		for( SelectorThread t : selectorThreads() ) {
			t.start();
		}
		for( Entry< String, CommListener > entry : listenersMap.entrySet() ) {
			entry.getValue().start();
		}
	}

	private PollingThread pollingThread = null;

	private PollingThread pollingThread() {
		synchronized( this ) {
			if( pollingThread == null ) {
				pollingThread = new PollingThread();
				pollingThread.start();
			}
		}
		return pollingThread;
	}

	private class PollingThread extends Thread {
		private final Set< CommChannel > channels = new HashSet<>();

		private PollingThread() {
			super( threadGroup, interpreter.programFilename() + "-PollingThread" );
		}

		@Override
		public void run() {
			Iterator< CommChannel > it;
			CommChannel channel;
			while( active ) {
				synchronized( this ) {
					if( channels.isEmpty() ) {
						// Do not busy-wait for no reason
						try {
							this.wait();
						} catch( InterruptedException e ) {
						}
					}
					it = channels.iterator();
					while( it.hasNext() ) {
						channel = it.next();
						try {
							if( ((PollableCommChannel) channel).isReady() ) {
								it.remove();
								scheduleReceive( channel, channel.parentInputPort() );
							}
						} catch( IOException e ) {
							e.printStackTrace();
						}
					}
				}
				try {
					Thread.sleep( 50 ); // msecs
				} catch( InterruptedException e ) {
				}
			}

			channels.forEach( ( c ) -> {
				try {
					c.closeImpl();
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			} );
		}

		public void register( CommChannel channel )
			throws IOException {
			if( !(channel instanceof PollableCommChannel) ) {
				throw new IOException(
					"Channels registering for polling must implement PollableCommChannel interface" );
			}

			synchronized( this ) {
				channels.add( channel );
				if( channels.size() == 1 ) { // set was empty
					this.notify();
				}
			}
		}
	}

	/**
	 * Registers a <code>CommChannel</code> for input polling. The registered channel must implement the
	 * {@link PollableCommChannel <code>PollableCommChannel</code>} interface.
	 * 
	 * @param channel the channel to register for polling
	 * @throws java.io.IOException in case the channel could not be registered for polling
	 * @see CommChannel
	 * @see PollableCommChannel
	 */
	public void registerForPolling( CommChannel channel )
		throws IOException {
		pollingThread().register( channel );
	}

	private class SelectorThread extends NativeJolieThread {
		// We use a custom class for debugging purposes (the profiler gives us the class name)
		private class SelectorMutex {
		}

		private final Selector selector;
		private final SelectorMutex selectingMutex = new SelectorMutex();
		private final Deque< Runnable > selectorTasks = new ArrayDeque<>();

		public SelectorThread( Interpreter interpreter )
			throws IOException {
			super( interpreter, threadGroup, interpreter.programFilename() + "-SelectorThread" );
			this.selector = Selector.open();
		}

		private Deque< Runnable > runKeys( SelectionKey[] selectedKeys )
			throws IOException {
			boolean keepRun;
			synchronized( this ) {
				do {
					for( final SelectionKey key : selectedKeys ) {
						if( key.isValid() ) {
							final SelectableStreamingCommChannel channel =
								(SelectableStreamingCommChannel) key.attachment();
							if( channel.rwLock.tryLock() ) {
								key.cancel();
								selectorTasks.add( () -> {
									try {
										try {
											try {
												key.channel().configureBlocking( true );
												if( channel.isOpen() ) {
													/*
													 * if ( channel.selectionTimeoutHandler() != null ) {
													 * interpreter.removeTimeoutHandler(
													 * channel.selectionTimeoutHandler() ); }
													 */
													scheduleReceive( channel, channel.parentInputPort() );
												} else {
													channel.closeImpl();
												}
											} catch( ClosedChannelException e ) {
												channel.closeImpl();
											}
										} catch( IOException e ) {
											throw e;
										} finally {
											channel.rwLock.unlock();
										}
									} catch( IOException e ) {
										if( channel.rwLock.isHeldByCurrentThread() ) {
											channel.rwLock.unlock();
										}
										interpreter.logWarning( e );
									}
								} );
							}
						}
					}
					synchronized( selectingMutex ) {
						if( selector.selectNow() > 0 ) { // Clean up the cancelled keys
							// If some new channels are selected, run again
							selectedKeys = selector.selectedKeys().toArray( new SelectionKey[ 0 ] );
							keepRun = true;
						} else {
							keepRun = false;
						}
					}
				} while( keepRun );
			}
			return selectorTasks;
		}

		private void runTasks( Deque< Runnable > tasks )
			throws IOException {
			Runnable r;
			while( (r = tasks.poll()) != null ) {
				r.run();
			}
		}

		@Override
		public void run() {
			while( active ) {
				try {
					SelectionKey[] selectedKeys;
					synchronized( selectingMutex ) {
						selector.select();
						selectedKeys = selector.selectedKeys().toArray( new SelectionKey[ 0 ] );
					}
					final Deque< Runnable > tasks = runKeys( selectedKeys );
					runTasks( tasks );
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}

			synchronized( this ) {
				for( SelectionKey key : selector.keys() ) {
					try {
						((SelectableStreamingCommChannel) key.attachment()).closeImpl();
					} catch( IOException e ) {
						interpreter.logWarning( e );
					}
				}
			}
		}

		public void register( SelectableStreamingCommChannel channel, int index ) {
			try {
				if( channel.inputStream().available() > 0 ) {
					scheduleReceive( channel, channel.parentInputPort() );
					return;
				}

				synchronized( this ) {
					if( !isSelecting( channel ) ) {
						selector.wakeup();
						SelectableChannel c = channel.selectableChannel();
						c.configureBlocking( false );
						synchronized( selectingMutex ) {
							c.register( selector, SelectionKey.OP_READ, channel );
							selector.wakeup();
							channel.setSelectorIndex( index );
						}
					}
				}
			} catch( ClosedChannelException e ) {
				interpreter.logWarning( e );
			} catch( IOException e ) {
				interpreter.logSevere( e );
			}
		}

		public void unregister( SelectableStreamingCommChannel channel )
			throws IOException {
			synchronized( this ) {
				if( isSelecting( channel ) ) {
					selector.wakeup();
					synchronized( selectingMutex ) {
						SelectionKey key = channel.selectableChannel().keyFor( selector );
						if( key != null ) {
							key.cancel();
						}
						selector.selectNow();
					}
					channel.selectableChannel().configureBlocking( true );
				}
			}
		}
	}

	protected boolean isSelecting( SelectableStreamingCommChannel channel ) {
		SelectableChannel c = channel.selectableChannel();
		return c != null && c.isRegistered();
	}

	protected void unregisterForSelection( SelectableStreamingCommChannel channel )
		throws IOException {
		selectorThreads()[ channel.selectorIndex() ].unregister( channel );
	}

	private final AtomicInteger nextSelector = new AtomicInteger( 0 );

	protected void registerForSelection( final SelectableStreamingCommChannel channel )
		throws IOException {
		final int i = nextSelector.getAndIncrement() % selectorThreads().length;
		selectorThreads()[ i ].register( channel, i );
		/*
		 * final TimeoutHandler handler = new TimeoutHandler( interpreter.persistentConnectionTimeout() ) {
		 * 
		 * @Override public void onTimeout() { try { if ( isSelecting( channel ) ) {
		 * selectorThread().unregister( channel ); channel.setToBeClosed( true ); channel.close(); } }
		 * catch( IOException e ) { interpreter.logSevere( e ); } } }; channel.setSelectionTimeoutHandler(
		 * handler ); if ( selectorThread().register( channel ) ) { interpreter.addTimeoutHandler( handler
		 * ); } else { channel.setSelectionTimeoutHandler( null ); }
		 */
	}

	/** Shutdowns the communication core, interrupting every communication-related thread. */
	public synchronized void shutdown( long timeout ) {
		if( active ) {
			active = false;
			listenersMap.entrySet().forEach( ( entry ) -> entry.getValue().shutdown() );

			try {
				for( SelectorThread t : selectorThreads() ) {
					t.selector.wakeup();
					try {
						t.join();
					} catch( InterruptedException e ) {
					}
				}
			} catch( IOException e ) {
				interpreter.logSevere( e );
			}

			try {
				channelHandlersLock.writeLock().tryLock( CHANNEL_HANDLER_TIMEOUT, TimeUnit.SECONDS );
			} catch( InterruptedException e ) {
			}
			executorService.shutdown();
			try {
				executorService.awaitTermination( timeout, TimeUnit.MILLISECONDS );
			} catch( InterruptedException e ) {
			}
			threadGroup.interrupt();
		}
	}

	private boolean active = false;
}
