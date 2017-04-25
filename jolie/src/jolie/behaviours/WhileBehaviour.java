/* 
 * Copyright (C) 2006-2017 Fabrizio Montesi <famontesi@gmail.com>
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
import jolie.runtime.expression.Expression;

public class WhileBehaviour implements KillableBehaviour
{
	private final Expression condition;
	private final Behaviour process;

	public WhileBehaviour( Expression condition, Behaviour process )
	{
		this.condition = condition;
		this.process = process;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new WhileBehaviour(
			condition.cloneExpression( reason ),
			process.clone( reason )
		);
	}
	
	@Override
	public void run( StatefulContext ctx )
		throws FaultException, ExitingException
	{
		ctx.ifNotKilled( () -> {
			if( condition.evaluate().boolValue() ) {
				ctx.executeNext( process, this );
			}
		} );
	}
}
