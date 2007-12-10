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

import jolie.ExecutionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariablePath;
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
public class NDChoiceProcess implements CorrelatedInputProcess
{
	public class Execution implements InputProcess
	{
		private NDChoiceProcess parent;
		private ChoicePair pair;
	
		public Execution( NDChoiceProcess parent )
		{
			this.parent = parent;
		}
		
		public InputHandler inputHandler()
		{
			return null;
		}
	
		public void run()
			throws FaultException
		{
			for( ChoicePair cp : inputMap.values() )
				cp.inputProcess().inputHandler().signForMessage( this );
			
			synchronized( this ) {
				if( pair == null ) {
					try {
						this.wait();
					} catch( InterruptedException e ) {}
				}
			}
			assert( pair != null );
			
			pair.inputProcess.run();
			pair.process.run();			
		}
		
		public boolean hasReceivedMessage()
		{
			return ( pair != null );
		}
		
		public void recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null )
				parent.correlatedProcess.inputReceived();

			pair = parent.inputMap.get( message.inputId() );
			assert( pair != null );
			pair.inputProcess().recvMessage( channel, message );
			for( ChoicePair currPair : inputMap.values() )
				currPair.inputProcess().inputHandler().cancelWaiting( this );
			synchronized( this ) {
				this.notify();
			}
		}
		
		public GlobalVariablePath inputVarPath( String inputId )
		{
			for( ChoicePair cp : parent.inputMap.values() ) {
				if ( cp.inputProcess().inputHandler().id().equals( inputId ) )
					return ((InputOperationProcess)(cp.inputProcess())).inputVarPath();
			}
			return null;
		}
	}
	
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
	
	protected HashMap< String, ChoicePair > inputMap;
	protected CorrelatedProcess correlatedProcess;
	
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

	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	/** Runs the non-deterministic choice behaviour. */
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.killed() )
			return;
		
		(new Execution( this )).run();
	}	
}
