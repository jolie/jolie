/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

/** Skeletal implementation of Variable, provided for convenience.
 * 
 * @see Variable
 * @author Fabrizio Montesi
 *
 */

abstract public class AbstractVariable implements Variable
{
	private String strValue;
	private int intValue;
	private Type type;

	public AbstractVariable()
	{
		type = Type.UNDEFINED;
		intValue = 0;
		strValue = new String();
	}
	
	public final synchronized void setStrValue( String value )
	{
		type = Type.STRING;
		this.strValue = value;
	}
	
	public boolean isInt()
	{
		return ( type == Type.INT );
	}
	
	public boolean isString()
	{
		return ( type == Type.STRING );
	}
	
	public boolean isDefined()
	{
		return ( type != Type.UNDEFINED );
	}
	
	public final synchronized void setIntValue( int value )
	{
		type = Type.INT;
		this.intValue = value;
	}

	public synchronized String strValue()
	{
		if ( type == Type.INT )
			return Integer.toString( intValue );
		return strValue;
	}
	
	public synchronized int intValue()
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
	
	public synchronized void assignValue( Variable var )
	{
		if ( var.type() == Type.INT )
			setIntValue( var.intValue() );
		else
			setStrValue( var.strValue() );
	}
	
	public Type type()
	{
		return type;
	}
	
	public Variable evaluate()
	{
		return this;
	}
	
	public synchronized void add( Variable var )
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
	
	public synchronized void subtract( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() - var.intValue() );
		}
	}
	
	public synchronized void multiply( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else {
			if ( type == Type.INT )
				setIntValue( intValue() * var.intValue() );
		}
	}
	
	public synchronized void divide( Variable var )
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
}