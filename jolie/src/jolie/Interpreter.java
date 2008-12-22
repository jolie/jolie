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

package jolie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

/**
 * The Jolie interpreter engine.
 * Multiple Interpreter instances can be run in the same JavaVM;
 * this is the case, e.g., for service embedding.
 * @author Fabrizio Montesi
 */
public class Interpreter
{
	final private CommCore commCore;
	
	// TODO remove this and put it into a private method temporary variable
	private OLParser olParser;
	
	private boolean exiting = false;
	final private Set< List< VariablePath > > correlationSet =
				new HashSet< List< VariablePath > > ();
	private Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	final private Value globalValue = Value.create();
	final private String[] arguments;
	final private Vector< EmbeddedServiceLoader > embeddedServiceLoaders =
			new Vector< EmbeddedServiceLoader >();
	final private Logger logger = Logger.getLogger( "JOLIE" );
	
	final private Map< String, DefinitionProcess > definitions = 
				new HashMap< String, DefinitionProcess >();
	final private Map< String, OutputPort > outputPorts = new HashMap< String, OutputPort >();
	final private Map< String, InputOperation > inputOperations = 
				new HashMap< String, InputOperation>();
	
	final private HashMap< String, Object > locksMap =
				new HashMap< String, Object >();
	
	final private Set< CorrelatedProcess > sessionSpawners = 
				new HashSet< CorrelatedProcess >();
	
	final private ClassLoader parentClassLoader;
	
	final private String[] includePaths;
	final private String[] args;
	
	final private File programFile;
	
	/**
	 * Returns the arguments passed to this Interpreter.
	 * @return the arguments passed to this Interpreter
	 */
	public String[] args()
	{
		return args;
	}
	
	/**
	 * Returns the include paths this Interpreter is considering.
	 * @return the include paths this Interpreter is considering
	 */
	public String[] includePaths()
	{
		return includePaths;
	}

	public void registerSessionSpawner( CorrelatedProcess p )
	{
		sessionSpawners.add( p );
	}
	
	public void unregisterSessionSpawner( CorrelatedProcess p )
	{
		sessionSpawners.remove( p );
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

	public void register( String key, OutputPort value )
	{
		outputPorts.put( key, value );
	}
	
	public void register( String key, DefinitionProcess value )
	{
		definitions.put( key, value );
	}
	
	public void register( String key, InputOperation value )
	{
		inputOperations.put( key, value );
	}
	
	public void addEmbeddedServiceLoader( EmbeddedServiceLoader n )
	{
		embeddedServiceLoaders.add( n );
	}
	
	public Collection< EmbeddedServiceLoader > embeddedServiceLoaders()
	{
		return embeddedServiceLoaders;
	}
	
	public void exit()
	{
		exiting = true;
		for( CorrelatedProcess p : sessionSpawners ) {
			p.interpreterExit();
		}
	}
	
	public boolean exiting()
	{
		return exiting;
	}
	
	public void logUnhandledFault( FaultException f )
	{
		//if ( verbose )
		System.out.println( "Thrown unhandled fault: " + f.faultName() ); 
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
	
	public void setCorrelationSet( Set< List< VariablePath > > set )
	{
		correlationSet.clear();
		correlationSet.addAll( set );
	}
	
	/**
	 * Returns the correlation set of this Interpreter.
	 * @return the correlation set of this Interpreter
	 */
	public Set< List< VariablePath > > correlationSet()
	{
		return correlationSet;
	}
	
	public Logger logger()
	{
		return logger;
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
	
	/** Constructor.
	 * 
	 * @param args The command line arguments.
	 * @throws CommandLineException if the command line is not valid or asks for simple information. (like --help and --version)
	 * @throws FileNotFoundException if one of the passed input files is not found.
	 * @throws IOException if a Scanner constructor signals an error.
	 */
	public Interpreter( String[] args )
		throws CommandLineException, FileNotFoundException, IOException
	{
		CommandLineParser cmdParser = new CommandLineParser( args );
		this.args = args;
		programFile = new File( cmdParser.programFilepath() );
		arguments = cmdParser.arguments();
		commCore = new CommCore( this, cmdParser.connectionsLimit() );
		parentClassLoader = this.getClass().getClassLoader();
		classLoader = new JolieClassLoader( cmdParser.libURLs(), this, parentClassLoader );
		includePaths = cmdParser.includePaths();
		olParser = new OLParser( new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ), includePaths, parentClassLoader );
	}
	
	public File programFile()
	{
		return programFile;
	}
	
	public ClassLoader parentClassLoader()
	{
		return parentClassLoader;
	}
	
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
	
	public static class InterpreterStartFuture implements Future< Exception >
	{
		final private Lock lock;
		final private Condition initCompleted;
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
			lock.lock();
			if ( !isDone ) {
				if ( !initCompleted.await( timeout, unit ) ) {
					throw new TimeoutException();
				}
			}
			lock.unlock();
			return result;
		}
		
		public Exception get()
			throws InterruptedException
		{
			lock.lock();
			if ( !isDone ) {
				initCompleted.await();
			}
			lock.unlock();
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
			result = e;
			isDone = true;
			initCompleted.signalAll();
			lock.unlock();
		}
	}
	
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
			e.printStackTrace();
		}
	}
	
	/**
	 * Runs the interpreter behaviour specified by command line.
	 * The default behaviour is to execute the input code.
	 * @throws IOException if a Parser propagates a Scanner exception
	 * @throws InterpreterException if the interpretation tree could not be built
	 */
	public void run()
		throws InterpreterException, IOException
	{
		init();
		runMain();
		commCore.shutdown();
	}

	final private ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * Runs an asynchronous task in this Interpreter internal thread pool.
	 * @param r the Runnable object to execute
	 */
	public void execute( Runnable r )
	{
		executorService.execute( r );
	}
	
	private class StarterThread extends Thread
	{
		final private InterpreterStartFuture future;
		public StarterThread( InterpreterStartFuture future )
		{
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
			commCore.shutdown();
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
			Program program = olParser.parse();
			olParser = null; // Free memory
			program = (new OLParseTreeOptimizer( program )).optimize();
			if ( !(new SemanticVerifier( program )).validate() )
				throw new InterpreterException( "Exiting" );
			
			return (new OOITBuilder( this, program )).build();
		} catch( ParserException e ) {
			throw new InterpreterException( e );
		} catch( IOException e ) {
			throw new InterpreterException( e );
		}
	}
	
	/*@Override
	protected void finalize()
	{
		// Clean up here if we're a sub-interpreter
	}*/
}
