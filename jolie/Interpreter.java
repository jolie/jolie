/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.io.IOException;
import java.util.HashMap;
import java.io.InputStream;

public class Interpreter
{
	private static HashMap< String, MappedGlobalObject > idMap = 
		new HashMap< String, MappedGlobalObject >();

	private Parser parser;

	public Interpreter( InputStream stream, String sourceName, int port )
		throws IOException
	{
		Scanner scanner = new Scanner( stream, sourceName );
		parser = new Parser( scanner );
		
		CommCore.setPort( port );
	}
	
	public Interpreter( InputStream stream, String sourceName )
		throws IOException
	{
		Scanner scanner = new Scanner( stream, sourceName );
		parser = new Parser( scanner );
	}
	
	public void run()
		throws IOException, ParserException, InterpreterException
	{
		/*	Order is important:
		 	CommCore.init() needs the internal objects to be initialized by parser.parse()
		 */
		parser.parse();
		CommCore.init();

		try {
			Process main = Definition.getById( "main" );
			main.run();
		} catch ( InvalidIdException e ) {
			throw new InterpreterException( "main process undefined" );
		} finally {
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