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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.TypeCheckingException;
import jolie.util.Pair;

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
	public class Execution extends AbstractInputProcessExecution< NDChoiceProcess >
	{
		private class InputChoice {
			private final InputHandler inputHandler;
			private final InputProcess inputProcess;
			private final Process process;
			private InputChoice( InputHandler inputHandler, InputProcess inputProcess, Process process )
			{
				this.inputHandler = inputHandler;
				this.inputProcess = inputProcess;
				this.process = process;
			}
		}

		private Map< String, InputChoice > inputMap =
			new ConcurrentHashMap< String, InputChoice >();
		private InputChoice choice = null;
		private CommChannel channel;
		private CommMessage message;

		public Execution( NDChoiceProcess parent )
		{
			super( parent );
		}

		public Process clone( TransformationReason reason )
		{
			return new Execution( parent );
		}

		public void interpreterExit()
		{
			synchronized( this ) {
				this.notify();
			}
		}
	
		protected void runImpl()
			throws FaultException, ExitingException
		{
			final Interpreter interpreter = Interpreter.getInstance();
			for( Pair< InputProcess, Process > p : parent.branches ) {
				InputHandler handler = p.key().getInputHandler();
				inputMap.put( handler.id(), new InputChoice( handler, p.key(), p.value() ) );
				handler.signForMessage( this );
			}

			synchronized( this ) {
				if( choice == null && !interpreter.exiting() ) {
					try {
						this.wait();
					} catch( InterruptedException e ) {}
				}
			}

			if ( choice == null && interpreter.exiting() ) {
				throw new ExitingException();
			}

			for( InputChoice c : inputMap.values() ) {
				c.inputHandler.cancelWaiting( this );
			}

			assert( choice != null );

			choice.inputProcess.runBehaviour( channel, message );
		
			// Clean up for the garbage collector
			inputMap = null;
			channel = null;
			message = null;
			
			choice.process.run();
		}

		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
			throws TypeCheckingException
		{
			if ( choice != null ) {
				return false;
			}

			choice = inputMap.get( message.operationName() );
			assert( choice != null );

			try {
				choice.inputProcess.checkMessageType( message );
			} catch( TypeCheckingException e ) {
				Interpreter.getInstance().logWarning( "Received message TypeMismatch (Input operation " + message.operationName() + "): " + e.getMessage() );
				try {
					channel.send( CommMessage.createFaultResponse( message, new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
				} catch( IOException ioe ) {
					Interpreter.getInstance().logSevere( ioe );
				} finally {
					choice = null;
				}
				throw e;
			}


			if ( parent.correlatedProcess != null ) {
				if ( Interpreter.getInstance().exiting() ) {
					// Do not trigger session spawning if we're exiting
					this.notify();
					return false;
				}
				parent.correlatedProcess.inputReceived();
			}

			inputMap.remove( message.operationName() );
			
			this.channel = channel;
			this.message = message;

			this.notify();
			return true;
		}
		
		public synchronized VariablePath inputVarPath( String inputId )
		{
			InputChoice c = inputMap.get( inputId );
			if ( c != null && c.inputProcess instanceof InputOperationProcess ) {
				return ((InputOperationProcess)c.inputProcess).inputVarPath();
			}

			return null;
		}
		
		public boolean isKillable()
		{
			return true;
		}
	}

	private final Pair< InputProcess, Process >[] branches;
	private CorrelatedProcess correlatedProcess = null;
	
	/** Constructor */
	public NDChoiceProcess( Pair< InputProcess, Process >[] branches )
	{
		this.branches = branches;
	}
	
	public Process clone( TransformationReason reason )
	{
		Pair< InputProcess, Process >[] b = new Pair[ branches.length ];
		int i = 0;
		for( Pair< InputProcess, Process > pair : branches ) {
			b[ i++ ] = new Pair< InputProcess, Process >( pair.key(), pair.value().clone( reason ) );
		}
		return new NDChoiceProcess( b );
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	/** Runs the non-deterministic choice behaviour. */
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}
		
		(new Execution( this )).run();
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
