/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.net.http.json;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.Type;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Fabrizio Montesi
 */
public class JsonUtils
{
	private final static String ROOT_SIGN = "$";
	private final static String JSONARRAY_KEY = "_";
	private final static String JSONULL = "__NULL__";

	public static void valueToJsonString( Value value, Type type, StringBuilder builder )
		throws IOException
	{
		// TODO: handle the case (type == null)
		
		if ( value.children().isEmpty() ) {
			if ( value.isDefined() ) {
				builder.append( nativeValueToJsonString( value ) );
			} else {
				builder.append( "{}" );
			}
		} else {
			if ( value.hasChildren( JSONARRAY_KEY ) ) {
				valueVectorToJsonString( value.children().get( JSONARRAY_KEY ), builder, true, null );
			} else {
                                
                                int size = value.children().size();
				builder.append( '{' );
				if ( value.isDefined() ) {
					appendKeyColon( builder, ROOT_SIGN );
					builder.append( nativeValueToJsonString( value ) );
					if ( size > 0 ) {
						builder.append( ',' );
					}
				}

				int i = 0;
				for( Entry< String, ValueVector> child : value.children().entrySet() ) {
                                        Type subType = null;
                                        if ( type != null && type.subTypes() != null ) {
                                            subType = type.subTypes().get( child.getKey() );
                                        }
					appendKeyColon( builder, child.getKey() );
					valueVectorToJsonString( child.getValue(), builder, false, subType );
					if ( i++ < size - 1 ) {
						builder.append( ',' );
					}
				}
				builder.append( '}' );
			}
		}
	}

	private static void valueVectorToJsonString( ValueVector vector, StringBuilder builder, boolean isArray, Type type )
		throws IOException
	{
		if ( isArray || (!isArray 
                                &&  (( type!= null && type.cardinality().max() > 1) 
                                     || (type == null && vector.size() > 1 ) )  ) ) {
			builder.append( '[' );
			for( int i = 0; i < vector.size(); i++ ) {
				valueToJsonString( vector.get( i ), type, builder );
				if ( i < vector.size() - 1 ) {
					builder.append( ',' );
				}
			}
			builder.append( ']' );
		} else {
			valueToJsonString( vector.first(), type, builder );
		}
	}

	private static void appendKeyColon( StringBuilder builder, String key )
	{
		builder.append( '"' )
			.append( key )
			.append( "\":" );
	}

	private static String nativeValueToJsonString( Value value )
		throws IOException
	{
		if ( value.isInt() || value.isDouble() ) {
			return value.strValue();
		} else {
			if ( value.strValue().equals( JSONULL ) ) {
				return "null";
			} else {
				return '"' + JSONValue.escape( value.strValue() ) + '"';
			}
		}
	}

	public static void parseJsonIntoValue( Reader reader, Value value, boolean strictEncoding )
		throws IOException
	{
		try {
			Object obj = JSONValue.parseWithException( reader );
			if ( obj instanceof JSONArray ) {
				value.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
			} else {
				jsonObjectToValue( (JSONObject) obj, value, strictEncoding );
			}
		} catch( ParseException e ) {
			throw new IOException( e );
		} catch( ClassCastException e ) {
			throw new IOException( e );
		}
	}

	private static void jsonObjectToValue( JSONObject obj, Value value, boolean strictEncoding )
	{
		Map< String, Object> map = (Map< String, Object>) obj;
		ValueVector vec;
		for( Entry< String, Object> entry : map.entrySet() ) {
			if ( entry.getKey().equals( ROOT_SIGN ) ) {
				if ( entry.getValue() instanceof String ) {
					value.setValue( (String) entry.getValue() );
				} else if ( entry.getValue() instanceof Double ) {
					value.setValue( (Double) entry.getValue() );
				} else if ( entry.getValue() instanceof Integer ) {
					value.setValue( (Integer) entry.getValue() );
				} else if ( entry.getValue() instanceof Long ) {
					value.setValue( ((Long) entry.getValue()).intValue() );
				} else if ( entry.getValue() instanceof Boolean ) {
					Boolean b = (Boolean) entry.getValue();
					if ( b ) {
						value.setValue( 1 );
					} else {
						value.setValue( 0 );
					}
				} else {
					value.setValue( entry.getValue().toString() );
				}
			} else {
				vec = jsonObjectToValueVector( entry.getValue(), strictEncoding );
				value.children().put( entry.getKey(), vec );
			}
		}
	}

	private static ValueVector jsonObjectToValueVector( Object obj, boolean strictEncoding )
	{
		ValueVector vec = ValueVector.create();
		if ( obj instanceof JSONObject ) {
			Value val = Value.create();
			jsonObjectToValue( (JSONObject) obj, val, strictEncoding );
			vec.add( val );
		} else if ( obj instanceof JSONArray && strictEncoding ) {
			Value arrayValue = Value.create();
			vec.add( arrayValue );
			arrayValue.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
		} else if ( obj instanceof JSONArray && !strictEncoding ) {
			vec = jsonArrayToValueVector( (JSONArray) obj, strictEncoding );
		} else {
			vec.add( getBasicValue( obj ) );
		}
		return vec;
	}

	private static ValueVector jsonArrayToValueVector( JSONArray array, boolean strictEncoding )
	{
		ValueVector vec = ValueVector.create();
		for( Object element : array ) {
			if ( element instanceof JSONObject ) {
				Value val = Value.create();
				jsonObjectToValue( (JSONObject) element, val, strictEncoding );
				vec.add( val );
			} else {
				vec.add( getBasicValue( element ) );
			}
		}
		return vec;
	}

	private static Value getBasicValue( Object obj )
	{
		Value val = Value.create();
		if ( obj instanceof String ) {
			val.setValue( (String) obj );
		} else if ( obj instanceof Double ) {
			val.setValue( (Double) obj );
		} else if ( obj instanceof Integer ) {
			val.setValue( (Integer) obj );
		} else if ( obj instanceof Long ) {
			val.setValue( ((Long) obj).intValue() );
		} else if ( obj instanceof Boolean ) {
			Boolean b = (Boolean) obj;
			if ( b ) {
				val.setValue( 1 );
			} else {
				val.setValue( 0 );
			}
		} else if ( obj == null ) {
			val.setValue( JSONULL );
		} else {
			val.setValue( obj.toString() );
		}
		return val;
	}
}
