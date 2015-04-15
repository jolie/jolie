/***************************************************************************
 *   Copyright 2008-2014 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class ExecService extends JavaService
{
	public Value exec( Value request )
		throws FaultException
	{
		List< String > command = new LinkedList< String >();
		String[] str = request.strValue().split( " " );
		command.addAll( Arrays.asList( str ) );

		for( Value v : request.getChildren( "args" ) ) {
			command.add( v.strValue() );
		}

		ProcessBuilder builder = new ProcessBuilder( command );
		if ( request.hasChildren( "workingDirectory" ) ) {
			builder.directory( new File( request.getFirstChild( "workingDirectory" ).strValue() ) );
		}
		try {

			Value response = Value.create();
			boolean stdOutConsoleEnable = false;
			Process p = builder.start();
			StreamGobbler outputStreamGobbler = new StreamGobbler( p.getInputStream() );
			if ( request.hasChildren( "stdOutConsoleEnable" ) ) {
				if ( request.getFirstChild( "stdOutConsoleEnable" ).boolValue() ) {
					outputStreamGobbler.start();
					stdOutConsoleEnable = true;
				}
			}
			ValueVector waitFor = request.children().get( "waitFor" );
			if ( waitFor == null || waitFor.first().intValue() > 0 ) {
				int exitCode = p.waitFor();
				response.getNewChild( "exitCode" ).setValue( exitCode );
				if ( !stdOutConsoleEnable ) {
					int len = p.getInputStream().available();
					if ( len > 0 ) {
						char[] buffer = new char[ len ];
						BufferedReader reader = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
						reader.read( buffer, 0, len );
						response.setValue( new String( buffer ) );
					}
				}
				if ( p.getErrorStream() != null ) {
					int len = p.getErrorStream().available();
					if ( len > 0 ) {
						char[] buffer = new char[ len ];
						BufferedReader reader = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
						reader.read( buffer, 0, len );
						response.getFirstChild( "stderr" ).setValue( new String( buffer ) );
					}
				}
				if ( outputStreamGobbler.isAlive() ) {
					outputStreamGobbler.join();
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

	private class StreamGobbler extends Thread
	{

		InputStream is;

		private StreamGobbler( InputStream is )
		{
			this.is = is;
		}

		@Override
		public void run()
		{
			try {
				InputStreamReader isr = new InputStreamReader( is );
				BufferedReader br = new BufferedReader( isr );
				String line;
				while( (line = br.readLine()) != null ) {
					System.out.println( line );
				}
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		}
	}
}
