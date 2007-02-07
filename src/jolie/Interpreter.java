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

import jolie.deploy.DeployParser;
import jolie.net.CommCore;
import jolie.process.DefinitionProcess;

/**
 * The Jolie interpreter engine.
 * @author Fabrizio Montesi
 */
public class Interpreter
{
	private enum InterpreterTask {
		CodeExecution,
		ExportBPEL,
		ExportWSDL
	}
	
	private InterpreterTask task;

	private OLParser olparser;
	private DeployParser dolparser;
	
	private static final String VERSION = "JOLIE 0.3 beta1";
	private static final String COPYRIGHT = "(C) 2006-2007 the JOLIE team";
	
	private static final long serialVersionUID = 1L;

	public static final long serialVersionUID()
	{
		/* Future use, for the bytecode compiling. */
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
			else if ( "--export-bpel".equals( args[ i ] ) )
				task = InterpreterTask.ExportBPEL;
			else if ( "--export-wsdl".equals( args[ i ] ) )
				task = InterpreterTask.ExportWSDL;
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
	
	private void parse()
		throws IOException, ParserException
	{
		// This method can be called once.
		assert olparser != null || dolparser != null :
			"Internal error: parse() method called for the second time.";

		/*	Order is important:
		 *  DeployParser needs the internal objects created by OLParser.
		 */
		olparser.parse();
		dolparser.parse();

		// Free the memory allocated by the parsers.
		olparser = null;
		dolparser = null;
	}
	
	/**
	 * Runs the interpreter behaviour specified by command line.
	 * The default behaviour is to execute the input code.
	 * @throws IOException If a Parser propagates a Scanner exception.
	 * @throws ParserException If a Parser finds a syntax error.
	 */
	public void run()
		throws IOException, ParserException
	{
		if ( task == InterpreterTask.ExportBPEL )
			exportBPEL();
		else if ( task == InterpreterTask.ExportWSDL )
			exportWSDL();
		else
			executeCode();
	}
	
	private void exportBPEL()
		throws IOException, ParserException
	{
		parse();
	}
	
	private void exportWSDL()
		throws IOException, ParserException
	{
		parse();
	}
	
	private void executeCode()
		throws IOException, ParserException
	{
		/*	Order is important:
		 *	CommCore.init() needs the internal objects created by the parsers.
		 */
		parse();
		CommCore.init();

		try {
			DefinitionProcess main = DefinitionProcess.getById( "main" );
			main.optimize().run();
		} catch ( InvalidIdException e ) {
			// As the parser checks this for us, execution should never reach this point.
			assert false;
		} // todo -- implement exceptions
		/* catch( JolieException je ) {
			System.out.println( "Uncaught exception: " + je.exceptionName() \
								"\n\nJava stack trace follows:\n\n" );
			je.printStackTrace();
		} finally {
		CommCore.shutdown();
		}*/
		CommCore.shutdown();
	}
}