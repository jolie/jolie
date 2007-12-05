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

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;

import jolie.ExecutionThread;
import jolie.ProcessThread;
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InputHandler;

class InInputHandler extends ProcessThread implements InputHandler, CorrelatedInputProcess
{
	private InProcess parent;
	private GlobalVariablePath varPath;
	private ExecutionThread executionThread;
	private InputProcess inputProcess;
	private CorrelatedProcess correlatedProcess;
	
	public InInputHandler( InProcess parent, GlobalVariablePath varPath, CorrelatedProcess correlatedProcess )
	{
		this.parent = parent;
		this.varPath = varPath;
		this.correlatedProcess = correlatedProcess;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public ExecutionThread executionThread()
	{
		return executionThread;
	}
	
	public synchronized void signForMessage( NDChoiceProcess process )
	{
		executionThread = ExecutionThread.currentThread();

		inputProcess = process;
		if ( this.getState() == Thread.State.NEW )
			this.start();
		// This should make a notify to run in order to avoid race conditions
	}
	
	public synchronized void cancelWaiting( NDChoiceProcess process )
	{
		if ( inputProcess == process )
			inputProcess = null;

		parent.setHandler( new InInputHandler( parent, varPath, correlatedProcess ) );
		this.interrupt();
	}
		
	public String id()
	{
		return this.toString();
	}
	
	public void run()
	{
		try {
			String buffer;
			synchronized( InProcess.mutex ) {
				BufferedReader stdin = new BufferedReader(
					new InputStreamReader(
						Channels.newInputStream(
							(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
				//Thread.yield();
				buffer = stdin.readLine();
			}
			
			if ( correlatedProcess != null )
				correlatedProcess.inputReceived();

			try {
				varPath.getValue().setIntValue( Integer.parseInt( buffer ) );
			} catch( NumberFormatException nfe ) {
				varPath.getValue().setStrValue( buffer );
			}
			if ( inputProcess != null ) {
				if ( inputProcess.recvMessage( new CommMessage( id() ) ) ) {
					synchronized( executionThread ) {
						executionThread.notify();
					}
				}
			}
		} catch( ClosedByInterruptException ce ) {
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}

public class InProcess implements InputProcess, CorrelatedInputProcess
{	
	private GlobalVariablePath varPath;
	private InInputHandler inputHandler = null;
	private CorrelatedProcess correlatedProcess = null;
	protected static final Object mutex = new Object();
	/*protected static BufferedReader stdin = new BufferedReader(
		new InputStreamReader(
				Channels.newInputStream(
						(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );*/
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}

	public InProcess( GlobalVariablePath varPath )
	{
		this.varPath = varPath;
	}
	
	public void run()
	{
		if ( ExecutionThread.killed() )
			return;
		try {
			String buffer;
			synchronized( mutex ) {
				BufferedReader stdin = new BufferedReader(
					new InputStreamReader(
						Channels.newInputStream(
							(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
				//Thread.yield();
				buffer = stdin.readLine();
			}
			
			if ( correlatedProcess != null )
				correlatedProcess.inputReceived();

			try {
				varPath.getValue().setIntValue( Integer.parseInt( buffer ) );
			} catch( NumberFormatException nfe ) {
				varPath.getValue().setStrValue( buffer );
			}
		} catch( ClosedByInterruptException ce ) {
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	
	protected void setHandler( InInputHandler handler )
	{
		inputHandler = handler;
	}
	
	public InputHandler inputHandler()
	{
		if ( inputHandler == null )
			inputHandler = new InInputHandler( this, varPath, correlatedProcess );

		return inputHandler;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		return true;
	}
}
