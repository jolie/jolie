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
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package joliex.util;


import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.ValueVector;

@AndJarDeps( { "commons-text.jar", "commons-lang3.jar" } )
public class StringUtils extends JavaService {
	public Integer length( String request ) {
		return request.length();
	}

	public Value sort( Value request ) {
		String[] list = new String[ request.getChildren( "item" ).size() ];
		int i = 0;
		for( Value v : request.getChildren( "item" ) ) {
			list[ i++ ] = v.strValue();
		}
		Arrays.sort( list );
		Value ret = Value.create();
		ValueVector items = ret.getChildren( "item" );
		for( String s : list ) {
			items.add( Value.create( s ) );
		}
		return ret;
	}

	public static class ReplaceRequest implements ValueConverter {
		private String self, regex, replacement;

		private ReplaceRequest() {}

		public static ReplaceRequest fromValue( Value value ) {
			ReplaceRequest ret = new ReplaceRequest();
			ret.self = value.strValue();
			ret.regex = value.getFirstChild( "regex" ).strValue();
			ret.replacement = value.getFirstChild( "replacement" ).strValue();
			return ret;
		}

		public static Value toValue( ReplaceRequest p ) {
			Value ret = Value.create();
			ret.setValue( p.self );
			ret.getFirstChild( "regex" ).setValue( p.regex );
			ret.getFirstChild( "replacement" ).setValue( p.replacement );
			return ret;
		}
	}



	public String replaceAll( ReplaceRequest request ) {
		return request.self.replaceAll( request.regex, request.replacement );
	}


	public String replaceFirst( ReplaceRequest request ) {
		return request.self.replaceFirst( request.regex, request.replacement );
	}

	public static class StartsWithRequest implements ValueConverter {
		private String self, prefix;

		private StartsWithRequest() {}

		public static StartsWithRequest fromValue( Value value ) {
			StartsWithRequest ret = new StartsWithRequest();
			ret.self = value.strValue();
			ret.prefix = value.getFirstChild( "prefix" ).strValue();
			return ret;
		}

		public static Value toValue( StartsWithRequest p ) {
			Value ret = Value.create();
			ret.setValue( p.self );
			ret.getFirstChild( "prefix" ).setValue( p.prefix );
			return ret;
		}
	}

	public Boolean startsWith( StartsWithRequest request ) {
		return request.self.startsWith( request.prefix );
	}

	public static class EndsWithRequest implements ValueConverter {
		private String self, suffix;

		private EndsWithRequest() {}

		public static EndsWithRequest fromValue( Value value ) {
			EndsWithRequest ret = new EndsWithRequest();
			ret.self = value.strValue();
			ret.suffix = value.getFirstChild( "suffix" ).strValue();
			return ret;
		}

		public static Value toValue( EndsWithRequest request ) {
			Value ret = Value.create();
			ret.setValue( request.self );
			ret.getFirstChild( "suffix" ).setValue( request.suffix );
			return ret;
		}
	}

	public Boolean endsWith( EndsWithRequest request ) {
		return request.self.endsWith( request.suffix );
	}

	public static class JoinRequest implements ValueConverter {
		private String delimiter;
		private ValueVector pieces;

		private JoinRequest() {}

		public static JoinRequest fromValue( Value value ) {
			JoinRequest ret = new JoinRequest();
			ret.delimiter = value.getFirstChild( "delimiter" ).strValue();
			ret.pieces = value.getChildren( "piece" );
			return ret;
		}

		public static Value toValue( JoinRequest p ) {
			Value ret = Value.create();
			ret.getFirstChild( "delimiter" ).setValue( p.delimiter );
			ret.children().put( "piece", p.pieces );
			return ret;
		}
	}


	public String join( JoinRequest request ) {
		int size = request.pieces.size() - 1;
		StringBuilder builder = new StringBuilder();
		if( size >= 0 ) {
			int i;
			for( i = 0; i < size; i++ ) {
				builder.append( request.pieces.get( i ).strValue() ).append( request.delimiter );
			}
			builder.append( request.pieces.get( i ).strValue() );
		}
		return builder.toString();
	}

	public String trim( String s ) {
		return s.trim();
	}

	public String substring( Value request ) {
		final int end;
		if( request.hasChildren( "end" ) ) {
			end = Math.min( request.getFirstChild( "end" ).intValue(), request.strValue().length() );
		} else {
			end = request.strValue().length();
		}

		return request.strValue().substring( request.getFirstChild( "begin" ).intValue(), end );
	}

	public Value split( Value request ) {
		String str = request.strValue();
		int limit = 0;
		Value lValue = request.getFirstChild( "limit" );
		if( lValue.isDefined() ) {
			limit = lValue.intValue();
		}
		String[] ss = str.split(
			request.getFirstChild( "regex" ).strValue(),
			limit );
		Value value = Value.create();
		for( String s : ss ) {
			value.getNewChild( "result" ).add( Value.create( s ) );
		}

		return value;
	}

	public Value splitByLength( Value request ) {
		String str = request.strValue();
		int length = request.getFirstChild( "length" ).intValue();
		Value responseValue = Value.create();
		ValueVector result = responseValue.getChildren( "result" );
		int stringLength = str.length();
		boolean keepRun = true;
		int offset = 0;
		while( keepRun ) {
			if( offset + length >= stringLength ) {
				keepRun = false;
				length = stringLength - offset;
			}
			result.add( Value.create( str.substring( offset, offset += length ) ) );
		}

		return responseValue;
	}

