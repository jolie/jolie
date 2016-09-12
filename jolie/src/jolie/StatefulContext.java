/**
 * *************************************************************************
 *   Copyright (C) 2006-2008 by Fabrizio Montesi <famontesi@gmail.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 **************************************************************************
 */
package jolie;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import jolie.behaviours.Behaviour;
import jolie.behaviours.TransformationReason;
import jolie.lang.Constants;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.correlation.CorrelationSet;
import jolie.util.Pair;

/**
 *
 * @author martin
 */
 public class StatefulContext extends ExecutionContext
{

	private static final AtomicLong idCounter = new AtomicLong( 1L );
	
	private final long id = idCounter.getAndIncrement();
	private final jolie.State state;
	private final List< SessionListener > listeners = new ArrayList<>();
	protected final Map< CorrelationSet, Deque< SessionMessage > > messageQueues = new HashMap<>();
	protected final Deque< SessionMessage > uncorrelatedMessageQueue = new ArrayDeque<>();
	private final Map< String, Deque< ExecutionContext > > messageWaiters =	new HashMap<>();
	private boolean executionCompleted = false;

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
		
	private StatefulContext( Behaviour process, ExecutionContext parent, jolie.State state )
	{
		super( process, parent );
		this.state = state;
		initMessageQueues();
	}

	public StatefulContext( Behaviour process, jolie.State state, ExecutionContext parent )
	{
		super( parent.interpreter(), process );
		this.state = state;		
		parent.scopeStack.forEach( s -> scopeStack.push( s.clone() ) );
		initMessageQueues();
	}
	
	@Override
	public final void run()
	{
		Thread t = Thread.currentThread();
		if ( t instanceof JolieExecutorThread ) {
			((JolieExecutorThread)t).sessionContext( this );
		}
		
		System.out.println( String.format( "[%s][%s] - ", Thread.currentThread(), this ) +  "SessionContext Loop started: " + this.toString() );
		while( !processStack.isEmpty() && !pauseExecution ) {
			try {
				try {
					Behaviour p = processStack.pop();
					String pad = "";
					for( int i = 0; i < processStack.size(); i++ ) {
						pad += "  ";
					}
					System.out.println( String.format( "[%s][%s] - ", Thread.currentThread(), this ) +  " - " + pad + p );
					p.run( this );
				} catch( ExitingException e ) {
				}
			} catch( FaultException f ) {
				Behaviour p = null;
				while( hasScope() && (p = getFaultHandler( f.faultName(), true )) == null ) {
					popScope();
				}

				try {
					if ( p == null ) {
						interpreter().logUnhandledFault( f );
						throw f;
					} else {
						Value scopeValue
							= new VariablePathBuilder( false ).add( currentScopeId(), 0 ).toVariablePath().getValue( this );
						scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
						try {
							p.run( this );
						} catch( ExitingException e ) {
						}
					}
				} catch( FaultException fault ) {
					listeners.forEach( listener -> listener.onSessionError( this, fault ) );
					markExecutionFinished();
				}

				listeners.forEach( listener -> listener.onSessionExecuted( this ) );
				markExecutionFinished();
				return;
			}
		}
		
		if (processStack.isEmpty() && !pauseExecution) {
			listeners.forEach( listener -> listener.onSessionExecuted( this ) );
			markExecutionFinished();
		}
		
		pauseExecution = false;		
		System.out.println( String.format( "[%s][%s] - ", Thread.currentThread(), this ) +  "SessionContext Loop stopped: " + this.toString() );
	}
	
