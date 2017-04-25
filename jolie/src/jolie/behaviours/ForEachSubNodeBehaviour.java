/*
 * Copyright (C) 2007-2017 Fabrizio Montesi <famontesi@gmail.com>
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
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class ForEachSubNodeBehaviour implements KillableBehaviour
{
	private class Step implements UnkillableBehaviour {
		private final String[] keys;
		private int counter;
		public Step( String[] keys )
		{
			this.keys = keys;
			this.counter = 0;
		}
		
		@Override
		public void run( StatefulContext ctx )
			throws FaultException, ExitingException
		{
			if ( counter < keys.length ) {
				keyPath.getValue( ctx ).setValue( keys[counter++] );
				ctx.executeNext( body, this );
			}
		}		
	}
	
	private final VariablePath keyPath, targetPath;
	private final Behaviour body;

	public ForEachSubNodeBehaviour(
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
		return new ForEachSubNodeBehaviour(
					(VariablePath) keyPath.cloneExpression( reason ),
					(VariablePath) targetPath.cloneExpression( reason ),
					body.clone( reason )
				);
	}
	
	@Override
	public void run( StatefulContext ctx )
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		Value v = targetPath.getValueOrNull( ctx );
		if ( v != null && v.hasChildren() ) {
			final String keys[];
			synchronized( v ) {
				keys = new String[ v.children().keySet().size() ];
				v.children().keySet().toArray( keys );
			}
			
			ctx.executeNext( new Step( keys ) );
		}
	}
}
