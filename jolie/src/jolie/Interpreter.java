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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.InputPort;
import jolie.net.OutputPort;
import jolie.net.PipeListener;
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
	final private Map< String, InputPort > inputPorts = new HashMap< String, InputPort >();
	final private Map< String, OutputPort > outputPorts = new HashMap< String, OutputPort >();
	final private Map< String, InputOperation > inputOperations = 
				new HashMap< String, InputOperation>();
	
	final private HashMap< String, Object > locksMap =
				new HashMap< String, Object >();
	
	final private static Map< String, PipeListener > pipes =
				new HashMap< String, PipeListener >();
	
	private String[] includePaths = new String[ 0 ];
	private String[] args = new String[ 0 ];
	
	public String[] args()
	{
		return args;
	}
	
	public String[] includePaths()
	{
		return includePaths;
	}

	public static void registerPipeListener( String key, PipeListener value )
	{
		pipes.put( key, value );
	}
	
	private JolieClassLoader classLoader;
	
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
	
	public InputOperation getInputOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}
		
	public OneWayOperation getOneWayOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null || !(ret instanceof OneWayOperation) )
			throw new InvalidIdException( key );
		return (OneWayOperation)ret;
	}
	
	public RequestResponseOperation getRequestResponseOperation( String key )
		throws InvalidIdException
	{
		InputOperation ret;
		if ( (ret=inputOperations.get( key )) == null || !(ret instanceof RequestResponseOperation) )
			throw new InvalidIdException( key );
		return (RequestResponseOperation)ret;
	}
	
	public OutputPort getOutputPort( String key )
		throws InvalidIdException
	{
		OutputPort ret;
		if ( (ret=outputPorts.get( key )) == null )
			throw new InvalidIdException( key );
		return (OutputPort)ret;
	}
	
	public InputPort getInputPort( String key )
		throws InvalidIdException
	{
		InputPort ret;
		if ( (ret=inputPorts.get( key )) == null )
			throw new InvalidIdException( key );
		return (InputPort)ret;
	}
	
	public DefinitionProcess getDefinition( String key )
		throws InvalidIdException
	{
		DefinitionProcess ret;
		if ( (ret=definitions.get( key )) == null )
			throw new InvalidIdException( key );
		return ret;
	}
	
	public void register( String key, InputPort value )
	{
		inputPorts.put( key, value );
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
	
	public Constants.ExecutionMode executionMode()
	{
		return executionMode;
	}
	
	public void setExecutionMode( Constants.ExecutionMode mode )
	{
		executionMode = mode;
	}
	
	public void setCorrelationSet( Set< List< VariablePath > > set )
	{
		correlationSet.clear();
		correlationSet.addAll( set );
	}
	
	public Set< List< VariablePath > > correlationSet()
	{
		return correlationSet;
	}
	
	public Logger logger()
	{
		return logger;
	}
	
	public static Interpreter getInstance()
	{
		return ((JolieThread)Thread.currentThread()).interpreter();
	}
	
	public JolieClassLoader getClassLoader()
	{
		return classLoader;
	}
	
	/** Constructor.
	 * 
	 * @param args The command line arguments.
	 * @throws CommandLineException If the command line is not valid or asks for simple information. (like --help and --version)
	 * @throws FileNotFoundException If one of the passed input files is not found.
	 * @throws IOException If a Scanner constructor signals an error.
	 */
	public Interpreter( String[] args )
		throws CommandLineException, FileNotFoundException, IOException
	{
		String olFilepath = null;
		int connectionsLimit = -1;
		Vector< String > includeVec = new Vector< String > ();
		String pwd = new File( "" ).getCanonicalPath();
		includeVec.add( pwd );
		includeVec.add( "include" );
		Vector< String > libVec = new Vector< String > ();
		libVec.add( pwd );
		libVec.add( "ext" );
		libVec.add( "lib" );
		for( int i = 0; i < args.length; i++ ) {
			if ( "--help".equals( args[ i ] ) || "-h".equals( args[ i ] ) ) {
				throw new CommandLineException( getHelpString() );
			} else if ( "-i".equals( args[ i ] ) ) {
				i++;
				String[] tmp = args[ i ].split( jolie.Constants.pathSeparator );
				for( String s : tmp ) {
					includeVec.add( s );
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
		
		includePaths = includeVec.toArray( includePaths );
		
		if ( olFilepath == null )
			throw new CommandLineException( "Input file not specified." );
		
		this.args = args;
		
		InputStream olStream = null;
		try {
			olStream = new FileInputStream( olFilepath );
		} catch( FileNotFoundException e ) {
			File f;
			for( int i = 0; i < includePaths.length && olStream == null; i++ ) {
				f = new File(
							includePaths[ i ] +
							jolie.Constants.fileSeparator +
							olFilepath
						);
				if ( f.exists() ) {
					olStream = new FileInputStream( f );
				}
			}
			if ( olStream == null ) {
				throw new FileNotFoundException( olFilepath );
			}
		}
		olParser = new OLParser( new Scanner( olStream, olFilepath ), includePaths );
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
	
	private String getOptionString( String option, String description )
	{
		return( '\t' + option + "\t\t" + description + '\n' );
	}
	
	private String getVersionString()
	{
		return( Constants.VERSION + "  " + Constants.COPYRIGHT );
	}
	
	public Value globalValue()
	{
		return globalValue;
	}
	
	private SessionThread mainExec;
	
	public SessionThread mainThread()
	{
		return mainExec;
	}
	
	/**
	 * Runs the interpreter behaviour specified by command line.
	 * The default behaviour is to execute the input code.
	 * @throws IOException If a Parser propagates a Scanner exception.
	 * @throws ParserException If a Parser finds a syntax error.
	 */
	public void run( boolean blocking )
		throws InterpreterException, IOException
	{
		/**
		 * Order is important. CommCore needs the OOIT to be initialized.
		 */
		DefinitionProcess main = null;
		if ( buildOOIT() == false ) {
			throw new InterpreterException( "Error: the interpretation environment couldn't have been initialized" );
		}

		commCore.init();
		
		try {
			main = getDefinition( "main" );
		} catch ( InvalidIdException e ) {
			// As the parser checks this for us, execution should never reach this point.
			assert false;
		}
		
		mainExec = new SessionThread( this, main );
		
		// Initialize program arguments in the args variabile.
		ValueVector jArgs = ValueVector.create();
		
		for( String s : arguments )
			jArgs.add( Value.create( s ) );
		
		mainExec.state().root().getChildren( "args" ).deepCopy( jArgs );
		
		mainExec.start();
		
		if ( blocking ) {
			try {
				mainExec.join();
			} catch( InterruptedException e ) {}
			
			commCore.shutdown();
		} else {
			(new JoiningThread( this, mainExec )).start();
		}
	}
	
	private class JoiningThread extends Thread
	{
		private Interpreter interpreter;
		private Thread thread;
		public JoiningThread( Interpreter interpreter, Thread thread )
		{
			this.interpreter = interpreter;
			this.thread = thread;
		}
		
		@Override
		public void run()
		{
			try {
				thread.join();
			} catch( InterruptedException e ) {}
			
			interpreter.commCore.shutdown();
		}
	}
	
	
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
	
	@Override
	protected void finalize()
	{
		// Clean up here if we're a sub-interpreter
	}
}
