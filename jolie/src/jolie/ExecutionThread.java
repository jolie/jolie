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

import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.process.CorrelatedProcess;
import jolie.process.NullProcess;
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

/**
 * @todo A correlated thread should offer the possibility to store objects into local memory.
 * @todo Think about exploiting polymorphism in a double automatic cast for accessing local thread memory.
 *
 */
abstract public class ExecutionThread extends Thread
{
	private Process process;
	protected Stack< Scope > scopeStack = new Stack< Scope >();
	private ExecutionThread parent;
	private boolean killed = false;
	
	private HashMap< Object, Object > localMap = new HashMap< Object, Object > ();
	
	private CorrelatedProcess notifyProc = null;
	
	private static ExecutionThread current = null;
	
	public ExecutionThread( Process process, ExecutionThread parent )
	{
		this.process = process;
		this.parent = parent;
	}
	
	public ExecutionThread( Process process, ExecutionThread parent, CorrelatedProcess notifyProc )
	{
		this.process = process;
		this.parent = parent;
		this.notifyProc = notifyProc;
	}

	public synchronized static void setCurrent( ExecutionThread thread )
	{
		current = thread;
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
	
	private synchronized Process _getCompensation( String id )
	{
		if ( scopeStack.empty() )
			return parent._getCompensation( id );
		
		return scopeStack.peek().getCompensation( id );
	}
	
	public static Process getCompensation( String id )
	{
		Process p = ExecutionThread.currentThread()._getCompensation( id );
		if ( p == null )
			return NullProcess.getInstance();
		return p;
	}
	
	/*private synchronized void _setFields( Object ref, Object fields )
	{
		fieldsMap.put( ref, fields );
	}
	
	public static void setFields( Object ref, Object fields )
	{
		currentThread()._setFields( ref, fields );
	}*/
	
	@SuppressWarnings("unchecked")
	private synchronized <T, V> T _getLocalObject( Object ref, Class<V> clazz )
	{
		Object obj = localMap.get( ref );
		if ( obj == null ) {
			try {
				obj = clazz.newInstance();
			} catch( InstantiationException e ) {
				Interpreter.logger().severe( "Runtime internal error in local thread casting" );
				e.printStackTrace();
			} catch( IllegalAccessException e ) {
				Interpreter.logger().severe( "Runtime internal error in local thread casting" );
				e.printStackTrace();
			}
			localMap.put( ref, obj );
		}

		return (T)obj;
	}
	
	public static <T,V> T getLocalObject( Object ref, Class<V> clazz )
	{
		return currentThread()._getLocalObject( ref, clazz );
	}
	
	private synchronized Process _getFaultHandler( String id )
	{
		if ( scopeStack.empty() )
			return parent._getFaultHandler( id );
		
		return scopeStack.peek().getFaultHandler( id );
	}
	
	public static Process getFaultHandler( String id )
	{
		return ExecutionThread.currentThread()._getFaultHandler( id );
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
		else if ( currThread instanceof SleepProcess.SleepInputHandler )
			return ((SleepProcess.SleepInputHandler)currThread).executionThread();
		
		CommChannel channel = CommCore.currentCommChannel();
		if ( channel != null ) {
			ExecutionThread t = CommCore.currentCommChannel().executionThread();
			if ( t != null )
				return t;
		}

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
