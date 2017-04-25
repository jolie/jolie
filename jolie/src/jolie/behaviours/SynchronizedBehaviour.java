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

package jolie.behaviours;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;
import jolie.util.Pair;

public class SynchronizedBehaviour implements Behaviour
{
	final private String id;
	final private Behaviour process;
	
	final private static HashMap<Object, Boolean> lockStates = new HashMap<>();
	final private static HashMap<Object, Queue<StatefulContext>> lockQueues = new HashMap<>();
	
	private class SynchronizedExecutionBehaviour implements UnkillableBehaviour {
		
		private final String scopeId;
		private FaultException fault = null;
		
		private final CatchBehaviour catchBehaviour = new CatchBehaviour();
		private final FinallyBehaviour finallyBehaviour = new FinallyBehaviour();

		public SynchronizedExecutionBehaviour( Behaviour behaviour )
		{
			this.scopeId = this.hashCode() + "-" + "-SynchronizedScope";
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			ArrayList<Pair<String, Behaviour>> faultHandlers = new ArrayList<>();
			faultHandlers.add( new Pair( Constants.Keywords.DEFAULT_HANDLER_NAME, catchBehaviour ) );
			Behaviour scopedBehaviour = new SequentialBehaviour(new Behaviour[] {
				new ScopeBehaviour(
					scopeId,
					new SequentialBehaviour(new Behaviour[] {
						new InstallBehaviour( faultHandlers ),
						process
					}),
					true, false
				),
				finallyBehaviour
			});
			ctx.executeNext( scopedBehaviour );
		}
		
		private class CatchBehaviour implements UnkillableBehaviour {

			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				Value scopeValue = new VariablePathBuilder( false ).add( scopeId, 0 ).toVariablePath().getValue( ctx );
				Value defaultFaultValue = scopeValue.getChildren( Constants.Keywords.DEFAULT_HANDLER_NAME ).get( 0 );
				Value userFaultValueValue = scopeValue.getChildren( defaultFaultValue.strValue() ).get( 0 );
				fault = new FaultException( defaultFaultValue.strValue(), userFaultValueValue );
			}

			@Override
			public Behaviour clone( TransformationReason reason )
			{
				return this;
			}
			
		}		
		private class FinallyBehaviour implements UnkillableBehaviour {
			
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				Object lock = ctx.interpreter().getLock( id );
				synchronized( lock ) {
					if ( !getState( lock ) ) {
						throw new IllegalStateException("This synchronized block is not running");
					} else {
//						System.out.println( ">> Release lock: " + id + " - " + lock.hashCode() );
						setState( lock, false );
						Queue<StatefulContext> waitingContexts = getLockQueue( lock );
						if (!waitingContexts.isEmpty()){
//							System.out.println( ">> wakeup waiter for lock: " + id + " - " + lock.hashCode() );
							waitingContexts.poll().start();
						}
					}
				}
				
				if (fault != null) {
					throw fault;
				}
			}
			
		}
	}
	
	public SynchronizedBehaviour( String id, Behaviour process )
	{
		this.id = id;
		this.process = process;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new SynchronizedBehaviour( id, process.clone( reason ) );
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
//		ctx.executeNext( process );
		Object lock = ctx.interpreter().getLock( id );
		synchronized ( lock ) {
			if ( !getState( lock ) ) {
//				System.out.println( ">> Lock lock: " + id + " - " + lock.hashCode() );
				setState( lock, true );
				ctx.executeNext( new SynchronizedExecutionBehaviour( process ) );
			} else {
//				System.out.println( ">> Wait for lock: " + id + " - " + lock.hashCode() );
				ctx.pauseExecution();
				ctx.executeNext( this );
				getLockQueue( lock ).add( ctx );
			}
		}
	}
		
	private void setState(Object lock, boolean state) {
		lockStates.put( lock, state );
	}

	private Boolean getState(Object lock) {
		lockStates.putIfAbsent( lock, false );
		return lockStates.get( lock );
	}
		
	private Queue<StatefulContext> getLockQueue(Object lock) {
		lockQueues.putIfAbsent( lock, new ArrayDeque<>() );
		return lockQueues.get( lock );
	}
		
	@Override
	public boolean isKillable()
	{
		return process.isKillable();
	}
}