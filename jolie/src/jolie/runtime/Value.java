/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *   Copyright (C) by Claudio Guidi                                        *
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

package jolie.runtime;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jolie.net.CommChannel;

class ValueLink extends Value implements Cloneable
{
	private GlobalVariablePath linkPath;

	public void writeExternal( ObjectOutput out )
		throws IOException
	{
		linkPath.getValue().writeExternal( out );
	}
	
	public ValueLink clone()
	{
		return new ValueLink( linkPath );
	}
	
	public void _deepCopy( Value value, boolean copyLinks )
	{
		linkPath.getValue()._deepCopy( value, copyLinks );
	}
	
	public ValueVector getChildren( String childId )
	{
		return linkPath.getValue().getChildren( childId );
	}
	
	public Value getNewChild( String childId )
	{
		return linkPath.getValue().getNewChild( childId );
	}
	
	public Map< String, ValueVector > children()
	{
		return linkPath.getValue().children();
	}
	
	public Map< String, Value > attributes()
	{
		return linkPath.getValue().attributes();
	}
	
	public Value getAttribute( String attributeId )
	{
		return linkPath.getValue().getAttribute( attributeId );
	}
	
	public void setValueObject( Object object )
	{
		linkPath.getValue().setValueObject( object );
	}
	
	public Object valueObject()
	{
		return linkPath.getValue().valueObject();
	}	
	
	public ValueLink( GlobalVariablePath path )
	{
		linkPath = path;
	}
	
	public boolean isLink()
	{
		return true;
	}
}

class ValueImpl extends Value implements Externalizable
{
	private static final long serialVersionUID = 1L;
	
	private Object valueObject;
	private ConcurrentHashMap< String, ValueVector > children = null;
	private ConcurrentHashMap< String, Value > attributes = null;
	
	public void readExternal( ObjectInput in )
		throws IOException, ClassNotFoundException
	{
		valueObject = in.readObject();
				
		int n = in.readInt(); // How many attributes?
		int i;
		ValueImpl v;
		String s;
		for( i = 0; i < n; i++ ) {
			s = in.readUTF();
			v = new ValueImpl();
			v.readExternal( in );
			attributes().put( s, v );
		}
		
		n = in.readInt(); // How many children?
		ValueVector vec;
		int size, k;
		for( i = 0; i < n; i++ ) {
			s = in.readUTF();
			vec = ValueVector.create();
			size = in.readInt();
			for( k = 0; k < size; k++ ) {
				v = new ValueImpl();
				v.readExternal( in );
				vec.add( v );
			}
			children().put( s, vec );
		}
	}
	
	public void writeExternal( ObjectOutput out )
		throws IOException
	{
		if ( valueObject != null && (valueObject instanceof Serializable || valueObject instanceof Externalizable) )
			out.writeObject( valueObject );
		else
			out.writeObject( null );
		
		out.writeInt( attributes().size() );
		for( Entry< String, Value > entry : attributes().entrySet() ) {
			out.writeUTF( entry.getKey() );
			entry.getValue().writeExternal( out );
		}
		
		out.writeInt( children().size() );
		for( Entry< String, ValueVector > entry : children().entrySet() ) {
			out.writeUTF( entry.getKey() );
			out.writeInt( entry.getValue().size() );
			for( Value v : entry.getValue() )
				v.writeExternal( out );
		}
	}
	
	public ValueImpl() {}
	
	public boolean isLink()
	{
		return false;
	}
	
