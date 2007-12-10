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


package jolie.net;

import java.io.IOException;

import jolie.ExecutionThread;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;

/** CommChannelHandler is used by the communication core to handle a newly opened 
 * communication channel.
 * CommChannelHandler objects are grouped in a ThreadGroup, in order to be able 
 * to interrupt them in case of network shutdown.
 * 
 * @todo this must become a ProcessThread, leaving the encumbrance from CommChannel
 * 
 */
public class CommChannelHandler extends Thread
{
	private static int index = 0;
	
	private CommChannel channel;
	private CommListener listener;
	private ExecutionThread executionThread;
	
	private static int runningHandlers = 0;
	private static Object mutex = new Object();
	
	private static final long heuristicMem = 300*1024;

	/** Constructor.
	 * 
	 * @param threadGroup the threadGroup to which the CommChannelHandler thread will be assigned.
	 * @param channel the channel to be handled.
	 */
	public CommChannelHandler( ThreadGroup threadGroup, CommChannel channel, CommListener listener )
	{
		super( threadGroup, "CommChannelHandler-" + index++ );
		this.channel = channel;
		this.listener = listener;
	}
	
	public static CommChannelHandler currentThread()
	{
		return ((CommChannelHandler)Thread.currentThread());
	}
	
	public void setExecutionThread( ExecutionThread thread )
	{
		executionThread = thread;
	}
	
	public ExecutionThread executionThread()
	{
		return executionThread;
	}
	
	/**
	 * @todo improve the memory heuristics
	 * @todo handle resource exhaustion exceptions, as impossibility to create another thread or outofmemory errors
	 */
	public static void startHandler( CommChannel channel, CommListener listener )
	{
		int limit = CommCore.connectionsLimit();
		synchronized( mutex ) {
			if ( limit < 0 ) {
				if( runningHandlers > 0 && Runtime.getRuntime().freeMemory() < heuristicMem ) {
					try {
						mutex.wait();
					} catch( InterruptedException ie ) {}
				}
			} else if( runningHandlers > limit ) {
				try {
					mutex.wait();
				} catch( InterruptedException ie ) {}
			}
			
			//int freeMem = Runtime.getRuntime().freeMemory();
			//(new CommChannelHandler( listener.getThreadGroup(), channel, listener )).start();
			//freeMem = 
		}
		
		(new CommChannelHandler( listener.getThreadGroup(), channel, listener )).start();
	}
	
	/** Runs the thread, making it waiting for a message.
	 * When a message is received, the thread creates a CommMessage object 
	 * and passes it to the relative InputOperation object, which will handle the
	 * received information.
	 * After the information has been sent to the InputOperation object, the 
	 * communication channel is closed and the thread terminates. 
	 */
	public void run()
	{
		synchronized( mutex ) {
			runningHandlers++;
		}
		try {
			CommMessage message = channel.recv();
			InputOperation operation =
					InputOperation.getById( message.inputId() );
			
			if ( listener.canHandleInputOperation( operation ) )
				operation.recvMessage( channel, message );
			else {
				CommCore.logger().warning(
							"Discarded a message for operation " + operation +
							", not specified in an input port at the receiving service."
						);
			}
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( InvalidIdException iie ) {
			iie.printStackTrace();
		}
		
		synchronized( mutex ) {
			runningHandlers--;
			int limit = CommCore.connectionsLimit();
			if ( limit < 0 || runningHandlers <= limit )
				mutex.notifyAll();
		}
	}
}