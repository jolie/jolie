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

package jolie.process;

import java.util.HashMap;
import java.util.List;

import jolie.ExecutionThread;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputHandler;

/** Implements a non-deterministic choice.
 * An NDChoiceProcess instance collects pairs which couple an 
 * InputProcess object with a Process object.
 * When the ChoiceProcess object is run, it waits for 
 * the receiving of a communication on one of its InputProcess objects.
 * When a communication is received, the following happens:
 * \li the communication is resolved by the corresponding InputProcess instance.
 * \li the paired Process object is executed.
 * 
 * After that, the ChoiceProcess terminates, so the other pairs are ignored.
 * 
 * @author Fabrizio Montesi
 */
public class NDChoiceProcess implements InputProcess, CorrelatedInputProcess
{
	private class ChoicePair
	{
		private InputProcess inputProcess;
		private Process process;
		
		public ChoicePair( InputProcess inputProcess, Process process )
		{
			this.inputProcess = inputProcess;
			this.process = process;
		}
		
		public InputProcess inputProcess()
		{
			return inputProcess;
		}
		
		public Process process()
		{
			return process;
		}
	}
	
	private HashMap< String, ChoicePair > inputMap;
	private CorrelatedProcess correlatedProcess;
	
	public static class Fields {
		private FaultException pendingFault = null;
		private Process pendingProcess = null;
	}
	
	/** Constructor */
	public NDChoiceProcess()
	{
		inputMap = new HashMap< String, ChoicePair >();
	}
	
	/** Adds an InputProcess<->Process pair: a possible non-deterministic choice.
	 * 
	 * @param inputProc the InputProcess instance.
	 * @param process the Process instance to be executed if the InputProcess receives a message.
	 */
	public void addChoice( InputProcess inputProc, Process process )
	{
		ChoicePair pair = new ChoicePair( inputProc, process );
		inputMap.put( inputProc.inputHandler().id(), pair );
	}

	public InputHandler inputHandler()
	{
		return null;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public List< GlobalVariable > inputVars( String operationId )
	{
		for( ChoicePair cp : inputMap.values() ) {
			if ( cp.inputProcess().inputHandler().id().equals( operationId ) )
				return ((InputOperationProcess)(cp.inputProcess())).inputVars();
		}
		return null;
	}
	
	/** Runs the non-deterministic choice behaviour. */
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.killed() )
			return;
		
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );

		for( ChoicePair cp : inputMap.values() ) {
			if ( cp.inputProcess() instanceof CorrelatedInputProcess )
				((CorrelatedInputProcess)cp.inputProcess()).setCorrelatedProcess( correlatedProcess );
			cp.inputProcess().inputHandler().signForMessage( this );
		}

		synchronized( ExecutionThread.currentThread() ) {
			if( fields.pendingProcess == null ) {
				try {
					ExecutionThread.currentThread().wait();
				} catch( InterruptedException e ) {}
			}
		}
		
		if ( fields.pendingFault != null )
			throw fields.pendingFault;

		fields.pendingProcess.run();
	}

	public boolean recvMessage( CommMessage message )
	{
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		
		synchronized( ExecutionThread.currentThread() ) {
			if ( fields.pendingProcess != null )
				return false;
			ChoicePair pair;
			pair = inputMap.get( message.inputId() );
			
			if ( pair != null ) {
				fields.pendingProcess = pair.process();
				pair.inputProcess().recvMessage( message );

				for( ChoicePair currPair : inputMap.values() )
					currPair.inputProcess().inputHandler().cancelWaiting( this );
			} else
				return false;
		}
		return true;
	}	
}
