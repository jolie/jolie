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

	public static void valueToJsonString( Value value, StringBuilder builder )
		throws IOException
	{
		builder.append( '{' );
		if ( value.isDefined() ) {
			appendKeyColon( builder, ROOT_SIGN );
			builder.append( nativeValueToJsonString( value ) );
		}
		int size = value.children().size();
		int i = 0;
		for( Entry< String, ValueVector > child : value.children().entrySet() ) {
			if ( child.getValue().isEmpty() == false ) {
				appendKeyColon( builder, child.getKey() );
				valueVectorToJsonString( child.getValue(), builder );
			}
			if ( i++ < size - 1 ) {
				builder.append( ',' );
			}
		}
		builder.append( '}' );
	}

	private static void valueVectorToJsonString( ValueVector vector, StringBuilder builder )
		throws IOException
	{
		if ( vector.size() > 1 ) {
			builder.append( '[' );
			for( int i = 0; i < vector.size(); i++ ) {
				valueToJsonString( vector.get( i ), builder );
				if ( i < vector.size() - 1 ) {
					builder.append( ',' );
				}
			}
			builder.append( ']' );
		} else {
			valueToJsonString( vector.first(), builder );
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
			return '"' + JSONValue.escape( value.strValue() ) + '"';
		}
	}

	public static void parseJsonIntoValue( Reader reader, Value value )
		throws IOException
	{
		try {
			jsonObjectToValue( (JSONObject)JSONValue.parseWithException( reader ), value );
		} catch( ParseException e ) {
			throw new IOException( e );
		} catch( ClassCastException e ) {
			throw new IOException( e );
		}
	}

	private static void jsonObjectToValue( JSONObject obj, Value value )
	{
		Map< String, Object > map = (Map< String, Object >)obj;
		ValueVector vec;
		for( Entry< String, Object > entry : map.entrySet() ) {
			if ( entry.getKey().equals( ROOT_SIGN ) ) {
				if ( entry.getValue() instanceof String ) {
					value.setValue( (String) entry.getValue() );
				} else if ( entry.getValue() instanceof Double ) {
					value.setValue( (Double)entry.getValue() );
				} else if ( entry.getValue() instanceof Integer ) {
					value.setValue( (Integer)entry.getValue() );
				} else if ( entry.getValue() instanceof Long ) {
					value.setValue( ((Long)entry.getValue()).intValue() );
				} else if ( entry.getValue() instanceof Boolean ){
					Boolean b = (Boolean)entry.getValue();
					if ( b ) {
						value.setValue( 1 );
					} else {
						value.setValue( 0 );
					}
				} else {
					value.setValue( entry.getValue().toString() );
				}
			} else {
				vec = jsonObjectToValueVector( entry.getValue() );
				value.children().put( entry.getKey(), vec );
			}
		}
	}

	private static ValueVector jsonObjectToValueVector( Object obj )
	{
		ValueVector vec = ValueVector.create();
		Value val;
		if ( obj instanceof JSONObject ) {
			val = Value.create();
			jsonObjectToValue( (JSONObject)obj, val );
			vec.add( val );
		} else if ( obj instanceof JSONArray ) {
			JSONArray array = (JSONArray) obj;
			for ( Object element : array ) {
				if ( element instanceof JSONObject ) {
					val = Value.create();
					jsonObjectToValue( (JSONObject)element, val );
					vec.add( val );
				}
			}
		} else {
			val = Value.create();
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
			} else {
				val.setValue( obj.toString() );
			}
			vec.add(  val );
		}
		return vec;
	}
}
