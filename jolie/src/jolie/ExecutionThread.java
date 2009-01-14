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

import jolie.lang.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jolie.net.CommChannelHandler;
import jolie.net.CommMessage;
import jolie.process.Process;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * Represents a JolieThread that is able to resolve a VariablePath, referring to a State.
 * @see JolieThread
 * @see VariablePath
 * @see jolie.State
 * @author Fabrizio Montesi
 */
abstract public class ExecutionThread extends JolieThread
{
	protected class Scope extends AbstractIdentifiableObject implements Cloneable {
		final private Map< String, Process > faultMap = new HashMap< String, Process >();
		final private Map< String, Process > compMap = new HashMap< String, Process >();
		
		@Override
		public Scope clone()
		{
			Scope ret = new Scope( id );
			ret.compMap.putAll( compMap );
			ret.faultMap.putAll( faultMap );
			return ret;
		}
	
		public Scope( String id )
		{
			super( id );
		}
		
		public void installCompensation( Process process )
		{
			compMap.put( id, process );
		}
		
		public void installFaultHandler( String f, Process process )
		{
			faultMap.put( f, process );
		}
		
		public Process getFaultHandler( String name, boolean erase )
		{
			Process p = faultMap.get( name );
			if ( erase ) { // Not called by cH (TODO: this is obscure!)
				if ( p == null ) {
					// Give the default handler
					name = Constants.Keywords.DEFAULT_HANDLER_NAME;
					p = faultMap.get( name );
				}
				if ( p != null ) {
					// Could still be null if there was not a default handler
					faultMap.remove( name );
				}
			}
				
			return p;
		}
		
		public Process getSelfCompensation()
		{
			return compMap.get( id );
		}
		
		public Process getCompensation( String name )
		{
			Process p = compMap.get( name );
			if ( p != null )
				compMap.remove( name );
			return p;
		}
		
		public void mergeCompensations( Scope s )
		{
			compMap.putAll( s.compMap );
		}
	}

	final protected Process process;
	final protected Stack< Scope > scopeStack = new Stack< Scope >();
	final protected ExecutionThread parent;
	private boolean canBeInterrupted = false;
	private FaultException killerFault = null;
	
	/**
	 * Sets if this thread can be interrupted by a fault signal or not.
	 */
	public void setCanBeInterrupted( boolean b )
	{
		canBeInterrupted = b;
	}
	
	/**
	 * Constructor
	 * @param process the Process to be executed by this thread
	 * @param parent the parent of this thread
	 */
	public ExecutionThread( Process process, ExecutionThread parent )
	{
		super( parent.interpreter() );
		this.process = process;
		this.parent = parent;
	}
	
	/**
	 * Constructor
	 * @param interpreter the Interpreter this thread should refer to
	 * @param process the Process to be executed by this thread
	 */
	public ExecutionThread( Interpreter interpreter, Process process )
	{
		super( interpreter );
		this.process = process;
		this.parent = null;
	}

	/**
	 * Kills this ExecutionThread, interrupting its activity as soon as possible.
	 * @param fault the fault causing the interruption.
	 */
	public void kill( FaultException fault )
	{
		killerFault = fault;
		if( canBeInterrupted ) {
			interrupt();
		}
	}
	
	/**
	 * Returns the fault which killed this thread, if any. null otherwise.
	 * @return the fault which killed this thread, if any. null otherwise.
	 */
	public FaultException killerFault()
	{
		return killerFault;
	}

	/**
	 * Resets the killed state of this thread, returning it to normal execution.
	 */
	public void clearKill()
	{
		killerFault = null;
	}

	/**
	 * Returns true if this thread is killed, false otherwise.
	 * @return true if this thread is killed, false otherwise.
	 */
	public boolean isKilled()
	{
		return (killerFault != null);
	}
	
	@Override
	abstract public void run();
	
	/**
	 * Returns the compensator of the current executing scope.
	 * @return the compensator of the current executing scope.
	 */
	public synchronized Process getCurrentScopeCompensation()
	{
		if( scopeStack.empty() && parent != null ) {
			return parent.getCurrentScopeCompensation();
		}
		
		return scopeStack.peek().getSelfCompensation();
	}
	
	/**
	 * Returns the compensator for scope name id.
	 * @param id the scope name owning the compensator to retrieve
	 * @return the compensator for scope name id.
	 */
	public synchronized Process getCompensation( String id )
	{
		if ( scopeStack.empty() && parent != null ) {
			return parent.getCompensation( id );
		}
		
		return scopeStack.peek().getCompensation( id );
	}
	
	/**
	 * Returns true if this thread is executing inside a scope.
	 * Use this method to check if calling a variant of popScope is safe.
	 * @see #popScope()
	 * @see #popScope(boolean)
	 * @return true if this thread is executing inside a scope.
	 */
	public synchronized boolean hasScope()
	{
		return !scopeStack.empty();
	}
	
