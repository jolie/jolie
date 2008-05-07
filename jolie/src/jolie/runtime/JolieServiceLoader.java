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


package jolie.runtime;

import java.io.IOException;

import jolie.CommandLineException;
import jolie.Interpreter;


public class JolieServiceLoader extends EmbeddedServiceLoader
{
	private Interpreter interpreter;
	
	public JolieServiceLoader( String servicePath )
		throws IOException, CommandLineException
	{
		String[] args = new String[ 0 ];
		Interpreter currInterpreter = Interpreter.getInstance();
		if ( currInterpreter != null ) {
			args = currInterpreter.args();
		}
		String[] ss = servicePath.split( " " );
		String[] newArgs = new String[ args.length + ss.length ];
		int i;
		for( i = 0; i < args.length; i++ ) {
			newArgs[ i ] = args[ i ];
		}
		for( int k = 0; k < ss.length; k++, i++ ) {
			newArgs[ i ] = ss[ k ];
		}
		interpreter = new Interpreter( newArgs );
	}

	public void load()
		throws EmbeddedServiceLoadingException
	{
		try {
			interpreter.run( false );
		} catch( Exception e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}
}