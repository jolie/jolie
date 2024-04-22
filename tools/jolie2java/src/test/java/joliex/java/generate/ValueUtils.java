/**
 * *************************************************************************
 * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
package joliex.java.generate;

import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.typing.TypeCheckingException;

public class ValueUtils {

	public static JolieValue invokeFromValue( Class< ? > cls, Value v ) throws TypeCheckingException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return JolieValue.class.cast( cls.getMethod( "fromValue", Value.class ).invoke( null, v ) );
	}

	public static Value invokeToValue( Class< ? > cls, JolieValue t ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return Value.class.cast( cls.getMethod( "toValue", cls ).invoke( null, t ) );
	}

	public static JolieValue invokeCreateFrom( Class< ? > cls, JolieValue t ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return JolieValue.class.cast( cls.getMethod( "createFrom", JolieValue.class ).invoke( null, t ) );
	}

	public static Object invokeConstructFrom( Class< ? > cls, JolieValue t ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return cls.getMethod( "constructFrom", JolieValue.class ).invoke( null, t );
	}

	public static void invokeSetter( Object builder, String name, Object argument ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		builder.getClass().getMethod( name, argument.getClass() ).invoke( builder, argument );
	}

	public static JolieValue invokeBuild( Object builder ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return JolieValue.class.cast( builder.getClass().getMethod( "build" ).invoke( builder ) );
	}

	public static void compareValues( Value v1, Value v2 ) throws AssertionError {
		compareValues( v1, v2, 0, "v1 -> v2" );
		compareValues( v2, v1, 0, "v2 -> v1" );
	}

	private static void compareValues( Value v1, Value v2, int level, String from ) throws AssertionError {
		assertTrue( 
			"level " + level + ", Root values are different: " + v1.toString() + "," + v2.toString(),
			checkRootValue( v1, v2 ) 
		);

		for( Entry< String, ValueVector > entry : v1.children().entrySet() ) {
			assertTrue( 
				"level " + level + ", from " + from + ", field " + entry.getKey() + ": not present", 
				v2.hasChildren( entry.getKey() ) );

			assertTrue( 
				"level " + level + ", from " + from + ", field " + entry.getKey() + ": The number of subnodes is different, " + entry.getValue().size() + " compared to " + v2.getChildren( entry.getKey() ).size(),
				entry.getValue().size() == v2.getChildren( entry.getKey() ).size() );

			for( int i = 0; i < entry.getValue().size(); i++ )
				compareValues( entry.getValue().get( i ), v2.getChildren( entry.getKey() ).get( i ), level + 1, from );
		}
	}

	private static boolean checkRootValue( Value v1, Value v2 ) {
		boolean resp = true;
		if( v1.isString() && !v2.isString() ) {
			resp = false;
		} else if( v1.isString() && v2.isString() && !(v1.strValue().equals( v2.strValue() )) ) {
			resp = false;
		}
		if( v1.isBool() && !v2.isBool() ) {
			resp = false;
		} else if( v1.isBool() && v2.isBool() && (v1.boolValue() != v2.boolValue()) ) {
			resp = false;
		}
		if( v1.isByteArray() && !v2.isByteArray() ) {
			resp = false;
		} else if( v1.isByteArray() && v2.isByteArray() ) {
			resp = compareByteArrays( v1.byteArrayValue(), v2.byteArrayValue() );
		}
		if( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if( v1.isDouble() && !v2.isDouble() && (v1.doubleValue() != v2.doubleValue()) ) {
			resp = false;
		}
		if( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if( v1.isDouble() && v2.isDouble() && (v1.intValue() != v2.intValue()) ) {
			resp = false;
		}
		if( v1.isLong() && !v2.isLong() ) {
			resp = false;
		} else if( v1.isLong() && v2.isLong() && (v1.longValue() != v2.longValue()) ) {
			resp = false;
		}

		if( !resp ) {
			System.out.println( "v1:" + v1.strValue() + ",isBool:" + v1.isBool() + ",isInt:" + v1.isInt() + ",isLong:"
				+ v1.isLong() + ",isDouble:" + v1.isDouble() + ",isByteArray:" + v1.isByteArray() );
			System.out.println( "v2:" + v2.strValue() + ",isBool:" + v2.isBool() + ",isInt:" + v2.isInt() + ",isLong:"
				+ v2.isLong() + ",isDouble:" + v2.isDouble() + ",isByteArray:" + v2.isByteArray() );
		}
		return resp;

	}

	private static boolean compareByteArrays( ByteArray b1, ByteArray b2 ) {
		if( b1.getBytes().length != b2.getBytes().length ) {
			System.out.println( "ByteArray sizes are different: " + b1.getBytes().length + "," + b2.getBytes().length );
			return false;
		} else {
			for( int i = 0; i < b1.getBytes().length; i++ ) {
				if( b1.getBytes()[ i ] != b2.getBytes()[ i ] ) {
					System.out.println(
						"Bytes at index " + i + " are different: " + b1.getBytes()[ i ] + "," + b2.getBytes()[ i ] );
					return false;
				}
			}
		}
		return true;
	}
}
