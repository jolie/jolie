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

package jolie.runtime;


import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import jolie.SessionListener;
import jolie.StatefulContext;
import jolie.behaviours.Behaviour;
import jolie.behaviours.SequentialBehaviour;
import jolie.behaviours.SimpleBehaviour;
import jolie.behaviours.SpawnBehaviour;

public class SpawnExecution
{
	private class SpawnedContext extends StatefulContext
	{
		private final int index;

		public SpawnedContext(
			StatefulContext parentContext,
			Behaviour process,
			int index
		)
		{
			super(
				new SequentialBehaviour(new Behaviour[] {
					new SimpleBehaviour()
					{
						@Override
						public void run( StatefulContext ctx ) throws FaultException, ExitingException
						{
							parentSpawnProcess.indexPath().getValue( ctx ).setValue( index );
						}
					},
					parentSpawnProcess.body()
				}), 
				parentContext );
			
			super.addSessionListener(new SessionListener()
			{
				@Override
				public void onSessionExecuted( StatefulContext session )
				{
					terminationNotify( (SpawnedContext)session );
				}

				@Override
				public void onSessionError( StatefulContext session, FaultException fault )
				{
					terminationNotify( (SpawnedContext)session );
				}
			});
			
			this.index = index;
		}
	}
	
	private final Collection< SpawnedContext > threads = new HashSet<>();
	private final SpawnBehaviour parentSpawnProcess;
	private final StatefulContext context;
	private CountDownLatch latch;

	public SpawnExecution( StatefulContext ctx, SpawnBehaviour parent )
	{
		this.parentSpawnProcess = parent;
		this.context = ctx;
	}
	
	public void run()
		throws FaultException
	{		
		if ( parentSpawnProcess.inPath() != null ) {
			parentSpawnProcess.inPath().undef( context );
		}
		int upperBound = parentSpawnProcess.upperBound().evaluate().intValue();
		latch = new CountDownLatch( upperBound );
		SpawnedContext thread;
		
		for( int i = 0; i < upperBound; i++ ) {			
			thread = new SpawnedContext(
				context,
				parentSpawnProcess.body(),
				i
			);
			threads.add( thread );
		}

		for( SpawnedContext t : threads ) {
			// We start threads in this other cycle to avoid race conditions on inPath
			t.start();
		}
		
		context.pauseExecution();
	}
	
	private void terminationNotify( SpawnedContext childContext )
	{
		synchronized( this ) {
			if ( parentSpawnProcess.inPath() != null ) {
				parentSpawnProcess.inPath().getValueVector( context.state().root() ).get( childContext.index )
					.deepCopy( parentSpawnProcess.inPath().getValueVector( childContext.state().root() ).first() );
				Object value =  parentSpawnProcess.inPath().getValueVector( childContext.state().root() );
				value = null;
			}
			latch.countDown();
			if (latch.getCount() == 0)
				context.start();
		}
	}
}
