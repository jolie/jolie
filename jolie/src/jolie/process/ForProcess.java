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

package jolie.process;

import jolie.ExecutionThread;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public final class ForProcess implements Process
{
	private final Expression condition;
	private final Process init, post, process;

	public ForProcess( Process init, Expression condition, Process post, Process process )
	{
		this.init = init;
		this.condition = condition;
		this.post = post;
		this.process = process;
	}
	
	@Override
	public Process clone( TransformationReason reason )
	{
		return new ForProcess(
			init.clone( reason ),
			condition.cloneExpression( reason ),
			post.clone( reason ),
			process.clone( reason )
		);
	}
	
	@Override
	public void run()
		throws FaultException, ExitingException
	{
		final ExecutionThread ethread = ExecutionThread.currentThread();
		if ( ethread.isKilled() ) {
			return;
		}
		
		init.run();
		while ( condition.evaluate().boolValue() ) {
			process.run();
			if ( ethread.isKilled() )
				return;
			post.run();
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
