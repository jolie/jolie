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

import jolie.runtime.FaultException;
import jolie.runtime.SpawnExecution;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public class SpawnProcess implements Process {
	private final VariablePath indexPath;
	private final VariablePath inPath; // may be null
	private final Expression upperBound;
	private final Process process;

	public SpawnProcess(
		VariablePath indexPath,
		Expression upperBound,
		VariablePath inPath,
		Process process ) {
		this.indexPath = indexPath;
		this.inPath = inPath;
		this.upperBound = upperBound;
		this.process = process;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new SpawnProcess(
			(VariablePath) indexPath.cloneExpression( reason ),
			upperBound.cloneExpression( reason ),
			(inPath == null) ? null : (VariablePath) inPath.cloneExpression( reason ),
			process.copy( reason ) );
	}

	@Override
	public void run()
		throws FaultException {
		new SpawnExecution( this ).run();
	}

	public Expression upperBound() {
		return upperBound;
	}

	public VariablePath indexPath() {
		return indexPath;
	}

	public VariablePath inPath() {
		return inPath;
	}

	public Process body() {
		return process;
	}

	@Override
	public boolean isKillable() {
		return process.isKillable();
	}
}
