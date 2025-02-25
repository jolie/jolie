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

package jolie.process;

import jolie.ExecutionThread;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public class PreDecrementProcess implements Process, Expression {
	final private VariablePath path;
	final private ParsingContext context;

	public PreDecrementProcess( VariablePath varPath, ParsingContext context ) {
		this.path = varPath;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new PreDecrementProcess( (VariablePath) path.cloneExpression( reason ), context );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new PreDecrementProcess( (VariablePath) path.cloneExpression( reason ), context );
	}

	@Override
	public void run() throws FaultException {
		if( ExecutionThread.currentThread().isKilled() )
			return;
		Value val = path.getValue();

		if( !val.isDefined() ) {
			val.setValue( 1 );
		} else {
			final Object o = val.valueObject();
			if( o instanceof Integer ) {
				val.setValue( ((Integer) o).intValue() - 1 );
			} else if( o instanceof Double ) {
				val.setValue( ((Double) o).doubleValue() - 1 );
			} else if( o instanceof Long ) {
				val.setValue( ((Long) o).longValue() - 1 );
			} else {
				throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "expected type int, long, or double." )
					.withContext( this.context );
			}
		}
	}

	@Override
	public Value evaluate() {
		Value val = path.getValue();
		if( !val.isDefined() ) {
			val.setValue( -1 );
		} else {
			final Object o = val.valueObject();
			if( o instanceof Integer ) {
				val.setValue( ((Integer) o).intValue() - 1 );
			} else if( o instanceof Double ) {
				val.setValue( ((Double) o).doubleValue() - 1 );
			} else if( o instanceof Long ) {
				val.setValue( ((Long) o).longValue() - 1 );
			} else {
				throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "expected type int, long, or double." )
					.withContext( this.context )
					.toRuntimeFaultException();
			}
		}
		return val;
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