	protected void _deepCopy( Value value, boolean copyLinks )
	{
		assignValue( value );
		Value currVal = null;
		
		for( Entry< String, Value > entry : value.attributes().entrySet() ) {
			if ( copyLinks && entry.getValue().isLink() ) {
				currVal = ((ValueLink)entry.getValue()).clone();
			} else {
				currVal = new ValueImpl();
				currVal._deepCopy( entry.getValue(), copyLinks );
			}
			attributes().put( entry.getKey(), currVal );
		}
	
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( copyLinks && entry.getValue().isLink() )
				children().put( entry.getKey(), ValueVector.createClone( entry.getValue() ) );
			else {
				ValueVector vec = getChildren( entry.getKey() );
				ValueVector otherVector = entry.getValue();
				Value v;
				for( int i = 0; i < otherVector.size(); i++ ) {
					v = otherVector.get( i );
					if ( copyLinks && v.isLink() )
						vec.set( Value.createClone( v ), i );
					else
						vec.get( i )._deepCopy( v, copyLinks );
				}
			}
		}
	}
	
	public ValueVector getChildren( String childId )
	{
		ValueVector v = children().get( childId );
		if ( v == null ) {
			v = ValueVector.create();
			children().put( childId, v );
		}
	
		return v;
	}
	
	public Value getNewChild( String childId )
	{
		ValueVector vec = getChildren( childId );
		Value retVal = new ValueImpl();
		vec.add( retVal );
		
		return retVal;
	}
	
	public Map< String, ValueVector > children()
	{
		if ( children == null )
			children = new ConcurrentHashMap< String, ValueVector > ();
		return children;
	}
	
	public Map< String, Value > attributes()
	{
		if ( attributes == null )
			attributes = new ConcurrentHashMap< String, Value >();
		return attributes;
	}
	
	public Object valueObject()
	{
		return valueObject;
	}
	
	public void setValueObject( Object object )
	{
		valueObject = object;
	}
	
	public Value getAttribute( String attributeId )
	{
		Value attr = attributes().get( attributeId );
		if ( attr == null ) {
			attr = new ValueImpl();
			attributes().put( attributeId, attr );
		}
		return attr;
	}
	
	public ValueImpl( String val )
	{
		super();
		setStrValue( val );
	}
	
	public ValueImpl( int val )
	{
		super();
		setIntValue( val );
	}
	
	public ValueImpl( double val )
	{
		super();
		setDoubleValue( val );
	}
	
	public ValueImpl( Value val )
	{
		valueObject = val.valueObject();
	}
}

/**
 * @author Fabrizio Montesi
 *
 * @todo Make the creation of the necessary internal data lazy? Less performance and less memory consumption.
 */
abstract public class Value implements Expression
{
	abstract public void writeExternal( ObjectOutput out )
		throws IOException;
	
	public static Value createFromExternal( ObjectInput in )
		throws IOException, ClassNotFoundException
	{
		ValueImpl v = new ValueImpl();
		v.readExternal( in );
		return v;
	}
	
	public abstract boolean isLink();
	
	public static Value createLink( GlobalVariablePath path )
	{
		return new ValueLink( path );
	}
	
	public static Value create()
	{
		return new ValueImpl();
	}
	
	public static Value create( String str )
	{
		return new ValueImpl( str );
	}
	
	public static Value create( int i )
	{
		return new ValueImpl( new Integer( i ) );
	}
	
	public static Value create( double d )
	{
		return new ValueImpl( new Double( d ) );
	}
	
	public static Value create( Value value )
	{
		return new ValueImpl( value );
	}
	
	public static Value createClone( Value value )
	{
		Value retVal = null;
		
		if ( value.isLink() ) {
			retVal = ((ValueLink)value).clone();
		} else {
			retVal = create();
			retVal._deepCopy( value, true );
		}
		
		return retVal;
	}
	
	/**
	 * Makes this value an identical copy of the parameter, considering also its sub-tree.
	 * In case of a sub-link, its pointed Value tree is copied.
	 * @param value The value to be copied. 
	 */
	public synchronized void deepCopy( Value value )
	{
		_deepCopy( value, false );
	}
		
	abstract protected void _deepCopy( Value value, boolean copyLinks );
	
	abstract public ValueVector getChildren( String childId );
	
	abstract public Value getNewChild( String childId );
	
	abstract public Map< String, ValueVector > children();

