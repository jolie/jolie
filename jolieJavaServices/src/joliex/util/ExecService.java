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

package joliex.util;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;

import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class ExecService extends JavaService
{
	public Value exec( Value request )
		throws FaultException
	{
		Vector< String > command = new Vector< String >();
		String[] str = request.strValue().split( " " );
		for( int i = 0; i < str.length; i++ ) {
			command.add( str[i] );
		}

		for( Value v : request.getChildren( "args" ) ) {
			command.add( v.strValue() );
		}

		ProcessBuilder builder = new ProcessBuilder( command );
		try {
			Value response = Value.create();
			Process p = builder.start();
			ValueVector waitFor = request.children().get( "waitFor" );
			if ( waitFor == null || waitFor.first().intValue() > 0 ) {
				int exitCode = p.waitFor();
				response.getNewChild( "exitCode" ).setValue( exitCode );
				int len = p.getInputStream().available();
				if ( len > 0 ) {
					char[] buffer = new char[ len ];
					BufferedReader reader = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
					reader.read( buffer, 0, len );
					response.setValue( new String( buffer ) );
				}
				if ( p.getErrorStream() != null ) {
					len = p.getErrorStream().available();
					if ( len > 0 ) {
						char[] buffer = new char[ len ];
						BufferedReader reader = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
						reader.read( buffer, 0, len );
						response.getFirstChild( "stderr" ).setValue( new String( buffer ) );
					}
				}

				p.getInputStream().close();
				p.getErrorStream().close();
				p.getOutputStream().close();
			}
			return response;
		} catch( Exception e ) {
			throw new FaultException( e );
		}
	}
}
