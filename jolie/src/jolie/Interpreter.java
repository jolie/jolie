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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
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
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.OutputPort;
import jolie.net.PipeListener;
import jolie.process.CorrelatedProcess;
import jolie.process.DefinitionProcess;
import jolie.runtime.EmbeddedServiceLoader;
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
	private OLParser olParser;
	//private boolean verbose = false;
	private boolean exiting = false;
	final private Set< List< VariablePath > > correlationSet =
				new HashSet< List< VariablePath > > ();
	private Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	final private Value globalValue = Value.create();
	final private LinkedList< String > arguments = new LinkedList< String >();
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
	
	final private static Map< String, PipeListener > pipes =
				new HashMap< String, PipeListener >();
	
	final private Set< CorrelatedProcess > sessionSpawners = 
				new HashSet< CorrelatedProcess >();
	
	private Integer activeSessions = new Integer( 0 );
	
	private String[] includePaths = new String[ 0 ];
	private String[] args = new String[ 0 ];
	
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

	public static void registerPipeListener( String key, PipeListener value )
	{
		pipes.put( key, value );
	}
	
	public void addActiveSession()
	{
		synchronized( activeSessions ) {
			activeSessions++;
		}
	}
	
	public void removeActiveSession()
	{
		synchronized( activeSessions ) {
			activeSessions--;
		}
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
	
	public static CommChannel getNewPipeChannel( String pipeId )
		throws InvalidIdException, IOException
	{
		PipeListener listener = pipes.get( pipeId );
		if ( listener == null )
			throw new InvalidIdException( pipeId );
		return listener.createPipeCommChannel();
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
	
	final private File programFile;
	
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
		String olFilepath = null;
		int connectionsLimit = -1;
		LinkedList< String > includeList = new LinkedList< String > ();
		String pwd = new File( "" ).getCanonicalPath();
		includeList.add( pwd );
		includeList.add( "include" );
		Vector< String > libVec = new Vector< String > ();
		libVec.add( pwd );
		libVec.add( "ext" );
		libVec.add( "lib" );
		for( int i = 0; i < args.length; i++ ) {
			if ( "--help".equals( args[ i ] ) || "-h".equals( args[ i ] ) ) {
				throw new CommandLineException( getHelpString() );
			} else if ( "-C".equals( args[ i ] ) ) {
				for( i++; i < args.length; i++ ) {
					
				}
				i = args.length;
			} else if ( "-i".equals( args[ i ] ) ) {
				i++;
				String[] tmp = args[ i ].split( jolie.Constants.pathSeparator );
				for( String s : tmp ) {
					includeList.add( s );
				}
			} else if ( "-l".equals( args[ i ] ) ) {
				i++;
				String[] tmp = args[ i ].split( jolie.Constants.pathSeparator );
				for( String s : tmp ) {
					libVec.add( s );
				}
			} else if ( "--connlimit".equals( args[ i ] ) ) {
				i++;
				connectionsLimit = Integer.parseInt( args[ i ] );
			} else if ( "--version".equals( args[ i ] ) ) {
				throw new CommandLineException( getVersionString() );
			} else if ( args[ i ].endsWith( ".ol" ) ) {
				if ( olFilepath == null ) {
					olFilepath = args[ i ];
				} else {
					throw new CommandLineException( "You can specify only an input file." );
				}
			} else {
				for( int j = i; j < args.length; j++ ) {
					arguments.add( args[ j ] );
				}
			}/* else
				throw new CommandLineException( "Unrecognized command line token: " + args[ i ] );*/
		}
		
		commCore = new CommCore( this, connectionsLimit );
		
		Vector< URL > urls = new Vector< URL >();
		for( String path : libVec ) {
			if ( path.endsWith( ".jar" ) ) {
				urls.add( new URL( "jar:file:" + path + "!/" ) );
			} else if ( new File( path ).isDirectory() ) {
				urls.add( new URL( "file:" + path + "/" ) );
			} else if ( path.endsWith( Constants.fileSeparator + "*" ) ) {
				File dir = new File( path.substring( 0, path.length() - 2 ) );
				String jars[] = dir.list( new FilenameFilter() {
					public boolean accept( File dir, String filename ) {
						return filename.endsWith( ".jar" );
					}
				});
				if ( jars != null ) {
					for( String jarPath : jars ) {
						urls.add( new URL( "jar:file:" + dir.getCanonicalPath() + Constants.fileSeparator + jarPath + "!/" ) );
					}
				}
			}
		}
		classLoader = new JolieClassLoader( urls.toArray( new URL[] {} ), this );
		
		if ( olFilepath == null ) {
			throw new CommandLineException( "Input file not specified." );
		}
		
		this.args = args;
		
		InputStream olStream = null;
		File f = new File( olFilepath );
		if ( f.exists() ) {
			olStream = new FileInputStream( f );
		} else {
			for( int i = 0; i < includeList.size() && olStream == null; i++ ) {
				f = new File(
							includeList.get( i ) +
							jolie.Constants.fileSeparator +
							olFilepath
						);
				if ( f.exists() ) {
					olStream = new FileInputStream( f );
				}
			}
		}
		if ( olStream == null ) {
			throw new FileNotFoundException( olFilepath );
		}
		if ( f.getParent() != null ) {
			includeList.addFirst( f.getParent() );
		}
		
		programFile = new File( olFilepath );
		
		includePaths = includeList.toArray( includePaths );
		olParser = new OLParser( new Scanner( olStream, olFilepath ), includePaths );
	}
	
	public File programFile()
	{
		return programFile;
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

	private String getHelpString()
	{
		StringBuilder helpBuilder = new StringBuilder();
		helpBuilder.append( getVersionString() );
		helpBuilder.append( "\n\nUsage: jolie [options] behaviour_file [options] [program arguments]\n\n" );
		helpBuilder.append( "Available options:\n" );
		helpBuilder.append(
				getOptionString( "-h, --help", "Display this help information" ) );
		//TODO include doc for -l and -i
		helpBuilder.append(
				getOptionString( "--connlimit [number]", "Set max connections limit" ) );
		helpBuilder.append(
				getOptionString( "--verbose", "Activate verbose mode" ) );
		helpBuilder.append(
				getOptionString( "--version", "Display this program version information" ) );
		return helpBuilder.toString();
	}
	
	private static String getOptionString( String option, String description )
	{
		return( '\t' + option + "\t\t" + description + '\n' );
	}
	
	private String getVersionString()
	{
		return( Constants.VERSION + "  " + Constants.COPYRIGHT );
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
