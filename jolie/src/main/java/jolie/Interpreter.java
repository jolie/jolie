/*
 * Copyright (C) 2006-2019 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jolie.lang.CodeCheckException;
import jolie.lang.Constants;
import jolie.lang.Constants.ExecutionMode;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.TypeChecker;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.module.ModuleParsingConfiguration;
import jolie.lang.parse.module.Modules;
import jolie.lang.parse.module.SymbolTable;
import jolie.monitoring.MonitoringEvent;
import jolie.monitoring.events.MonitorAttachedEvent;
import jolie.monitoring.events.OperationStartedEvent;
import jolie.monitoring.events.SessionEndedEvent;
import jolie.monitoring.events.SessionStartedEvent;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.net.ports.OutputPort;
import jolie.process.DefinitionProcess;
import jolie.process.InputOperationProcess;
import jolie.process.SequentialProcess;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.ValueVector;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.correlation.CorrelationSet;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderFactory;
import jolie.tracer.DummyTracer;
import jolie.tracer.FileTracer;
import jolie.tracer.PrintingTracer;
import jolie.tracer.Tracer;
import jolie.tracer.TracerUtils;

/**
 * The Jolie interpreter engine. Multiple Interpreter instances can be run in the same JavaVM; this
 * is used, e.g., for service embedding.
 * 
 * @author Fabrizio Montesi
 */
public class Interpreter {
	private final class InitSessionThread extends SessionThread {
		public InitSessionThread( Interpreter interpreter, jolie.process.Process process, jolie.State state ) {
			super( interpreter, process, state );
			addSessionListener( new SessionListener() {
				@Override
				public void onSessionExecuted( SessionThread session ) {
					onSuccessfulInitExecution();
				}

				@Override
				public void onSessionError( SessionThread session, FaultException fault ) {
					exit();
				}
			} );
		}

		private void onSuccessfulInitExecution() {
			if( executionMode == Constants.ExecutionMode.SINGLE ) {
				synchronized( correlationEngine ) {
					try {
						mainSession = new SessionThread( getDefinition( "main" ), initExecutionThread );
						correlationEngine.onSingleExecutionSessionStart( mainSession );
						mainSession.addSessionListener( correlationEngine );
						correlationEngine.onSessionExecuted( this );
					} catch( InvalidIdException e ) {
						assert false;
					}
				}
			} else {
				correlationEngine.onSessionExecuted( this );
			}

			/*
			 * We need to relay the messages we did not consume during the init procedure. We do this
			 * asynchronously, because calling correlationEngine.onMessageReceive will trigger a join() on this
			 * thread, leading to a deadlock if we were to call that directly from here.
			 */
			execute( new Runnable() {
				private void pushMessages( Deque< SessionMessage > queue ) {
					for( SessionMessage message : queue ) {
						try {
							correlationEngine.onMessageReceive( message.message(), message.channel() );
						} catch( CorrelationError e ) {
							logWarning( e );
							try {
								message.channel()
									.send( CommMessage.createFaultResponse( message.message(), new FaultException(
										"CorrelationError",
										"The message you sent can not be correlated with any session and can not be used to start a new session." ) ) );
							} catch( IOException ioe ) {
								logSevere( ioe );
							}
						}
					}
				}

				@Override
				public void run() {
					for( Deque< SessionMessage > queue : messageQueues.values() ) {
						pushMessages( queue );
					}
					pushMessages( uncorrelatedMessageQueue );
				}
			} );
		}

		@Override
		public boolean isInitialisingThread() {
			return true;
		}

	}

	private static class JolieExecutionThreadFactory implements ThreadFactory {
		private final Interpreter interpreter;

		public JolieExecutionThreadFactory( Interpreter interpreter ) {
			this.interpreter = interpreter;
		}

		@Override
		public Thread newThread( Runnable r ) {
			JolieExecutorThread t = new JolieExecutorThread( r, interpreter );
			if( r instanceof ExecutionThread ) {
				t.setExecutionThread( (ExecutionThread) r );
			}
			return t;
		}
	}

	private static class NativeJolieThreadFactory implements ThreadFactory {
		private final Interpreter interpreter;

		public NativeJolieThreadFactory( Interpreter interpreter ) {
			this.interpreter = interpreter;
		}

		@Override
		public Thread newThread( Runnable r ) {
			return new NativeJolieThread( interpreter, r );
		}
	}

	public static class SessionStarter {
		private final InputOperationProcess guard;
		private final jolie.process.Process body;
		private CorrelationSet correlationInitializer = null;

		public SessionStarter( InputOperationProcess guard, jolie.process.Process body ) {
			this.guard = guard;
			this.body = body;
		}

		public InputOperationProcess guard() {
			return guard;
		}

		public jolie.process.Process body() {
			return body;
		}

		public void setCorrelationInitializer( CorrelationSet cset ) {
			correlationInitializer = cset;
		}

		public CorrelationSet correlationInitializer() {
			return correlationInitializer;
		}
	}

	private static final Logger LOGGER = Logger.getLogger( Constants.JOLIE_LOGGER_NAME );

	private CommCore commCore;
	private Program internalServiceProgram = null;
	private final Value receivingEmbeddedValue;
	private Interpreter parentInterpreter = null;

	private Map< String, SessionStarter > sessionStarters = new HashMap<>();
	private volatile boolean exiting = false;
	private final Lock exitingLock;
	private final Condition exitingCondition;
	private final CorrelationEngine correlationEngine;
	private final List< CorrelationSet > correlationSets = new ArrayList<>();
	private final Map< String, CorrelationSet > operationCorrelationSetMap = new HashMap<>();
	private Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	private final Value globalValue = Value.createRootValue();
	private final Collection< EmbeddedServiceLoader > embeddedServiceLoaders = new ArrayList<>();

	private final Map< String, DefinitionProcess > definitions = new HashMap<>();
	private final Map< String, OutputPort > outputPorts = new HashMap<>();
	private final Map< String, InputOperation > inputOperations = new HashMap<>();

	private final HashMap< String, Object > locksMap = new HashMap<>();

	private final String[] includePaths;

	private final String logPrefix;
	private final Tracer tracer;

