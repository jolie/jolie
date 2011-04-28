/***************************************************************************
 *   Copyright (C) 2006-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jolie.lang.Constants;
import jolie.net.SessionMessage;
import jolie.process.Process;
import jolie.process.TransformationReason;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.util.Pair;

/**
 * An ExecutionThread representing a session, equipped
 * with a dedicated state and message queue.
 * @author Fabrizio Montesi
 */
public class SessionThread extends ExecutionThread
{
	private class SessionMessageFuture implements Future< SessionMessage >
	{
		private final Lock lock;
		private final Condition condition;
		private SessionMessage sessionMessage = null;
		private boolean isDone = false;

		public SessionMessageFuture()
		{
			lock = new ReentrantLock();
			condition = lock.newCondition();
		}

		public boolean cancel( boolean mayInterruptIfRunning )
		{
			return false;
		}

		public SessionMessage get( long timeout, TimeUnit unit )
			throws InterruptedException, TimeoutException
		{
			try {
				lock.lock();
				if ( !isDone ) {
					if ( !condition.await( timeout, unit ) ) {
						throw new TimeoutException();
					}
				}
			} finally {
				lock.unlock();
			}
			return sessionMessage;
		}

		public SessionMessage get()
			throws InterruptedException
		{
			try {
				lock.lock();
				if ( !isDone ) {
					condition.await();
				}
			} finally {
				lock.unlock();
			}
			return sessionMessage;
		}

		public boolean isCancelled()
		{
			return false;
		}

		public boolean isDone()
		{
			return isDone;
		}

		protected void setResult( SessionMessage sessionMessage )
		{
			lock.lock();
			try {
				this.sessionMessage = sessionMessage;
				isDone = true;
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}

	private class SessionMessageNDFuture extends SessionMessageFuture
	{
		private final String[] operationNames;

		public SessionMessageNDFuture( String[] operationNames )
		{
			super();
			this.operationNames = operationNames;
		}
	
		@Override
		protected void setResult( SessionMessage sessionMessage )
		{
			for( String operationName : operationNames ) {
				if ( operationName.equals( sessionMessage.message().operationName() ) == false ) {
					Deque< SessionMessageFuture > waitersList = messageWaiters.get( operationName );
					if ( waitersList != null ) {
						waitersList.remove( this );
					}
				}
			}
			super.setResult( sessionMessage );
		}
	}

	private final jolie.State state;
	private final List< SessionListener > listeners = new LinkedList< SessionListener >();
	protected final Deque< SessionMessage > messages = new LinkedList< SessionMessage >();
	private final Map< String, Deque< SessionMessageFuture > > messageWaiters =
		new HashMap< String, Deque< SessionMessageFuture > >();

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
	public static List< Pair< String, Process > > createDefaultFaultHandlers( final Interpreter interpreter )
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

	public SessionThread( Process process, jolie.State state, ExecutionThread parent )
	{
		super( parent.interpreter(), process );
		this.state = state;
		for( Scope s : parent.scopeStack ) {
			scopeStack.push( s.clone() );
		}
	}
	
	public boolean isInitialisingThread()
	{
		return false;
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

	public Future< SessionMessage > requestMessage( Map< String, InputOperation > operations )
	{
		SessionMessageFuture future = new SessionMessageNDFuture( operations.keySet().toArray( new String[0] ) );
		synchronized( messages ) {
			SessionMessage message = messages.peekFirst();
			InputOperation operation = null;
			if ( message != null ) {
				operation = operations.get( message.message().operationName() );
			}
			if ( message == null || operation == null ) {
				for( Map.Entry< String, InputOperation > entry : operations.entrySet() ) {
					addMessageWaiter( entry.getValue(), future );
				}
			} else {
				future.setResult( message );
				messages.removeFirst();

				// Check if we unlocked other receives
				boolean keepRun = true;
				while( keepRun && !messages.isEmpty() ) {
					message = messages.peekFirst();
					future = getMessageWaiter( message.message().operationName() );
					if ( future != null ) { // We found a waiter for the unlocked message
						future.setResult( message );
						messages.removeFirst();
					} else {
						keepRun = false;
					}
				}
			}
		}
		return future;
	}

	public Future< SessionMessage > requestMessage( InputOperation operation )
	{
		SessionMessageFuture future = new SessionMessageFuture();
		synchronized( messages ) {
			SessionMessage message = messages.peekFirst();
			if ( message == null
				|| message.message().operationName().equals( operation.id() ) == false
			) {
				addMessageWaiter( operation, future );
			} else {
				future.setResult( message );
				messages.removeFirst();

				// Check if we unlocked other receives
				boolean keepRun = true;
				SessionMessageFuture currFuture;
				while( keepRun && !messages.isEmpty() ) {
					message = messages.peekFirst();
					currFuture = getMessageWaiter( message.message().operationName() );
					if ( currFuture != null ) { // We found a waiter for the unlocked message
						currFuture.setResult( message );
						messages.removeFirst();
					} else {
						keepRun = false;
					}
				}
			}
		}
		return future;
	}

	private void addMessageWaiter( InputOperation operation, SessionMessageFuture future )
	{
		Deque< SessionMessageFuture > waitersList = messageWaiters.get( operation.id() );
		if ( waitersList == null ) {
			waitersList = new LinkedList< SessionMessageFuture >();
			messageWaiters.put( operation.id(), waitersList );
		}
		waitersList.addLast( future );
	}

	private SessionMessageFuture getMessageWaiter( String operationName )
	{
		Deque< SessionMessageFuture > waitersList = messageWaiters.get( operationName );
		if ( waitersList == null || waitersList.isEmpty() ) {
			return null;
		}

		if ( waitersList.size() == 1 ) {
			messageWaiters.remove( operationName );
		}

		return waitersList.removeFirst();
	}

	public void pushMessage( SessionMessage message )
	{
		synchronized( messages ) {
			SessionMessageFuture future = getMessageWaiter( message.message().operationName() );
			if ( future == null ) {
				messages.addLast( message );
			} else {
				future.setResult( message );
			}
		}
	}

	public void run()
	{
		try {
			try {
				process().run();
			} catch( ExitingException e ) {}
			for( SessionListener listener : listeners ) {
				listener.onSessionExecuted( this );
			}
		} catch( FaultException f ) {
			Process p = null;
			while( hasScope() && (p = getFaultHandler( f.faultName(), true )) == null ) {
				popScope();
			}

			try {
				if ( p == null ) {
					Interpreter.getInstance().logUnhandledFault( f );
				} else {
					Value scopeValue =
						new VariablePathBuilder( false ).add( currentScopeId(), 0 ).toVariablePath().getValue();
					scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
					try {
						p.run();
					} catch( ExitingException e ) {}
				}
			} catch( FaultException fault ) {
				for( SessionListener listener : listeners ) {
					listener.onSessionError( this, fault );
				}
			}

			for( SessionListener listener : listeners ) {
				listener.onSessionExecuted( this );
			}
		}
	}
}
