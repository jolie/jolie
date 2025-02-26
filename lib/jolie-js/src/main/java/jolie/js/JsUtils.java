/*
 * Copyright (C) 2011-2015 by Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2013 by Claudio Guidi
 * Copyright (C) 2015 by Matthias Dieter Wallnöfer
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package jolie.js;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.Type;

public class JsUtils {
	/**
	 * Jolie values consist of a root value with optional attribute/child values. JSON defines primitive
	 * values, arrays and objects. JSON objects may contain attributes, but no root value. For this
	 * reason Jolie introduces a "ROOT_SIGN" named attribute on each mapped Jolie value with a root
	 * value set.
	 */
	private static final String ROOT_SIGN = "$";

	/**
	 * Jolie values do not support multi-dimensional arrays as JSON, hence val[i][j] in Jolie becomes
	 * val._[i]._[j] with two nested single- dimensional arrays "JSONARRAY_KEY".
	 */
	public static final String JSONARRAY_KEY = "_";

	// Jolie value -> JSON string
	private static void appendKeyColon( StringBuilder builder, String key ) {
		builder.append( '"' ).append( key ).append( "\":" );
	}

	private static String nativeValueToJsonString( Value value ) throws IOException {
		if( !value.isDefined() ) {
			return "null";
		} else if( value.isInt() || value.isLong() || value.isBool() || value.isDouble() ) {
			return value.strValue();
		} else {
			return String.format( "\"%s\"", JSONValue.escape( value.strValue() ).replace( "\\/", "/" ) );
		}
	}

	private static void valueVectorToJsonString( ValueVector vector, StringBuilder builder, boolean isArray, Type type )
		throws IOException {
		if( vector.size() > 1 || isArray || (type != null && type.cardinality().max() > 1) ) {
			builder.append( '[' );
			for( int i = 0; i < vector.size(); i++ ) {
				valueToJsonString( vector.get( i ), false, type, builder );
				if( i < vector.size() - 1 ) {
					builder.append( ',' );
				}
			}
			builder.append( ']' );
		} else {
			valueToJsonString( vector.first(), false, type, builder );
		}
	}

	public static void valueToJsonString( Value value, boolean extendedRoot, Type type, StringBuilder builder )
		throws IOException {
		if( value.hasChildren( JSONARRAY_KEY ) ) {
			Type subType = (type != null ? type.findSubType( JSONARRAY_KEY ) : null);
			valueVectorToJsonString( value.children().get( JSONARRAY_KEY ), builder, true, subType );
			return;
		}
		int size = value.children().size();
		if( size == 0 ) {
			if( extendedRoot ) {
				builder.append( '{' );
				if( value.isDefined() ) {
					appendKeyColon( builder, ROOT_SIGN );
					builder.append( nativeValueToJsonString( value ) );
				}
				builder.append( '}' );
			} else {
				builder.append( nativeValueToJsonString( value ) );
			}
		} else {
			builder.append( '{' );
			if( value.isDefined() ) {
				appendKeyColon( builder, ROOT_SIGN );
				builder.append( nativeValueToJsonString( value ) );
				builder.append( ',' );
			}
			int i = 0;
			for( Map.Entry< String, ValueVector > child : value.children().entrySet() ) {
				final Type subType =
					(type != null ? type.findSubType( child.getKey(), child.getValue().first() ) : null);
				appendKeyColon( builder, child.getKey() );
				valueVectorToJsonString( child.getValue(), builder, false, subType );
				if( i++ < size - 1 ) {
					builder.append( ',' );
				}
			}
			builder.append( '}' );
		}
	}

	public static void valueToNdJsonString( Value value, boolean extendedRoot, Type type, StringBuilder builder )
		throws IOException {
		if( !value.hasChildren( "item" ) ) {
			throw new IOException( "ndJson requires at least one child node 'item'" );
		}

		for( Value item : value.getChildren( "item" ) ) {
			valueToJsonString( item, extendedRoot, type, builder );
			builder.append( '\n' );
		}
	}

	public static void faultValueToJsonString( Value value, Type type, StringBuilder builder ) throws IOException {
		builder.append( "{\"error\":{\"message\":\"" )
			.append( value.getFirstChild( "error" ).getFirstChild( "message" ).strValue() )
			.append( "\",\"code\":" )
			.append( value.getFirstChild( "error" ).getFirstChild( "code" ).intValue() )
			.append( ",\"data\":" );
		valueToJsonString( value.getFirstChild( "error" ).getFirstChild( "data" ), false, type, builder );
		builder.append( "}}" );
	}

	// JSON string -> Jolie value
	private static void objectToBasicValue( Object obj, Value val ) {
		if( obj instanceof String ) {
			val.setValue( (String) obj );
		} else if( obj instanceof Double ) {
			val.setValue( (Double) obj );
		} else if( obj instanceof Long ) {
			long lval = (Long) obj;
			if( lval > Integer.MAX_VALUE || lval < Integer.MIN_VALUE ) {
				val.setValue( lval );
			} else {
				val.setValue( (int) lval );
			}
		} else if( obj instanceof Boolean ) {
			val.setValue( (Boolean) obj );
		} else if( obj != null ) {
			val.setValue( obj.toString() );
		}
	}

	private static void jsonObjectToValue( JSONObject obj, Value value, boolean strictEncoding ) {
		@SuppressWarnings( "unchecked" )
		Map< String, Object > map = (Map< String, Object >) obj;
		ValueVector vec;
		for( Map.Entry< String, Object > entry : map.entrySet() ) {
			if( entry.getKey().equals( ROOT_SIGN ) ) {
				objectToBasicValue( entry.getValue(), value );
			} else {
				vec = jsonObjectToValueVector( entry.getValue(), strictEncoding );
				value.children().put( entry.getKey(), vec );
			}
		}
	}

	private static ValueVector jsonObjectToValueVector( Object obj, boolean strictEncoding ) {
		ValueVector vec = ValueVector.create();
		if( obj instanceof JSONObject ) {
			Value val = Value.create();
			jsonObjectToValue( (JSONObject) obj, val, strictEncoding );
			vec.add( val );
		} else if( obj instanceof JSONArray && strictEncoding ) {
			Value arrayValue = Value.create();
			vec.add( arrayValue );
			arrayValue.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
		} else if( obj instanceof JSONArray && !strictEncoding ) {
			vec = jsonArrayToValueVector( (JSONArray) obj, strictEncoding );
		} else {
			Value val = Value.create();
			objectToBasicValue( obj, val );
			vec.add( val );
		}
		return vec;
	}

	private static ValueVector jsonArrayToValueVector( JSONArray array, boolean strictEncoding ) {
		ValueVector vec = ValueVector.create();
		for( Object element : array ) {
			Value value = Value.create();
			if( element instanceof JSONArray ) {
				value.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) element, strictEncoding ) );
			} else if( element instanceof JSONObject ) {
				jsonObjectToValue( (JSONObject) element, value, strictEncoding );
			} else {
				objectToBasicValue( element, value );
			}
			vec.add( value );
		}
		return vec;
	}

	public static void parseJsonIntoValue( Reader reader, Value value, boolean strictEncoding )
		throws IOException {
		try {
			Object obj = JSONValue.parseWithException( reader );
			if( obj instanceof JSONArray ) {
				value.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
			} else if( obj instanceof JSONObject ) {
				jsonObjectToValue( (JSONObject) obj, value, strictEncoding );
			} else {
				objectToBasicValue( obj, value );
			}
		} catch( ParseException | ClassCastException e ) {
			throw new IOException( e );
		}
	}


	public static void parseNdJsonIntoValue( BufferedReader reader, Value value, boolean strictEncoding )
		throws IOException {
		List< String > stringItemVector = reader.lines().collect( Collectors.toList() );

		for( String stringItem : stringItemVector ) {
			StringReader itemReader = new StringReader( stringItem );
			try {
				Value itemValue = Value.create();
				Object obj = JSONValue.parseWithException( itemReader );
				if( obj instanceof JSONArray ) {
					itemValue.children().put( JSONARRAY_KEY,
						jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
				} else if( obj instanceof JSONObject ) {
					jsonObjectToValue( (JSONObject) obj, itemValue, strictEncoding );
				} else {
					objectToBasicValue( obj, itemValue );
				}
				value.getChildren( "item" ).add( itemValue );
			} catch( ParseException | ClassCastException e ) {
				throw new IOException( e );
			}

		}

	}
}
