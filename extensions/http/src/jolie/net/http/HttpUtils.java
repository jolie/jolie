/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;

import jolie.net.CommChannel;

/**
 * Utilities for handling HTTP messages.
 * @author Fabrizio Montesi
 */
public class HttpUtils
{
	public final static String CRLF = new String(new char[]{13, 10});
	public final static String URL_DECODER_ENC = "UTF-8";

	// Checks if the message requests the channel to be closed or kept open
	public static void recv_checkForChannelClosing( HttpMessage message, CommChannel channel )
	{
		if ( channel != null ) {
			HttpMessage.Version version = message.version();
			if ( version == null || version.equals( HttpMessage.Version.HTTP_1_1 ) ) {
				// The default is to keep the connection open, unless Connection: close is specified
				if ( message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ) ) {
					channel.setToBeClosed( true );
				} else {
					channel.setToBeClosed( false );
				}
			} else if ( version.equals( HttpMessage.Version.HTTP_1_0 ) ) {
				// The default is to close the connection, unless Connection: Keep-Alive is specified
				if ( message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" ) ) {
					channel.setToBeClosed( false );
				} else {
					channel.setToBeClosed( true );
				}
			}
		}
	}

	public static String httpMessageTypeToString( HttpMessage.Type type ) {
		switch ( type ) {
		case GET:
			return "get";
		case HEAD:
			return "head";
		case POST:
			return "post";
		case DELETE:
			return "delete";
		case PUT:
			return "put";
		}
		return null;
	}

	public static void recv_error_generator( OutputStream ostream, IOException e ) throws IOException {
		Writer writer = new OutputStreamWriter( ostream );
		if ( e instanceof UnsupportedEncodingException ) {
			writer.write( "HTTP/1.1 415 Unsupported Media Type" + CRLF );
		} else if ( e instanceof UnsupportedMethodException ) {
			UnsupportedMethodException ex = ( UnsupportedMethodException ) e;
			if ( ex.allowedMethods() == null ) {
				writer.write( "HTTP/1.1 501 Not Implemented" + CRLF );
			} else {
				writer.write( "HTTP/1.1 405 Method Not Allowed" + CRLF );
				writer.write( "Allowed: " );
				Method[] methods = ex.allowedMethods();
				for ( int i = 0; i < methods.length; i++ ) {
					writer.write( methods[i].id() + ( i+1 < methods.length ? ", " : "" ) );
				}
				writer.write( CRLF );
			}
		} else if ( e instanceof UnsupportedHttpVersionException ) {
			writer.write( "HTTP/1.1 505 HTTP Version Not Supported" + CRLF );
		} else {
			writer.write( "HTTP/1.1 500 Internal Server Error" + CRLF );
		}
		writer.write( "Server: Jolie" + CRLF );
		writer.write( "Content-Type: text/plain; charset=utf-8" + CRLF );
		writer.write( "Content-Length: " + e.getMessage().length() + CRLF + CRLF );
		writer.write( e.getMessage() );
		writer.flush();
	}

	public static String getCharset( String defaultCharset, HttpMessage message )
	{
		if ( message != null && message.getProperty( "content-type" ) != null ) {
			String[] contentType = message.getProperty( "content-type" ).split( ";" );
			for ( int i = 1; i < contentType.length; i++ ) {
				if ( contentType[i].toLowerCase().contains( "charset" ) ) {
					String pair[] = contentType[i].split( "=", 2 );
					if ( pair.length == 2 ) {
						return pair[1];
					}
				}
			}
		}
		if ( defaultCharset != null && !defaultCharset.isEmpty() ) {
			return defaultCharset;
		}
		return "utf-8"; // Jolie's default charset which is today's standard
	}
}
