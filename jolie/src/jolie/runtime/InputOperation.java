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


package jolie.runtime;

import java.util.HashMap;
import java.util.LinkedList;

import jolie.ExecutionThread;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.process.InputOperationProcess;
import jolie.process.InputProcess;
import jolie.process.NDChoiceProcess;
import jolie.util.Pair;

/**
 * @author Fabrizio Montesi
 * 
 * @todo switch every input process to async signForMessage
 * @todo switch to a custom locking system
 * @todo re-implement async receiving of notifications
 *
 */
abstract public class InputOperation extends Operation implements InputHandler
{
	private static HashMap< String, InputOperation > idMap = 
		new HashMap< String, InputOperation >();
	
	private LinkedList< Pair< ExecutionThread, InputProcess > > procsList =
						new LinkedList< Pair< ExecutionThread, InputProcess > >();
	//private LinkedList< CommMessage > mesgList;
	
	public InputOperation( String id )
	{
		super( id );
		//mesgList = new LinkedList< CommMessage >();
	}
	
	public static InputOperation getById( String id )
		throws InvalidIdException
	{
		InputOperation retVal = idMap.get( id );
		if ( retVal == null )
			throw new InvalidIdException( id );

		return retVal;
	}
	
	public void register()
	{
		idMap.put( id(), this );
	}
	
	/**
	 * Receives a message from CommCore and passes it to the right InputOperation.
	 * If no suitable InputOperation is found, the message is enqueued in memory.
	 * @param message
	 */
	public void recvMessage( CommMessage message )
	{
		boolean received = false;
		Pair< ExecutionThread, InputProcess > pair = getCorrelatedPair( message );

		while( !received ) {
			while( pair == null ) {
				synchronized( this ) {
					try {
						this.wait();
					} catch( InterruptedException e ) {}
				}
				pair = getCorrelatedPair( message );
			}
			CommCore.currentCommChannel().setExecutionThread( pair.key() );
			received = pair.value().recvMessage( message );
			if ( received ) {
				synchronized( pair.key() ) {
					// @todo Check this
					pair.key().notifyAll();
				}
			} else {
				pair = getCorrelatedPair( message );
			}
		}
		//}
	}
	
	/**
	 * @todo this does not work anymore!
	 */
	private synchronized Pair< ExecutionThread, InputProcess > getCorrelatedPair( CommMessage message )
	{
		GlobalVariablePath path = null;
		for( Pair< ExecutionThread, InputProcess > pair : procsList ) {
			if ( pair.value() instanceof InputOperationProcess )
				path = ((InputOperationProcess) pair.value()).inputVarPath();
			else if ( pair.value() instanceof NDChoiceProcess )
				path = ((NDChoiceProcess) pair.value()).inputVarPath( this.id() );
		
			CommCore.currentCommChannel().setExecutionThread( pair.key() );
			if ( pair.key().checkCorrelation( path, message ) ) {
				procsList.remove( pair );
				return pair;
			}	
		}
		
		return null;
	}
	
	/*private CommMessage getCorrelatedMessage( InputProcess process )
	{
		CorrelatedThread correlatedThread = CorrelatedThread.currentThread();
		List< GlobalVariable > vars = null;
		if ( process instanceof InputOperationProcess )
			vars = ((InputOperationProcess) process).inputVars();
		else if ( process instanceof NDChoiceProcess )
			vars = ((NDChoiceProcess) process).inputVars( this.id() );
		
		for( CommMessage mesg : mesgList ) {
			if ( correlatedThread.checkCorrelation( vars, mesg ) ) {
				mesgList.remove( mesg );
				return mesg;
			}
		}
		
		return null;
	}*/
	
	public void getMessage( InputProcess process )
	{
		Thread currThread = Thread.currentThread();
		Pair< ExecutionThread, InputProcess > pair;
		synchronized( this ) {
			pair =
				new Pair< ExecutionThread, InputProcess >(
								ExecutionThread.currentThread(),
								process
								);

			procsList.addFirst( pair );
			this.notifyAll();
		}

		synchronized( currThread ) {
			try {
				boolean wait = false;
				synchronized( this ) {
					wait = procsList.contains( pair );
				}
				if ( wait )
					currThread.wait();
			} catch( InterruptedException e ) {}
		}
	}
			
	public synchronized void signForMessage( NDChoiceProcess process )
	{
		procsList.addFirst(
						new Pair< ExecutionThread, InputProcess >(
								ExecutionThread.currentThread(),
								process
								)
						);
		this.notifyAll();
	}
	
	public synchronized void cancelWaiting( NDChoiceProcess process ) 
	{
		ExecutionThread t = ExecutionThread.currentThread();
		for ( Pair< ExecutionThread, InputProcess > pair : procsList ) {
			if ( pair.key() == t && pair.value() == process ) {
				procsList.remove( pair );
				break;
			}
		}
	}
}