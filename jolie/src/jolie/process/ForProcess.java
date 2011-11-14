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
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public class ForProcess implements Process
{
	final private Expression condition;
	final private Process init, post, process;

	public ForProcess( Process init, Expression condition, Process post, Process process )
	{
		this.init = init;
		this.condition = condition;
		this.post = post;
		this.process = process;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new ForProcess(
					init.clone( reason ),
					condition.cloneExpression( reason ),
					post.clone( reason ),
					process.clone( reason )
				);
	}
	
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}
		
		init.run();
		while ( condition.evaluate().boolValue() ) {
			process.run();
			if ( ExecutionThread.currentThread().isKilled() )
				return;
			post.run();
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