	abstract public Map< String, Value > attributes();
	
	abstract public Value getAttribute( String attributeId );
	
	public Value evaluate()
	{
		return this;
	}
	
	abstract protected Object valueObject();
	abstract protected void setValueObject( Object object );
	
	public boolean equals( Value val )
	{
		if ( val.isDefined() ) {
			/*if ( val.isInt() )
				return ( isInt() && intValue() == val.intValue() );
			else if ( val.isDouble() )
				return ( isDouble() && doubleValue() == val.doubleValue() );
			else
				return ( isString() && strValue().equals( val.strValue() ) );*/
			Object o = valueObject();
			return( o != null && o.equals( val.valueObject() ) );
		}
		return( !isDefined() );
	}
	
	public void setStrValue( String value )
	{
		setValueObject( value );
	}
	
	public void setDoubleValue( double value )
	{
		setValueObject( new Double( value ) );
	}
	
	public final boolean isInt()
	{
		return ( valueObject() instanceof Integer );
	}
	
	public final boolean isDouble()
	{
		return ( valueObject() instanceof Double );
	}
	
	public final boolean isString()
	{
		return ( valueObject() instanceof String );
	}
	
	public final boolean isChannel()
	{
		return ( valueObject() instanceof CommChannel );
	}
	
	public final boolean isDefined()
	{
		return ( valueObject() != null );
	}
	
	public void setChannel( CommChannel value )
	{
		setValueObject( value );
	}
	
	public CommChannel channelValue()
	{
		if( !isChannel() )
			return null;
		return (CommChannel)valueObject();
	}
	
	public void setIntValue( int value )
	{
		setValueObject( new Integer( value ) );
	}

	public String strValue()
	{
		if ( valueObject() == null )
			return new String();
		return valueObject().toString();
	}
	
	public int intValue()
	{
		int r = 0;
		Object o = valueObject();
		if ( o == null ) {
			return 0;
		} else if ( o instanceof Integer ) {
			r = ((Integer)o).intValue();
		} else if ( o instanceof Double ) {
			r = ((Double)o).intValue();
		} else if ( o instanceof String ) {
			try {
				r = Integer.parseInt( (String)o );
			} catch( NumberFormatException nfe ) {
				r = ((String)o).length();
			}
		}
		return r;
	}
	
	public double doubleValue()
	{
		double r = 0;
		Object o = valueObject();
		if ( o == null ) {
			return 0;
		} else if ( o instanceof Integer ) {
			r = ((Integer)o).doubleValue();
		} else if ( o instanceof Double ) {
			r = ((Double)o).doubleValue();
		} else if ( o instanceof String ) {
			try {
				r = Double.parseDouble( (String)o );
			} catch( NumberFormatException nfe ) {
				r = ((String)o).length();
			}
		}
		return r;
	}
	
	public final synchronized void add( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() + val.intValue() );
			else if ( isDouble() )
				setDoubleValue( doubleValue() + val.doubleValue() );
			else
				setStrValue( strValue() + val.strValue() );
		} else
			assignValue( val );
	}
	
	public final synchronized void subtract( Value val )
	{
		if ( !isDefined() )
			assignValue( val );
		else if ( isInt() )
			setIntValue( intValue() - val.intValue() );
		else if ( isDouble() )
			setDoubleValue( doubleValue() - val.doubleValue() );
	}
	
	public final synchronized void multiply( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() * val.intValue() );
			else if ( isDouble() )
				setDoubleValue( doubleValue() * val.doubleValue() );
		} else
			assignValue( val );
	}
	
	public final synchronized void divide( Value val )
	{
		if ( !isDefined() )
			assignValue( val );
		else if ( isInt() )
			setIntValue( intValue() / val.intValue() );
		else if ( isDouble() )
			setDoubleValue( doubleValue() / val.doubleValue() );
	}
	
	public final synchronized void assignValue( Value val )
	{
		setValueObject( val.valueObject() );
	}
}