	/**
	 * Returns the id of the current executing scope.
	 * @return the id of the current executing scope.
	 */
	public synchronized String currentScopeId()
	{
		if( scopeStack.empty() && parent != null ) {
			return parent.currentScopeId();
		}
		
		return scopeStack.peek().id();
	}

	/**
	 * Returns the current fault handler for fault id.
	 * @param id the id of the fault handler to retrieve.
	 * @param erase <code>true</code> if the fault handler should be
	 *		removed before returning it.
	 * @return the current fault handler for fault id.
	 */
	public synchronized Process getFaultHandler( String id, boolean erase )
	{
		if ( scopeStack.empty() && parent != null ) {
			return parent.getFaultHandler( id, erase );
		}
		
		return scopeStack.peek().getFaultHandler( id, erase );
	}
	
	/**
	 * Pushes scope id as the new current executing scope in the scope stack of this thread.
	 * @param id the id of the scope to push.
	 */
	public synchronized void pushScope( String id )
	{
		scopeStack.push( new Scope( id ) );
	}
	
	/**
	 * Pops the current executing scope from the scope stack of this thread.
	 * @param merge <code>true</code> if the popped scope compensators
	 *		should be propagated upstream to the parent scope.
	 */
	public synchronized void popScope( boolean merge )
	{
		Scope s = scopeStack.pop();
		if ( merge ) {
			mergeCompensations( s );
		}
	}
	
	/**
	 * Pops the current executing scope from the scope stack of this thread.
	 * This method is a shortcut for <code>popScope(true)</code>.
	 */
	public synchronized void popScope()
	{
		popScope( true );
	}
	
	private synchronized void mergeCompensations( Scope s )
	{
		if ( scopeStack.empty() ) {
			if ( parent != null )
				parent.mergeCompensations( s );
		} else
			scopeStack.peek().mergeCompensations( s );
	}
	
	/**
	 * Installs process as the compensator for the current scope.
	 * @param process the process to install as compensator for the current scope
	 */
	public synchronized void installCompensation( Process process )
	{
		if ( scopeStack.empty() && parent != null )
			parent.installCompensation( process );
		else
			scopeStack.peek().installCompensation( process );
	}
	
	/**
	 * Installs process as the fault handler for fault id.
	 * @param id the fault to be handled by process
	 * @param process the Process to be called for handling fault id
	 */
	public synchronized void installFaultHandler( String id, Process process )
	{
		if ( scopeStack.empty() && parent != null ) {
			parent.installFaultHandler( id, process );
		} else {
			scopeStack.peek().installFaultHandler( id, process );
		}
	}
	
	/**
	 * Returns the ExecutionThread the current thread should refer to.
	 * This method can be useful, e.g., for resolving VariablePaths outside the
	 * execution of an ExecutionThread.
	 * @return the ExecutionThread the current thread should refer to.
	 */
	public static ExecutionThread currentThread()
	{
		Thread currThread = Thread.currentThread();
		if ( currThread instanceof ExecutionThread ) {
			return ((ExecutionThread) currThread);
		} else if ( currThread instanceof CommChannelHandler ) {
			return ((CommChannelHandler)currThread).executionThread();
		}

		return null;
	}

	/**
	 * Returns the State this ExecutionThread refers to.
	 * @return the State this ExecutionThread refers to
	 * @see jolie.State
	 */
	abstract public jolie.State state();
	
	/**
	 * Checks if message correlates (i.e. can be received) by this ExecutionThread.
	 * @param recvPath the VariablePath this message would be received in
	 * @param message the message to correlate
	 * @return true if the message correlates correctly to this thread, false otherwise
	 */
	public synchronized boolean checkCorrelation( VariablePath recvPath, CommMessage message )
	{
		if ( recvPath == null ) {
			return true;
		}

		VariablePath path;
		Value correlationValue;
		Value mValue = null;
		Value cValue;
		for( List< VariablePath > list : Interpreter.getInstance().correlationSet() ) {
			cValue = null;
			correlationValue = list.get( 0 ).getValue();
			for( VariablePath p : list ) {
				if ( (path=recvPath.containedSubPath( p )) != null ) {
					mValue = path.getValue( message.value() );
					if (
						correlationValue.isDefined() &&
						!mValue.equals( correlationValue )
					) {
						return false;
					} else if ( !correlationValue.isDefined() ) {
						// Every correlated value must be equal
						if ( cValue != null && !cValue.equals( mValue ) )
							return false;
						else if ( cValue == null )
							cValue = mValue;
					}
				}
			}
			
			// TODO this must become a List!
			// CSet values should be set only if the whole message is valid.
			if ( cValue != null )
				correlationValue.assignValue( cValue );
		}
		
		return true;
	}
	
	protected Process process()
	{
		return process;
	}
}
