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

import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;

public class ScopeBehaviour implements Behaviour
{
	public class Execution implements Behaviour
	{
		
		private class MergeScopeBehaviour extends SimpleBehaviour {
			
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				if ( autoPop ) {
					ctx.popScope( shouldMerge || forceMerge, replacedExecution );
				}
				if ( (shouldMerge || forceMerge) && fault != null ) {
					throw fault;
				}
			}
		}
		
		private class ScopeTerminationBehaviour extends SimpleBehaviour {
			
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				if ( ctx.isKilled() ) {
					shouldMerge = false;
					Behaviour b = ctx.getCompensation( id );
					if ( b != null ) { // Termination handling
						FaultException f = ctx.killerFault();
						ctx.clearKill();
						ctx.executeNext( b, new ScopeKillBehaviour( f ) );
					}
				}
			}
		}		
		
		private class ScopeKillBehaviour extends SimpleBehaviour {
			private final FaultException fault;

			public ScopeKillBehaviour( FaultException fault )
			{
				this.fault = fault;
			}
			
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				ctx.kill( fault );
			}
		}
		
		final private ScopeBehaviour parent;
		private Execution replacedExecution = null;
		private boolean shouldMerge = true;
		private FaultException fault = null;
		private final ScopeTerminationBehaviour terminationBehaviour = new ScopeTerminationBehaviour();
		private final MergeScopeBehaviour mergeScopeBehaviour =  new MergeScopeBehaviour();
		
		public Execution( ScopeBehaviour parent )
		{
			this.parent = parent;
		}
		
		@Override
		public void run(StatefulContext ctx)
			throws FaultException, ExitingException
		{
			replacedExecution = ctx.pushScope( parent.id, this );
			ctx.executeNext( parent.process, terminationBehaviour, mergeScopeBehaviour);
		}
		
		@Override
		public Behaviour clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}
		
		public void pop(StatefulContext ctx) {
			ctx.popScope( replacedExecution );
		}
		
		public void catchFault(StatefulContext ctx, FaultException f) {
			Behaviour behaviour = ctx.getFaultHandler( f.faultName(), true );
			if ( behaviour != null ) {
				Value scopeValue =
						new VariablePathBuilder(false )
						.add( ctx.currentScopeId(), 0 )
						.toVariablePath()
						.getValue( ctx );
				scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
				scopeValue.getFirstChild( Constants.Keywords.DEFAULT_HANDLER_NAME ).setValue( f.faultName() );
				ctx.executeNext( behaviour, terminationBehaviour, mergeScopeBehaviour );
			} else {
				fault = f;
				ctx.executeNext( mergeScopeBehaviour );
			}
		}
	}
	
	private final String id;
	private final Behaviour process;
	private final boolean autoPop;
	private final boolean forceMerge;
	
	public ScopeBehaviour( String id, Behaviour process, boolean autoPop )
	{
		this( id, process, autoPop, false );
	}
	
		public ScopeBehaviour( String id, Behaviour process, boolean autoPop, boolean forceMerge )
	{
		this.id = id;
		this.process = process;
		this.autoPop = autoPop;
		this.forceMerge = forceMerge;
	}

	public ScopeBehaviour( String id, Behaviour process )
	{
		this( id, process, true, false );
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ScopeBehaviour( id, process.clone( reason ), autoPop );
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		ctx.executeNext( new Execution(this) );
	}
	
	@Override
	public boolean isKillable()
	{
		return process.isKillable();
	}
}
