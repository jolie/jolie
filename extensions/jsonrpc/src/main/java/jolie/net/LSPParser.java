/***************************************************************************
 *   Copyright (C) 2019 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2019 by Eros Fabrici <eros.fabrici@gmail.com>           *
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

package jolie.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jolie.lang.parse.Scanner;
import jolie.net.http.HttpScanner;

/**
 *
 * @author eferos93
 */
public class LSPParser {
	private final HttpScanner scanner;
	private Scanner.Token token;

	public LSPParser( InputStream iStream ) throws IOException {
		scanner = new HttpScanner( iStream );
		// , URI.create( "urn:network" ) );
	}

	private void nextToken() throws IOException {
		token = scanner.getToken();
	}

	private void parseHeaderProperties( LSPMessage message ) throws IOException {
		nextToken();
		while( token.is( Scanner.TokenType.ID ) ) {
			String name, value;
			name = token.content().toLowerCase();
			nextToken();
			value = scanner.readLine();
			nextToken();
			message.setProperty( name, value );
		}
	}

	private static void blockingRead( InputStream stream, byte[] buffer, int offset, int length )
		throws IOException {
		int s = 0;
		do {
			int r = stream.read( buffer, offset + s, length - s );
			if( r == -1 ) {
				throw new EOFException();
			}
			s += r;
		} while( s < length );
	}

	private void readContent( LSPMessage message ) throws IOException {
		boolean chunked = false;
		int contentLength = -1;

		String p = message.getProperty( "transfer-encoding" );

		if( p != null && p.startsWith( "chunked" ) ) {
			// Transfer-encoding has the precedence over Content-Length
			chunked = true;
		} else {
			p = message.getProperty( "content-length" );
			if( p != null && !p.isEmpty() ) {
				try {
					contentLength = Integer.parseInt( p );
					if( contentLength == 0 ) {
						message.setContent( new byte[ 0 ] );
						return;
					}
				} catch( NumberFormatException e ) {
					throw new IOException( "Illegal Content-Length value " + p );
				}
			}
		}
		byte[] buffer = null;
		InputStream stream = scanner.inputStream();
		if( chunked ) {
			// Link: http://tools.ietf.org/html/rfc2616#section-3.6.1
			List< byte[] > chunks = new ArrayList<>();
			int l = -1, totalLen = 0;
			scanner.readChar();
			do {
				// the chunk header contains the size in hex format
				// and could contain additional parameters which we ignore atm
				String chunkHeader = scanner.readLine( false );
				String chunkSize = chunkHeader.split( ";", 2 )[ 0 ];
				try {
					l = Integer.parseInt( chunkSize, 16 );
				} catch( NumberFormatException e ) {
					throw new IOException( "Illegal chunk size " + chunkSize );
				}
				// parses the real chunk with the specified size, follwed by CR-LF
				if( l > 0 ) {
					totalLen += l;
					byte[] chunk = new byte[ l ];
					blockingRead( stream, chunk, 0, l );
					chunks.add( chunk );
					scanner.readChar();
					scanner.eatSeparators();
				}
			} while( l > 0 );
			// parse optional trailer (additional HTTP headers)
			parseHeaderProperties( message );
			ByteBuffer b = ByteBuffer.allocate( totalLen );
			chunks.forEach( b::put );
			buffer = b.array();
		} else if( contentLength > 0 ) {
			buffer = new byte[ contentLength ];
			blockingRead( stream, buffer, 0, contentLength );
		}

		if( buffer != null ) {
			message.setContent( buffer );
		} else {
			message.setContent( new byte[ 0 ] );
		}
	}

	public LSPMessage parse() throws IOException {
		LSPMessage message = new LSPMessage();
		parseHeaderProperties( message );
		readContent( message );
		return message;
	}
}
