/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.util.HashMap;

/** Implements a non-deterministic choice.
 * A ChoiceProcess instance collects pairs which couple an 
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
public class ChoiceProcess implements InputProcess
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
	private boolean mesgReceived;
	private Process execProc;
	
	/** Constructor */
	public ChoiceProcess()
	{
		inputMap = new HashMap< String, ChoicePair >();
		mesgReceived = false;
		execProc = null;
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
		//inputProc.inputHandler().signForMessage( this );
	}

	public InputHandler inputHandler()
	{
		return null;
	}
	
	/** Runs the non-deterministic choice behaviour. */
	public void run()
	{
		for( ChoicePair cp : inputMap.values() )
			cp.inputProcess.inputHandler().signForMessage( this );

		mesgReceived = false;
		synchronized( this ) {
			if ( execProc == null ) {
				try {
					wait();
				} catch( InterruptedException e ) {}
			}
			execProc.run();
		}
	}

	public synchronized boolean recvMessage( CommMessage message )
	{
		if ( mesgReceived )
			return false;
		
		ChoicePair pair;
		pair = inputMap.get( message.inputId() );
		if ( pair != null ) {
			inputMap.remove( message.inputId() );
			pair.inputProcess().recvMessage( message );
			execProc = pair.process();
			
			for( ChoicePair currPair : inputMap.values() )
				currPair.inputProcess().inputHandler().cancelWaiting( this );

			//inputMap = null;	// Free memory (todo -- check this, is it safe?)
		} else
			return false;

		notify();
		mesgReceived = true;
		return true;
	}
}
