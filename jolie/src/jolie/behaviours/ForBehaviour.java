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

public final class ForBehaviour implements Behaviour
{
	private final Expression condition;
	private final Behaviour init, post, body, postStep;
	
	private final Behaviour step = new UnkillableBehaviour() {
		@Override
		public void run( StatefulContext ctx )
		{
			if ( condition.evaluate().boolValue() ) {
				ctx.executeNext( body , postStep );
			}
		}
	};

	public ForBehaviour( Behaviour init, Expression condition, Behaviour post, Behaviour body )
	{
		this.init = init;
		this.condition = condition;
		this.post = Behaviour.unkillableLater( ctx -> ctx.executeNext( post, step ) );
		this.postStep = Behaviour.unkillableLater( ctx -> ctx.executeNext( post, step ) );
		this.body = body;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ForBehaviour(
			init.clone( reason ),
			condition.cloneExpression( reason ),
			post.clone( reason ),
			body.clone( reason )
		);
	}
	
	@Override
	public void run( StatefulContext ctx )
		throws FaultException, ExitingException
	{
		ctx.ifNotKilled( () -> ctx.executeNext( init, step ) );
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
