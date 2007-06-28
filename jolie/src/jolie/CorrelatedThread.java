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
import java.util.Stack;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.process.CorrelatedProcess;
import jolie.process.Process;
import jolie.process.SleepProcess;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;

class Scope {
	private HashMap< String, Process > faultMap = new HashMap< String, Process >();
	private HashMap< String, Process > compMap = new HashMap< String, Process >();
	private String id;

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
	
	public Process getFaultHandler( String name )
	{
		Process p = faultMap.get( name );
		if ( p != null )
			faultMap.remove( name );
		return p;
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

abstract public class CorrelatedThread extends Thread
{
	private Process process;
	private CorrelatedProcess notifyProc = null;
	protected Stack< Scope > scopeStack = new Stack< Scope >();
	private FaultException pendingFault = null;
	private Process pendingProcess = null;
	private CorrelatedThread parent;
	private boolean killed = false;
	
	private static CorrelatedThread current = null;

	public synchronized static void setCurrent( CorrelatedThread cthread )
	{
		current = cthread;
	}
	
	public synchronized void setPendingNDProcess( Process p )
	{
		pendingProcess = p;
	}
	
	public synchronized Process pendingNDProcess()
	{
		return pendingProcess;
	}
	
	public void kill()
	{
		killed = true;
		interrupt();
	}
	
	public static void clearKill()
	{
		currentThread().killed = false;
	}
	
	public static void setKill()
	{
		currentThread().killed = true;
	}
	
	public static boolean killed()
	{
		return currentThread().killed;
	}
	
	public CorrelatedThread( Process process, CorrelatedThread parent )
	{
		this.process = process;
		this.parent = parent;
	}
	
	public CorrelatedThread( Process process, CorrelatedThread parent, CorrelatedProcess notifyProc )
	{
		this.process = process;
		this.parent = parent;
		this.notifyProc = notifyProc;
	}
	
	public void setPendingFault( FaultException f )
	{
		pendingFault = f;
	}
	
	public void throwPendingFault()
		throws FaultException
	{
		if ( pendingFault != null ) {
			FaultException f = pendingFault;
			pendingFault = null;
			throw f;
		}
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
	
	public synchronized Process getCompensation( String id )
	{
		if ( scopeStack.empty() )
			return parent.getCompensation( id );
		
		return scopeStack.peek().getCompensation( id );
	}
	
	public synchronized Process getFaultHandler( String id )
	{
		if ( scopeStack.empty() )
			return parent.getFaultHandler( id );
		
		return scopeStack.peek().getFaultHandler( id );
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
	
	public static CorrelatedThread currentThread()
	{
		Thread currThread = Thread.currentThread();
		if ( currThread instanceof CorrelatedThread )
			return ((CorrelatedThread) currThread);
		else if ( currThread instanceof SleepProcess.SleepInputHandler )
			return ((SleepProcess.SleepInputHandler)currThread).correlatedThread();
		
		CorrelatedThread t = CommCore.currentCommChannel().correlatedThread();
		if ( t != null )
			return t;

		return current;
	}
	
	public boolean checkCorrelation( List< GlobalVariable > vars, CommMessage message )
	{
		return state().checkCorrelation( vars, message );
	}
	
	abstract public jolie.State state();
	
	protected Process process()
	{
		return process;
	}
}
