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

package jolie.behaviours;

import jolie.StatefulContext;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public final class ForBehaviour implements Behaviour
{
	private final Expression condition;
	private final Behaviour init, post, process;
	
	private final Behaviour step = new Behaviour() {
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			if ( condition.evaluate().boolValue() ) {
				ctx.executeNext( process , postStep );
			} else {
				System.out.println( "FOR LOOP CONDITION FALSE... " + ForBehaviour.this );
			}
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			return false;
		}
	};
	
	private final Behaviour postStep = new Behaviour() {
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			if (ctx.isKilled())
				return;
			ctx.executeNext( post, step );
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			return false;
		}
	};

	public ForBehaviour( Behaviour init, Expression condition, Behaviour post, Behaviour process )
	{
		this.init = new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				System.out.println( "RUN FOR INIT "  + ForBehaviour.this );
				init.run( ctx );
			}
		};
		this.condition = condition;
		this.post = new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				System.out.println( "RUN FOR POST "  + ForBehaviour.this );
				post.run( ctx );
			}
		};
		this.process = new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				System.out.println( "RUN FOR PROCESS "  + ForBehaviour.this );
				process.run( ctx );
			}
		};
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ForBehaviour(
			init.clone( reason ),
			condition.cloneExpression( reason ),
			post.clone( reason ),
			process.clone( reason )
		);
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() )
			return;
		
		ctx.executeNext( init, step );
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
