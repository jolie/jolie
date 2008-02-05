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
import java.util.Stack;
import java.util.Vector;

import jolie.net.CommChannelHandler;
import jolie.net.CommMessage;
import jolie.process.CorrelatedProcess;
import jolie.process.Process;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.Value;

abstract public class ExecutionThread extends Thread
{
	protected class Scope implements Cloneable {
		private HashMap< String, Process > faultMap = new HashMap< String, Process >();
		private HashMap< String, Process > compMap = new HashMap< String, Process >();
		private String id;
		
		public Scope clone()
		{
			Scope ret = new Scope( id );
			ret.compMap.putAll( compMap );
			ret.faultMap.putAll( faultMap );
			return ret;
		}
	
		public Scope( String id )
		{
			this.id = id;
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

	private Process process;
	protected Stack< Scope > scopeStack = new Stack< Scope >();
	private ExecutionThread parent;
	private boolean killed = false;
	
	private CorrelatedProcess notifyProc = null;
	
	public ExecutionThread( Process process, ExecutionThread parent, CorrelatedProcess notifyProc )
	{
		this.process = process;
		this.parent = parent;
		this.notifyProc = notifyProc;
	}

	public void kill()
	{
		killed = true;
		interrupt();
	}

	public void clearKill()
	{
		killed = false;
	}

	public boolean isKilled()
	{
		return killed;
	}
	
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
				Interpreter.logUnhandledFault( f );
		}
	}
	
	public synchronized Process getCurrentScopeCompensation()
	{
		if( scopeStack.empty() )
			return parent.getCurrentScopeCompensation();
		
		return scopeStack.peek().getSelfCompensation();
	}
	
	public synchronized Process getCompensation( String id )
	{
		if ( scopeStack.empty() )
			return parent.getCompensation( id );
		
		return scopeStack.peek().getCompensation( id );
	}
	
	public synchronized boolean hasScope()
	{
		return !scopeStack.empty();
	}

	public synchronized Process getFaultHandler( String id, boolean erase )
	{
		if ( scopeStack.empty() )
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
		if ( scopeStack.empty() )
			parent.installCompensation( process );
		else
			scopeStack.peek().installCompensation( process );
	}
	
	public synchronized void installFaultHandler( String id, Process process )
	{
		if ( scopeStack.empty() )
			parent.installFaultHandler( id, process );
		else
			scopeStack.peek().installFaultHandler( id, process );
	}
	
	public static ExecutionThread currentThread()
	{
		Thread currThread = Thread.currentThread();
		if ( currThread instanceof ExecutionThread )
			return ((ExecutionThread) currThread);
		else if ( currThread instanceof ProcessThread )
			return ((ProcessThread)currThread).executionThread();
		else if ( currThread instanceof CommChannelHandler )
			return ((CommChannelHandler)currThread).executionThread();

		return null;
	}
	
	abstract public jolie.State state();
	abstract protected void setState( jolie.State state );
	
	public synchronized boolean checkCorrelation( GlobalVariablePath path, CommMessage message )
	{
		Vector< Value > origCSetValues = new Vector< Value >();
		for( GlobalVariablePath p : Interpreter.correlationSet() )
			origCSetValues.add( Value.create( p.getValue() ) );

		jolie.State origState = state();
		setState( origState.clone() );
		
		if ( path != null )
			path.getValue().deepCopy( message.value() );

		Vector< Value > newCSetValues = new Vector< Value >();
		for( GlobalVariablePath p : Interpreter.correlationSet() )
			newCSetValues.add( p.getValue() );
		
		Value origV, newV;
		for( int i = 0; i < origCSetValues.size(); i++ ) {
			origV = origCSetValues.elementAt( i );
			newV = newCSetValues.elementAt( i );
			if ( /*origV.isDefined() && (
					 != newV.type() ||
					(origV.isInt() && origV.intValue() != newV.intValue()) ||
					(origV.isDouble() && origV.doubleValue() != newV.doubleValue()) ||
					(origV.isString() && !origV.strValue().equals( newV.strValue() ))
					)*/
				!origV.equals( newV )
					)
			{
				setState( origState );
				return false;
			}
		}
		
		setState( origState );
		return true;
	}
	
	protected Process process()
	{
		return process;
	}
}
