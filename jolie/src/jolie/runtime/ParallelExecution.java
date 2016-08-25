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
import jolie.SessionContext;
import jolie.SessionListener;
import jolie.TransparentContext;
import jolie.process.Process;
import jolie.process.SimpleProcess;

public class ParallelExecution
{
	private class ParallelContext extends TransparentContext
	{
		public ParallelContext( Process process, SessionContext parentCtx )
		{
			super( process, parentCtx );
			super.addSessionListener( new SessionListener()
			{
				@Override
				public void onSessionExecuted( SessionContext session )
				{
					terminationNotify( session );
				}

				@Override
				public void onSessionError( SessionContext session, FaultException fault )
				{
					signalFault( session, fault );
				}
			});
		}
	}
	
	final private Collection< SessionContext > runningContexts = new HashSet<>();
	private final SessionContext context;
	private FaultException fault = null;
	private boolean isKilled = false;
	private boolean childrenKilled = false;

	public ParallelExecution( Process[] procs, SessionContext parentCtx)
	{
		for( Process proc : procs ) {
			runningContexts.add( new ParallelContext( proc, parentCtx ) );
		}
		context = parentCtx;
	}
	
	public void run()
		throws FaultException
	{
		context.pauseExecution();
		synchronized( this ) {
			for( SessionContext t : runningContexts ) {
				t.start();
			}
		}
	}
	
	private void terminationNotify( SessionContext ctx )
	{
		synchronized( this ) {
			runningContexts.remove( ctx );
			
			if ( runningContexts.isEmpty() ) {
				childTerminated( ctx );
			}
		}
	}
	
		
	private void signalFault( SessionContext ctx, FaultException f )
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
	
	private void childTerminated(SessionContext childCtx) {
		
		synchronized( this ) {
			if ( context.isKilled() && !runningContexts.isEmpty() && !isKilled) {
				isKilled = true;
				for( SessionContext	runningCtx : runningContexts ) {
					runningCtx.kill( context.killerFault() );
				}
			} else if ( fault != null && !childrenKilled && !isKilled ) {
				childrenKilled = true;
				for( SessionContext	runningCtx : runningContexts ) {
					runningCtx.kill( fault );
				}
			}
			if (runningContexts.isEmpty()) {
				if (fault != null) {
					context.executeNext( new SimpleProcess()
					{
						@Override
						public void run( SessionContext ctx ) throws FaultException, ExitingException
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
