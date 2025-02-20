/*
 * Copyright (C) 2006-2015 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.process;

import jolie.ExecutionThread;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public class PostIncrementProcess implements Process, Expression {
	private final VariablePath path;
	private final ParsingContext context;

	public PostIncrementProcess( VariablePath varPath, ParsingContext context ) {
		this.path = varPath;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new PostIncrementProcess( (VariablePath) path.cloneExpression( reason ), context );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new PostIncrementProcess( (VariablePath) path.cloneExpression( reason ), context );
	}

	@Override
	public void run() throws FaultException {
		if( ExecutionThread.currentThread().isKilled() )
			return;
		final Value val = path.getValue();

		if( !val.isDefined() ) {
			val.setValue( 1 );
		} else {
			final Object o = val.valueObject();
			if( o instanceof Integer ) {
				val.setValue( ((Integer) o).intValue() + 1 );
			} else if( o instanceof Double ) {
				val.setValue( ((Double) o).doubleValue() + 1 );
			} else if( o instanceof Long ) {
				val.setValue( ((Long) o).longValue() + 1 );
			} else {
				throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "expected type int, long, or double." )
					.addContext( this.context );
			}
		}
	}

	@Override
	public Value evaluate() {
		final Value val = path.getValue();
		Value orig = null;
		if( !val.isDefined() ) {
			orig = Value.create( 0 );
			val.setValue( 1 );
		} else {
			final Object o = val.valueObject();
			if( o instanceof Integer ) {
				orig = Value.create( ((Integer) o).intValue() );
				val.setValue( ((Integer) o).intValue() + 1 );
			} else if( o instanceof Double ) {
				orig = Value.create( ((Double) o).doubleValue() );
				val.setValue( ((Double) o).doubleValue() + 1 );
			} else if( o instanceof Long ) {
				orig = Value.create( ((Long) o).longValue() );
				val.setValue( ((Long) o).longValue() + 1 );
			} else {
				throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "expected type int, long, or double." )
					.addContext( this.context )
					.toRuntimeFaultException();
			}
		}
		return orig;
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
