/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
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
import jolie.runtime.VariablePath;

public class ByRefExpression implements Expression {
	private final VariablePath path;

	public ByRefExpression( VariablePath path ) {
		this.path = path;
	}

	public Expression cloneExpression( TransformationReason reason ) {
		return new ByRefExpression( path );
	}

	public Value evaluate() {
		final Value v = path.getValueOrNull();
		path.undef();
		return (v == null) ? Value.create() : v;
	}
}
