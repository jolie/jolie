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

import jolie.runtime.*;
import jolie.util.Pair;

public class ForEachArrayItemProcess implements Process
{
	private final VariablePath keyPath, targetPath;
	private final Process process;

	public ForEachArrayItemProcess(
		VariablePath keyPath,
		VariablePath targetPath,
		Process process )
	{
		this.keyPath = keyPath;
		this.targetPath = targetPath;
		this.process = process;
	}

	public Process clone( TransformationReason reason )
	{
		return new ForEachArrayItemProcess(
			keyPath.clone(),
			targetPath.clone(),
			process.clone( reason )
		);
	}

	public void run()
		throws FaultException, ExitingException
	{
		ValueVector targetVector = targetPath.getValueVector();
		int size = targetVector.size();
		VariablePath target = targetPath.clone();
		int length = target.path().length;

		for( int i = 0; i < size; i++ ) {
			target.path()[ length - 1 ] = new Pair<>( target.path()[ length - 1 ].key(), Value.create( i ) );
			keyPath.makePointer( target );
			process.run();
		}
	}

	public boolean isKillable()
	{
		return true;
	}
}
