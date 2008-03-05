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
import java.util.Map.Entry;

import jolie.ExecutionThread;
import jolie.net.CommChannel;
import jolie.net.CommChannelHandler;
import jolie.net.CommMessage;
import jolie.process.InputOperationProcess;
import jolie.process.InputProcessExecution;
import jolie.process.NDChoiceProcess;
import jolie.util.Pair;

/**
 * @author Fabrizio Montesi
 * 
 */
abstract public class InputOperation extends AbstractIdentifiableObject implements InputHandler
{
	private HashMap< InputProcessExecution, ExecutionThread > procsMap =
						new HashMap< InputProcessExecution, ExecutionThread >();
	
	private LinkedList< Pair< CommChannel, CommMessage > > mesgList =
						new LinkedList< Pair< CommChannel, CommMessage > > ();
	
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
	public synchronized void recvMessage( CommChannel channel, CommMessage message )
	{
		VariablePath path = null;
		InputProcessExecution pe = null;
		for( Entry< InputProcessExecution, ExecutionThread > entry : procsMap.entrySet() ) {
			pe = entry.getKey();
			if ( pe instanceof NDChoiceProcess.Execution )
				path = ((NDChoiceProcess.Execution)pe).inputVarPath( message.inputId() );
			else if ( pe.parent() instanceof InputOperationProcess )
				path = ((InputOperationProcess)pe.parent()).inputVarPath();
			
			CommChannelHandler.currentThread().setExecutionThread( entry.getValue() );
			if ( entry.getValue().checkCorrelation( path, message )
					&& entry.getKey().recvMessage( channel, message )
					) {
				procsMap.remove( entry.getKey() );
				return;
			}
		}
		
		mesgList.add( new Pair< CommChannel, CommMessage >( channel, message ) );
	}

	public synchronized void signForMessage( InputProcessExecution process )
	{
		ExecutionThread ethread = ExecutionThread.currentThread();
		VariablePath path = null;
		for( Pair< CommChannel, CommMessage > pair : mesgList ) {
			if ( process instanceof InputOperationProcess )
				path = ((InputOperationProcess) process).inputVarPath();
			else if ( process instanceof NDChoiceProcess.Execution )
				path = ((NDChoiceProcess.Execution) process).inputVarPath( pair.value().inputId() );
			
			if ( ethread.checkCorrelation( path, pair.value() )
					&& process.recvMessage( pair.key(), pair.value() ) ) {
				mesgList.remove( pair );
				return;
			}
		}
		
		procsMap.put( process, ethread );
	}
	
	public synchronized void cancelWaiting( InputProcessExecution process ) 
	{
		procsMap.remove( process );
	}
}