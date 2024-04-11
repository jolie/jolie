/*
 * Copyright (C) 2016 Fabrizio Montesi <famontesi@gmail.com>
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

import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.util.Pair;

public class ForEachArrayItemProcess implements Process {
	private final VariablePath keyPath, targetPath;
	private final Process process;

	public ForEachArrayItemProcess(
		VariablePath keyPath,
		VariablePath targetPath,
		Process process ) {
		this.keyPath = keyPath;
		this.targetPath = targetPath;
		this.process = process;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ForEachArrayItemProcess(
			keyPath.copy(),
			targetPath.copy(),
			process.copy( reason ) );
	}

	@Override
	public void run()
		throws FaultException, ExitingException {
		final ValueVector targetVector = targetPath.getValueVectorOrNull();
		if( targetVector != null ) {
			int size = targetVector.size();
			VariablePath target = targetPath.copy();
			int length = target.path().length;

			for( int i = 0; i < size; i++ ) {
				target.path()[ length - 1 ] = new Pair<>( target.path()[ length - 1 ].key(), Value.create( i ) );
				keyPath.makePointer( target );
				process.run();
			}
		}
		// keyPath.undef();
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
