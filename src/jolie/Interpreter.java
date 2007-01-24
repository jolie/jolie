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
import java.util.HashMap;

import jolie.deploy.DeployParser;
import jolie.net.CommCore;
import jolie.process.Optimizable;
import jolie.process.Process;

/**
 * The Jolie interpreter engine.
 * @author Fabrizio Montesi
 */
public class Interpreter
{
	private static HashMap< String, MappedGlobalObject > idMap = 
		new HashMap< String, MappedGlobalObject >();

	private OLParser olparser;
	private DeployParser dolparser;
	
	private static final String VERSION = "Jolie 0.3 beta1";
	private static final String COPYRIGHT = "(C) 2006-2007 the Jolie team";
	
	private static final long serialVersionUID = 1L;

	public static final long serialVersionUID()
	{
		return serialVersionUID;
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
		String dolFilepath = null;

		for( int i = 0; i < args.length; i++ ) {
			if ( "--help".equals( args[ i ] ) || "-h".equals( args[ i ] ) )
				throw new CommandLineException( getHelpString() );
			else if ( "--version".equals( args[ i ] ) )
				throw new CommandLineException( getVersionString() );
			else if ( "-p".equals( args[ i ] ) || "--port".equals( args[ i ] ) )
				CommCore.setPort( Integer.parseInt( args[ ++i ] ) );
			else if ( args[ i ].endsWith( ".ol" ) ) {
				if ( olFilepath == null )
					olFilepath = args[ i ];
				else
					throw new CommandLineException( "You can specify only a behaviour file." );
			} else if ( args[ i ].endsWith( ".dol" ) ) {
				if ( olFilepath != null )
					dolFilepath = args[ i ];
				else
					throw new CommandLineException(
						"You must specify the behaviour file before the deploy (.dol) one." );
			} else
				throw new CommandLineException( "Unrecognized command line token: " + args[ i ] );
		}
		
		if ( olFilepath == null )
			throw new CommandLineException( "Behaviour file not specified." );
		
		if ( dolFilepath == null )
			dolFilepath = olFilepath.substring( 0, olFilepath.length() - 3 ) + ".dol";
		
		InputStream olStream = new FileInputStream( olFilepath );
		InputStream dolStream = new FileInputStream( dolFilepath );
		
		olparser = new OLParser( new Scanner( olStream, olFilepath ) );
		dolparser = new DeployParser( new Scanner( dolStream, dolFilepath ) );
	}
	
	private String getHelpString()
	{
		StringBuilder helpBuilder = new StringBuilder();
		helpBuilder.append( getVersionString() );
		helpBuilder.append( "\n\nUsage: jolie [options] behaviour_file [options] [deploy_file] [options]\n\n" );
		helpBuilder.append( "Available options:\n" );
		helpBuilder.append(
				getOptionString( "-h, --help", "Display this help information" ) );
		helpBuilder.append(
				getOptionString( "-p, --port", "Change the input network port" ) );
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
		return( VERSION + "  " + COPYRIGHT );
	}
	
	/**
	 * Parses and executes the code contained in the input files.
	 * @throws IOException If a Parser propagates a Scanner exception.
	 * @throws ParserException If a Parser finds a syntax error.
	 */
	public void run()
		throws IOException, ParserException
	{
		/*	Order is important:
		 *  DeployParser needs the internal objects created by OLParser.
		 *	CommCore.init() needs the internal objects created by both parsers.
		 */
		olparser.parse();
		dolparser.parse();
		CommCore.init();

		// Free the memory allocated by the parsers.
		olparser = null;
		dolparser = null;

		try {
			Process main = Definition.getById( "main" );
			main = ((Optimizable)main).optimize();
			main.run();
		} catch ( InvalidIdException e ) {
			// As the parser checks this for us, execution should never reach this point.
			assert false;
		} // todo -- implement exceptions
		/* catch( JolieException je ) {
			System.out.println( "Uncaught exception: " + je.exceptionName() \
								"\n\nJava stack trace follows:\n\n" );
			je.printStackTrace();
		} */finally {
			CommCore.shutdown();
		}
	}
	
	public synchronized static boolean registerObject( String id, MappedGlobalObject obj )
	{
		if ( idMap.containsKey( id ) )
			return false;
		
		idMap.put( id, obj );
		return true;
	}
	
	public synchronized static MappedGlobalObject getObjectById( String id )
	{
		return idMap.get( id );
	}
}