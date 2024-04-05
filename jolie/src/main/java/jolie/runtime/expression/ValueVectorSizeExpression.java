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

import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

public class ValueVectorSizeExpression implements Expression {
	private final VariablePath path;

	public ValueVectorSizeExpression( VariablePath path ) {
		this.path = path;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new ValueVectorSizeExpression( path );
	}

	@Override
	public Value evaluate() {
		ValueVector vector = path.getValueVectorOrNull();
		return Value.create( (vector == null) ? 0 : vector.size() );
	}
}
