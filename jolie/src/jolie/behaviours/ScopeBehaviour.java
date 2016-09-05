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
	private class Execution
	{
		final private ScopeBehaviour parent;
		final private StatefulContext ctx;
		private boolean shouldMerge = true;
		private FaultException fault = null;
		
		public Execution( ScopeBehaviour parent, StatefulContext ctx )
		{
			this.parent = parent;
			this.ctx = ctx;
		}
		
		public void run()
			throws FaultException, ExitingException
		{
			ctx.pushScope( parent.id );
			runScope( parent.process );
			if ( autoPop ) {
				ctx.popScope( shouldMerge );
			}
			if ( shouldMerge && fault != null ) {
				throw fault;
			}
		}
		
		private void runScope( Behaviour p )
			throws ExitingException
		{
			try {
				p.run( ctx );
				if ( ctx.isKilled() ) {
					shouldMerge = false;
					p = ctx.getCompensation( id );
					if ( p != null ) { // Termination handling
						FaultException f = ctx.killerFault();
						ctx.clearKill();
						this.runScope( p );
						ctx.kill( f );
					}
				}
			} catch( FaultException f ) {
				p = ctx.getFaultHandler( f.faultName(), true );
				if ( p != null ) {
					Value scopeValue =
							new VariablePathBuilder( false )
							.add( ctx.currentScopeId(), 0 )
							.toVariablePath()
							.getValue( ctx );
					scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
                                        scopeValue.getFirstChild( Constants.Keywords.DEFAULT_HANDLER_NAME ).setValue( f.faultName() );
					this.runScope( p );
				} else {
					fault = f;
				}
			}
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
		(new Execution( this, ctx )).run();
	}
	
	@Override
	public boolean isKillable()
	{
		return process.isKillable();
	}
}
