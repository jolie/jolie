/*
 * Copyright (C) 2016-2017 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.behaviours;

import jolie.StatefulContext;
import jolie.runtime.*;
import jolie.util.Pair;

public class ForEachArrayItemBehaviour implements KillableBehaviour
{
	private class Step implements UnkillableBehaviour {
		private final VariablePath target;
		private final int size, length;
		private int counter;

		public Step()
		{
			this.target = targetPath.clone();
			this.size = target.getValueVector().size();
			this.length = target.path().length;
			this.counter = 0;
		}
		
		@Override
		public void run( StatefulContext ctx )
			throws FaultException, ExitingException
		{
			if ( counter < size ) {
				target.path()[ length - 1 ] = new Pair<>( target.path()[ length - 1 ].key(), Value.create( counter++ ) );
				keyPath.makePointer( ctx, target );
				ctx.executeNext( body, this );
			}
		}		
	}
	
	private final VariablePath keyPath, targetPath;
	private final Behaviour body;

	public ForEachArrayItemBehaviour(
		VariablePath keyPath,
		VariablePath targetPath,
		Behaviour body )
	{
		this.keyPath = keyPath;
		this.targetPath = targetPath;
		this.body = body;
	}

	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ForEachArrayItemBehaviour(
			keyPath.clone(),
			targetPath.clone(),
			body.clone( reason )
		);
	}

	@Override
	public void run( StatefulContext ctx )
		throws FaultException, ExitingException
	{
		ctx.executeNext( new Step() );
	}
}
