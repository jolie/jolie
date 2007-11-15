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

public class SumExpression implements Expression
{
	private Vector< Operand > children;
	
	public SumExpression()
	{
		children = new Vector< Operand >();
	}
	
	public Value evaluate()
	{
		Value val = Value.create();
		//TempVariable var = new TempVariable();
		
		for( Operand operand : children ) {
			if ( operand.type() == Constants.OperandType.ADD )
				val.add( operand.expression().evaluate() );
			else
				val.subtract( operand.expression().evaluate() );
		}
		
		return val;
	}
	
	public void add( Expression expression )
	{
		Operand op = new Operand( Constants.OperandType.ADD, expression );
		children.add( op );
	}
	
	public void subtract( Expression expression )
	{
		Operand op = new Operand( Constants.OperandType.SUBTRACT, expression );
		children.add( op );
	}
}
