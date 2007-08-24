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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticValidator;
import jolie.lang.parse.ast.Program;
import jolie.net.CommCore;
import jolie.process.DefinitionProcess;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Value;

/**
 * The Jolie interpreter engine.
 * @author Fabrizio Montesi
 */
public class Interpreter
{
	private OLParser olParser;
	private static boolean verbose = false;
	private static boolean exiting = false;
	private static Set< GlobalVariable > correlationSet = new HashSet< GlobalVariable > ();
	private static Constants.StateMode stateMode = Constants.StateMode.PERSISTENT;
	private static Constants.ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	private LinkedList< String > arguments = new LinkedList< String >();
	
	private static Logger logger = Logger.getLogger( "JOLIE" );
	
	public static void exit()
	{
		exiting = true;
	}
	
	public static boolean exiting()
	{
		return exiting;
	}
	
	public static void logUnhandledFault( FaultException f )
	{
		if ( verbose )
			System.out.println( "Thrown unhandled fault: " + f.fault() ); 
	}
	
	public static Constants.StateMode stateMode()
	{
		return stateMode;
	}
	
	public static Constants.ExecutionMode executionMode()
	{
		return executionMode;
	}
	
	public static void setExecutionMode( Constants.ExecutionMode mode )
	{
		executionMode = mode;
	}
	
	public static void setStateMode( Constants.StateMode mode )
	{
		stateMode = mode;
	}
	
	public static Vector< Value > getValues( GlobalVariable var )
	{
		return ExecutionThread.currentThread().state().getValues( var );
	}
	
	public static void setCorrelationSet( Set< GlobalVariable > set )
	{
		correlationSet = set;
	}
	
	public static Set< GlobalVariable > correlationSet()
	{
		return correlationSet;
	}
	
	public static Logger logger()
	{
		return logger;
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

		for( int i = 0; i < args.length; i++ ) {
			if ( "--help".equals( args[ i ] ) || "-h".equals( args[ i ] ) )
				throw new CommandLineException( getHelpString() );
			else if ( "-l".equals( args[ i ] ) ) {
				i++;
				CommCore.setConnectionsLimit( Integer.parseInt( args[ i ] ) );
			} else if ( "--version".equals( args[ i ] ) )
				throw new CommandLineException( getVersionString() );
			else if ( "--verbose".equals( args[ i ] ) )
				verbose = true;
			else if ( args[ i ].endsWith( ".ol" ) ) {
				if ( olFilepath == null )
					olFilepath = args[ i ];
				else
					throw new CommandLineException( "You can specify only an input file." );
			} else {
				for( int j = i; j < args.length; j++ ) {
					arguments.add( args[ j ] );
				}
			}/* else
				throw new CommandLineException( "Unrecognized command line token: " + args[ i ] );*/
		}
		
		if ( olFilepath == null )
			throw new CommandLineException( "Input file not specified." );
		
		InputStream olStream = new FileInputStream( olFilepath );
		
		olParser = new OLParser( new Scanner( olStream, olFilepath ) );
	}
	
	private String getHelpString()
	{
		StringBuilder helpBuilder = new StringBuilder();
		helpBuilder.append( getVersionString() );
		helpBuilder.append( "\n\nUsage: jolie [options] behaviour_file [options] [program arguments]\n\n" );
		helpBuilder.append( "Available options:\n" );
		helpBuilder.append(
				getOptionString( "-h, --help", "Display this help information" ) );
		helpBuilder.append(
				getOptionString( "-l [number]", "Set max connections limit" ) );
		helpBuilder.append(
				getOptionString( "--verbose", "Activate verbose mode" ) );
		helpBuilder.append(
				getOptionString( "--version", "Display this program version information" ) );
		helpBuilder.append( "\n\nNote: if the deploy file (.dol) is not specified, Jolie " +
				"will search for it taking the behaviour_file name and searching for a " +
				"file with the same name and the .dol extension.\n" );
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
	
	/**
	 * Runs the interpreter behaviour specified by command line.
	 * The default behaviour is to execute the input code.
	 * @throws IOException If a Parser propagates a Scanner exception.
	 * @throws ParserException If a Parser finds a syntax error.
	 */
	public void run()
		throws InterpreterException, IOException
	{
		/**
		 * Order is important. CommCore needs the OOIT to initialize.
		 */
		DefinitionProcess main = null;
		if ( buildOOIT() == false )
			throw new InterpreterException( "Error: the interpretation environment couldn't have been initialized" );
		
		CommCore.init();

		try {
			main = DefinitionProcess.getById( "main" );
		} catch ( InvalidIdException e ) {
			// As the parser checks this for us, execution should never reach this point.
			assert false;
		}
		
		StatefulThread mainExec = new StatefulThread( main, null );
		
		// Initialize program arguments in the args variabile.
		Vector< Value > args =
			mainExec.state().getValues( GlobalVariable.getById( "args" ) );
		
		args.clear();
		for( String s : arguments ) {
			args.add( new Value( s ) );
		}
		
		mainExec.start();
		try {
			mainExec.join();
		} catch( InterruptedException e ) {}

		CommCore.shutdown();
	}
	
	private boolean buildOOIT()
		throws InterpreterException
	{
		try {
			Program program = olParser.parse();
			program = (new OLParseTreeOptimizer( program )).optimize();
			if ( !(new SemanticValidator( program )).validate() )
				throw new InterpreterException( "Exiting" );
			
			return (new OOITBuilder( program )).build();
		} catch( ParserException e ) {
			throw new InterpreterException( e );
		} catch( IOException e ) {
			throw new InterpreterException( e );
		}
	}
}
