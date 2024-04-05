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
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;


public class ThrowProcess implements Process {
	final private String faultName;
	final private Expression expression;

	public ThrowProcess( String faultName, Expression expression ) {
		this.faultName = faultName;
		this.expression = expression;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ThrowProcess( faultName, expression );
	}

	@Override
	public void run()
		throws FaultException {
		if( ExecutionThread.currentThread().isKilled() )
			return;

		if( expression == null ) {
			throw new FaultException( faultName );
		} else {
			throw new FaultException( faultName, expression.evaluate() );
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
