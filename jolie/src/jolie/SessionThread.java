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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jolie.lang.Constants;
import jolie.process.CorrelatedProcess;
import jolie.process.Process;
import jolie.process.TransformationReason;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.util.Pair;

/**
 * An ExecutionThread with a dedicated State.
 * @author Fabrizio Montesi
 */
public class SessionThread extends ExecutionThread implements Cloneable
{
	private final jolie.State state;
	private final List< SessionListener > listeners = new LinkedList< SessionListener >();

	private final static VariablePath typeMismatchPath;
	private final static VariablePath ioExceptionPath;

	static {
		typeMismatchPath =
			new VariablePathBuilder( false )
			.add( "main", 0 )
			.add( Constants.TYPE_MISMATCH_FAULT_NAME, 0 )
			.toVariablePath();
		ioExceptionPath =
			new VariablePathBuilder( false )
			.add( "main", 0 )
			.add( Constants.IO_EXCEPTION_FAULT_NAME, 0 )
			.add( "stackTrace", 0 )
			.toVariablePath();
	}

	/**
	 * Creates and returns a default list of handlers, initialized
	 * with default fault handlers for built-in faults like, e.g., TypeMismatch.
	 * @param interpreter the <code>Interpreter</code> in which the returned map will be used
	 * @return a newly created default list of handlers
	 */
	public static final List< Pair< String, Process > > createDefaultFaultHandlers( final Interpreter interpreter )
	{
		final List< Pair< String, Process > > instList = new ArrayList< Pair< String, Process > >();
		instList.add( new Pair< String, Process >(
			Constants.TYPE_MISMATCH_FAULT_NAME,
			new Process() {
				public void run() throws FaultException, ExitingException
				{
					interpreter.logWarning( typeMismatchPath.getValue().strValue() );
				}

				public Process clone( TransformationReason reason )
				{
					return this;
				}

				public boolean isKillable()
				{
					return true;
				}
			}
		) );
		instList.add( new Pair< String, Process >(
			Constants.IO_EXCEPTION_FAULT_NAME,
			new Process() {
				public void run() throws FaultException, ExitingException
				{
					interpreter.logWarning( ioExceptionPath.getValue().strValue() );
				}

				public Process clone( TransformationReason reason )
				{
					return this;
				}

				public boolean isKillable()
				{
					return true;
				}
			}
		) );
		return instList;
	}

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

	/**
	 * Registers a <code>SessionListener</code> for receiving events from this
	 * session.
	 * @param listener the <code>SessionListener</code> to register
	 */
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
