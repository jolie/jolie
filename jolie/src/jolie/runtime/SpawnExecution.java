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
import jolie.SessionContext;
import jolie.SessionListener;
import jolie.TransparentContext;
import jolie.process.Process;
import jolie.process.SimpleProcess;
import jolie.process.SpawnProcess;

public class SpawnExecution
{
	private class SpawnedContext extends TransparentContext
	{
		private final int index;
		private final CountDownLatch latch;

		public SpawnedContext(
			SessionContext parentContext,
			Process process,
			int index,
			CountDownLatch latch
		)
		{
			super( process, parentContext );
			this.index = index;
			this.latch = latch;
			super.addSessionListener( new SessionListener()
			{
				@Override
				public void onSessionExecuted( SessionContext session )
				{
					terminationNotify( SpawnedContext.this );
				}

				@Override
				public void onSessionError( SessionContext session, FaultException fault )
				{
					terminationNotify( SpawnedContext.this );
				}
			});
			executeNext( new SimpleProcess()
			{
				@Override
				public void run( SessionContext ctx ) throws FaultException, ExitingException
				{
					parentSpawnProcess.indexPath().getValue().setValue( index );
				}
			});
		}
	}
	
	private final Collection< SpawnedContext > threads = new HashSet<>();
	private final SpawnProcess parentSpawnProcess;
	private final SessionContext context;
	private CountDownLatch latch;

	public SpawnExecution( SessionContext ctx, SpawnProcess parent )
	{
		this.parentSpawnProcess = parent;
		this.context = ctx;
	}
	
	public void run()
		throws FaultException
	{		
		if ( parentSpawnProcess.inPath() != null ) {
			parentSpawnProcess.inPath().undef();
		}
		int upperBound = parentSpawnProcess.upperBound().evaluate().intValue();
		latch = new CountDownLatch( upperBound );
		SpawnedContext thread;
		
		for( int i = 0; i < upperBound; i++ ) {
			thread = new SpawnedContext(
				context,
				parentSpawnProcess.body(),
				i,
				latch
			);
			threads.add( thread );
		}

		for( SpawnedContext t : threads ) {
			// We start threads in this other cycle to avoid race conditions on inPath
			t.start();
		}
		
		try {
			latch.await();
		} catch( InterruptedException e ) {
			context.interpreter().logWarning( e );
		}
	}
	
	private void terminationNotify( SpawnedContext childContext )
	{
		synchronized( this ) {
			if ( parentSpawnProcess.inPath() != null ) {
				parentSpawnProcess.inPath().getValueVector( context.state().root() ).get( childContext.index )
					.deepCopy( parentSpawnProcess.inPath().getValueVector().first() );
			}
			
			latch.countDown();
		}
	}
}
