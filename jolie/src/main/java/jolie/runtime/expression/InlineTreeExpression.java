/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;


/**
 * Implements inline tree definitions.
 *
 * @author Fabrizio Montesi
 */
public class InlineTreeExpression implements Expression {
	public interface Operation {
		Operation cloneOperation( TransformationReason reason );

		void run( Value inlineValue );
	}

	public static class DeepCopyOperation implements Operation {
		private final VariablePath path;
		private final Expression expression;

		public DeepCopyOperation( VariablePath path, Expression expression ) {
			this.path = path;
			this.expression = expression;
		}

		@Override
		public Operation cloneOperation( TransformationReason reason ) {
			return new DeepCopyOperation( path.copy(), expression.cloneExpression( reason ) );
		}

		@Override
		public void run( Value inlineValue ) {
			if( expression instanceof VariablePath ) {
				Object myObj = ((VariablePath) expression).getValueOrValueVector();
				if( myObj instanceof Value ) {
					path.getValue( inlineValue ).deepCopyWithLinks( (Value) myObj );
				} else if( myObj instanceof ValueVector ) {
					path.getValueVector( inlineValue ).deepCopyWithLinks( (ValueVector) myObj );
				} else {
					throw new RuntimeException( "incomplete case analysis" );
				}
			} else {
				path.getValue( inlineValue ).deepCopyWithLinks( expression.evaluate() );
			}
		}
	}

	public static class AssignmentOperation implements Operation {
		private final VariablePath path;
		private final Expression expression;

		public AssignmentOperation( VariablePath path, Expression expression ) {
			this.path = path;
			this.expression = expression;
		}

		@Override
		public Operation cloneOperation( TransformationReason reason ) {
			return new AssignmentOperation( path.copy(), expression.cloneExpression( reason ) );
		}

		@Override
		public void run( Value inlineValue ) {
			path.getValue( inlineValue ).assignValue( expression.evaluate() );
		}
	}

	public static class PointsToOperation implements Operation {
		private final VariablePath path;
		private final VariablePath target;

		public PointsToOperation( VariablePath path, VariablePath target ) {
			this.path = path;
			this.target = target;
		}

		@Override
		public Operation cloneOperation( TransformationReason reason ) {
			return new PointsToOperation( path.copy(), target.copy() );
		}

		@Override
		public void run( Value inlineValue ) {
			path.makePointer( inlineValue, target );
		}
	}

	private final Expression rootExpression;
	private final Operation[] operations;

	public InlineTreeExpression(
		Expression rootExpression,
		Operation[] operations ) {
		this.rootExpression = rootExpression;
		this.operations = operations;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		Operation[] clonedOperations = new Operation[ operations.length ];
		int i = 0;
		for( Operation operation : operations ) {
			clonedOperations[ i++ ] = operation.cloneOperation( reason );
		}

		return new InlineTreeExpression(
			rootExpression.cloneExpression( reason ),
			clonedOperations );
	}

	@Override
	public Value evaluate() {
		Value inlineValue = Value.create();
		inlineValue.assignValue( rootExpression.evaluate() );

		for( Operation operation : operations ) {
			operation.run( inlineValue );
		}

		return inlineValue;
	}
}
