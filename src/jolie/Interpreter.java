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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import jolie.deploy.DeployParser;
import jolie.net.CommCore;
import jolie.process.Process;

public class Interpreter
{
	private static HashMap< String, MappedGlobalObject > idMap = 
		new HashMap< String, MappedGlobalObject >();

	private OLParser olparser;
	private DeployParser dolparser;

	public Interpreter( String sourceName, int port )
		throws IOException
	{
		sourceName = sourceName.replace( ".ol", "" );
		String olPath = sourceName + ".ol";
		String dolPath = sourceName + ".dol";

		InputStream olStream = new FileInputStream( olPath );
		InputStream dolStream = new FileInputStream( dolPath );
		
		olparser = new OLParser( new Scanner( olStream, olPath ) );
		dolparser = new DeployParser( new Scanner( dolStream, dolPath ) );
		
		CommCore.setPort( port );
	}
	
	public Interpreter( String sourceName )
		throws IOException
	{
		sourceName = sourceName.replace( ".ol", "" );
		String olPath = sourceName + ".ol";
		String dolPath = sourceName + ".dol";

		InputStream olStream = new FileInputStream( olPath );
		InputStream dolStream = new FileInputStream( dolPath );
		
		olparser = new OLParser( new Scanner( olStream, olPath ) );
		dolparser = new DeployParser( new Scanner( dolStream, dolPath ) );
	}
	
	public void run()
		throws IOException, ParserException, InterpreterException
	{
		/*	Order is important:
		 	CommCore.init() needs the internal objects to be initialized by parser.parse()
		 */
		olparser.parse();
		dolparser.parse();
		CommCore.init();

		// todo -- may we free the parser and scanner memory here?
		try {
			Process main = Definition.getById( "main" );
			//main = ((Optimizable)main).optimize();
			main.run();
		} catch ( InvalidIdException e ) {
			throw new InterpreterException( "main process undefined" );
		} // todo -- implement exceptions
		/* catch( JolieException je ) {
			System.out.println( "Uncaught exception: " + je.exceptionName() \
								"\n\nJava stack trace follows:\n\n" );
			je.printStackTrace();
		} */finally {
			CommCore.shutdown();
			System.exit( 0 ); // Workaround for InProcess java bug.
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