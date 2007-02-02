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


package jolie;

/** A Jolie variable.
 * 
 * @see Variable
 * @author Fabrizio Montesi
 *
 */
abstract public class Variable implements Expression
{
	public enum Type {
		UNDEFINED,	///< Undefined variable.
		INT,		///< Integer variable. Used also by operations for type management.
		STRING,		///< String variable. Used also by operations for type management.
		VARIANT		///< Variant variable. Used only by operations for type management.
	}
	
	private String strValue;
	private int intValue;
	private Type type;

	public Variable()
	{
		type = Type.UNDEFINED;
		intValue = 0;
		strValue = new String();
	}
	
	public Variable( Variable variable )
	{
		Variable.Type varType = variable.type();
		if ( varType == Type.INT )
			setIntValue( variable.intValue() );
		else if ( varType == Type.STRING )
			setStrValue( variable.strValue() );
		else {
			type = Type.UNDEFINED;
			intValue = 0;
			strValue = new String();
		}
	}
	
	public final synchronized void setStrValue( String value )
	{
		type = Type.STRING;
		this.strValue = value;
	}
	
	public final boolean isInt()
	{
		return ( type == Type.INT );
	}
	
	public final boolean isString()
	{
		return ( type == Type.STRING );
	}
	
	public final boolean isDefined()
	{
		return ( type != Type.UNDEFINED );
	}
	
	public final synchronized void setIntValue( int value )
	{
		type = Type.INT;
		this.intValue = value;
	}

	public final synchronized String strValue()
	{
		if ( type == Type.INT )
			return Integer.toString( intValue );
		return strValue;
	}
	
	public final synchronized int intValue()
	{
		if ( type == Type.STRING ) {
			try {
				return Integer.parseInt( strValue );
			} catch( NumberFormatException e ) {
				return strValue.length();
			}
		}
		
		return intValue;
	}
	
	public final synchronized void assignValue( Variable var )
	{
		if ( var.isInt() )
			setIntValue( var.intValue() );
		else
			setStrValue( var.strValue() );
	}
	
	public final Type type()
	{
		return type;
	}
	
	public final Variable evaluate()
	{
		return this;
	}
	
	public final synchronized void add( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() + var.intValue() );
			else
				setStrValue( strValue() + var.strValue() );
		}
	}
	
	public final synchronized void subtract( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() - var.intValue() );
		}
	}
	
	public final synchronized void multiply( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() * var.intValue() );
		}
	}
	
	public final synchronized void divide( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() / var.intValue() );
		}
	}
	
	public String id()
	{
		return "undefined";
	}
	
	public void castTo( Type newType )
	{
		if ( newType != type ) {
			if ( newType == Type.STRING )
				setStrValue( strValue() );
			else if ( newType == Type.INT )
				setIntValue( intValue() );
		}
	}
}