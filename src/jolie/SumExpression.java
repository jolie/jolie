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

import java.util.Vector;

public class SumExpression implements Expression
{
	private Vector< Operand > children;
	
	public SumExpression()
	{
		children = new Vector< Operand >();
	}
	
	public Variable evaluate()
	{
		TempVariable var = new TempVariable();
		
		for( Operand operand : children ) {
			if ( operand.type() == Operand.Type.ADD )
				var.add( operand.expression().evaluate() );
			else
				var.subtract( operand.expression().evaluate() );
		}
		
		return var;
	}
	
	public void add( ProductExpression expression )
	{
		Operand op = new Operand( Operand.Type.ADD, expression );
		children.add( op );
	}
	
	public void subtract( ProductExpression expression )
	{
		Operand op = new Operand( Operand.Type.SUBTRACT, expression );
		children.add( op );
	}
}
