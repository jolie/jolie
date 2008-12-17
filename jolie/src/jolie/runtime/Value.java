/***************************************************************************
 *   Copyright (C) 2006 by Fabrizio Montesi <famontesi@gmail.com>          *
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

	public boolean hasChildren()
	{
		return getLinkedValue().hasChildren();
	}

	public boolean hasChildren( String childId )
	{
		return getLinkedValue().hasChildren( childId );
	}

	protected void _refCopy( Value value )
	{
		getLinkedValue()._refCopy( value );
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

	protected void _refCopy( Value value )
	{
		setValueObject( value.valueObject() );
		this.children = value.children();
	}
	
	public void erase()
	{
		valueObject = null;
		children = null;
	}
	
	protected ValueImpl() {}
	
	public boolean isLink()
	{
		return false;
	}

	public boolean hasChildren()
	{
		if ( children == null ) {
			return false;
		}
		return !children.isEmpty();
	}

	public boolean hasChildren( String childId )
	{
		return ( children != null && children.containsKey( childId ) );
	}
	
	protected void _deepCopy( Value value, boolean copyLinks )
	{
		/**
		 * TODO: check if a << b | b << a can generate deadlocks
		 */
		assignValue( value );

		if ( value.hasChildren() ) {
			int i;
			ValueImpl newValue;
			Map< String, ValueVector > myChildren = children();
			for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
				if ( copyLinks && entry.getValue().isLink() ) {
					myChildren.put( entry.getKey(), ValueVector.createClone( entry.getValue() ) );
				} else {
					List< Value > otherVector = entry.getValue().values();
					ValueVector vec = getChildren( entry.getKey(), myChildren );
					i = 0;
					for( Value v : otherVector ) {
						if ( copyLinks && v.isLink() ) {
							vec.set( ((ValueLink)v).clone(), i );
						} else {
							newValue = new ValueImpl();
							newValue._deepCopy( v, copyLinks );
							vec.set( newValue, i );
						}
						i++;
					}
				}
			}
		}
	}
	
	private static ValueVector getChildren( String childId, Map< String, ValueVector > children )
	{
		ValueVector vec = children.get( childId );
		if ( vec == null ) {
			vec = ValueVector.create();
			children.put( childId, vec );
		}

		return vec;
	}

	private static int INITIAL_CAPACITY = 8;
	private static float LOAD_FACTOR = 0.75f;
	
	public synchronized Map< String, ValueVector > children()
	{
		if ( children == null ) {
			children = new HashMap< String, ValueVector > ( INITIAL_CAPACITY, LOAD_FACTOR );
		}
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

	public static Value create( ByteArray b )
	{
		return new ValueImpl( b );
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
	 * Makes this value an identical copy (by value) of the parameter, considering also its sub-tree.
	 * In case of a sub-link, its pointed Value tree is copied.
	 * @param value The value to be copied. 
	 */
	public synchronized void deepCopy( Value value )
	{
		_deepCopy( value, false );
	}

	public synchronized void refCopy( Value value )
	{
		_refCopy( value );
	}

	abstract protected void _refCopy( Value value );
	abstract public void erase();
	abstract protected void _deepCopy( Value value, boolean copyLinks );
	abstract public Map< String, ValueVector > children();
	abstract public Object valueObject();
	abstract protected void setValueObject( Object object );
	abstract public boolean hasChildren();
	abstract public boolean hasChildren( String childId );
	
	public synchronized ValueVector getChildren( String childId )
	{
		final Map< String, ValueVector > myChildren = children();
		ValueVector v = myChildren.get( childId );
		if ( v == null ) {
			v = ValueVector.create();
			myChildren.put( childId, v );
		}

		return v;
	}
	
	public synchronized Value getNewChild( String childId )
	{
		final ValueVector vec = getChildren( childId );
		Value retVal = new ValueImpl();
		vec.add( retVal );
		
		return retVal;
	}
	
	public synchronized Value getFirstChild( String childId )
	{
		return getChildren( childId ).get( 0 );
	}
	
	public Value evaluate()
	{
		return this;
	}
	
	public synchronized void setValue( Object object )
	{
		setValueObject( object );
	}
	public synchronized void setValue( String object )
	{
		setValueObject( object );
	}
	public synchronized void setValue( Integer object )
	{
		setValueObject( object );
	}
	public synchronized void setValue( Double object )
	{
		setValueObject( object );
	}
		
	public synchronized boolean equals( Value val )
	{
		boolean r = false;
		if ( val.isDefined() ) {
			if ( isString() ) {
				r = strValue().equals( val.strValue() );
			} else if ( isInt() ) {
				r = intValue() == val.intValue();
			} else if ( isDouble() ) {
				r = doubleValue() == val.doubleValue();
			} else if ( valueObject() != null ) {
				r = valueObject().equals( val.valueObject() );
			}
		} else {
			// undefined == undefined
			r = !isDefined();
		}
		return r;
	}
	
	public synchronized final boolean isInt()
	{
		return ( valueObject() instanceof Integer );
	}
	
	public synchronized final boolean isByteArray()
	{
		return ( valueObject() instanceof ByteArray );
	}
	
	public synchronized final boolean isDouble()
	{
		return ( valueObject() instanceof Double );
	}
	
	public synchronized final boolean isString()
	{
		return ( valueObject() instanceof String );
	}
	
	public synchronized final boolean isChannel()
	{
		return ( valueObject() instanceof CommChannel );
	}
	
	public synchronized final boolean isDefined()
	{
		return ( valueObject() != null );
	}
	
	public synchronized void setValue( CommChannel value )
	{
		setValueObject( value );
	}
	
	public synchronized CommChannel channelValue()
	{
		if( isChannel() == false ) {
			return null;
		}
		return (CommChannel)valueObject();
	}

	public synchronized String strValue()
	{
		Object o = valueObject();
		if ( valueObject() == null ) {
			return ""; // new String();
		} else if ( o instanceof String ) {
			return (String)o;
		}
		return o.toString();
	}

	public synchronized ByteArray byteArrayValue()
	{
		ByteArray r = null;
		Object o = valueObject();
		if ( o == null ) {
			r = new ByteArray( new byte[0] );
		} else if ( o instanceof ByteArray ) {
			r = (ByteArray)o;
		} else if ( o instanceof Integer ) {
			// TODO: This is slow
			ByteArrayOutputStream bbstream = new ByteArrayOutputStream( 4 );
			try {
				new DataOutputStream( bbstream ).writeInt( (Integer)o );
				r = new ByteArray( bbstream.toByteArray() );
			} catch( IOException e ) {
				r = new ByteArray( new byte[0] );
			}
		} else if ( o instanceof String ) {
			r = new ByteArray( ((String)o).getBytes() );
		} else if ( o instanceof Double ) {
			// TODO: This is slow
			ByteArrayOutputStream bbstream = new ByteArrayOutputStream();
			try {
				new DataOutputStream( bbstream ).writeDouble( (Integer)o );
				r = new ByteArray( bbstream.toByteArray() );
			} catch( IOException e ) {
				r = new ByteArray( new byte[0] );
			}
		}
		return r;
	}
	
	public synchronized int intValue()
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
	
	public synchronized double doubleValue()
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
	
	public synchronized final void add( Value val )
	{
		if ( isDefined() ) {
			if ( val.isString() ) {
				setValue( strValue() + val.strValue() );
			} else if ( isInt() ) {
				setValue( intValue() + val.intValue() );
			} else if ( isDouble() ) {
				setValue( doubleValue() + val.doubleValue() );
			} else {
				setValue( strValue() + val.strValue() );
			}
		} else {
			assignValue( val );
		}
	}
	
	public synchronized final void subtract( Value val )
	{
		if ( !isDefined() ) {
			if ( val.isDouble() ) {
				setValue( 0.0 - val.doubleValue() );
			} else if ( val.isInt() ) {
				setValue( 0 - val.intValue() );
			} else {
				assignValue( val );
			}
		} else if ( isInt() ) {
			setValue( intValue() - val.intValue() );
		} else if ( isDouble() ) {
			setValue( doubleValue() - val.doubleValue() );
		}
	}
	
	public synchronized final void multiply( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() ) {
				setValue( intValue() * val.intValue() );
			} else if ( isDouble() ) {
				setValue( doubleValue() * val.doubleValue() );
			}
		} else {
			assignValue( val );
		}
	}
	
	public synchronized final void divide( Value val )
	{
		if ( !isDefined() ) {
			setValue( 0 );
		} else if ( isInt() ) {
			setValue( intValue() / val.intValue() );
		} else if ( isDouble() ) {
			setValue( doubleValue() / val.doubleValue() );
		}
	}
	
	public synchronized final void modulo( Value val )
	{
		if ( !isDefined() ) {
			assignValue( val );
		} else if ( isInt() ) {
			setValue( intValue() % val.intValue() );
		} else if ( isDouble() ) {
			setValue( doubleValue() % val.doubleValue() );
		}
	}
	
	public synchronized final void assignValue( Value val )
	{
		setValue( val.valueObject() );
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		return Value.createClone( this );
	}
}
