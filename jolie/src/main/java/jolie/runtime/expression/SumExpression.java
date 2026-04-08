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


package jolie.runtime.expression;


import jolie.lang.Constants;
import jolie.process.TransformationReason;
import jolie.runtime.Value;

public final class SumExpression implements Expression {
	final Operand[] children;

	public SumExpression( Operand[] children ) {
		this.children = children;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		Operand[] cc = new Operand[ children.length ];

		int i = 0;
		for( Operand operand : children ) {
			cc[ i++ ] = new Operand( operand.type(), operand.expression().cloneExpression( reason ) );
		}
		return new SumExpression( cc );
	}

	@Override
	public Value evaluate() {
		final Value val = Value.create( children[ 0 ].expression().evaluate() );
		for( int i = 1; i < children.length; i++ ) {
			if( children[ i ].type() == Constants.OperandType.ADD ) {
				val.add( children[ i ].expression().evaluate() );
			} else {
				val.subtract( children[ i ].expression().evaluate() );
			}
		}

		return val;
	}
}
