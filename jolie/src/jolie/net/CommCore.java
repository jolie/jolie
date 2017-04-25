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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.JolieThreadPoolExecutor;
import jolie.StatefulContext;
import jolie.behaviours.Behaviour;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.TimeoutHandler;
import jolie.runtime.VariablePath;

/**
 * Handles the communications mechanisms for an Interpreter instance.
 *
 * Each CommCore is related to an Interpreter, and each Interpreter owns one and
 * only CommCore instance.
 *
 * @author Fabrizio Montesi
 */
public class CommCore
{
	private final Map< String, CommListener> listenersMap = new HashMap<>();
	private final static int CHANNEL_HANDLER_TIMEOUT = 5;
	private final ThreadGroup threadGroup;

	private static final Logger logger = Logger.getLogger( "JOLIE" );

	private final int connectionsLimit;
	// private final int connectionCacheSize;
	private final Interpreter interpreter;

	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );

	// Location URI -> Protocol name -> Persistent CommChannel object
	private final Map< URI, Map< String, CommChannel>> persistentChannels = new HashMap<>();
	
	private final EventLoopGroup oioGroup;
	private final EventLoopGroup workerGroup;
	private final EventLoopGroup bossGroup;
	
	public EventLoopGroup getBlockingEventLoopGroup() {
		return oioGroup;
	}

	private void removePersistentChannel( URI location, String protocol, Map< String, CommChannel> protocolChannels )
	{
		protocolChannels.remove( protocol );
		if ( protocolChannels.isEmpty() ) {
			persistentChannels.remove( location );
		}
	}

	private void removePersistentChannel( URI location, String protocol, CommChannel channel )
	{
		if ( persistentChannels.containsKey( location ) ) {
			if ( persistentChannels.get( location ).get( protocol ) == channel ) {
				removePersistentChannel( location, protocol, persistentChannels.get( location ) );
			}
		}
	}

	public CommChannel getPersistentChannel( URI location, String protocol, StatefulContext context )
	{
		CommChannel ret = null;
		synchronized( persistentChannels ) {
			Map< String, CommChannel> protocolChannels = persistentChannels.get( location );
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
							//if ( ret.isThreadSafe() == false ) {
							removePersistentChannel( location, protocol, protocolChannels );
							//} else {
							// If we return a channel, make sure it will not timeout!
							ret.setTimeoutHandler( null );
							//if ( ret.timeoutHandler() != null ) {
							//interpreter.removeTimeoutHandler( ret.timeoutHandler() );
							// ret.setTimeoutHandler( null );
							//}
							//}
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

	private void setTimeoutHandler( final CommChannel channel, final URI location, final String protocol )
	{
		/*if ( channel.timeoutHandler() != null ) {
			interpreter.removeTimeoutHandler( channel.timeoutHandler() );
		}*/

		final TimeoutHandler handler = new TimeoutHandler( interpreter.persistentConnectionTimeout() )
		{
			@Override
			public void onTimeout()
			{
				try {
					synchronized( persistentChannels ) {
						if ( channel.timeoutHandler() == this ) {
							removePersistentChannel( location, protocol, channel );
							channel.close();
							channel.setTimeoutHandler( null );
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		};
		channel.setTimeoutHandler( handler );
		interpreter.addTimeoutHandler( handler );
	}

	public void putPersistentChannel( URI location, String protocol, final CommChannel channel )
	{
		synchronized( persistentChannels ) {
			Map< String, CommChannel> protocolChannels = persistentChannels.get( location );
			if ( protocolChannels == null ) {
				protocolChannels = new HashMap<>();
				persistentChannels.put( location, protocolChannels );
			}
			// Set the timeout
			setTimeoutHandler( channel, location, protocol );
			// Put the protocol in the cache (may overwrite another one)
			protocolChannels.put( protocol, channel );
			/*if ( protocolChannels.size() <= connectionCacheSize && protocolChannels.containsKey( protocol ) == false ) {
				// Set the timeout
				setTimeoutHandler( channel );
				// Put the protocol in the cache
				protocolChannels.put( protocol, channel );
			} else {
				try {
					if ( protocolChannels.get( protocol ) != channel ) {
						channel.close();
					} else {
						setTimeoutHandler( channel );
					}
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			}*/
		}
	}

	/**
	 * Returns the Interpreter instance this CommCore refers to.
	 *
	 * @return the Interpreter instance this CommCore refers to
	 */
	public Interpreter interpreter()
	{
		return interpreter;
	}

	/**
	 * Constructor.
	 *
	 * @param interpreter the Interpreter to refer to for this CommCore
	 * operations
	 * @param connectionsLimit if more than zero, specifies an upper bound to
	 * the connections handled in parallel.
	 * @throws java.io.IOException
	 */
	public CommCore( Interpreter interpreter, int connectionsLimit /*, int connectionsCacheSize */ )
		throws IOException
	{
		this.interpreter = interpreter;
		this.localListener = LocalListener.create( interpreter );
		this.connectionsLimit = connectionsLimit;
		this.threadGroup = new ThreadGroup( "CommCore-" + interpreter.hashCode() );

		executorService = new JolieThreadPoolExecutor( new CommThreadFactory() );

		bossGroup = new NioEventLoopGroup( 4, new ExecutionContextThreadFactory() );
		workerGroup = new NioEventLoopGroup( 4, new ExecutionContextThreadFactory() );
		oioGroup = new OioEventLoopGroup(0, new ExecutionContextThreadFactory() );
		
		//TODO make socket an extension, too?
		CommListenerFactory listenerFactory = new NioSocketListenerFactory( this, bossGroup, workerGroup );
		listenerFactories.put( "socket", listenerFactory );
		CommChannelFactory channelFactory = new NioSocketCommChannelFactory( this, workerGroup );
		channelFactories.put( "socket", channelFactory );
	}

	public class ExecutionContextThread extends Thread
	{
		private Interpreter interpreter;
		private StatefulContext executionContext = null;

		private ExecutionContextThread( Runnable r, Interpreter interpreter )
		{
			super( r );
			this.interpreter = interpreter;
		}

		public void executionContext( StatefulContext context )
		{
			executionContext = context;
		}

		public StatefulContext executionContext()
		{
			return executionContext;
		}

		public Interpreter interpreter()
		{
			return interpreter;
		}

	}

	public class ExecutionContextThreadFactory implements ThreadFactory
	{

		@Override
		public Thread newThread( Runnable r )
		{
			ExecutionContextThread t = new ExecutionContextThread( r, interpreter() );
			if ( r instanceof StatefulContext ) {
				t.executionContext((StatefulContext) r );
			}
			return t;
		}

	}

	/**
	 * Returns the Logger used by this CommCore.
	 *
	 * @return the Logger used by this CommCore
	 */
	public Logger logger()
	{
		return logger;
	}

	/**
	 * Returns the connectionsLimit of this CommCore.
	 *
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

	private final Collection< Behaviour> protocolConfigurations = new LinkedList<>();

	public Collection< Behaviour> protocolConfigurations()
	{
		return protocolConfigurations;
	}

	public CommListener getListenerByInputPortName( String serviceName )
	{
		return listenersMap.get( serviceName );
	}

	private final Map< String, CommChannelFactory> channelFactories = new HashMap<>();

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

	public CommChannel createCommChannel( URI uri, OutputPort port, StatefulContext ctx  )
		throws IOException
	{
		String medium = uri.getScheme();
		CommChannelFactory factory = getCommChannelFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}
		
		CommChannel ret = factory.createChannel( uri, port, ctx );
		return ret;
	}

	private final Map< String, CommProtocolFactory> protocolFactories = new HashMap<>();

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

	private final Map< String, CommListenerFactory> listenerFactories = new HashMap<>();

	private final LocalListener localListener;

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}
	
	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}
	
	public LocalCommChannel getLocalCommChannel()
	{
		return new LocalCommChannel( interpreter, localListener );
	}

	public LocalCommChannel getLocalCommChannel( CommListener listener )
	{
		return new LocalCommChannel( interpreter, listener );
	}

	public CommListenerFactory getCommListenerFactory( String name )
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

	public void addLocalInputPort( InputPort inputPort )
		throws IOException
	{
		localListener.mergeInterface( inputPort.getInterface() );
		localListener.addAggregations( inputPort.aggregationMap() );
		localListener.addRedirections( inputPort.redirectionMap() );
		listenersMap.put( inputPort.name(), localListener );
	}

	/**
	 * Adds an input port to this <code>CommCore</code>. This method is not
	 * thread-safe.
	 *
	 * @param inputPort the {@link InputPort} to add
	 * @param protocolFactory the <code>CommProtocolFactory</code> to use for
	 * the input port
	 * @param protocolConfigurationProcess the protocol configuration process to
	 * execute for configuring the created protocols
	 * @throws java.io.IOException in case of some underlying implementation
	 * error
	 * @see URI
	 * @see CommProtocolFactory
	 */
	public void addInputPort(
		InputPort inputPort,
		CommProtocolFactory protocolFactory,
		Behaviour protocolConfigurationProcess
	)
		throws IOException
	{
		protocolConfigurations.add( protocolConfigurationProcess );

		String medium = inputPort.location().getScheme();
		CommListenerFactory factory = getCommListenerFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}

		CommListener listener = factory.createListener(
			interpreter,
			protocolFactory,
			inputPort
		);
		listenersMap.put( inputPort.name(), listener );
	}

	private final ExecutorService executorService;

	private final static class CommThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread( Runnable r )
		{
			return new CommChannelHandler( r );
		}
	}

	private final static Pattern pathSplitPattern = Pattern.compile( "/" );

	/**
	 * Schedules the receiving of a message on this <code>CommCore</code>
	 * instance.
	 *
	 * @param channel the <code>CommChannel</code> to use for receiving the
	 * message
	 * @param port the <code>Port</code> responsible for the message receiving
	 */
	@Deprecated
	public void scheduleReceive( CommChannel channel, InputPort port )
	{
		//executorService.execute( new CommChannelHandlerRunnable( channel, port ) );
	}

	/**
	 * Initializes the communication core, starting its communication listeners.
	 * This method is asynchronous. When it returns, every communication
	 * listener has been issued to start, but they are not guaranteed to be
	 * ready to receive messages. This method throws an exception if some
	 * listener cannot be issued to start; other errors will be logged by the
	 * listener through the interpreter logger.
	 *
	 * @throws IOException in case of some underlying <code>CommListener</code>
	 * initialization error
	 * @see CommListener
	 */
	public void init()
		throws IOException
	{
		listenersMap.entrySet().forEach( ( entry ) -> {
			entry.getValue().start();
		} );
	}

	/**
	 * Shutdowns the communication core, interrupting every
	 * communication-related thread.
	 */
	public synchronized void shutdown()
	{
		if ( active ) {
			active = false;
			
			listenersMap.entrySet().forEach( ( entry ) -> {
				entry.getValue().shutdown();
			} );

			try {
				channelHandlersLock.writeLock().tryLock( CHANNEL_HANDLER_TIMEOUT, TimeUnit.SECONDS );
			} catch( InterruptedException e ) {
			}
			executorService.shutdown();
			try {
				executorService.awaitTermination( interpreter.persistentConnectionTimeout(), TimeUnit.MILLISECONDS );
			} catch( InterruptedException e ) {
			}
			threadGroup.interrupt();
		}
	}
	
	public synchronized void shutdownNetty() {
		
		Future bossShutdown = null, workerShutdown = null;
		if (!bossGroup.isShuttingDown())
			bossShutdown = bossGroup.shutdownGracefully();
		if (!workerGroup.isShuttingDown())
			workerShutdown = workerGroup.shutdownGracefully();
		
		try {
			if (bossShutdown != null)
				bossShutdown.get();
			if (workerShutdown != null)
				workerShutdown.get();
		} catch( InterruptedException | ExecutionException ex ) {
			Logger.getLogger( CommCore.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	private boolean active = false;
}