	public Value match( Value request ) {
		Pattern p = Pattern.compile( request.getFirstChild( "regex" ).strValue() );
		Matcher m = p.matcher( request.strValue() );
		Value response = Value.create();
		if( m.matches() ) {
			response.setValue( 1 );
			if( m.groupCount() > 0 ) {
				ValueVector groups = response.getChildren( "group" );
				groups.add( Value.create( (m.group( 0 ) == null) ? "" : m.group( 0 ) ) );
				for( int i = 0; i < m.groupCount(); i++ ) {
					groups.add( Value.create( (m.group( i + 1 ) == null) ? "" : m.group( i + 1 ) ) );
				}
			}
		} else {
			response.setValue( 0 );
		}
		return response;
	}

	public Value find( Value request ) {
		Pattern p = Pattern.compile( request.getFirstChild( "regex" ).strValue() );
		Matcher m = p.matcher( request.strValue() );
		Value response = Value.create();
		if( m.find() ) {
			response.setValue( 1 );
			if( m.groupCount() > 0 ) {
				ValueVector groups = response.getChildren( "group" );
				groups.add( Value.create( (m.group( 0 ) == null) ? "" : m.group( 0 ) ) );
				for( int i = 0; i < m.groupCount(); i++ ) {
					groups.add( Value.create( (m.group( i + 1 ) == null) ? "" : m.group( i + 1 ) ) );
				}
			}
		} else {
			response.setValue( 0 );
		}
		return response;
	}

	public Value findAll( Value request ) {
		Pattern p = Pattern.compile( request.getFirstChild( "regex" ).strValue() );
		Matcher m = p.matcher( request.strValue() );
		Value response = Value.create();
		ValueVector groups = response.getChildren( "group" );
		boolean found = false;
		while( m.find() ) {
			found = true;
			response.setValue( 1 );


			groups.add( Value.create( (m.group( 0 ) == null) ? "" : m.group( 0 ) ) );
			for( int i = 0; i < m.groupCount(); i++ ) {
				groups.add( Value.create( (m.group( i + 1 ) == null) ? "" : m.group( i + 1 ) ) );
			}

		}
		response.setValue( found ? 1 : 0 );
		return response;
	}

	public String leftPad( Value request ) {
		String orig = request.strValue();
		int length = request.getFirstChild( "length" ).intValue();
		if( orig.length() >= length ) {
			return orig;
		}

		char padChar = request.getFirstChild( "char" ).strValue().charAt( 0 );

		StringBuilder builder = new StringBuilder();
		int padLength = length - orig.length();
		for( int i = 0; i < padLength; i++ ) {
			builder.append( padChar );
		}
		builder.append( orig );
		return builder.toString();
	}

	public String valueToPrettyString( Value request ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( request, writer, "root" );
		try {
			printer.run();
		} catch( IOException e ) {
		} // Should never happen
		return writer.toString();
	}

	public Integer indexOf( Value request ) {
		return request.strValue().indexOf( request.getFirstChild( "word" ).strValue() );
	}

	public String rightPad( Value request ) {
		String orig = request.strValue();
		int length = request.getFirstChild( "length" ).intValue();
		if( orig.length() >= length ) {
			return orig;
		}

		char padChar = request.getFirstChild( "char" ).strValue().charAt( 0 );

		StringBuilder builder = new StringBuilder();
		builder.append( orig );
		int padLength = length - orig.length();
		for( int i = 0; i < padLength; i++ ) {
			builder.append( padChar );
		}
		return builder.toString();
	}

	public String getRandomUUID( Value request ) {
		return UUID.randomUUID().toString();
	}

	public String toLowerCase( String str ) {
		return str.toLowerCase();
	}

	public String toUpperCase( String str ) {
		return str.toUpperCase();
	}

	public Boolean contains( Value request ) {
		return request.strValue().contains( request.getFirstChild( "substring" ).strValue() );
	}

	public String urlEncode( Value request ) throws FaultException {
		String charset = request.firstChildOrDefault( "charset", Value::strValue, "UTF-8" );
		try {
			return URLEncoder.encode( request.strValue(), charset );
		} catch( UnsupportedEncodingException e ) {
			throw new FaultException( "UnsupportedEncodingException" );
		}

	}

	public String urlDecode( Value request ) throws FaultException {
		String charset = request.firstChildOrDefault( "charset", Value::strValue, "UTF-8" );
		try {
			return URLDecoder.decode( request.strValue(), charset );
		} catch( UnsupportedEncodingException e ) {
			throw new FaultException( "UnsupportedEncodingException" );
		}
	}

	public String fmt( Value request ) {
		final var hasLocale = request.valueObject() == null;
		final var data = hasLocale ? request.getFirstChild( "data" ) : request;
		final var format = hasLocale ? request.getFirstChild( "format" ).strValue() : request.strValue();

		StringLookup lookup = key -> {
			var keyParts = key.split( ",", 2 );
			var varName = keyParts[ 0 ];
			var formatting = keyParts.length > 1 ? Optional.of( keyParts[ 1 ] ) : Optional.empty();

			var field = data.getFirstChild( varName );

			final String formattedValue;
			if( formatting.isPresent() ) {
				String pattern = "{0," + formatting.get() + "}";
				MessageFormat messageFormat = hasLocale
					? new MessageFormat( pattern,
						Locale.forLanguageTag( request.getFirstChild( "locale" ).strValue() ) )
					: new MessageFormat( pattern );
				formattedValue =
					messageFormat.format( new Object[] { field.valueObject() } );
			} else {
				formattedValue = field.strValue();
			}

			return formattedValue;
		};
		return new StringSubstitutor( lookup, "{", "}", '\\' ).replace( format );
	}
}
