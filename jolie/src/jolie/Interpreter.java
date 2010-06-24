/***************************************************************************
 *   Copyright (C) 2006-09-10 by Fabrizio Montesi <famontesi@gmail.com>    *
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

package jolie;

import java.io.ByteArrayOutputStream;
import jolie.lang.Constants;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import jolie.lang.parse.OLParseTreeOptimizer;

import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.net.CommCore;
import jolie.net.OutputPort;
import jolie.process.CorrelatedProcess;
import jolie.process.DefinitionProcess;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.TimeoutHandler;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

/**
 * The Jolie interpreter engine.
 * Multiple Interpreter instances can be run in the same JavaVM;
 * this is used, e.g., for service embedding.
 * @author Fabrizio Montesi
 */
public class Interpreter
{
	private CommCore commCore;
	private CommandLineParser cmdParser;
	
	private boolean exiting = false;
	private final Collection< List< VariablePath > > correlationSet =
				new HashSet< List< VariablePath > > ();
	private Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	private final Value globalValue = Value.createRootValue();
	private final String[] arguments;
	private final Collection< EmbeddedServiceLoader > embeddedServiceLoaders =
			new LinkedList< EmbeddedServiceLoader >();
	private final Logger logger = Logger.getLogger( "JOLIE" );
	
	private final Map< String, DefinitionProcess > definitions =
				new HashMap< String, DefinitionProcess >();
	private final Map< String, OutputPort > outputPorts = new HashMap< String, OutputPort >();
	private final Map< String, InputOperation > inputOperations =
				new HashMap< String, InputOperation>();
	
	private final HashMap< String, Object > locksMap =
				new HashMap< String, Object >();
	
	private final Set< CorrelatedProcess > sessionSpawners =
				new HashSet< CorrelatedProcess >();
	
	private final ClassLoader parentClassLoader;
	private final String[] includePaths;
	private final String[] optionArgs;
	private final String logPrefix;
	private final boolean verbose;
	private final Timer timer;
	private long inputMessageTimeout = 24 * 60 * 60 * 1000; // 1 day
	private long openInputConnectionTimeout = 24 * 60 * 60 * 1000; // 1 day

	private final Queue< TimeoutHandler > timeoutHandlerQueue = new PriorityQueue< TimeoutHandler >( 11, new TimeoutHandler.Comparator() );
	private final ExecutorService timeoutHandlerExecutor = Executors.newSingleThreadExecutor();

	private final String programFilename;
	private final File programDirectory;

	public long inputMessageTimeout()
	{
		return inputMessageTimeout;
	}

	public long openInputConnectionTimeout()
	{
		return openInputConnectionTimeout;
	}

	public void schedule( TimerTask task, long delay )
	{
		if ( exiting == false ) {
			timer.schedule( task, delay );
		}
	}

	public void addTimeoutHandler( TimeoutHandler handler )
	{
		synchronized( timeoutHandlerQueue ) {
			timeoutHandlerQueue.add( handler );
			checkForExpiredTimeoutHandlers();
		}
	}

	public void removeTimeoutHandler( TimeoutHandler handler )
	{
		synchronized( timeoutHandlerQueue ) {
			timeoutHandlerQueue.remove( handler );
		}
	}

	private void checkForExpiredTimeoutHandlers()
	{
		long currentTime = System.currentTimeMillis();
		TimeoutHandler handler = timeoutHandlerQueue.peek();
		while( handler != null && handler.time() < currentTime ) {
			final TimeoutHandler h = handler;
			timeoutHandlerExecutor.execute( new Runnable() {
				public void run()
				{
					h.onTimeout();
				}
			} );
			timeoutHandlerQueue.remove();
			handler = timeoutHandlerQueue.peek();
		}
	}
	
	/**
	 * Returns the option arguments passed to this Interpreter.
	 * @return the option arguments passed to this Interpreter
	 */
	public String[] optionArgs()
	{
		return optionArgs;
	}
	
	/**
	 * Returns the include paths this Interpreter is considering.
	 * @return the include paths this Interpreter is considering
	 */
	public String[] includePaths()
	{
		return includePaths;
	}

	/**
	 * Registers a session spawner on this <code>Interpreter</code>.
	 * This must be done for all session spawners in order to ensure correct
	 * execution termination.
	 * @param process the session spawner to register
	 */
	public void registerSessionSpawner( CorrelatedProcess process )
	{
		synchronized( sessionSpawners ) {
			sessionSpawners.add( process );
		}
	}

	/**
	 * Unregisters a session spawner.
	 * @param process the session spawner to unregister
	 */
	public void unregisterSessionSpawner( CorrelatedProcess process )
	{
		synchronized( sessionSpawners ) {
			sessionSpawners.remove( process );
		}
	}
	
