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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jolie.net.CommChannel;
import jolie.process.TransformationReason;

/**
 * Handles JOLIE internal data representation.
 * @author Fabrizio Montesi
 * 2007 - Claudio Guidi: added support for double values
 * 2008 - Fabrizio Montesi: new system for internal value storing
 */


class ValueLink extends Value implements Cloneable
{
	final private VariablePath linkPath;
	
	private Value getLinkedValue()
	{
		return linkPath.getValue();
	}
	
	public void setValueObject( Object object )
	{
		getLinkedValue().setValueObject( object );
	}
	
	public void erase()
	{
		getLinkedValue().erase();
	}
	
	@Override
	public ValueLink clone()
	{
		return new ValueLink( linkPath );
	}
	
	public void _deepCopy( Value value, boolean copyLinks )
	{
		getLinkedValue()._deepCopy( value, copyLinks );
	}
	
	public Map< String, ValueVector > children()
	{
		return getLinkedValue().children();
	}
		
	public Object valueObject()
	{
		return getLinkedValue().valueObject();
	}	
	
	public ValueLink( VariablePath path )
	{
		assert( path != null );
		linkPath = path;
	}
	
	public boolean isLink()
	{
		return true;
	}
}

class ValueImpl extends Value
{
	private static final long serialVersionUID = 1L;
	
	private Object valueObject = null;
	private Map< String, ValueVector > children = null;
	
	public void setValueObject( Object object )
	{
		valueObject = object;
	}
	
	public void erase()
	{
		valueObject = null;
		children = null;
	}
	
	public ValueImpl() {}
	
	public boolean isLink()
	{
		return false;
	}
	
	protected void _deepCopy( Value value, boolean copyLinks )
	{
		assignValue( value );

		Map< String, ValueVector > myChildren = children();
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( copyLinks && entry.getValue().isLink() ) {
				myChildren.put( entry.getKey(), ValueVector.createClone( entry.getValue() ) );
			} else {
				ValueVector vec = getChildren( entry.getKey(), myChildren );
				ValueVector otherVector = entry.getValue();
				Value v;
				for( int i = 0; i < otherVector.size(); i++ ) {
					v = otherVector.get( i );
					if ( copyLinks && v.isLink() ) {
						vec.set( ((ValueLink)v).clone(), i );
					} else {
						vec.get( i )._deepCopy( v, copyLinks );
					}
				}
			}
		}
	}
	
	private static ValueVector getChildren( String childId, Map< String, ValueVector > children )
	{
		ValueVector v = children.get( childId );
		if ( v == null ) {
			v = ValueVector.create();
			children.put( childId, v );
		}
		
		return v;
	}
	
	public Map< String, ValueVector > children()
	{
		if ( children == null )
			children = new HashMap< String, ValueVector > ();
		return children;
	}
	
	public Object valueObject()
	{
		return valueObject;
	}

	protected ValueImpl( Object object )
	{
		valueObject = object;
	}

	public ValueImpl( Value val )
	{
		valueObject = val.valueObject();
	} 
}

/**
 * @author Fabrizio Montesi
 *
 */
abstract public class Value implements Expression
{
	abstract public boolean isLink();
	
	static final public Value UNDEFINED_VALUE = Value.create();
	
	public static Value createLink( VariablePath path )
	{
		return new ValueLink( path );
	}
	
	public static Value create()
	{
		return new ValueImpl();
	}
	
	public static Value create( Boolean bool )
	{
		return new ValueImpl( ( bool == true ) ? 1 : 0 );
	}
	
	public static Value create( String str )
	{
		return new ValueImpl( str );
	}
	
	public static Value create( Integer i )
	{
		return new ValueImpl( i );
	}
	
	public static Value create( Double d )
	{
		return new ValueImpl( d );
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
	
	public static Value createDeepCopy( Value value )
	{
		Value ret = Value.create();
		ret.deepCopy( value );
		return ret;
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
	
	abstract public void erase();
	abstract protected void _deepCopy( Value value, boolean copyLinks );
	abstract public Map< String, ValueVector > children();
	abstract public Object valueObject();
	abstract protected void setValueObject( Object object );

	public boolean hasChildren( String childId )
	{
		return ( children().get( childId ) != null );
	}
	
	public ValueVector getChildren( String childId )
	{
		final Map< String, ValueVector > myChildren = children();
		ValueVector v = myChildren.get( childId );
		if ( v == null ) {
			v = ValueVector.create();
			myChildren.put( childId, v );
		}

		return v;
	}
	
	public Value getNewChild( String childId )
	{
		final ValueVector vec = getChildren( childId );
		Value retVal = new ValueImpl();
		vec.add( retVal );
		
		return retVal;
	}
	
	public Value getFirstChild( String childId )
	{
		return getChildren( childId ).get( 0 );
	}
	
	public Value evaluate()
	{
		return this;
	}
	
	public void setValue( Object object )
	{
		setValueObject( object );
	}
	public void setValue( String object )
	{
		setValueObject( object );
	}
	public void setValue( Integer object )
	{
		setValueObject( object );
	}
	public void setValue( Double object )
	{
		setValueObject( object );
	}
		
	public boolean equals( Value val )
	{
		if ( val.isDefined() ) {
			if ( isString() ) {
				return strValue().equals( val.strValue() );
			} else if ( isInt() ) {
				return intValue() == val.intValue();
			} else if ( isDouble() ) {
				return doubleValue() == val.doubleValue();
			} else if ( valueObject() != null ) {
				return valueObject().equals( val.valueObject() );
			}
		}
		return( !isDefined() );
	}
	
	public final boolean isInt()
	{
		return ( valueObject() instanceof Integer );
	}
	
	public final boolean isByteArray()
	{
		return ( valueObject() instanceof ByteArray );
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
	
	public void setValue( CommChannel value )
	{
		setValueObject( value );
	}
	
	public CommChannel channelValue()
	{
		if( !isChannel() )
			return null;
		return (CommChannel)valueObject();
	}

	public String strValue()
	{
		Object o = valueObject();
		if ( valueObject() == null ) {
			return ""; // new String();
		} else if ( o instanceof String ) {
			return (String)o;
		}
		return o.toString();
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
	
	public final void add( Value val )
	{
		if ( isDefined() ) {
			if ( val.isString() )
				setValue( strValue() + val.strValue() );
			else if ( isInt() )
				setValue( intValue() + val.intValue() );
			else if ( isDouble() )
				setValue( doubleValue() + val.doubleValue() );
			else
				setValue( strValue() + val.strValue() );
		} else
			assignValue( val );
	}
	
	public final void subtract( Value val )
	{
		if ( !isDefined() )
			assignValue( val );
		else if ( isInt() )
			setValue( intValue() - val.intValue() );
		else if ( isDouble() )
			setValue( doubleValue() - val.doubleValue() );
	}
	
	public final void multiply( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setValue( intValue() * val.intValue() );
			else if ( isDouble() )
				setValue( doubleValue() * val.doubleValue() );
		} else
			assignValue( val );
	}
	
	public final void divide( Value val )
	{
		if ( !isDefined() )
			assignValue( val );
		else if ( isInt() )
			setValue( intValue() / val.intValue() );
		else if ( isDouble() )
			setValue( doubleValue() / val.doubleValue() );
	}
	
	public final void assignValue( Value val )
	{
		setValue( val.valueObject() );
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		return Value.createClone( this );
	}
}
