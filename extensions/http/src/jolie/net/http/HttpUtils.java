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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import jolie.net.CommChannel;
import jolie.net.CommMessage;

import jolie.runtime.ByteArray;

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

	private static void errorGenerator( OutputStream ostream, IOException e ) throws IOException {
		StringBuilder httpMessage = new StringBuilder();
		if ( e instanceof UnsupportedEncodingException ) {
			httpMessage.append( "HTTP/1.1 415 Unsupported Media Type" + CRLF );
		} else if ( e instanceof UnsupportedMethodException ) {
			UnsupportedMethodException ex = ( UnsupportedMethodException ) e;
			if ( ex.allowedMethods() == null ) {
				httpMessage.append( "HTTP/1.1 501 Not Implemented" + CRLF );
			} else {
				httpMessage.append( "HTTP/1.1 405 Method Not Allowed" + CRLF );
				httpMessage.append( "Allowed: " );
				Method[] methods = ex.allowedMethods();
				for ( int i = 0; i < methods.length; i++ ) {
					httpMessage.append( methods[i].id() + ( i+1 < methods.length ? ", " : "" ) );
				}
				httpMessage.append( CRLF );
			}
		} else if ( e instanceof UnsupportedHttpVersionException ) {
			httpMessage.append( "HTTP/1.1 505 HTTP Version Not Supported" + CRLF );
		} else {
			httpMessage.append( "HTTP/1.1 500 Internal Server Error" + CRLF );
		}
		String message = e.getMessage() != null ? e.getMessage() : e.toString();
		ByteArray content = new ByteArray( message.getBytes( "utf-8" ) );
		httpMessage.append( "Server: Jolie" + CRLF );
		httpMessage.append( "Content-Type: text/plain; charset=utf-8" + CRLF );
		httpMessage.append( "Content-Length: " + content.size() + CRLF + CRLF );
		ostream.write( httpMessage.toString().getBytes( "utf-8" ) );
		ostream.write( content.getBytes() );
		ostream.flush();
	}

	public static interface HttpProtocol
	{
		CommMessage recv_internal( InputStream istream, OutputStream ostream ) throws IOException;
		void send_internal( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException;
	}

	public static CommMessage recv( InputStream istream, OutputStream ostream, boolean inInputPort, CommChannel channel, HttpProtocol service )
		throws IOException
	{
		try {
			return service.recv_internal( istream, ostream );
		} catch ( IOException e ) {
			if ( inInputPort && channel.isOpen() ) {
				HttpUtils.errorGenerator( ostream, e );
			}
			throw e;
		}
	}

	public static void send( OutputStream ostream, CommMessage message, InputStream istream, boolean inInputPort, CommChannel channel, HttpProtocol service )
		throws IOException
	{
		try {
			service.send_internal( ostream, message, istream );
		} catch ( IOException e ) {
			if ( inInputPort && channel.isOpen() ) {
				HttpUtils.errorGenerator( ostream, e );
			}
			throw e;
		}
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
		return "iso-8859-1"; // this follows RFC 2616 3.4.1 Missing Charset
	}

	public static ByteArray encode( String encoding, ByteArray content, StringBuilder headerBuilder ) throws IOException
	{
		// RFC 7231 section-5.3.4 introduced the "*" (any) option, we opt for gzip as a sane default
		if ( encoding.contains( "gzip" ) || encoding.contains( "*" ) ) {
			ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
			GZIPOutputStream outStream = new GZIPOutputStream( baOutStream );
			outStream.write( content.getBytes() );
			outStream.close();
			content = new ByteArray( baOutStream.toByteArray() );
			headerBuilder.append( "Content-Encoding: gzip" + HttpUtils.CRLF );
		} else if ( encoding.contains( "deflate" ) ) {
			ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
			DeflaterOutputStream outStream = new DeflaterOutputStream( baOutStream );
			outStream.write( content.getBytes() );
			outStream.close();
			content = new ByteArray( baOutStream.toByteArray() );
			headerBuilder.append( "Content-Encoding: deflate" + HttpUtils.CRLF );
		}
		return content;
	}
}
