/*
 * Copyright (C) 2006-2021 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import jolie.lang.Constants;
import jolie.net.SessionMessage;
import jolie.process.Process;
import jolie.process.Processes;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.correlation.CorrelationSet;
import jolie.util.Pair;

/**
 * An ExecutionThread representing a session, equipped with a dedicated state and message queue.
 * 
 * @author Fabrizio Montesi
 */
public class SessionThread extends ExecutionThread {
	private static final AtomicLong ID_COUNTER = new AtomicLong( 1L );

	private final long id = ID_COUNTER.getAndIncrement();
	private final jolie.State state;
	private final List< SessionListener > listeners = new ArrayList<>();

	protected final Map< CorrelationSet, Deque< SessionMessage > > messageQueues = new HashMap<>();
	protected final Deque< SessionMessage > uncorrelatedMessageQueue = new ArrayDeque<>();
	private final Map< String, Set< CompletableFuture< SessionMessage > > > messageWaiters = new HashMap<>();

	private final static VariablePath TYPE_MISMATCH_PATH;
	private final static VariablePath IO_EXCEPTION_PATH;

	static {
		TYPE_MISMATCH_PATH =
			new VariablePathBuilder( false )
				.add( "main", 0 )
				.add( Constants.TYPE_MISMATCH_FAULT_NAME, 0 )
				.toVariablePath();
		IO_EXCEPTION_PATH =
			new VariablePathBuilder( false )
				.add( "main", 0 )
				.add( Constants.IO_EXCEPTION_FAULT_NAME, 0 )
				.add( "stackTrace", 0 )
				.toVariablePath();
	}

	/**
	 * Creates and returns a default list of handlers, initialized with default fault handlers for
	 * built-in faults like, e.g., TypeMismatch.
	 * 
	 * @param interpreter the <code>Interpreter</code> in which the returned map will be used
	 * @return a newly created default list of handlers
	 */
	public static List< Pair< String, Process > > createDefaultFaultHandlers( final Interpreter interpreter ) {
		final List< Pair< String, Process > > instList = new ArrayList<>();
		instList.add( Pair.of(
			Constants.TYPE_MISMATCH_FAULT_NAME,
			Processes.stateless( () -> interpreter.logInfo( TYPE_MISMATCH_PATH.getValue().strValue() ) ) ) );
		instList.add( Pair.of(
			Constants.IO_EXCEPTION_FAULT_NAME,
			Processes.stateless( () -> interpreter.logInfo( IO_EXCEPTION_PATH.getValue().strValue() ) ) ) );
		return instList;
	}

	public SessionThread( Process process, jolie.State state, ExecutionThread parent ) {
		super( parent.interpreter(), process );
		this.state = state;
		parent.scopeStack.forEach( s -> scopeStack.push( s.clone() ) );
		initMessageQueues();
	}

	public boolean isInitialisingThread() {
		return false;
	}

	/**
	 * Registers a <code>SessionListener</code> for receiving events from this session.
	 * 
	 * @param listener the <code>SessionListener</code> to register
	 */
	public void addSessionListener( SessionListener listener ) {
		listeners.add( listener );
	}

	/**
	 * Constructs a SessionThread with a fresh State.
	 * 
	 * @param interpreter the Interpreter this thread must refer to
	 * @param process the Process this thread has to execute
	 */
	public SessionThread( Interpreter interpreter, Process process ) {
		super( interpreter, process );
		state = new jolie.State();
		initMessageQueues();
	}

	private void initMessageQueues() {
		for( CorrelationSet cset : interpreter().correlationSets() ) {
			messageQueues.put( cset, new ArrayDeque<>() );
		}
	}

	/**
	 * Constructs a SessionThread cloning another ExecutionThread, copying the State and Scope stack of
	 * the parent.
	 * 
	 * @param process the Process this thread has to execute
	 * @param parent the ExecutionThread to copy
	 */
	public SessionThread( Process process, ExecutionThread parent ) {
		super( process, parent );
		initMessageQueues();
		assert (parent != null);
		state = parent.state().clone();
		parent.scopeStack.forEach( s -> scopeStack.push( s.clone() ) );
	}

	public SessionThread( Interpreter interpreter, Process process, State state ) {
		super( interpreter, process );
		this.state = state;
		initMessageQueues();
	}

	public SessionThread getNewSessionThread() {
		return new SessionThread( this.interpreter(), process, state.clone() );
	}

	/**
	 * Returns the State of this thread.
	 * 
	 * @return the State of this thread
	 * @see State
	 */
	@Override
	public jolie.State state() {
		return state;
	}