	private void markExecutionFinished() {
		System.out.println( String.format( "[%s][%s] - ", Thread.currentThread(), this ) +  "SessionContext Marked as finished: " + this.toString() );
		synchronized (completionLock) {
			assert(!executionCompleted);
			executionCompleted = true;
			completionLock.notifyAll();
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
	 * Constructs a SessionContext with a fresh State.
	 * @param interpreter the Interpreter this thread must refer to
	 * @param process the Process this thread has to execute
	 */
	public StatefulContext( Interpreter interpreter, Behaviour process )
	{
		super( interpreter, process );
		state = new jolie.State();
		initMessageQueues();
	}
	
	private void initMessageQueues()
	{
		interpreter().correlationSets().forEach(
			cset -> messageQueues.put( cset, new ArrayDeque<>() )
		);
	}
	
	/**
	 * Constructs a SessionContext cloning another ExecutionThread, copying the 
	 * State and Scope stack of the parent.
	 * 
	 * @param process the Process this thread has to execute
	 * @param parent the ExecutionThread to copy
	 */
	public StatefulContext( Behaviour process, ExecutionContext parent )
	{
		super( process, parent );
		initMessageQueues();
		assert( parent != null );
		state = parent.state().clone();
		parent.scopeStack.forEach( s -> scopeStack.push( s.clone() ) );
	}
	
	/**
	 * Returns the State of this thread.
	 * @return the State of this thread
	 * @see State
	 */
	@Override
	public jolie.State state()
	{
		return state;
	}

	@Override
	public SessionMessage requestMessage( Map< String, InputOperation > operations, ExecutionContext ctx )
	{
		SessionMessage message = null;
		synchronized( messageQueues ) {
			Deque< SessionMessage > queue = null;
			InputOperation operation = null;

			Iterator< Deque< SessionMessage > > it = messageQueues.values().iterator();
			while( operation == null && it.hasNext() ) {
				queue = it.next();
				message = queue.peekFirst();
				if ( message != null ) {
					operation = operations.get( message.message().operationName() );
				}
			}
			if ( message == null ) {
				queue = uncorrelatedMessageQueue;
				message = queue.peekFirst();
				if ( message != null ) {
					operation = operations.get( message.message().operationName() );
				}
			}

			if ( message == null || operation == null ) {
				operations.entrySet().forEach(
					entry -> addMessageWaiter( entry.getValue(), ctx )
				);
			} else {
				queue.removeFirst();

				// Check if we unlocked other receives
				if( !queue.isEmpty() ) {
					/* 
						Only unlock/wake-up 1 waitingContext at the time, without 
						popping the message from the queue. So that the waiting 
						context can retrieve the message from the queue.
					*/
					final SessionMessage otherMessage = queue.peekFirst();
					final ExecutionContext waitingContext = getMessageWaiter( otherMessage.message().operationName() );
					if ( waitingContext != null ) {
						waitingContext.start();
					}
				}
			}
		}
		return message;
	}

	@Override
	public SessionMessage requestMessage( InputOperation operation, ExecutionContext ctx )
	{
		final CorrelationSet cset = interpreter().getCorrelationSetForOperation( operation.id() );
		final Deque< SessionMessage > queue =
				cset == null ? uncorrelatedMessageQueue
				: messageQueues.get( cset );
		SessionMessage message = null;
		synchronized( messageQueues ) {
			message = queue.peekFirst();
			if ( message == null
				|| message.message().operationName().equals( operation.id() ) == false
			) {
				addMessageWaiter( operation, ctx );
			} else {
				queue.removeFirst();
				// Check if we unlocked other receives
				if( !queue.isEmpty() ) {
					final SessionMessage otherMessage = queue.peekFirst();
					final ExecutionContext waitingCtx = getMessageWaiter( otherMessage.message().operationName() );
					if ( waitingCtx != null ) { // We found a waiter for the unlocked message
						waitingCtx.start();
					}
				}
			}
		}
		return message;
	}

	private void addMessageWaiter( InputOperation operation, ExecutionContext ctx )
	{
		Deque< ExecutionContext > waitersList = messageWaiters.get( operation.id() );
		if ( waitersList == null ) {
			waitersList = new ArrayDeque<>();
			messageWaiters.put( operation.id(), waitersList );
		}
		waitersList.addLast( ctx );
	}

	private ExecutionContext getMessageWaiter( String operationName )
	{
		Deque< ExecutionContext > waitersList = messageWaiters.get( operationName );
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
		synchronized( messageQueues ) {
			Deque< SessionMessage > queue;
			CorrelationSet cset = interpreter().getCorrelationSetForOperation( message.message().operationName() );
			if ( cset != null ) {
				queue = messageQueues.get( cset );
			} else {
				queue = uncorrelatedMessageQueue;
			}
			ExecutionContext waitingCtx = getMessageWaiter( message.message().operationName() );
			if ( waitingCtx != null && queue.isEmpty() ) {
				waitingCtx.start();
			}
			// We will add the message to the queue in any case so that it can be fetched from the queue by requestMessage.
			queue.addLast( message );
		}
	}

	@Override
	public String getSessionId()
	{
		return Long.toString( id );
	}
	
	private final Object completionLock = new Object(); 
	public void join() throws InterruptedException {
		synchronized (completionLock) {
			if (!executionCompleted)
				completionLock.wait();
		}
		System.out.println( this + " - JOINED with - " + Thread.currentThread() );
	}
	
	/**
	 * Creates and returns a default list of handlers, initialized
	 * with default fault handlers for built-in faults like, e.g., TypeMismatch.
	 * @param interpreter the <code>Interpreter</code> in which the returned map will be used
	 * @return a newly created default list of handlers
	 */
	public static List< Pair< String, Behaviour > > createDefaultFaultHandlers( final Interpreter interpreter )
	{
		final List< Pair< String, Behaviour > > instList = new ArrayList<>();
		instList.add(new Pair<>(
			Constants.TYPE_MISMATCH_FAULT_NAME,
			new Behaviour() {
				@Override
				public void run( StatefulContext ctx ) throws FaultException, ExitingException
				{
					interpreter.logInfo( typeMismatchPath.getValue().strValue() );
				}

				@Override
				public Behaviour clone( TransformationReason reason )
				{
					return this;
				}

				@Override
				public boolean isKillable()
				{
					return true;
				}
			}
		) );
		instList.add(new Pair<>(
			Constants.IO_EXCEPTION_FAULT_NAME,
			new Behaviour() {
				@Override
				public void run( StatefulContext ctx ) throws FaultException, ExitingException
				{
					interpreter.logInfo( ioExceptionPath.getValue().strValue() );
				}

				@Override
				public Behaviour clone( TransformationReason reason )
				{
					return this;
				}

				@Override
				public boolean isKillable()
				{
					return true;
				}
			}
		) );
		return instList;
	}

	/***
	 * Returns current Context.
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static final StatefulContext currentContext() {
		Thread t = Thread.currentThread();
		if (t instanceof JolieExecutorThread) {
			return ((JolieExecutorThread)t).sessionContext();
		}
		return null;
	}
}
