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

package jolie;

import java.util.LinkedList;
import java.util.List;
import jolie.process.CorrelatedProcess;
import jolie.process.Process;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;

/**
 * An ExecutionThread with a dedicated State.
 * @author Fabrizio Montesi
 */
public class SessionThread extends ExecutionThread implements Cloneable
{
	final private jolie.State state;
	final private List< SessionListener > listeners = new LinkedList< SessionListener >();
	
	private SessionThread( Process process, ExecutionThread parent, jolie.State state )
	{
		super( process, parent );
		this.state = state;
	}
	
	@Override
	public SessionThread clone()
	{
		SessionThread ret = new SessionThread( process, parent, state.clone() );
		for( Scope s : scopeStack ) {
			ret.scopeStack.push( s.clone() );
		}
		return ret;
	}

	public void addSessionListener( SessionListener listener )
	{
		listeners.add( listener );
	}

	/**
	 * Constructs a SessionThread with a fresh State.
	 * @param interpreter the Interpreter this thread must refer to
	 * @param process the Process this thread has to execute
	 */
	public SessionThread( Interpreter interpreter, Process process )
	{
		super( interpreter, process );
		state = new jolie.State();
	}
	
	/**
	 * Constructs a SessionThread cloning another ExecutionThread, copying the 
	 * State and Scope stack of the parent.
	 * 
	 * @param process the Process this thread has to execute
	 * @param parent the ExecutionThread to copy
	 * @param notifyProc the CorrelatedProcess to notify when this session expires
	 * @see CorrelatedProcess
	 */
	public SessionThread( Process process, ExecutionThread parent )
	{
		super( process, parent );

		assert( parent != null );

		state = parent.state().clone();
		for( Scope s : parent.scopeStack ) {
			scopeStack.push( s.clone() );
		}
	}
	
	/**
	 * Returns the State of this thread.
	 * @return the State of this thread
	 * @see State
	 */
	public jolie.State state()
	{
		return state;
	}

	public void run()
	{
		try {
			try {
				process().run();
			} catch( ExitingException e ) {}
			for( SessionListener listener : listeners ) {
				listener.sessionExecuted( this );
			}
		} catch( FaultException f ) {
			if ( listeners.isEmpty() ) {
				Interpreter.getInstance().logUnhandledFault( f );
			} else {
				for( SessionListener listener : listeners ) {
					listener.sessionError( this, f );
				}
			}
		}
	}
}
