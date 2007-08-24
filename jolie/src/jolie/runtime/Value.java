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

package jolie.runtime;

import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import jolie.Constants;

public class Value implements Expression
{
	private String strValue = new String();
	private int intValue = 0;
	private Constants.VariableType type = Constants.VariableType.UNDEFINED;
	
	private HashMap< String, Vector< Value > > children =
					new HashMap< String, Vector< Value > >();
	private HashMap< String, Value > attributes =
					new HashMap< String, Value >();
	
	public void deepCopy( Value value )
	{
		assignValue( value );
		Value currVal;
		children.clear();
		attributes.clear();
		for( Entry< String, Value > entry : value.attributes.entrySet() ) {
			currVal = new Value();
			currVal.deepCopy( entry.getValue() );
			attributes.put( entry.getKey(), currVal );
		}
		
		for( Entry< String, Vector< Value > > entry : value.children.entrySet() ) {
			Vector< Value > vec = new Vector< Value >();
			for( Value val : entry.getValue() ) {
				currVal = new Value();
				currVal.deepCopy( val );
				vec.add( currVal );
			}
			children.put( entry.getKey(), vec );
		}
	}
	
	public Vector< Value > getChildren( String childId )
	{
		Vector< Value > v = children.get( childId );
		if ( v == null ) {
			v = new Vector< Value > ();
			v.add( new Value() );
			children.put( childId, v );
		}
		
		return v;
	}
	
	public Value getNewChild( String childId )
	{
		Vector< Value > v = children.get( childId );
		if ( v == null ) {
			v = new Vector< Value > ();
			children.put( childId, v );
		}
		Value retVal = new Value();
		v.add( retVal );
		
		return retVal;
	}
	
	public HashMap< String, Vector< Value > > children()
	{
		return children;
	}
	
	public HashMap< String, Value > attributes()
	{
		return attributes;
	}
	
	public Value getAttribute( String attributeId )
	{
		Value attr = attributes.get( attributeId );
		if ( attr == null ) {
			attr = new Value();
			attributes.put( attributeId, attr );
		}
		
		return attr;
	}
	
	public Value evaluate()
	{
		return this;
	}
	
	public Value( Value val )
	{
		if ( val.isDefined() ) {
			if ( val.type() == Constants.VariableType.INT )
				setIntValue( val.intValue() );
			else
				setStrValue( val.strValue() );
		}
	}
	
	public boolean equals( Value val )
	{
		if ( val.isDefined() ) {
			if ( val.isInt() )
				return ( isInt() && intValue() == val.intValue() );
			else
				return ( isString() && strValue().equals( val.strValue() ) );
		}

		return( !isDefined() );
	}
	
	public Value() {}
	
	public Value( String val )
	{
		super();
		setStrValue( val );
	}
	
	public Value( int val )
	{
		super();
		setIntValue( val );
	}
	
	public final synchronized void setStrValue( String value )
	{
		type = Constants.VariableType.STRING;
		this.strValue = value;
	}
	
	public final boolean isInt()
	{
		return ( type == Constants.VariableType.INT );
	}
	
	public final boolean isString()
	{
		return ( type == Constants.VariableType.STRING );
	}
	
	public final boolean isDefined()
	{
		return ( type != Constants.VariableType.UNDEFINED );
	}
	
	public final synchronized void setIntValue( int value )
	{
		type = Constants.VariableType.INT;
		this.intValue = value;
	}

	public final synchronized String strValue()
	{
		if ( type == Constants.VariableType.INT )
			return Integer.toString( intValue );
		return strValue;
	}
	
	public final synchronized int intValue()
	{
		if ( type == Constants.VariableType.STRING ) {
			try {
				return Integer.parseInt( strValue );
			} catch( NumberFormatException e ) {
				return strValue.length();
			}
		}
		
		return intValue;
	}
	
	public Constants.VariableType type()
	{
		return type;
	}
	
	public final synchronized void add( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() + val.intValue() );
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
	}
	
	public final synchronized void multiply( Value val )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() * val.intValue() );
		} else
			assignValue( val );
	}
	
	public final synchronized void divide( Value val )
	{
		if ( !isDefined() )
			assignValue( val );
		else if ( isInt() )
				setIntValue( intValue() / val.intValue() );
	}
	
	public final synchronized void assignValue( Value val )
	{
		if ( val.isInt() )
			setIntValue( val.intValue() );
		else
			setStrValue( val.strValue() );
	}
}
