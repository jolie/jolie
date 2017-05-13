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

		ctx.requestMessage(
			inputOperationsMap,
			ctx,
			m -> {
				Pair< InputOperationBehaviour, Behaviour > branch = provide.branches().get( m.message().operationName() );
				if ( branch == null ) {
					// It is an until branch
					branch = until.branches().get( m.message().operationName() );
				} else {
					// Since it is not an until branch, we re-schedule for execution.
					ctx.executeNext( this );
				}
				ctx.executeNext( branch.key().receiveMessage( m, ctx ), branch.value() );
			}
		);
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
