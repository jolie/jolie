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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jolie.net.CommChannelHandler;
import jolie.net.CommMessage;
import jolie.process.CorrelatedProcess;
import jolie.process.Process;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

abstract public class ExecutionThread extends JolieThread
{
	protected class Scope extends AbstractIdentifiableObject implements Cloneable {
		private Map< String, Process > faultMap = new HashMap< String, Process >();
		private Map< String, Process > compMap = new HashMap< String, Process >();
		
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
			if ( p != null && erase )
				faultMap.remove( name );
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

	protected Process process;
	protected Stack< Scope > scopeStack = new Stack< Scope >();
	protected ExecutionThread parent;
	private boolean canBeInterrupted = false;
	private FaultException killerFault = null;
	
	protected CorrelatedProcess notifyProc = null;
	
	public void setCanBeInterrupted( boolean b )
	{
		canBeInterrupted = b;
	}
	
	public ExecutionThread( Process process, ExecutionThread parent, CorrelatedProcess notifyProc )
	{
		super( parent.interpreter() );
		this.process = process;
		this.parent = parent;
		this.notifyProc = notifyProc;
	}
	
	public ExecutionThread( Interpreter interpreter, Process process )
	{
		super( interpreter );
		this.process = process;
		this.parent = null;
		this.notifyProc = null;
	}

	public void kill( FaultException fault )
	{
		killerFault = fault;
		if( canBeInterrupted )
			interrupt();
	}
	
	public FaultException killerFault()
	{
		return killerFault;
	}

	public void clearKill()
	{
		killerFault = null;
	}

	public boolean isKilled()
	{
		return (killerFault != null);
	}
	
	@Override
	public void run()
	{
		try {
			process.run();
			if ( notifyProc != null )
				notifyProc.sessionTerminated();
		} catch( FaultException f ) {
			if ( notifyProc != null )
				notifyProc.signalFault( f );
			else
				Interpreter.getInstance().logUnhandledFault( f );
		}
	}
	
	public synchronized Process getCurrentScopeCompensation()
	{
		if( scopeStack.empty() && parent != null )
			return parent.getCurrentScopeCompensation();
		
		return scopeStack.peek().getSelfCompensation();
	}
	
	public synchronized Process getCompensation( String id )
	{
		if ( scopeStack.empty() && parent != null )
			return parent.getCompensation( id );
		
		return scopeStack.peek().getCompensation( id );
	}
	
	public synchronized boolean hasScope()
	{
		return !scopeStack.empty();
	}
	
	public synchronized String currentScopeId()
	{
		if( scopeStack.empty() && parent != null )
			return parent.currentScopeId();
		
		return scopeStack.peek().id();
	}

	public synchronized Process getFaultHandler( String id, boolean erase )
	{
		if ( scopeStack.empty() && parent != null )
			return parent.getFaultHandler( id, erase );
		
		return scopeStack.peek().getFaultHandler( id, erase );
	}
	
	public synchronized void pushScope( String id )
	{
		scopeStack.push( new Scope( id ) );
	}
	
	public synchronized void popScope( boolean merge )
	{
		Scope s = scopeStack.pop();
		if ( merge )
			mergeCompensations( s );
	}
	
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
	
	public synchronized void installCompensation( Process process )
	{
		if ( scopeStack.empty() && parent != null )
			parent.installCompensation( process );
		else
			scopeStack.peek().installCompensation( process );
	}
	
	public synchronized void installFaultHandler( String id, Process process )
	{
		if ( scopeStack.empty() && parent != null )
			parent.installFaultHandler( id, process );
		else
			scopeStack.peek().installFaultHandler( id, process );
	}
	
	public static ExecutionThread currentThread()
	{
		Thread currThread = Thread.currentThread();
		if ( currThread instanceof ExecutionThread )
			return ((ExecutionThread) currThread);
		else if ( currThread instanceof CommChannelHandler )
			return ((CommChannelHandler)currThread).executionThread();

		return null;
	}
	
	abstract public jolie.State state();
	abstract protected void setState( jolie.State state );
	
	public synchronized boolean checkCorrelation( VariablePath recvPath, CommMessage message )
	{
		if ( recvPath == null )
			return true;

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
