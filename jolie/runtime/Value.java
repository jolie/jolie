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

import jolie.Constants;

public class Value
{
	private String strValue = new String();
	private int intValue = 0;
	private Constants.VariableType type = Constants.VariableType.UNDEFINED;
	
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
}
