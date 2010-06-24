/***************************************************************************
 *   Copyright (C) 2007 by Fabrizio Montesi <famontesi@gmail.com>          *
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommChannelHandler;
import jolie.net.CommMessage;
import jolie.process.InputOperationProcess;
import jolie.process.InputProcessExecution;
import jolie.process.NDChoiceProcess;
import jolie.util.Pair;
import jolie.runtime.typing.TypeCheckingException;

/**
 * @author Fabrizio Montesi
 * 
 */
public abstract class InputOperation extends AbstractIdentifiableObject implements InputHandler
{
	private final Map< InputProcessExecution, ExecutionThread > procsMap =
						new HashMap< InputProcessExecution, ExecutionThread >();
	
	private final List< Pair< CommChannel, CommMessage > > mesgList =
						new LinkedList< Pair< CommChannel, CommMessage > > ();

	/**
	 * Constructor
	 * @param id the name that identified this input operation
	 */
	public InputOperation( String id )
	{
		super( id );
	}
	
	/**
	 * Receives a message from CommCore and passes it to the right InputProcess.
	 * If no suitable InputProcess is found, the message is enqueued in memory.
	 * @param channel The channel which received the message. Useful if the operation wants to send a response.
	 * @param message The received message.
	 */
	public synchronized void recvMessage( CommChannel channel, final CommMessage message )
	{
		VariablePath path = null;
		InputProcessExecution pe = null;
		for( Entry< InputProcessExecution, ExecutionThread > entry : procsMap.entrySet() ) {
			pe = entry.getKey();
			if ( pe instanceof NDChoiceProcess.Execution ) {
				path = ((NDChoiceProcess.Execution)pe).inputVarPath( message.operationName() );
			} else if ( pe.parent() instanceof InputOperationProcess ) {
				path = ((InputOperationProcess)pe.parent()).inputVarPath();
			}
			
			CommChannelHandler.currentThread().setExecutionThread( entry.getValue() );
			if ( entry.getValue().checkCorrelation( path, message ) ) {
				try {
					if ( entry.getKey().recvMessage( channel, message ) ) {
						procsMap.remove( entry.getKey() );
						return;
					}
				} catch( TypeCheckingException e ) {
					return;
				}
			}
		}

		final Pair< CommChannel, CommMessage > pair = new Pair< CommChannel, CommMessage >( channel, message );
		mesgList.add( pair );
		final Interpreter interpreter = Interpreter.getInstance();
		interpreter.addTimeoutHandler( new TimeoutHandler( interpreter.inputMessageTimeout() ) {
			@Override
			public void onTimeout()
			{
				boolean removed;
				synchronized( this ) {
					removed = mesgList.remove( pair );
				}
				if ( removed && interpreter.verbose() ) {
					interpreter.logInfo( "Message " + message.id() + " discarded for timeout" );
				}
			}
		} );
	}

	/**
	 * Registers an {@link InputProcessExecution} instance for receiving a message
	 * regarding this input operation.
	 * If a message is not available, the requester is put in queue for receiving
	 * one as soon as possible.
	 * @param process
	 */
	public synchronized void signForMessage( InputProcessExecution process )
	{
		ExecutionThread ethread = ExecutionThread.currentThread();
		VariablePath path = null;
		for( Pair< CommChannel, CommMessage > pair : mesgList ) {
			if ( process instanceof NDChoiceProcess.Execution ) {
				path = ((NDChoiceProcess.Execution) process).inputVarPath( pair.value().operationName() );
			} else {
				if ( process.parent() instanceof InputOperationProcess ) {
					path = ((InputOperationProcess)process.parent()).inputVarPath();
				}
			}
			
			if ( ethread.checkCorrelation( path, pair.value() ) ) {
				try {
					if ( process.recvMessage( pair.key(), pair.value() ) ) {
						mesgList.remove( pair );
						return;
					}
				} catch( TypeCheckingException e ) {
					mesgList.remove( pair );
				}
			}
		}
		
		procsMap.put( process, ethread );
	}

	/**
	 * Removes an {@link InputProcessExecution} instance from the list
	 * of waiters for receiving a message.
	 * @param process
	 */
	public synchronized void cancelWaiting( InputProcessExecution process ) 
	{
		procsMap.remove( process );
	}
}