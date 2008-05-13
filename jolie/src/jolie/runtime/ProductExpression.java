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
import jolie.process.TransformationReason;

public class ProductExpression implements Expression
{
	final private Vector< Operand > children;
	
	public ProductExpression()
	{
		children = new Vector< Operand >();
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		ProductExpression ret = new ProductExpression();
		for( Operand operand : children ) {
			ret.children.add(
					new Operand(
							operand.type(),
							operand.expression().cloneExpression( reason )
						)
					);
		}
		return ret;
	}
	
	public Value evaluate()
	{
		if ( children.size() == 1 )
			return children.firstElement().expression().evaluate();

		Value val = Value.create();
		
		if ( children.size() > 0 )
			val.assignValue( children.firstElement().expression().evaluate() );

		Operand o;
		for ( int i = 1; i < children.size(); i++ ) {
			o = children.elementAt( i );
			if ( o.type() == Constants.OperandType.MULTIPLY )
				val.multiply( o.expression().evaluate() );
			else
				val.divide( o.expression().evaluate() );
		}
		
		return val;
	}
	
	public void multiply( Expression expression )
	{
		Operand op = new Operand( Constants.OperandType.MULTIPLY, expression );
		children.add( op );
	}
	
	public void divide( Expression expression )
	{
		Operand op = new Operand( Constants.OperandType.DIVIDE, expression );
		children.add( op );
	}
}
