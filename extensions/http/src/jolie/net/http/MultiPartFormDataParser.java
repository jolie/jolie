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
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;

public class MultiPartFormDataParser
{
	private final String boundary;
	private final Value value;
	private final HttpMessage message;
	private final Map< String, PartProperties > partPropertiesMap = new HashMap<>();

	private static final Pattern parametersSplitPattern = Pattern.compile( ";" );
	private static final Pattern keyValueSplitPattern = Pattern.compile( "=" );

	public class PartProperties {
		private String filename = null;
		private void setFilename( String filename )
		{
			this.filename = filename;
		}
		public String filename()
		{
			return filename;
		}
	}
	
	public MultiPartFormDataParser( HttpMessage message, Value value )
		throws IOException
	{
		final String[] params = parametersSplitPattern.split( message.getProperty( "content-type" ) );
		String b = null;
		try {
			for( String param : params ) {
				param = param.trim();
				if ( param.startsWith( "boundary" ) ) {
					b = "--" + keyValueSplitPattern.split( param, 2 )[1];
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

	private PartProperties getPartProperties( String partName )
	{
		PartProperties ret = partPropertiesMap.get( partName );
		if ( ret == null ) {
			ret = new PartProperties();
			partPropertiesMap.put( partName, ret );
		}
		return ret;
	}

	public Collection< Entry< String, PartProperties > > getPartPropertiesSet()
	{
		return partPropertiesMap.entrySet();
	}
	
	private void parsePart( String part, int offset )
		throws IOException
	{
		boolean hasContentType;
		// Split header from content
		String[] hc = part.split( HttpUtils.CRLF + HttpUtils.CRLF, 2 );
		BufferedReader reader =
			new BufferedReader(
			new StringReader( hc[0] ) );
		String line, name = null, filename = null;
		String[] params;

		// Parse part header
		hasContentType = false;
		while( (line=reader.readLine()) != null && !line.isEmpty() ) {
			params = parametersSplitPattern.split( line );
			for( String param : params ) {
				param = param.trim();
				if ( param.startsWith( "name" ) ) {
					try {
						name = keyValueSplitPattern.split( param, 2 )[1];
						// Names are surronded by "": cut them.
						name = URLDecoder.decode( name.substring( 1, name.length() - 1 ), HttpUtils.URL_DECODER_ENC );
					} catch( ArrayIndexOutOfBoundsException e ) {
						throw new IOException( "Invalid name specified in multipart form data element" );
					}
				} else if ( param.startsWith( "filename" ) ) {
					try {
						filename = keyValueSplitPattern.split( param, 2 )[1];
						// Filenames are surronded by quotes "": cut them.
						filename = URLDecoder.decode( filename.substring( 1, filename.length() - 1 ), HttpUtils.URL_DECODER_ENC );
					} catch( ArrayIndexOutOfBoundsException e ) {
						throw new IOException( "Invalid filename specified in multipart form data element" );
					}
				} else if ( param.startsWith( "Content-Type" ) ) {
					// TODO: parse content-type better, now it checks only if it exists or not
					hasContentType = true;
				}
			}
		}
		if ( name == null ) {
			throw new IOException( "Invalid multipart form data element: missing name" );
		}
		
		offset += hc[0].length() + 4;
		
		Value child = value.getNewChild( name );
		if ( hc.length > 1 ) {
			if ( hasContentType == true ) {
				child.setValue( new ByteArray( Arrays.copyOfRange( message.content(), offset, offset + hc[1].length() ) ) );
			} else {
				child.setValue( new String( Arrays.copyOfRange( message.content(), offset, offset + hc[1].length() ) ) );
			}
		}/* else {
			value.getNewChild( name ).setValue( new ByteArray( new byte[0] ) );
		}*/

		if ( filename != null ) {
			getPartProperties( name ).setFilename( filename );
		}
	}

	public void parse()
		throws IOException
	{
		// this needs to be strictly parsed with US-ASCII, since we are dealing with raw data
		String[] parts = (HttpUtils.CRLF + new String( message.content(), "US-ASCII" )).split( boundary + "--" );
		parts = (parts[0] + boundary + HttpUtils.CRLF).split( HttpUtils.CRLF + boundary + HttpUtils.CRLF );

		// The first one is always empty, so we start from 1
		int offset = boundary.length() + 2;
		for( int i = 1; i < parts.length; i++ ) {
			parsePart( parts[i], offset );
			offset += parts[i].length() + boundary.length() + 4;
		}
	}
}