	private boolean check = false;
	private final long persistentConnectionTimeout = 2 * 1000; // 2 seconds
	private final long awaitTerminationTimeout = 5 * 1000; // 5 seconds

	private final Map< URI, SymbolTable > symbolTables;

	private final Configuration configuration;

	private final ScheduledExecutorService scheduledExecutor =
		Executors.newSingleThreadScheduledExecutor( new NativeJolieThreadFactory( this ) );

	private final File programDirectory;
	private OutputPort monitor = null;

	private final Cleaner cleaner = Cleaner.create();

	public Cleaner cleaner() {
		return cleaner;
	}

	public void setMonitor( OutputPort monitor ) {
		this.monitor = monitor;
		fireMonitorEvent( new MonitorAttachedEvent() );
	}

	public boolean isMonitoring() {
		return monitor != null;
	}

	/*
	 * public long inputMessageTimeout() { return inputMessageTimeout; }
	 */

	public String logPrefix() {
		return logPrefix;
	}

	public Tracer tracer() {
		return tracer;
	}

	public void fireMonitorEvent( MonitoringEvent event ) {
		if( monitor != null ) {
			CommMessage m = CommMessage.createRequest( "pushEvent", "/", MonitoringEvent.toValue( event ) );
			CommChannel channel = null;
			try {
				channel = monitor.getCommChannel();
				channel.send( m );
				CommMessage response;
				do {
					response = channel.recvResponseFor( m ).get();
				} while( response == null );
			} catch( URISyntaxException | InterruptedException | ExecutionException | IOException e ) {
				logWarning( e );
			} finally {
				if( channel != null ) {
					try {
						channel.release();
					} catch( IOException e ) {
						logWarning( e );
					}
				}
			}
		}
	}

	public long persistentConnectionTimeout() {
		return persistentConnectionTimeout;
	}

	public long responseTimeout() {
		return configuration.responseTimeout();
	}

	public CorrelationEngine correlationEngine() {
		return correlationEngine;
	}

	public Future< ? > schedule( Runnable task, long delay ) {
		return scheduledExecutor.schedule( task, delay, TimeUnit.MILLISECONDS );
	}

	/**
	 * Returns the option arguments passed to this Interpreter.
	 * 
	 * @return the option arguments passed to this Interpreter
	 */
	public String[] optionArgs() {
		return configuration.optionArgs();
	}

	/**
	 * Returns the include paths this Interpreter is considering.
	 * 
	 * @return the include paths this Interpreter is considering
	 */
	public String[] includePaths() {
		return includePaths;
	}

	/**
	 * Registers a session starter on this <code>Interpreter</code>.
	 * 
	 * @param guard the input guard for this session starter
	 * @param body the body of this session starter
	 */
	public void registerSessionStarter( InputOperationProcess guard, jolie.process.Process body ) {
		sessionStarters.put( guard.inputOperation().id(), new SessionStarter( guard, body ) );
	}

	/**
	 * Returns the output ports of this Interpreter.
	 * 
	 * @return the output ports of this Interpreter
	 */
	public Collection< OutputPort > outputPorts() {
		return outputPorts.values();
	}

	/**
	 * Returns the InputOperation identified by key.
	 * 
	 * @param key the name of the InputOperation to return
	 * @return the InputOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested
	 *         InputOperation
	 * @see InputOperation
	 */
	public InputOperation getInputOperation( String key )
		throws InvalidIdException {
		InputOperation ret = inputOperations.get( key );
		if( ret == null ) {
			throw new InvalidIdException( key );
		}
		return ret;
	}

	/**
	 * As {@link #getInputOperation(String) getInputOperation}, with the additional constraint that key
	 * must identify a OneWayOperation.
	 * 
	 * @param key the name of the OneWayOperation to return
	 * @return the OneWayOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested
	 *         OneWayOperation
	 * @see OneWayOperation
	 */
	public OneWayOperation getOneWayOperation( String key )
		throws InvalidIdException {
		InputOperation ret;
		if( (ret = inputOperations.get( key )) == null || !(ret instanceof OneWayOperation) )
			throw new InvalidIdException( key );
		return (OneWayOperation) ret;
	}

	/**
	 * As {@link #getInputOperation(String) getInputOperation}, with the additional constraint that key
	 * must identify a RequestResponseOperation.
	 * 
	 * @param key the name of the RequestResponseOperation to return
	 * @return the RequestResponseOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested
	 *         RequestResponseOperation
	 * @see RequestResponseOperation
	 */
	public RequestResponseOperation getRequestResponseOperation( String key )
		throws InvalidIdException {
		InputOperation ret;
		if( (ret = inputOperations.get( key )) == null || !(ret instanceof RequestResponseOperation) )
			throw new InvalidIdException( key );
		return (RequestResponseOperation) ret;
	}

