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

import jolie.process.CorrelatedProcess;
import jolie.process.Process;

/**
 * An ExecutionThread with a dedicated State.
 * @author Fabrizio Montesi
 */
public class SessionThread extends ExecutionThread implements Cloneable
{
	final private jolie.State state;
	
	private SessionThread( Process process, ExecutionThread parent, CorrelatedProcess notifyProc, jolie.State state )
	{
		super( process, parent, notifyProc );
		this.state = state;
	}
	
	@Override
	public SessionThread clone()
	{
		SessionThread ret = new SessionThread( process, parent, notifyProc, state.clone() );
		for( Scope s : scopeStack ) {
			ret.scopeStack.push( s.clone() );
		}
		return ret;
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
	public SessionThread( Process process, ExecutionThread parent, CorrelatedProcess notifyProc )
	{
		super( process, parent, notifyProc );

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
}
