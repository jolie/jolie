/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

/** Implements a non-deterministic choice.
 * An NDChoiceProcess instance collects pairs which couple an 
 * InputOperationProcess object with a Process object.
 * When the ChoiceProcess object is run, it waits for 
 * the receiving of a communication on one of its InputProcess objects.
 * When a communication is received, the following happens:
 * \li the communication is resolved by the corresponding InputProcess instance.
 * \li the paired Process object is executed.	
 * 
 * After that, the ChoiceProcess terminates, so the other pairs are ignored.
 * 
 * @author Fabrizio Montesi
 */
public class NDChoiceBehaviour implements Behaviour
{
	private Map< String, Pair< InputOperationBehaviour, Behaviour > > branches = new HashMap<>();
	private Map< String, InputOperation > inputOperationsMap = new HashMap<>();
	
	/** Constructor
	 * @param branches
	 */
	public NDChoiceBehaviour( Pair< InputOperationBehaviour, Behaviour >[] branches )
	{
		for( Pair< InputOperationBehaviour, Behaviour > pair : branches ) {
			this.branches.put( pair.key().inputOperation().id(), pair );
			this.inputOperationsMap.put( pair.key().inputOperation().id(), pair.key().inputOperation() );
		}
		this.branches = Collections.unmodifiableMap( this.branches );
		this.inputOperationsMap = Collections.unmodifiableMap( this.inputOperationsMap );
	}
	
	protected Map< String, Pair< InputOperationBehaviour, Behaviour > > branches()
	{
		return branches;
	}
	
	protected Map< String, InputOperation > inputOperations()
	{
		return inputOperationsMap;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		Pair< InputOperationBehaviour, Behaviour >[] b = new Pair[ branches.values().size() ];
		int i = 0;
		for( Pair< InputOperationBehaviour, Behaviour > pair : branches.values() ) {
			b[ i++ ] = new Pair<>( pair.key(), pair.value().clone( reason ) );
		}
		return new NDChoiceBehaviour( b );
	}
	
	/** Runs the non-deterministic choice behaviour.
	 * @param ctx
	 * @throws jolie.runtime.FaultException
	 * @throws jolie.runtime.ExitingException
	 */
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		SessionMessage message = ctx.requestMessage( inputOperationsMap, ctx );
		if ( message == null) {
			ctx.executeNext( this );
			ctx.pauseExecution();
			return;
		}
		
		Pair< InputOperationBehaviour, Behaviour > branch = branches.get( message.message().operationName() );
		ctx.executeNext( branch.key().receiveMessage( message, ctx ), branch.value());
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
