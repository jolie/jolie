/*
 * Copyright (C) 2007-2016 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */


package jolie.runtime.expression;

import java.util.List;
import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

public class ValueVectorSizeExpression implements Expression {
	// Union type for size target
	private sealed interface SizeTarget
		permits VariablePathTarget, ExpressionTarget {
	}

	private record VariablePathTarget(VariablePath path) implements SizeTarget {
	}

	private record ExpressionTarget(CurrentValueExpression expr) implements SizeTarget {
	}

	private final SizeTarget target;

	// Constructor for #variablePath
	public ValueVectorSizeExpression( VariablePath path ) {
		this.target = new VariablePathTarget( path );
	}

	// Constructor for #(expression)
	public ValueVectorSizeExpression( CurrentValueExpression expression ) {
		this.target = new ExpressionTarget( expression );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return switch( target ) {
		case VariablePathTarget(var path) -> new ValueVectorSizeExpression( path );
		case ExpressionTarget(var cve) -> new ValueVectorSizeExpression(
			(CurrentValueExpression) cve.cloneExpression( reason ) );
		};
	}

	/**
	 * Called by WhereEvaluator.bind() to propagate candidate binding to nested expressions.
	 */
	public void bind( Candidate candidate ) {
		switch( target ) {
		case VariablePathTarget(var path) -> {
		} // No binding needed for VariablePath
		case ExpressionTarget(var cve) -> WhereEvaluator.bind( cve, candidate );
		}
	}

	@Override
	public Value evaluate() {
		return switch( target ) {
		// Legacy: #variablePath - delegate to common logic
		case VariablePathTarget(var path) -> evaluateVariablePath( path );

		// New: #($.field) - navigate from current candidate
		case ExpressionTarget(var cve) -> {
			List< Candidate > results =
				ValueNavigator.navigateFromCandidate( cve.currentCandidate, cve.operations );
			yield Value.create( results.isEmpty() ? 0 : results.get( 0 ).vector().size() );
		}
		};
	}

	private Value evaluateVariablePath( VariablePath path ) {
		ValueVector vector = path.getValueVectorOrNull();
		return Value.create( (vector == null) ? 0 : vector.size() );
	}
}
