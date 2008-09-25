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

import java.io.File;
import java.io.IOException;

import java.util.Vector;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.net.LocalCommChannel;


public class JolieServiceLoader extends EmbeddedServiceLoader
{
	final private Interpreter interpreter;
	
	public JolieServiceLoader( Interpreter currInterpreter, String servicePath )
		throws IOException, CommandLineException
	{
		String[] args = new String[ 0 ];
		if ( currInterpreter != null ) {
			args = currInterpreter.args();
		}
		String[] ss = servicePath.split( " " );
		Vector< String > newArgs = new Vector< String >();
		
		int i;
		for( i = 0; i < args.length; i++ ) {
			if ( !args[ i ].endsWith( ".ol" ) ) {
				newArgs.add( args[ i ] );
			} else {
				newArgs.add( "-i" );
				newArgs.add( new File( servicePath ).getParent() );
			}
		}
		for( int k = 0; k < ss.length; k++, i++ ) {
			newArgs.add( ss[ k ] );
		}
		interpreter = new Interpreter( newArgs.toArray( args ) );
	}

	public void load()
		throws EmbeddedServiceLoadingException
	{
		try {
			interpreter.run( false );
			setChannel( new LocalCommChannel( interpreter ) );
		} catch( Exception e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}
}