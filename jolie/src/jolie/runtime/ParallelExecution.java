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
import jolie.SessionListener;
import jolie.StatefulContext;
import jolie.TransparentContext;
import jolie.behaviours.Behaviour;
import jolie.behaviours.SimpleBehaviour;

public class ParallelExecution
{
	private class ParallelContext extends TransparentContext
	{
		public ParallelContext( Behaviour process, StatefulContext parentCtx )
		{
			super( process, parentCtx );
			super.addSessionListener(new SessionListener()
			{
				@Override
				public void onSessionExecuted( StatefulContext session )
				{
					terminationNotify( session );
				}

				@Override
				public void onSessionError( StatefulContext session, FaultException fault )
				{
					signalFault( session, fault );
				}
			});
		}
	}
	
	final private Collection< StatefulContext > runningContexts = new HashSet<>();
	private final StatefulContext context;
	private FaultException fault = null;
	private boolean isKilled = false;
	private boolean childrenKilled = false;

	public ParallelExecution( Behaviour[] procs, StatefulContext parentCtx)
	{
		for( Behaviour proc : procs ) {
			runningContexts.add( new ParallelContext( proc, parentCtx ) );
		}
		context = parentCtx;
	}
	
	public void run()
		throws FaultException
	{
		context.pauseExecution();
		synchronized( this ) {
			for( StatefulContext t : runningContexts ) {
				t.start();
			}
		}
	}
	
	private void terminationNotify( StatefulContext ctx )
	{
		synchronized( this ) {
			runningContexts.remove( ctx );
			
			if ( runningContexts.isEmpty() ) {
				childTerminated( ctx );
			}
		}
	}
	
		
	private void signalFault( StatefulContext ctx, FaultException f )
	{
		synchronized( this ) {
			runningContexts.remove( ctx );
			if ( !isKilled && fault == null ) {
				fault = f;
				childTerminated( ctx );
			} else if ( runningContexts.isEmpty() ) {
				childTerminated( ctx );
			}
		}
	}
	
	private void childTerminated(StatefulContext childCtx) {
		
		synchronized( this ) {
			if ( context.isKilled() && !runningContexts.isEmpty() && !isKilled) {
				isKilled = true;
				for( StatefulContext	runningCtx : runningContexts ) {
					runningCtx.kill( context.killerFault() );
				}
			} else if ( fault != null && !childrenKilled && !isKilled ) {
				childrenKilled = true;
				for( StatefulContext	runningCtx : runningContexts ) {
					runningCtx.kill( fault );
				}
			}
			if (runningContexts.isEmpty()) {
				if (fault != null) {
					context.executeNext(new SimpleBehaviour()
					{
						@Override
						public void run( StatefulContext ctx ) throws FaultException, ExitingException
						{
							throw fault;
						}
					});
				}
				context.start();
			}
		}
		
	}

}
