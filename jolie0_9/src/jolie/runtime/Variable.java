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

import java.util.Vector;

import jolie.Constants;

/** A Jolie variable.
 * 
 * @see Variable
 * @author Fabrizio Montesi
 *
 */
abstract public class Variable implements Expression
{
	abstract protected Value value();
	
	public boolean equals( Variable var )
	{
		return value().equals( var.value() );
	}
	
	public int intValue()
	{
		return value().intValue();
	}
	
	public final void setIntValue( int val )
	{
		value().setIntValue( val );
	}
	
	public String strValue()
	{
		return value().strValue();
	}
	
	public final void setStrValue( String val )
	{
		value().setStrValue( val );
	}
	
	public boolean isInt()
	{
		return value().isInt();
	}
	
	public boolean isDefined()
	{
		return value().isDefined();
	}
	
	public boolean isString()
	{
		return value().isString();
	}
	
	public Constants.VariableType type()
	{
		return value().type();
	}
	
	public final synchronized void assignValue( Variable var )
	{
		if ( var.isInt() )
			setIntValue( var.intValue() );
		else
			setStrValue( var.strValue() );
	}
	
	public final Variable evaluate()
	{
		return this;
	}
	
	public final synchronized void add( Variable var )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() + var.intValue() );
			else
				setStrValue( strValue() + var.strValue() );
		} else
			assignValue( var );
	}
	
	public final synchronized void subtract( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else if ( isInt() )
				setIntValue( intValue() - var.intValue() );
	}
	
	public final synchronized void multiply( Variable var )
	{
		if ( isDefined() ) {
			if ( isInt() )
				setIntValue( intValue() * var.intValue() );
		} else
			assignValue( var );
	}
	
	public final synchronized void divide( Variable var )
	{
		if ( !isDefined() )
			assignValue( var );
		else if ( isInt() )
				setIntValue( intValue() / var.intValue() );
	}
	
	public String id()
	{
		return "undefined";
	}
	
	public void castTo( Constants.VariableType newType )
	{
		if ( newType != type() ) {
			if ( newType == Constants.VariableType.STRING )
				setStrValue( strValue() );
			else if ( newType == Constants.VariableType.INT )
				setIntValue( intValue() );
		}
	}
	
	public static void castAll( Vector< ? extends Variable > varsVec, Vector< Constants.VariableType > varTypesVec )
	{
		int i = 0;
		for( Variable var : varsVec )
			var.castTo( varTypesVec.elementAt( i++ ) );
	}
	
	public static Vector< String > getNames( Vector< ? extends Variable > varsVec )
	{
		Vector< String > namesVec = new Vector< String >();
		for( Variable var : varsVec )
			namesVec.add( var.id() );

		return namesVec;
	}
}