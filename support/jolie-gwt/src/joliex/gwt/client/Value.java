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


package joliex.gwt.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Value implements Serializable, IsSerializable
{
	public enum Type implements IsSerializable {
		UNDEFINED, STRING, INT, DOUBLE, LONG, BOOLEAN, BYTEARRAY
	}

	// This field must not be final or GWT will not serialize it correctly
	private Map< String, ValueVector > children = new HashMap< String, ValueVector >();

	private String valueObject = null;
	private Type type = Type.UNDEFINED;
	
	public Value()
	{}
	
	public Value( String value )
	{
		setValue( value );
	}
	
	public Value( Integer value )
	{
		setValue( value );
	}
	
	public Value( Double value )
	{
		setValue( value );
	}
        // Added by Balint Maschio 
	public Value( Long value )
	{
		setValue( value );
	}
        public Value( Boolean value )
	{
		setValue( value );
	}

	public Value( ByteArray value )
	{
		setValue( value );
	}

	public boolean isString()
	{
		return type == Type.STRING;
	}
	
	public boolean isInt()
	{
		return type == Type.INT;
	}
	
	public boolean isDouble()
	{
		return type == Type.DOUBLE;
	}
        
	// Added by Balint Maschio
	public boolean isLong()
	{
		return type == Type.LONG;
	}

	// Added by Balint Maschio
	public boolean isBool()
	{
		return type == Type.BOOLEAN;
	}

	public boolean isByteArray()
	{
		return type == Type.BYTEARRAY;
	}

	public boolean isDefined()
	{
		return type != Type.UNDEFINED;
	}
	
	public ValueVector getChildren( String id )
	{
		ValueVector v = children.get( id );
		if ( v == null ) {
			v = new ValueVector();
			children.put( id, v );
		}
	
		return v;	
	}
	
	public boolean hasChildren()
	{
		return !children.isEmpty();
	}
	
	public boolean hasChildren( String id )
	{
		return children.get( id ) != null;
	}

	public void deepCopy( Value otherValue )
	{
		valueObject = otherValue.valueObject;
                type = otherValue.type;
		ValueVector myVector;
		Value myValue;
		for( Entry< String, ValueVector > entry : otherValue.children.entrySet() ) {
			myVector = new ValueVector();
			for( Value v : entry.getValue() ) {
				myValue = new Value();
				myValue.deepCopy( v );
				myVector.add( v );
			}
			children.put( entry.getKey(), myVector );
		}
	}

	public String strValue()
	{
		if ( valueObject == null )
			return new String();
		return valueObject;
	}

	public int intValue()
	{
		if ( valueObject == null )
			return 0;
		return Integer.valueOf( valueObject );
	}

	public double doubleValue()
	{
		if ( valueObject == null )
			return 0.0;
		return Double.valueOf( valueObject );
	}

	// Added by Balint Maschio
	public long longValue()
	{
		if ( valueObject == null )
			return 0L;
		return Long.valueOf( valueObject );
	}

	public boolean boolValue()
	{
		if ( valueObject == null )
			return false;
		return Boolean.valueOf( valueObject );
	}

        public ByteArray byteArrayValue() {
                
                ByteArray r = null;
		if ( valueObject == null ) {
                        byte[] resp = new byte[0];
			return new ByteArray( resp );
		} else {
                    char[] chars = valueObject.toCharArray();
                    byte[] byteArrayToReturn= new byte[chars.length * 2 ];  //bytes per char = 2
                    for (int i = 0; i < chars.length; i++)
                    {
                        for (int j = 0; j < 2; j++)
                            byteArrayToReturn[i * 2 + j] = (byte) (chars[i] >>> (8 * (1 - j)));
                    }
                    
                    return new ByteArray( byteArrayToReturn );
                }
        }
	
	public Value getNewChild( String childId )
	{
		ValueVector vec = getChildren( childId );
		Value retVal = new Value();
		vec.add( retVal );
		
		return retVal;
	}
	
	public Map< String, ValueVector > children()
	{
		return children;
	}
	
	public Value getFirstChild( String id )
	{
		return getChildren( id ).first();
	}
	
	public final void setValue( String obj )
	{
		valueObject = obj;
		type = Type.STRING;
	}

	public final void setValue( Integer obj )
	{
		valueObject = obj.toString();
		type = Type.INT;
	}
	
	public final void setValue( Double obj )
	{
		valueObject = obj.toString();
		type = Type.DOUBLE;
                
	}
	
	// Added by Balint Maschio
	public final void setValue( Long obj )
	{
		valueObject = obj.toString();
		type = Type.LONG;
                
	}
        
	public final void setValue( Boolean obj )
	{
		valueObject = obj.toString();
		type = Type.BOOLEAN;
	}
        
	public final void setValue( ByteArray obj )
	{
		valueObject = obj.toString();
		type = Type.BYTEARRAY;
	}
        
}
