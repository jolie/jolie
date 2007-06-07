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

import java.util.LinkedList;
import java.util.Vector;

import jolie.Constants;
import jolie.CorrelatedThread;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.process.InputOperationProcess;
import jolie.process.InputProcess;
import jolie.process.NDChoiceProcess;
import jolie.util.Triple;

/**
 * @author Fabrizio Montesi
 * 
 *
 */
abstract public class InputOperation extends Operation implements InputHandler
{
	private LinkedList< Triple< CorrelatedThread, InputProcess, Thread > > procsList;
	//private LinkedList< CommMessage > mesgList;
	private Vector< Constants.VariableType > inVarTypes;
	
	public InputOperation( String id, Vector< Constants.VariableType > inVarTypes )
	{
		super( id );
		procsList = new LinkedList< Triple< CorrelatedThread, InputProcess, Thread > >();
		//mesgList = new LinkedList< CommMessage >();
		this.inVarTypes = inVarTypes;
	}
	
	public Vector< Constants.VariableType > inVarTypes()
	{
		return inVarTypes;
	}
	
	public static InputOperation getById( String id )
		throws InvalidIdException
	{
		Operation obj = Operation.getById( id );
		if ( !( obj instanceof InputOperation ) )
			throw new InvalidIdException( id );
		return (InputOperation)obj;
	}
	
	/**
	 * Receives a message from CommCore and passes it to the right InputOperation.
	 * If no suitable InputOperation is found, the message is enqueued in memory.
	 * @todo Deprecate the use of boolean received?
	 * @todo Check for other possible instances of tasks with an already allocated cset
	 * @param message
	 */
	public void recvMessage( CommMessage message )
	{
			//CommChannel channel = CommCore.currentCommChannel();
			//synchronized( channel ) {
			Triple< CorrelatedThread, InputProcess, Thread > triple;
			boolean received = false;
			triple = getCorrelatedTriple( message );
			//if ( pair == null )
				//mesgList.addFirst( message );
			
			while( !received ) {
				while( triple == null ) {
					synchronized( this ) {
						try {
							this.wait();
						} catch( InterruptedException e ) {}
					}
					triple = getCorrelatedTriple( message );
				}
				CommCore.currentCommChannel().setCorrelatedThread( triple.first() );
				received = triple.second().recvMessage( message );
				if ( received ) {
					//mesgList.remove( message );
					synchronized( triple.third() ) {
						triple.third().notify();
					}
				} else {
					triple = getCorrelatedTriple( message );
				}
			}
		//}
	}
	
	private synchronized Triple< CorrelatedThread, InputProcess, Thread > getCorrelatedTriple( CommMessage message )
	{
		for( Triple< CorrelatedThread, InputProcess, Thread > triple : procsList ) {
			if ( triple.second() instanceof InputOperationProcess ) {
				InputOperationProcess process = (InputOperationProcess) triple.second();
				if ( triple.first().checkCorrelation( process.inputVars(), message ) ) {
					procsList.remove( triple );
					return triple;
				}
			} else if ( triple.second() instanceof NDChoiceProcess ) {
				NDChoiceProcess process = (NDChoiceProcess) triple.second();
				if ( triple.first().checkCorrelation( process.inputVars( this.id() ), message ) ) {
					procsList.remove( triple );
					return triple;
				}
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
	
	public void getMessage( InputProcess process, boolean wait )
	{
		Triple< CorrelatedThread, InputProcess, Thread > triple;
		synchronized( this ) {
			triple =
				new Triple< CorrelatedThread, InputProcess, Thread >(
								CorrelatedThread.currentThread(),
								process,
								Thread.currentThread()
								);

			procsList.addFirst( triple );
			this.notifyAll();
		}
		if ( wait ) {
			synchronized( Thread.currentThread() ) {
				try {
					if ( procsList.contains( triple ) )
						Thread.currentThread().wait();
				} catch( InterruptedException e ) {}
			}
		}
	}
	
	public void getMessage( InputProcess process )
	{
		getMessage( process, true );
	}
		
	public synchronized void signForMessage( NDChoiceProcess process )
	{
		procsList.addFirst(
						new Triple< CorrelatedThread, InputProcess, Thread >(
								CorrelatedThread.currentThread(),
								process,
								Thread.currentThread()
								)
						);
		this.notifyAll();
	}
	
	public synchronized void cancelWaiting( NDChoiceProcess process ) 
	{
		CorrelatedThread ct = CorrelatedThread.currentThread();
		Thread t = Thread.currentThread();
		for ( Triple< CorrelatedThread, InputProcess, Thread > triple : procsList ) {
			if ( triple.first() == ct && triple.second() == process && triple.third() == t ) {
				procsList.remove( triple );
				break;
			}
		}
	}
}