	private JolieClassLoader classLoader;
	
	/**
	 * Returns the output ports of this Interpreter.
	 * @return the output ports of this Interpreter
	 */
	public Collection< OutputPort > outputPorts()
	{
		return outputPorts.values();
	}
		
	/**
	 * Returns the InputOperation identified by key.
	 * @param key the name of the InputOperation to return
	 * @return the InputOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested InputOperation
	 * @see InputOperation
	 */
	public InputOperation getInputOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}
	
	/**
	 * As {@link #getInputOperation(String) getInputOperation}, with the additional constraint that key must identify a OneWayOperation.
	 * @param key the name of the OneWayOperation to return
	 * @return the OneWayOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested OneWayOperation
	 * @see OneWayOperation
	 */
	public OneWayOperation getOneWayOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null || !(ret instanceof OneWayOperation) )
			throw new InvalidIdException( key );
		return (OneWayOperation)ret;
	}
	
	/**
	 * As {@link #getInputOperation(String) getInputOperation}, with the additional constraint that key must identify a RequestResponseOperation.
	 * @param key the name of the RequestResponseOperation to return
	 * @return the RequestResponseOperation identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested RequestResponseOperation
	 * @see RequestResponseOperation
	 */
	public RequestResponseOperation getRequestResponseOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null || !(ret instanceof RequestResponseOperation) )
			throw new InvalidIdException( key );
		return (RequestResponseOperation)ret;
	}
	
	/**
	 * Returns the OutputPort identified by key.
	 * @param key the name of the OutputPort to return
	 * @return the OutputPort identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested OutputPort
	 */
	public synchronized OutputPort getOutputPort( String key )
		throws InvalidIdException
	{
		OutputPort ret;
		if ( (ret=outputPorts.get( key )) == null )
			throw new InvalidIdException( key );
		return (OutputPort)ret;
	}
	
	/**
	 * Removes a registered OutputPort.
	 * @param key the name of the OutputPort to remove
	 */
	public synchronized void removeOutputPort( String key )
	{
		outputPorts.remove( key );
	}

	/**
	 * Returns the Definition identified by key.
	 * @param key the name of the Definition to return
	 * @return the Definition identified by key
	 * @throws jolie.runtime.InvalidIdException if this Interpreter does not own the requested Definition
	 */
	public DefinitionProcess getDefinition( String key )
		throws InvalidIdException
	{
		DefinitionProcess ret;
		if ( (ret=definitions.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}

	/**
	 * Registers an <code>OutputPort</code> on this interpreter.
	 * @param key the name of the <code>OutputPort</code> to register
	 * @param value the <code>OutputPort</code> to register
	 */
	public void register( String key, OutputPort value )
	{
		outputPorts.put( key, value );
	}

	/**
	 * Registers a defined sub-routine on this interpreter.
	 * @param key the name of the defined sub-routine to register
	 * @param value the defined sub-routine to register
	 */
	public void register( String key, DefinitionProcess value )
	{
		definitions.put( key, value );
	}

	/**
	 * Registers an <code>InputOperation</code> on this interpreter.
	 * @param key the name of the <code>InputOperation</code> to register
	 * @param value the <code>InputOperation</code> to register
	 */
	public void register( String key, InputOperation value )
	{
		inputOperations.put( key, value );
	}

	/**
	 * Registers an <code>EmbeddedServiceLoader</code> on this interpreter.
	 * @param key the name of the <code>EmbeddedServiceLoader</code> to register
	 * @param value the <code>EmbeddedServiceLoader</code> to register
	 */
	public void addEmbeddedServiceLoader( EmbeddedServiceLoader n )
	{
		embeddedServiceLoaders.add( n );
	}

	/**
	 * Returns the <code>EmbeddedServiceLoader</code> instances registered on this interpreter.
	 * @return the <code>EmbeddedServiceLoader</code> instances registered on this interpreter
	 */
	public Collection< EmbeddedServiceLoader > embeddedServiceLoaders()
	{
		return embeddedServiceLoaders;
	}

	/**
	 * Makes this <code>Interpreter</code> entering in exiting mode.
	 * When in exiting mode, an interpreter waits for each session to finish
	 * its execution and then terminates gracefully the execution of the entire program.
	 * An interpreter in exiting mode cannot receive any more messages.
	 *
	 * Multiple calls of this method are redundant.
	 *
	 * The fact that the interpreter cannot receive any more messages after
	 * entering exiting mode can cause deadlocks if a session is waiting for a
	 * message to finish its execution. Use this method with caution.
	 */
	public void exit()
	{
		synchronized( this ) {
			if ( exiting ) {
				return;
			} else {
				exiting = true;
			}
		}

		commCore.shutdown();
		synchronized( sessionSpawners ) {
			for( CorrelatedProcess p : sessionSpawners ) {
				p.interpreterExit();
			}
		}
		timer.cancel();
	}

	/**
	 * Returns <code>true</code> if this interpreter is in exiting mode, <code>false</code> otherwise.
	 * @return <code>true</code> if this interpreter is in exiting mode, <code>false</code> otherwise
	 * @see #exit()
	 */
	public boolean exiting()
	{
		return exiting;
	}

	/**
	 * Logs an unhandled fault using the logger of this interpreter.
	 * This method is used by sessions that had to terminate due to a fault
	 * which could not be handled, due to a missing fault handler.
	 * @param fault the <code>FaultException</code> that could not be handled
	 */
	public void logUnhandledFault( FaultException fault )
	{
		logger.info( logPrefix + "Thrown unhandled fault: " + fault.faultName() );
	}

	/**
	 * Logs an information message using the logger of this interpreter.
	 * @param message the message to log
	 */
	public void logInfo( String message )
	{
		logger.info( logPrefix + message );
	}

	/**
	 * Logs a severe error message using the logger of this interpreter.
	 * @param message the message to log
	 */
	public void logSevere( String message )
	{
		logger.severe( logPrefix + message );
	}

	/**
	 * Logs a warning message using the logger of this interpreter.
	 * @param message the message to log
	 */
	public void logWarning( String message )
	{
		logger.warning( logPrefix + message );
	}

	/**
	 * Logs a severe error message, created by reading the stack trace of the passed
	 * <code>Throwable</code>, using the logger of this interpreter.
	 * @param t the <code>Throwable</code> object whose stack trace has to be logged
	 */
	public void logSevere( Throwable t )
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		t.printStackTrace( new PrintStream( bs ) );
		logger.severe( logPrefix + bs.toString() );
	}

	/**
	 * Logs a warning message, created by reading the stack trace of the passed
	 * <code>Throwable</code>, using the logger of this interpreter.
	 * @param t the <code>Throwable</code> object whose stack trace has to be logged
	 */
	public void logWarning( Throwable t )
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		t.printStackTrace( new PrintStream( bs ) );
		logger.warning( logPrefix + bs.toString() );
	}
	
	/**
	 * Returns the execution mode of this Interpreter.
	 * @return the execution mode of this Interpreter
	 * @see Constants.ExecutionMode
	 */
	public Constants.ExecutionMode executionMode()
	{
		return executionMode;
	}
	
	/**
	 * Sets the execution mode of this Interpreter.
	 * @param mode the execution mode to set
	 * @see Constants.ExecutionMode
	 */
	public void setExecutionMode( Constants.ExecutionMode mode )
	{
		executionMode = mode;
	}

	/**
	 * Sets the correlation set of this interpreter.
	 * @param set the correlation set to set
	 */
	public void setCorrelationSet( Set< List< VariablePath > > set )
	{
		correlationSet.clear();
		correlationSet.addAll( set );
	}
	
	/**
	 * Returns the correlation set of this Interpreter.
	 * @return the correlation set of this Interpreter
	 */
	public Collection< List< VariablePath > > correlationSet()
	{
		return correlationSet;
	}
	
	/**
	 * Returns the Interpreter the current thread is referring to.
	 * @return the Interpreter the current thread is referring to
	 */
	public static Interpreter getInstance()
	{
		return ((JolieThread)Thread.currentThread()).interpreter();
	}
	
	/**
	 * Returns the JolieClassLoader this Interpreter is using.
	 * @return the JolieClassLoader this Interpreter is using
	 */
	public JolieClassLoader getClassLoader()
	{
		return classLoader;
	}

	/**
	 * Returns <code>true</code> if this interpreter is in verbose mode.
	 * @return <code>true</code> if this interpreter is in verbose mode
	 */
	public boolean verbose()
	{
		return verbose;
	}

	/** Constructor.
	 *
	 * @param args The command line arguments.
	 * @param parentClassLoader the parent ClassLoader to fall back when not finding resources.
	 * @throws CommandLineException if the command line is not valid or asks for simple information. (like --help and --version)
	 * @throws FileNotFoundException if one of the passed input files is not found.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, FileNotFoundException, IOException
	{
		this( args, parentClassLoader, null );
	}
	
	/** Constructor.
	 *
	 * @param args The command line arguments.
	 * @param parentClassLoader the parent ClassLoader to fall back when not finding resources.
	 * @param programDirectory the program directory of this Interpreter, necessary if it is run inside a JAP file.
	 * @throws CommandLineException if the command line is not valid or asks for simple information. (like --help and --version)
	 * @throws FileNotFoundException if one of the passed input files is not found.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( String[] args, ClassLoader parentClassLoader, File programDirectory )
		throws CommandLineException, FileNotFoundException, IOException
	{
		this.parentClassLoader = parentClassLoader;
		cmdParser = new CommandLineParser( args, parentClassLoader );
		classLoader = cmdParser.jolieClassLoader();
		optionArgs = cmdParser.optionArgs();
		programFilename = new File( cmdParser.programFilepath() ).getName();
		arguments = cmdParser.arguments();
		commCore = new CommCore( this, cmdParser.connectionsLimit(), cmdParser.connectionsCache() );
		includePaths = cmdParser.includePaths();

		StringBuilder builder = new StringBuilder();
		builder.append( '[' );
		builder.append( programFilename );
		builder.append( "] " );
		logPrefix = builder.toString();

		verbose = cmdParser.verbose();
		timer = new Timer( programFilename + "-Timer" );

		if ( cmdParser.programDirectory() == null ) {
			this.programDirectory = programDirectory;
		} else {
			this.programDirectory = cmdParser.programDirectory();
		}
		if ( this.programDirectory == null ) {
			throw new IOException( "Could not localize the service execution directory. This is probably a bug in the JOLIE interpreter, please report it to jolie-devel@lists.sf.net" );
		}
	}

	/**
	 * Returns the parent directory of the program executed by this Interpreter.
	 * @return the parent directory of the program executed by this Interpreter.
	 */
	public File programDirectory()
	{
		return programDirectory;
	}

	/**
	 * Returns the program filename this interpreter was launched with.
	 * @return the program filename this interpreter was launched with
	 */
	public String programFilename()
	{
		return programFilename;
	}

	/**
	 * Returns the parent class loader passed to the constructor of this interpreter.
	 * @return the parent class loader passed to the constructor of this interpreter
	 */
	public ClassLoader parentClassLoader()
	{
		return parentClassLoader;
	}

	/**
	 * Returns the global lock registered on this interpreter with the passed identifier.
	 * If a global lock with such identifier is not registered, a new one is
	 * automatically created, registered and returned.
	 * @param id the global lock identifier
	 * @return the global lock registered on this interpreter with the specified identifier
	 */
	public Object getLock( String id )
	{
		Object l = locksMap.get( id );
		if ( l == null ) {
			l = new Object();
			locksMap.put( id, l );
		}
		return l;
	}

	/**
	 * Returns the {@code global} value of this Interpreter.
	 * @return the {@code global} value of this Interpreter
	 */
	public Value globalValue()
	{
		return globalValue;
	}
	
	private SessionThread mainExec;
	
	/**
	 * Returns the SessionThread of the Interpreter that started the program execution.
	 * @return the SessionThread of the Interpreter that started the program execution
	 */
	public SessionThread mainThread()
	{
		return mainExec;
	}
	
	private static class InterpreterStartFuture implements Future< Exception >
	{
		private final Lock lock;
		private final Condition initCompleted;
		private Exception result;
		private boolean isDone = false;
		
		public InterpreterStartFuture()
		{
			lock = new ReentrantLock();
			initCompleted = lock.newCondition();
		}
		
		public boolean cancel( boolean mayInterruptIfRunning )
		{
			return false;
		}
		
		public Exception get( long timeout, TimeUnit unit )
			throws InterruptedException, TimeoutException
		{
			try {
				lock.lock();
				if ( !isDone ) {
					if ( !initCompleted.await( timeout, unit ) ) {
						throw new TimeoutException();
					}
				}
			} finally {
				lock.unlock();
			}
			return result;
		}
		
		public Exception get()
			throws InterruptedException
		{
			try {
				lock.lock();
				if ( !isDone ) {
					initCompleted.await();
				}
			} finally {
				lock.unlock();
			}
			return result;
		}
		
		public boolean isCancelled()
		{
			return false;
		}
		
		public boolean isDone()
		{
			return isDone;
		}
		
		private void setResult( Exception e )
		{
			lock.lock();
			try {
				result = e;
				isDone = true;
				initCompleted.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}
	
	/**
	 * Starts this interpreter, returning a <code>Future</code> which can
	 * be interrogated to know when the interpreter start procedure has been 
	 * completed and the interpreter is ready to receive messages.
	 * @return a <code>Future</code> which can
	 *		be interrogated to know when the interpreter start procedure has been 
	 *		completed and the interpreter is ready to receive messages.
	 */
	public Future< Exception > start()
	{
		InterpreterStartFuture f = new InterpreterStartFuture();
		(new StarterThread( f )).start();
		return f;
	}
	
	private void init()
		throws InterpreterException, IOException
	{
		/**
		 * Order is important. CommCore needs the OOIT to be initialized.
		 */
		if ( buildOOIT() == false ) {
			throw new InterpreterException( "Error: the interpretation environment couldn't have been initialized" );
		}
		commCore.init();
	}
	
	private void runMain()
	{
		DefinitionProcess main = null;
		try {
			main = getDefinition( "main" );
		} catch ( InvalidIdException e ) {
			// As the parser checks this for us, execution should never reach this point.
			assert false;
		}
		
		mainExec = new SessionThread( this, main );
		
		// Initialize program arguments in the args variabile.
		ValueVector jArgs = ValueVector.create();
		
		for( String s : arguments ) {
			jArgs.add( Value.create( s ) );
		}
		mainExec.state().root().getChildren( "args" ).deepCopy( jArgs );
		
		mainExec.start();
		try {
			mainExec.join();
		} catch( InterruptedException e ) {
			logSevere( e );
		}
	}
	
	/**
	 * Runs the interpreter behaviour specified by command line.
	 * The default behaviour is to execute the input code.
	 *
	 * Note that you must shutdown the CommCore of this Interpreter
	 * manually after calling this method.
	 * @throws IOException if a Parser propagates a Scanner exception
	 * @throws InterpreterException if the interpretation tree could not be built
	 */
	public void run()
		throws InterpreterException, IOException
	{
		init();
		runMain();
	}

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * Runs an asynchronous task in this Interpreter internal thread pool.
	 * @param r the Runnable object to execute
	 */
	public void execute( Runnable r )
	{
		executorService.execute( r );
	}

	private static final AtomicInteger starterThreadCounter = new AtomicInteger();

	private static String createStarterThreadName( String programFilename )
	{
		return programFilename + "-StarterThread-" + starterThreadCounter.incrementAndGet();
	}

	private class StarterThread extends Thread
	{
		private final InterpreterStartFuture future;
		public StarterThread( InterpreterStartFuture future )
		{
			super( createStarterThreadName( programFilename ) );
			this.future = future;
			setContextClassLoader( classLoader );
		}
		
		@Override
		public void run()
		{
			try {
				init();
				future.setResult( null );
			} catch( Exception e ) {
				future.setResult( e );
			}
			runMain();
			//commCore.shutdown();
			exit();
			free();
		}

		private void free()
		{
			/* We help the Java(tm) Garbage Collector.
			 * Looks like it needs this or the Interpreter
			 * does not get collected.
			 */
			definitions.clear();
			inputOperations.clear();
			locksMap.clear();
			mainExec = null;
			synchronized( sessionSpawners ) {
				sessionSpawners.clear();
			}
			outputPorts.clear();
			correlationSet.clear();
			globalValue.erase();
			embeddedServiceLoaders.clear();
			classLoader = null;
			commCore = null;
			//System.gc();
		}
	}
	
	/**
	 * Returns the CommCore of this Interpreter.
	 * @return the CommCore of this Interpreter
	 */
	public CommCore commCore()
	{
		return commCore;
	}
	
	private boolean buildOOIT()
		throws InterpreterException
	{
		try {
			Program program = null;
			if ( cmdParser.isProgramCompiled() ) {
				ObjectInputStream istream = new ObjectInputStream( cmdParser.programStream() );
				Object o = istream.readObject();
				if ( o instanceof Program ) {
					program = (Program)o;
				} else {
					throw new InterpreterException( "Input compiled program is not a JOLIE program" );
				}
			} else {
				OLParser olParser = new OLParser( new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ), includePaths, classLoader );
				olParser.putConstants( cmdParser.definedConstants() );
				program = olParser.parse();
				OLParseTreeOptimizer optimizer = new OLParseTreeOptimizer( program );
				program = optimizer.optimize();
			}
			SemanticVerifier semanticVerifier = new SemanticVerifier( program );
			if ( !semanticVerifier.validate() ) {
				throw new InterpreterException( "Exiting" );
			}

			return (new OOITBuilder( this, program, semanticVerifier.isConstantMap() )).build();
		} catch( IOException e ) {
			throw new InterpreterException( e );
		} catch( ParserException e ) {
			throw new InterpreterException( e );
		} catch( ClassNotFoundException e ) {
			throw new InterpreterException( e );
		} finally {
			cmdParser = null; // Free memory
		}
	}
}
