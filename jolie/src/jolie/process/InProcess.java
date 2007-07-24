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
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputHandler;

class InInputHandler extends Thread implements InputHandler
{
	GlobalVariable var;
	private InputProcess inputProcess;
	private BufferedReader stdin = new BufferedReader(
		new InputStreamReader(
				Channels.newInputStream(
						(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );

	public InInputHandler( GlobalVariable var )
	{
		this.var = var;
	}
	
	public synchronized void signForMessage( NDChoiceProcess process )
	{
		inputProcess = process;
		if ( this.getState() == Thread.State.NEW )
			this.start();
	}
	
	public synchronized void cancelWaiting( NDChoiceProcess process )
	{
		if ( inputProcess == process )
			inputProcess = null;

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
			synchronized( InProcess.mutex() ) {
				Thread.yield();
				buffer = stdin.readLine();
			}
			try {
				var.setIntValue( Integer.parseInt( buffer ) );
			} catch( NumberFormatException nfe ) {
				var.setStrValue( buffer );
			}
			if ( inputProcess != null )
				inputProcess.recvMessage( new CommMessage( id() ) );
		} catch( ClosedByInterruptException ce ) {
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}

public class InProcess implements InputProcess
{	
	private GlobalVariable var;
	private InInputHandler inputHandler = null;
	private static final Object mutex = new Object();
	private BufferedReader stdin = new BufferedReader(
		new InputStreamReader(
				Channels.newInputStream(
						(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
	
	public static Object mutex()
	{
		return mutex;
	}

	public InProcess( GlobalVariable var )
	{
		this.var = var;
	}
	
	public void run()
	{
		if ( ExecutionThread.killed() )
			return;
		try {
			String buffer;
			synchronized( mutex() ) {
				Thread.yield();
				buffer = stdin.readLine();
			}
			try {
				var.setIntValue( Integer.parseInt( buffer ) );
			} catch( NumberFormatException nfe ) {
				var.setStrValue( buffer );
			}
		} catch( ClosedByInterruptException ce ) {
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	
	public InputHandler inputHandler()
	{
		if ( inputHandler == null )
			inputHandler = new InInputHandler( var );
		return inputHandler;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		return true;
	}
}
