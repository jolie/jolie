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
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;

public class ScopeBehaviour implements Behaviour
{
	private class Execution implements Behaviour
	{
		final private ScopeBehaviour parent;
		private boolean shouldMerge = true;
		private FaultException fault = null;
		
		public Execution( ScopeBehaviour parent )
		{
			this.parent = parent;
		}
		
		@Override
		public void run(StatefulContext ctx)
			throws FaultException, ExitingException
		{
			ctx.pushScope( parent.id );
			ctx.executeNext(
				new SimpleBehaviour()
				{
					@Override
					public void run( StatefulContext ctx ) throws FaultException, ExitingException
					{
						if ( autoPop ) {
							ctx.popScope( shouldMerge );
						}
						if ( shouldMerge && fault != null ) {
							throw fault;
						}
					}
				}
			);
			runScope( ctx, parent.process );
		}
		
		private void runScope( StatefulContext ctx, Behaviour behaviour )
			throws ExitingException
		{
			ctx.executeNext( 
				behaviour,
				new SimpleBehaviour()
				{
					@Override
					public void run( StatefulContext ctx ) throws FaultException, ExitingException
					{
						if ( ctx.isKilled() ) {
							shouldMerge = false;
							Behaviour b = ctx.getCompensation( id );
							if ( b != null ) { // Termination handling
								FaultException f = ctx.killerFault();
								ctx.clearKill();
								ctx.executeNext( new SimpleBehaviour()
									{
										@Override
										public void run( StatefulContext ctx ) throws FaultException, ExitingException
										{
											ctx.kill( f );
										}
									}
								);
								runScope( ctx, b );
							}
						}
					}
				}
			);
//			
//			try {
//				p.run( ctx );
//				if ( ctx.isKilled() ) {
//					shouldMerge = false;
//					p = ctx.getCompensation( id );
//					if ( p != null ) { // Termination handling
//						FaultException f = ctx.killerFault();
//						ctx.clearKill();
//						this.runScope( p );
//						ctx.kill( f );
//					}
//				}
//			} catch( FaultException f ) {
//				p = ctx.getFaultHandler( f.faultName(), true );
//				if ( p != null ) {
//					Value scopeValue =
//							new VariablePathBuilder( false )
//							.add( ctx.currentScopeId(), 0 )
//							.toVariablePath()
//							.getValue( ctx );
//					scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
//                                        scopeValue.getFirstChild( Constants.Keywords.DEFAULT_HANDLER_NAME ).setValue( f.faultName() );
//					this.runScope( p );
//				} else {
//					fault = f;
//				}
//			}
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
	}
	
	private final String id;
	private final Behaviour process;
	private final boolean autoPop;
	
	public ScopeBehaviour( String id, Behaviour process, boolean autoPop )
	{
		this.id = id;
		this.process = process;
		this.autoPop = autoPop;
	}

	public ScopeBehaviour( String id, Behaviour process )
	{
		this( id, process, true );
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