	@Override
	public synchronized Future< SessionMessage > requestMessage( Map< String, InputOperation > operations,
		ExecutionThread ethread ) {
		final var messageFuture = new CompletableFuture< SessionMessage >();
		ethread.cancelIfKilled( messageFuture );
		if( messageFuture.isCancelled() ) {
			return messageFuture;
		}

		operations.forEach( ( opName, operation ) -> {
			final var queue = getQueueForOperation( operation.id() );
			final var message = queue.peekFirst();
			if( message != null && message.message().operationName().equals( operation.id() ) ) {
				messageFuture.complete( message );
				queue.removeFirst();
				checkPendingWaiters( queue );
			}
		} );

		if( !messageFuture.isDone() ) {
			operations.entrySet().forEach(
				entry -> addMessageWaiter( entry.getValue(), messageFuture ) );
		}

		return messageFuture;
	}

	@Override
	public synchronized Future< SessionMessage > requestMessage( InputOperation operation, ExecutionThread ethread ) {
		final var messageFuture = new CompletableFuture< SessionMessage >();
		ethread.cancelIfKilled( messageFuture );
		if( messageFuture.isCancelled() ) {
			return messageFuture;
		}

		final var queue = getQueueForOperation( operation.id() );
		final SessionMessage message = queue.peekFirst();
		if( message != null && message.message().operationName().equals( operation.id() ) ) {
			messageFuture.complete( message );
			queue.removeFirst();
			checkPendingWaiters( queue );
		} else {
			addMessageWaiter( operation, messageFuture );
		}

		return messageFuture;
	}

	private void checkPendingWaiters( Deque< SessionMessage > queue ) {
		boolean keepRun = true;
		while( keepRun && !queue.isEmpty() ) {
			final var message = queue.peekFirst();
			final var future = getMessageWaiter( message.message().operationName() );
			if( future != null ) { // We found a waiter for the unlocked message
				completeWaiter( future, message );
				queue.removeFirst();
			} else {
				keepRun = false;
			}
		}
	}

	private void addMessageWaiter( InputOperation operation, CompletableFuture< SessionMessage > future ) {
		var waitersList = messageWaiters.computeIfAbsent( operation.id(), opName -> new HashSet<>() );
		waitersList.add( future );
	}

	private CompletableFuture< SessionMessage > getMessageWaiter( String operationName ) {
		var waitersList = messageWaiters.get( operationName );
		if( waitersList == null || waitersList.isEmpty() ) {
			return null;
		}

		if( waitersList.size() == 1 ) {
			messageWaiters.remove( operationName );
		}

		final var it = waitersList.iterator();
		final var f = it.next();
		it.remove();
		return f;
	}

	private Deque< SessionMessage > getQueueForOperation( String operationName ) {
		final var cset = interpreter().getCorrelationSetForOperation( operationName );
		return cset == null ? uncorrelatedMessageQueue : messageQueues.get( cset );
	}

	private void completeWaiter( CompletableFuture< SessionMessage > waiter, SessionMessage message ) {
		waiter.complete( message );
		messageWaiters.values().forEach( waiterSet -> waiterSet.remove( waiter ) );
	}

	public synchronized void pushMessage( SessionMessage message ) {
		final var queue = getQueueForOperation( message.message().operationName() );
		final var future = getMessageWaiter( message.message().operationName() );
		if( future != null && queue.isEmpty() ) {
			completeWaiter( future, message );
		} else {
			queue.addLast( message );
		}
	}

	@Override
	public void runProcess() {
		try {
			try {
				try {
					process().run();
				} catch( ExitingException e ) {
				}
				listeners.forEach( listener -> listener.onSessionExecuted( this ) );
			} catch( FaultException.RuntimeFaultException rf ) {
				throw rf.faultException();
			}
		} catch( FaultException f ) {
			Process p = null;
			while( hasScope() && (p = getFaultHandler( f.faultName(), true )) == null ) {
				popScope();
			}

			try {
				try {
					if( p == null ) {
						// Interpreter.getInstance().logUnhandledFault( f );
						throw f;
					} else {
						Value scopeValue =
							new VariablePathBuilder( false ).add( currentScopeId(), 0 ).toVariablePath().getValue();
						scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
						try {
							p.run();
						} catch( ExitingException e ) {
						}
					}
				} catch( FaultException.RuntimeFaultException rf ) {
					throw rf.faultException();
				}
			} catch( FaultException fault ) {
				Interpreter.getInstance().logUnhandledFault( fault );

				listeners.forEach( listener -> listener.onSessionError( this, fault ) );
			}
			listeners.forEach( listener -> listener.onSessionExecuted( this ) );
		}
	}

	@Override
	public String getSessionId() {
		return Long.toString( id );
	}
}
