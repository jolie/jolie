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

package jolie.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import jolie.net.HttpProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;

public class MultiPartFormDataParser
{
	//final private BufferedReader reader;
	final private String boundary;
	final private Value value;
	final private HttpMessage message;
	
	public MultiPartFormDataParser( HttpMessage message, Value value )
		throws IOException
	{
		final String[] params = message.getProperty( "content-type" ).split( ";" );
		String b = null;
		try {
			for( String param : params ) {
				param = param.trim();
				if ( param.startsWith( "boundary" ) ) {
					b = "--" + param.split( "=" )[1];
				}
			}
			if ( b == null ) {
				throw new IOException( "Invalid boundary in multipart/form-data http message" );
			}
		} catch( ArrayIndexOutOfBoundsException e ) {
			throw new IOException( "Invalid boundary in multipart/form-data http message" );
		}
		
		this.value = value;
		this.boundary = b;
		this.message = message;
		
	}
	
	private void parsePart( String part, int offset )
		throws IOException
	{
		// Split header from content
		String[] hc = part.split( HttpProtocol.CRLF + HttpProtocol.CRLF );
		BufferedReader reader =
						new BufferedReader(
							new StringReader( hc[0] )
						);
		String line, name = null;
		String[] params;
		
		// Parse part header
		while( (line=reader.readLine()) != null && !line.isEmpty() ) {
			params = line.split( ";" );
			for( String param : params ) {
				param = param.trim();
				if ( param.startsWith( "name" ) ) {
					try {
						name = param.split( "=" )[1];
						// Names are surronded by "": cut them.
						name = name.substring( 1, name.length() - 1 );
					} catch( ArrayIndexOutOfBoundsException e ) {
						throw new IOException( "Invalid name specified in multipart form data element" );
					}
				}
				// TODO: parse content-type and use it appropriately
			}
		}
		if ( name == null ) {
			throw new IOException( "Invalid multipart form data element: missing name" );
		}
		
		offset += hc[0].length() + 4;
		
		Value child = value.getNewChild( name );
		if ( hc.length > 1 ) {
			child.setValue( new ByteArray( Arrays.copyOfRange( message.content(), offset, offset + hc[1].length() ) ) );
		}/* else {
			value.getNewChild( name ).setValue( new ByteArray( new byte[0] ) );
		}*/
	}

	public void parse()
		throws IOException
	{
		String[] parts = (HttpProtocol.CRLF + new String( message.content(), "US-ASCII" )).split( boundary + "--" );
		parts = (parts[0] + boundary + HttpProtocol.CRLF).split( HttpProtocol.CRLF + boundary + HttpProtocol.CRLF );

		// The first one is always empty, so we start from 1
		int offset = boundary.length() + 2;
		for( int i = 1; i < parts.length; i++ ) {
			parsePart( parts[i], offset );
			offset += parts[i].length() + boundary.length() + 4;
		}
	}
}
