/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.behaviours;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jolie.StatefulContext;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.util.Pair;

/** 
 * @author Fabrizio Montesi
 */
public class ProvideUntilBehaviour implements Behaviour
{
	
	private final NDChoiceBehaviour provide, until;
	private Map< String, InputOperation > inputOperationsMap = new HashMap<>();
	
	public ProvideUntilBehaviour( NDChoiceBehaviour provide, NDChoiceBehaviour until )
	{
		this.provide = provide;
		this.until = until;
		this.inputOperationsMap.putAll( provide.inputOperations() );
		this.inputOperationsMap.putAll( until.inputOperations() );
		this.inputOperationsMap = Collections.unmodifiableMap( this.inputOperationsMap );
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ProvideUntilBehaviour( (NDChoiceBehaviour)provide.clone( reason ), (NDChoiceBehaviour)until.clone( reason ) );
	}

	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		SessionMessage message = ctx.requestMessage( inputOperationsMap, ctx );
		if ( message == null) {
			System.out.println( "did not find any waiting messages. go to sleep.." );
			ctx.executeNext( this );
			ctx.pauseExecution();
			return;
		}

		Pair< InputOperationBehaviour, Behaviour > branch = provide.branches().get( message.message().operationName() );
		if ( branch == null ) {
			// It is an until branch
			branch = until.branches().get( message.message().operationName() );
		} else {
			// It is not
			ctx.executeNext( this );
		}
		Behaviour b1 = branch.key().receiveMessage( message, ctx );
		Behaviour b2 = branch.value();
		ctx.executeNext( b1, b2 );
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