	/**
	 * Returns the OutputPort identified by key.
	 * 
	 * @param key the name of the OutputPort to return
	 * @return the OutputPort identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested
	 *         OutputPort
	 */
	public synchronized OutputPort getOutputPort( String key )
		throws InvalidIdException {
		OutputPort ret;
		if( (ret = outputPorts.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}

	/**
	 * Removes a registered OutputPort.
	 * 
	 * @param key the name of the OutputPort to remove
	 */
	public synchronized void removeOutputPort( String key ) {
		outputPorts.remove( key );
	}

	/**
	 * Returns the Definition identified by key.
	 * 
	 * @param key the name of the Definition to return
	 * @return the Definition identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested
	 *         Definition
	 */
	public DefinitionProcess getDefinition( String key )
		throws InvalidIdException {
		DefinitionProcess ret;
		if( (ret = definitions.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}

	/**
	 * Registers an <code>OutputPort</code> on this interpreter.
	 * 
	 * @param key the name of the <code>OutputPort</code> to register
	 * @param value the <code>OutputPort</code> to register
	 */
	public void register( String key, OutputPort value ) {
		outputPorts.put( key, value );
	}

	/**
	 * Registers a defined sub-routine on this interpreter.
	 * 
	 * @param key the name of the defined sub-routine to register
	 * @param value the defined sub-routine to register
	 */
	public void register( String key, DefinitionProcess value ) {
		definitions.put( key, value );
	}

	/**
	 * Registers an <code>InputOperation</code> on this interpreter.
	 * 
	 * @param key the name of the <code>InputOperation</code> to register
	 * @param value the <code>InputOperation</code> to register
	 */
	public void register( String key, InputOperation value ) {
		inputOperations.put( key, value );
	}

	/**
	 * Registers an <code>EmbeddedServiceLoader</code> on this interpreter.
	 * 
	 * @param n the <code>EmbeddedServiceLoader</code> to register
	 */
	public void addEmbeddedServiceLoader( EmbeddedServiceLoader n ) {
		embeddedServiceLoaders.add( n );
	}

	/**
	 * Returns the <code>EmbeddedServiceLoader</code> instances registered on this interpreter.
	 * 
	 * @return the <code>EmbeddedServiceLoader</code> instances registered on this interpreter
	 */
	public Collection< EmbeddedServiceLoader > embeddedServiceLoaders() {
		return embeddedServiceLoaders;
	}

	/**
	 * Makes this <code>Interpreter</code> entering in exiting mode. When in exiting mode, an
	 * interpreter waits for each session to finish its execution and then terminates gracefully the
	 * execution of the entire program. An interpreter in exiting mode cannot receive any more messages.
	 *
	 * Multiple calls of this method are redundant.
	 *
	 * The fact that the interpreter cannot receive any more messages after entering exiting mode can
	 * cause deadlocks if a session is waiting for a message to finish its execution. Use this method
	 * with caution.
	 */
	public void exit() {
		exit( executionMode == ExecutionMode.SINGLE ? 0L : awaitTerminationTimeout );
	}

	/**
	 * Makes this <code>Interpreter</code> entering in exiting mode. When in exiting mode, an
	 * interpreter waits for each session to finish its execution and then terminates gracefully the
	 * execution of the entire program. An interpreter in exiting mode cannot receive any more messages.
	 *
	 * Multiple calls of this method are redundant.
	 *
	 * The fact that the interpreter cannot receive any more messages after entering exiting mode can
	 * cause deadlocks if a session is waiting for a message to finish its execution. Use this method
	 * with caution.
	 * 
	 * @param terminationTimeout the timeout for the wait of the termination of running processes
	 */
	public void exit( long terminationTimeout ) {
		synchronized( this ) {
			if( exiting ) {
				return;
			} else {
				exiting = true;
			}
		}
		exitingLock.lock();
		try {
			exitingCondition.signalAll();
		} finally {
			exitingLock.unlock();
		}
		final List< Runnable > pendingTimedTasks = scheduledExecutor.shutdownNow();
		execute( () -> pendingTimedTasks.forEach( Runnable::run ) );
		processExecutorService.shutdown();
		nativeExecutorService.shutdown();
		commCore.shutdown( terminationTimeout );
		try {
			nativeExecutorService.awaitTermination( terminationTimeout, TimeUnit.MILLISECONDS );
		} catch( InterruptedException e ) {
		}
		try {
			processExecutorService.awaitTermination( terminationTimeout, TimeUnit.MILLISECONDS );
		} catch( InterruptedException e ) {
		}
		try {
			scheduledExecutor.awaitTermination( terminationTimeout, TimeUnit.MILLISECONDS );
		} catch( InterruptedException e ) {
		}
		free();
	}

	/**
	 * Returns <code>true</code> if this interpreter is in exiting mode, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if this interpreter is in exiting mode, <code>false</code> otherwise
	 * @see #exit()
	 */
	public boolean exiting() {
		return exiting;
	}

	/**
	 * Logs an unhandled fault using the logger of this interpreter. This method is used by sessions
	 * that had to terminate due to a fault which could not be handled, due to a missing fault handler.
	 * 
	 * @param fault the <code>FaultException</code> that could not be handled
	 */
	public void logUnhandledFault( FaultException fault ) {
		StringWriter writer = new StringWriter();
		try {
			new ValuePrettyPrinter( fault.value(), writer,
				"Thrown unhandled fault: " + fault.faultName() + "\nContent (if any)" ).run();
			logInfo( writer.toString() );
		} catch( IOException e ) {
			logInfo( "Thrown unhandled fault: " + fault.faultName() );
		}
	}

	private String buildLogMessage( String message ) {
		return '[' + logPrefix + "] " + message;
	}

	/**
	 * Logs an information message using the logger of this interpreter.
	 * 
	 * @param message the message to logLevel
	 */
	public void logInfo( String message ) {
		LOGGER.log( buildLogRecord( Level.INFO, buildLogMessage( message ) ) );
	}

	/**
	 * Logs an information message using the logger of this interpreter (logger level: fine).
	 * 
	 * @param message the message to logLevel
	 */
	public void logFine( String message ) {
		LOGGER.log( buildLogRecord( Level.FINE, buildLogMessage( message ) ) );
	}

	private String buildLogMessage( Throwable t ) {
		String ret;
		if( configuration.printStackTraces() ) {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			t.printStackTrace( new PrintStream( bs ) );
			ret = bs.toString();
		} else {
			ret = t.getMessage();
		}
		return ret;
	}

	private LogRecord buildLogRecord( Level level, String message ) {
		LogRecord record = new LogRecord( level, message );
		record.setSourceClassName( configuration.programFilepath().getName() );
		return record;
	}

	/**
	 * Logs an information message using the logger of this interpreter (logger level: fine).
	 * 
	 * @param t the <code>Throwable</code> object whose stack trace has to be logged
	 */
	public void logFine( Throwable t ) {
		LOGGER.log( buildLogRecord( Level.FINE, buildLogMessage( t ) ) );
	}

	/**
	 * Logs a severe error message using the logger of this interpreter.
	 * 
	 * @param message the message to logLevel
	 */
	public void logSevere( String message ) {
		LOGGER.log( buildLogRecord( Level.SEVERE, buildLogMessage( message ) ) );
	}

	/**
	 * Logs a warning message using the logger of this interpreter.
	 * 
	 * @param message the message to logLevel
	 */
	public void logWarning( String message ) {
		LOGGER.log( buildLogRecord( Level.WARNING, buildLogMessage( message ) ) );
	}

	/**
	 * Logs a severe error message, created by reading the stack trace of the passed
	 * <code>Throwable</code>, using the logger of this interpreter.
	 * 
	 * @param t the <code>Throwable</code> object whose stack trace has to be logged
	 */
	public void logSevere( Throwable t ) {
		LOGGER.log( buildLogRecord( Level.SEVERE, buildLogMessage( t ) ) );
	}

	/**
	 * Logs a warning message, created by reading the stack trace of the passed <code>Throwable</code>,
	 * using the logger of this interpreter.
	 * 
	 * @param t the <code>Throwable</code> object whose stack trace has to be logged
	 */
	public void logWarning( Throwable t ) {
		LOGGER.log( buildLogRecord( Level.WARNING, buildLogMessage( t ) ) );
	}

	/**
	 * Returns the execution mode of this Interpreter.
	 * 
	 * @return the execution mode of this Interpreter
	 * @see Constants.ExecutionMode
	 */
	public Constants.ExecutionMode executionMode() {
		return executionMode;
	}

	/**
	 * Sets the execution mode of this Interpreter.
	 * 
	 * @param mode the execution mode to set
	 * @see Constants.ExecutionMode
	 */
	public void setExecutionMode( Constants.ExecutionMode mode ) {
		executionMode = mode;
	}

	/**
	 * Adds a correlation set to this interpreter.
	 * 
	 * @param set the correlation set to add
	 */
	public void addCorrelationSet( CorrelationSet set ) {
		correlationSets.add( set );
		for( String operation : set.correlatingOperations() ) {
			operationCorrelationSetMap.put( operation, set );
		}
	}

	public CorrelationSet getCorrelationSetForOperation( String operationName ) {
		return operationCorrelationSetMap.get( operationName );
	}

	/**
	 * Returns the correlation sets of this Interpreter.
	 * 
	 * @return the correlation sets of this Interpreter
	 */
	public List< CorrelationSet > correlationSets() {
		return correlationSets;
	}

	/**
	 * Returns the Interpreter the current thread is referring to.
	 * 
	 * @return the Interpreter the current thread is referring to
	 */
	public static Interpreter getInstance() {
		Thread t = Thread.currentThread();
		if( t instanceof InterpreterThread ) {
			return ((InterpreterThread) t).interpreter();
		}

		return null;
	}

	/**
	 * Returns the JolieClassLoader this Interpreter is using.
	 * 
	 * @return the JolieClassLoader this Interpreter is using
	 */
	public JolieClassLoader getClassLoader() {
		return configuration.jolieClassLoader();
	}

	/**
	 * returns this interpreter's configuration
	 * 
	 * @return
	 */
	public Configuration configuration() {
		return configuration;
	}

	/**
	 * Constructor.
	 *
	 * @param programDirectory the program directory of this Interpreter, necessary if it is run inside
	 *        a JAP file.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( Configuration configuration,
		File programDirectory, Optional< Value > params, Optional< String > parentLogPrefix )
		throws IOException {
		TracerUtils.TracerLevels tracerLevel = TracerUtils.TracerLevels.ALL;
		this.configuration = configuration;

		this.symbolTables = new HashMap<>();

		switch( configuration.tracerLevel() ) {
		case "comm":
			tracerLevel = TracerUtils.TracerLevels.COMM;
			break;
		case "comp":
			tracerLevel = TracerUtils.TracerLevels.COMP;
			break;
		}

		this.correlationEngine = configuration.correlationAlgorithm().createInstance( this );

		commCore = new CommCore( this, configuration.connectionsLimit() /* , cmdParser.connectionsCache() */ );
		includePaths = configuration.includePaths();

		logPrefix =
			(parentLogPrefix.isPresent() ? parentLogPrefix.get() + " -> " + configuration.programFilepath().getName()
				: configuration.programFilepath().getName())
				+ (configuration.executionTarget() != null ? " -> " + configuration.executionTarget() : "");

		if( configuration.tracer() ) {
			if( configuration.tracerMode().equals( "file" ) ) {
				tracer = new FileTracer( this, tracerLevel );
			} else {
				tracer = new PrintingTracer( this, tracerLevel );
			}
		} else {
			tracer = new DummyTracer();
		}

		LOGGER.setLevel( configuration.logLevel() );

		exitingLock = new ReentrantLock();
		exitingCondition = exitingLock.newCondition();

		if( configuration.programDirectory() == null ) {
			this.programDirectory = programDirectory;
		} else {
			this.programDirectory = configuration.programDirectory();
		}

		if( this.programDirectory == null ) {
			throw new IOException(
				"Could not localise the service execution directory. This might be a bug in the Jolie interpreter, please report it to https://github.com/jolie/jolie" );
		}

		this.receivingEmbeddedValue = params.orElse( Value.create() );
	}

	/**
	 * Constructor.
	 *
	 * @param programDirectory the program directory of this Interpreter, necessary if it is run inside
	 *        a JAP file.
	 * @param parentInterpreter
	 * @param internalServiceProgram
	 * @throws FileNotFoundException if one of the passed input files is not found.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( Configuration configuration,
		File programDirectory, Interpreter parentInterpreter, Program internalServiceProgram )
		throws FileNotFoundException, IOException {
		this( configuration, programDirectory, Optional.empty(), Optional.of( parentInterpreter.logPrefix() ) );

		this.parentInterpreter = parentInterpreter;
		this.internalServiceProgram = internalServiceProgram;
	}

	/**
	 * Constructor. for the JolieServiceNodeLoader
	 *
	 * @param programDirectory the program directory of this Interpreter, necessary if it is run inside
	 *        a JAP file.
	 * @param parentSymbolTables symbol table from the parent service
	 * @param internalServiceProgram
	 * @param receivingEmbeddedValue
	 * @throws FileNotFoundException if one of the passed input files is not found.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( Configuration configuration,
		File programDirectory, Map< URI, SymbolTable > parentSymbolTables, Program internalServiceProgram,
		Value receivingEmbeddedValue, String parentLogPrefix )
		throws FileNotFoundException, IOException {
		this( configuration, programDirectory, Optional.of( receivingEmbeddedValue ), Optional.of( parentLogPrefix ) );
		this.internalServiceProgram = internalServiceProgram;
		this.symbolTables.putAll( parentSymbolTables );
	}

	/**
	 * Returns the parent directory of the program executed by this Interpreter.
	 * 
	 * @return the parent directory of the program executed by this Interpreter.
	 */
	public File programDirectory() {
		return programDirectory;
	}

	public Interpreter parentInterpreter() {
		return parentInterpreter;
	}

	/**
	 * Returns the receiving value from Service node's embed statement.
	 * 
	 * @return the receiving value from Service node's embed statement.
	 */
	public Value receivingEmbeddedValue() {
		return receivingEmbeddedValue;
	}

	/**
	 * Returns the program filename this interpreter was launched with.
	 * 
	 * @return the program filename this interpreter was launched with
	 */
	public String programFilename() {
		return configuration.programFilepath().getName();
	}

	/**
	 * Returns the path at which the file to be interpreted has been found
	 * 
	 * @return the path at which the file to be interpreted has been found
	 */
	public String programFilepath() {
		return configuration.programFilepath().toString();
	}

	/**
	 * Returns the global lock registered on this interpreter with the passed identifier. If a global
	 * lock with such identifier is not registered, a new one is automatically created, registered and
	 * returned.
	 * 
	 * @param id the global lock identifier
	 * @return the global lock registered on this interpreter with the specified identifier
	 */
	public synchronized Object getLock( String id ) {
		return locksMap.computeIfAbsent( id, k -> new Object() );
	}

	public SessionStarter getSessionStarter( String operationName ) {
		return sessionStarters.get( operationName );
	}

	/**
	 * Returns the {@code global} value of this Interpreter.
	 * 
	 * @return the {@code global} value of this Interpreter
	 */
	public Value globalValue() {
		return globalValue;
	}

	private InitSessionThread initExecutionThread;
	private SessionThread mainSession = null;
	private final Queue< SessionThread > waitingSessionThreads = new LinkedList<>();

	/**
	 * Returns the {@link SessionThread} of the Interpreter that started the program execution.
	 * 
	 * @return the {@link SessionThread} of the Interpreter that started the program execution
	 */
	public SessionThread initThread() {
		return initExecutionThread;
	}

	/**
	 * Starts this interpreter, returning a <code>Future</code> which can be interrogated to know when
	 * the interpreter start procedure has been completed and the interpreter is ready to receive
	 * messages.
	 * 
	 * @return a <code>Future</code> which can be interrogated to know when the interpreter start
	 *         procedure has been completed and the interpreter is ready to receive messages.
	 */
	public Future< Exception > start() {
		CompletableFuture< Exception > f = new CompletableFuture<>();
		(new StarterThread( f )).start();
		return f;
	}

	private void init( State initState )
		throws InterpreterException, IOException {
		/**
		 * Order is important. 1 - CommCore needs the OOIT to be initialized. 2 - initExec must be
		 * instantiated before we can receive communications.
		 */
		if( !buildOOIT( initState.root() ) && !check ) {
			throw new InterpreterException( "Error: service initialisation failed" );
		}
		if( check ) {
			exit();
		} else {
			sessionStarters = Collections.unmodifiableMap( sessionStarters );
			try {
				initExecutionThread = new InitSessionThread( this, getDefinition( "init" ), initState );

				commCore.init();

				// Initialize program arguments in the args variabile.
				ValueVector jArgs = ValueVector.create();
				for( String s : configuration.arguments() ) {
					jArgs.add( Value.create( s ) );
				}
				initExecutionThread.state().root().getChildren( "args" ).deepCopy( jArgs );
				/*
				 * initExecutionThread.addSessionListener( new SessionListener() { public void onSessionExecuted(
				 * SessionThread session ) {} public void onSessionError( SessionThread session, FaultException
				 * fault ) { exit(); } });
				 */

				correlationEngine.onSingleExecutionSessionStart( initExecutionThread );
				// initExecutionThread.addSessionListener( correlationEngine );
				initExecutionThread.start();
			} catch( InvalidIdException e ) {
				assert false;
			}
		}
	}

	private void runCode() {
		if( !check ) {
			SessionThread t;
			synchronized( this ) {
				t = initExecutionThread;
			}
			try {
				if( t != null ) {
					t.join();
				}
			} catch( InterruptedException e ) {
				logSevere( e );
			}

			if( executionMode == Constants.ExecutionMode.SINGLE ) {
				synchronized( this ) {
					t = mainSession;
				}
				try {
					if( t != null ) {
						mainSession.start();
						mainSession.join();
					}
				} catch( InterruptedException e ) {
					logSevere( e );
				}
			} else {
				exitingLock.lock();
				try {
					exitingCondition.await();
				} catch( InterruptedException e ) {
					logSevere( e );
				} finally {
					exitingLock.unlock();
				}
			}
		}
	}

	/**
	 * Runs the interpreter behaviour specified by command line. The default behaviour is to execute the
	 * input code.
	 *
	 * Note that you must shutdown the CommCore of this Interpreter manually after calling this method.
	 * 
	 * @throws IOException if a Parser propagates a Scanner exception
	 * @throws InterpreterException if the interpretation tree could not be built
	 */
	public void run()
		throws InterpreterException, IOException {
		init( new jolie.State() );
		runCode();
	}

	private final ExecutorService nativeExecutorService =
		new JolieThreadPoolExecutor( new NativeJolieThreadFactory( this ) );
	private final ExecutorService processExecutorService =
		new JolieThreadPoolExecutor( new JolieExecutionThreadFactory( this ) );

	/**
	 * Runs an asynchronous task in this Interpreter internal thread pool.
	 * 
	 * @param r the Runnable object to execute
	 */
	public void execute( Runnable r ) {
		nativeExecutorService.execute( r );
	}

	public Future< ? > runJolieThread( Runnable task ) {
		return processExecutorService.submit( task );
	}

	private static final AtomicInteger STARTER_THREAD_COUNTER = new AtomicInteger();

	private static String createStarterThreadName( String programFilename ) {
		return programFilename + "-Starter-" + STARTER_THREAD_COUNTER.incrementAndGet();
	}

	public class StarterThread extends Thread {
		private final CompletableFuture< Exception > future;
		private final jolie.State initState = new jolie.State();

		public StarterThread( CompletableFuture< Exception > future ) {
			super( createStarterThreadName( configuration.programFilepath().getName() ) );
			this.future = future;
			setContextClassLoader( configuration.jolieClassLoader() );
		}

		@Override
		public void run() {
			try {
				init( initState );
				future.complete( null );
			} catch( IOException | InterpreterException e ) {
				future.complete( e );
			}
			runCode();
			// commCore.shutdown();
			exit();
		}

		public jolie.State initState() {
			return initState;
		}
	}

	private void free() {
		/*
		 * We help the Java(tm) Garbage Collector. Looks like it needs this or the Interpreter does not get
		 * collected.
		 */
		definitions.clear();
		inputOperations.clear();
		locksMap.clear();
		initExecutionThread = null;
		sessionStarters = new HashMap<>();
		outputPorts.clear();
		correlationSets.clear();
		globalValue.erase();
		embeddedServiceLoaders.clear();
		configuration.clear();
		symbolTables.clear();
		Modules.freeCache( configuration.programFilepath().toURI() );
		commCore = null;
		// System.gc();
	}

	/**
	 * Returns the CommCore of this Interpreter.
	 * 
	 * @return the CommCore of this Interpreter
	 */
	public CommCore commCore() {
		return commCore;
	}

	private boolean buildOOIT( Value initValue )
		throws InterpreterException {
		try {
			Program program;
			if( configuration.isProgramCompiled() ) {
				try( final ObjectInputStream istream = new ObjectInputStream( configuration.inputStream() ) ) {
					final Object o = istream.readObject();
					if( o instanceof Program ) {
						program = (Program) o;
					} else {
						throw new InterpreterException( "Input compiled program is not a JOLIE program" );
					}
				}
			} else {
				if( this.internalServiceProgram != null ) {
					program = this.internalServiceProgram;
					program = OLParseTreeOptimizer.optimize( program );
				} else {
					ModuleParsingConfiguration configuration = new ModuleParsingConfiguration(
						configuration().charset(),
						configuration().includePaths(),
						configuration().packagePaths(),
						configuration().jolieClassLoader(),
						configuration().constants(),
						false,
						true );
					Modules.ModuleParsedResult parsedResult =
						Modules.parseModule( configuration, configuration().inputStream(),
							configuration().programFilepath().toURI() );
					this.symbolTables.putAll( parsedResult.symbolTables() );
					program = parsedResult.mainProgram();
				}
			}

			configuration.inputStream().close();

			check = configuration.check();

			final SemanticVerifier semanticVerifier;

			SemanticVerifier.Configuration conf =
				new SemanticVerifier.Configuration( configuration.executionTarget() );

			if( check ) {
				conf.setCheckForMain( false );
			}
			semanticVerifier = new SemanticVerifier( program, this.symbolTables, conf );

			try {
				semanticVerifier.validate();
			} catch( CodeCheckException e ) {
				LOGGER.severe( e.getMessage() );
				throw new InterpreterException( "Exiting" );
			}

			if( configuration.typeCheck() ) {
				TypeChecker typeChecker = new TypeChecker(
					program,
					semanticVerifier.executionMode(),
					semanticVerifier.correlationFunctionInfo() );
				if( !typeChecker.check() ) {
					throw new InterpreterException( "Exiting" );
				}
			}

			if( check ) {
				return false;
			} else {
				return (new OOITBuilder(
					this,
					program,
					semanticVerifier.constantFlags(),
					semanticVerifier.correlationFunctionInfo(),
					initValue ))
						.build();
			}

		} catch( IOException | ParserException | ClassNotFoundException | ModuleException e ) {
			throw new InterpreterException( e );
		}
	}

	/**
	 * Starts a service session.
	 * 
	 * @param message the message triggering the session start
	 * @param channel the channel of the message triggering the session start
	 * @return {@code true} if the service session is started, {@code false} otherwise
	 */
	public boolean startServiceSession( final CommMessage message, CommChannel channel ) {
		if( executionMode == Constants.ExecutionMode.SINGLE ) {
			return false;
		}

		SessionStarter starter = sessionStarters.get( message.operationName() );
		if( starter == null ) {
			return false;
		}

		try {
			initExecutionThread.join();
		} catch( InterruptedException e ) {
			return false;
		}

		final SessionThread spawnedSession;

		if( executionMode == Constants.ExecutionMode.CONCURRENT ) {
			State state = initExecutionThread.state().clone();
			jolie.process.Process sequence = new SequentialProcess( new jolie.process.Process[] {
				starter.guard.receiveMessage( new SessionMessage( message, channel ), state ),
				starter.body
			} );
			spawnedSession = new SessionThread(
				sequence, state, initExecutionThread );
			correlationEngine.onSessionStart( spawnedSession, starter, message );
			spawnedSession.addSessionListener( correlationEngine );

			logSessionStart( message, spawnedSession.getSessionId() );

			spawnedSession.addSessionListener( new SessionListener() {
				public void onSessionExecuted( SessionThread session ) {
					logSessionEnd( message, session.getSessionId() );
				}

				public void onSessionError( SessionThread session, FaultException fault ) {
					logSessionEnd( message, session.getSessionId() );
				}
			} );
			spawnedSession.start();
		} else if( executionMode == Constants.ExecutionMode.SEQUENTIAL ) {
			/*
			 * We use sessionThreads to handle sequential execution of spawn requests
			 */
			State state = initExecutionThread.state().clone();
			jolie.process.Process sequence = new SequentialProcess( new jolie.process.Process[] {
				starter.guard.receiveMessage( new SessionMessage( message, channel ), state ),
				starter.body
			} );
			spawnedSession = new SessionThread(
				sequence, state, initExecutionThread );
			correlationEngine.onSessionStart( spawnedSession, starter, message );

			logSessionStart( message, spawnedSession.getSessionId() );
			spawnedSession.addSessionListener( correlationEngine );
			spawnedSession.addSessionListener( new SessionListener() {
				public void onSessionExecuted( SessionThread session ) {
					synchronized( waitingSessionThreads ) {
						if( !waitingSessionThreads.isEmpty() ) {
							waitingSessionThreads.poll();
							if( !waitingSessionThreads.isEmpty() ) {
								waitingSessionThreads.peek().start();
							}
						}
					}
					logSessionEnd( message, session.getSessionId() );
				}

				public void onSessionError( SessionThread session, FaultException fault ) {
					synchronized( waitingSessionThreads ) {
						if( !waitingSessionThreads.isEmpty() ) {
							waitingSessionThreads.poll();
							if( !waitingSessionThreads.isEmpty() ) {
								waitingSessionThreads.peek().start();
							}
						}
					}
					logSessionEnd( message, session.getSessionId() );
				}
			} );
			synchronized( waitingSessionThreads ) {
				if( waitingSessionThreads.isEmpty() ) {
					waitingSessionThreads.add( spawnedSession );
					spawnedSession.start();
				} else {
					waitingSessionThreads.add( spawnedSession );
				}
			}
		}
		return true;
	}

	private void logSessionStart( CommMessage commMessage, String sessionId ) {
		if( isMonitoring() ) {
			fireMonitorEvent( new SessionStartedEvent( commMessage.operationName(), sessionId,
				Long.toString( commMessage.id() ) ) );
			fireMonitorEvent(
				new OperationStartedEvent( commMessage.operationName(), sessionId,
					Long.toString( commMessage.requestId() ), commMessage.value(),
					Long.toString( commMessage.id() ) ) );
		}
	}

	private void logSessionEnd( CommMessage commMessage, String sessionId ) {
		if( isMonitoring() ) {
			fireMonitorEvent(
				new SessionEndedEvent( commMessage.operationName(), sessionId, Long.toString( commMessage.id() ) ) );
		}
	}

	private final Map< String, EmbeddedServiceLoaderFactory > embeddingFactories = new ConcurrentHashMap<>();

	public EmbeddedServiceLoaderFactory getEmbeddedServiceLoaderFactory( String name )
		throws IOException {
		EmbeddedServiceLoaderFactory factory = embeddingFactories.get( name );
		if( factory == null ) {
			factory = getClassLoader().createEmbeddedServiceLoaderFactory( name, this );
			if( factory != null ) {
				embeddingFactories.put( name, factory );
			}
		}
		return factory;
	}

	public Map< URI, SymbolTable > symbolTables() {
		return this.symbolTables;
	}

	public static class Configuration {
		private final Integer connectionsLimit;
		private final int cellId;
		private final CorrelationEngine.Type correlationAlgorithm;
		private final String[] includePaths;
		private final String[] optionArgs;
		private final String[] arguments;
		private final URL[] libURLs;
		private final InputStream inputStream;
		private final String charset;
		private final File programFilepath;
		private final Map< String, Scanner.Token > constants = new HashMap<>();
		private JolieClassLoader jolieClassLoader;
		private final boolean isProgramCompiled;
		private final boolean typeCheck;
		private final boolean tracer;
		private final String tracerMode;
		private final String tracerLevel;
		private final boolean check;
		private final long responseTimeout;
		private final boolean printStackTraces;
		private final Level logLevel;
		private final File programDirectory;
		private final String[] packagePaths;
		private final String executionTarget;
		private final Optional< Path > parametersFilePath;

		private Configuration( int connectionsLimit,
			int cellId,
			CorrelationEngine.Type correlationAlgorithm,
			String[] includeList,
			String[] optionArgs,
			URL[] libUrls,
			InputStream inputStream,
			String charset,
			File programFilepath,
			String[] arguments,
			Map< String, Scanner.Token > constants,
			JolieClassLoader jolieClassLoader,
			boolean programCompiled,
			boolean typeCheck,
			boolean tracer,
			String tracerLevel,
			String tracerMode,
			boolean check,
			boolean printStackTraces,
			long responseTimeout,
			Level logLevel,
			File programDirectory,
			String[] packagePaths,
			String executionTarget,
			Optional< Path > parametersFilePath ) {
			this.connectionsLimit = connectionsLimit;
			this.cellId = cellId;
			this.correlationAlgorithm = correlationAlgorithm;
			this.includePaths = includeList;
			this.optionArgs = optionArgs;
			this.libURLs = libUrls;
			this.inputStream = inputStream;
			this.charset = charset;
			this.programFilepath = programFilepath;
			this.arguments = arguments;
			this.constants.putAll( constants );
			this.jolieClassLoader = jolieClassLoader;
			this.isProgramCompiled = programCompiled;
			this.typeCheck = typeCheck;
			this.tracer = tracer;
			this.tracerLevel = tracerLevel;
			this.tracerMode = tracerMode;
			this.check = check;
			this.printStackTraces = printStackTraces;
			this.responseTimeout = responseTimeout;
			this.logLevel = logLevel;
			this.programDirectory = programDirectory;
			this.packagePaths = packagePaths;
			this.executionTarget = executionTarget;
			this.parametersFilePath = parametersFilePath;
		}

		public static Configuration create( int connectionsLimit,
			int cellId,
			CorrelationEngine.Type correlationAlgorithm,
			String[] includeList,
			String[] optionArgs,
			URL[] libUrls,
			InputStream inputStream,
			String charset,
			File programFilepath,
			String[] arguments,
			Map< String, Scanner.Token > constants,
			JolieClassLoader jolieClassLoader,
			boolean programCompiled,
			boolean typeCheck,
			boolean tracer,
			String tracerLevel,
			String tracerMode,
			boolean check,
			boolean printStackTraces,
			long responseTimeout,
			Level logLevel,
			File programDirectory,
			String[] packagePaths,
			String executionTarget,
			Optional< Path > parametersFilePath ) {
			return new Configuration( connectionsLimit, cellId, correlationAlgorithm, includeList, optionArgs, libUrls,
				inputStream, charset, programFilepath, arguments, constants, jolieClassLoader, programCompiled,
				typeCheck, tracer, tracerLevel, tracerMode, check, printStackTraces, responseTimeout, logLevel,
				programDirectory, packagePaths, executionTarget, parametersFilePath );
		}

		public static Configuration create( Configuration config,
			File programFilepath,
			InputStream inputStream ) {
			return create( config.connectionsLimit, config.cellId, config.correlationAlgorithm, config.includePaths,
				config.optionArgs,
				config.libURLs, inputStream, config.charset, programFilepath, config.arguments, config.constants,
				config.jolieClassLoader, config.isProgramCompiled, config.typeCheck, config.tracer, config.tracerLevel,
				config.tracerMode, config.check, config.printStackTraces, config.responseTimeout, config.logLevel,
				config.programDirectory, config.packagePaths, config.executionTarget, config.parametersFilePath );
		}

		public static Configuration create( Configuration config,
			File programFilepath,
			InputStream inputStream,
			String executionTarget ) {
			return create( config.connectionsLimit, config.cellId, config.correlationAlgorithm, config.includePaths,
				config.optionArgs,
				config.libURLs, inputStream, config.charset, programFilepath, config.arguments, config.constants,
				config.jolieClassLoader, config.isProgramCompiled, config.typeCheck, config.tracer, config.tracerLevel,
				config.tracerMode, config.check, config.printStackTraces, config.responseTimeout, config.logLevel,
				config.programDirectory, config.packagePaths, executionTarget, config.parametersFilePath );
		}

		/**
		 * Returns the connection limit parameter passed by command line with the -c option.
		 *
		 * @return the connection limit parameter passed by command line
		 */
		public Integer connectionsLimit() {
			return this.connectionsLimit;
		}

		/**
		 * Returns the cellId parameter passed by command line with the --cellId option.
		 *
		 * @return the cellId parameter passed by command line
		 */
		public int cellId() {
			return this.cellId;
		}

		/**
		 * Returns the type of correlation algorithm that has been specified.
		 *
		 * @return the type of correlation algorithm that has been specified.
		 * @see CorrelationEngine
		 */
		public CorrelationEngine.Type correlationAlgorithm() {
			return this.correlationAlgorithm;
		}

		/**
		 * Returns the include paths passed by command line with the -i option.
		 *
		 * @return the include paths passed by command line
		 */
		public String[] includePaths() {
			return includePaths;
		}

		/**
		 * Returns the command line options passed to this command line parser. This does not include the
		 * name of the program.
		 *
		 * @return the command line options passed to this command line parser.
		 */
		public String[] optionArgs() {
			return optionArgs;
		}

		/**
		 * Returns the library URLs passed by command line with the -l option.
		 *
		 * @return the library URLs passed by command line
		 */
		public URL[] libUrls() {
			return libURLs;
		}

		/**
		 * Returns an InputStream for the program code to execute.
		 *
		 * @return an InputStream for the program code to execute
		 */
		public InputStream inputStream() {
			return this.inputStream;
		}

		/**
		 * Returns the program's character encoding
		 *
		 * @return the program's character encoding
		 */
		public String charset() {
			return this.charset;
		}

		/**
		 * Returns the file path of the JOLIE program to execute.
		 *
		 * @return the file path of the JOLIE program to execute
		 */
		public File programFilepath() {
			return this.programFilepath;
		}

		/**
		 * Returns the arguments passed to the JOLIE program.
		 *
		 * @return the arguments passed to the JOLIE program.
		 */
		public String[] arguments() {
			return arguments;
		}

		/**
		 * Returns a map containing the constants defined by command line.
		 *
		 * @return a map containing the constants defined by command line
		 */
		public Map< String, Scanner.Token > constants() {
			return this.constants;
		}

		/**
		 * Returns the classloader to use for the program.
		 *
		 * @return the classloader to use for the program.
		 */
		public JolieClassLoader jolieClassLoader() {
			return jolieClassLoader;
		}

		/**
		 * Returns {@code true} if the program is compiled, {@code false} otherwise.
		 *
		 * @return {@code true} if the program is compiled, {@code false} otherwise.
		 */
		public boolean isProgramCompiled() {
			return isProgramCompiled;
		}

		/**
		 * Returns the value of the --typecheck option.
		 *
		 * @return the value of the --typecheck option.
		 */
		public boolean typeCheck() {
			return this.typeCheck;
		}

		/**
		 * Returns <code>true</code> if the tracer option has been specified, false otherwise.
		 *
		 * @return <code>true</code> if the verbose option has been specified, false otherwise
		 */
		public boolean tracer() {
			return this.tracer;
		}

		/**
		 * Returns <code>true</code> if the tracer option has been specified, false otherwise.
		 *
		 * @return <code>true</code> if the verbose option has been specified, false otherwise
		 */
		public String tracerMode() {
			return tracerMode;
		}

		/**
		 * Returns the selected tracer level [all | comm | comp]
		 *
		 * all: all the traces comp: only computation traces comm: only communication traces
		 */
		public String tracerLevel() {
			return tracerLevel;
		}

		/**
		 * Returns <code>true</code> if the check option has been specified, false otherwise.
		 *
		 * @return <code>true</code> if the verbose option has been specified, false otherwise
		 */
		public boolean check() {
			return this.check;
		}

		/**
		 * Returns the response timeout parameter passed by command line with the --responseTimeout option.
		 *
		 * @return the response timeout parameter passed by command line
		 */
		public long responseTimeout() {
			return responseTimeout;
		}

		public boolean printStackTraces() {
			return printStackTraces;
		}

		/**
		 * Returns the execution service target of this interpreter.
		 *
		 * @return the execution service target of this interpreter.
		 */
		public String executionTarget() {
			return this.executionTarget;
		}

		/**
		 * Returns the {@link Level} of the logger of this interpreter.
		 *
		 * @return the {@link Level} of the logger of this interpreter.
		 */
		public Level logLevel() {
			return this.logLevel;
		}

		/**
		 * Returns the package paths passed by command line with the -p option.
		 *
		 * @return the package paths passed by command line
		 */
		public String[] packagePaths() {
			return packagePaths;
		}

		/**
		 * Returns the directory in which the main program is located.
		 *
		 * @return the directory in which the main program is located.
		 */
		public File programDirectory() {
			return programDirectory;
		}

		public void clear() {
			jolieClassLoader = null;
		}

		public Optional< Path > parametersPath() {
			return parametersFilePath;
		}
	}
}
