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

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.InternalLink;

public class LinkInProcess implements InputProcess
{
	private class Execution extends AbstractInputProcessExecution< LinkInProcess >
	{
		protected CommMessage message = null;
		
		public Execution( LinkInProcess parent )
		{
			super( parent );
		}
		
		public Process clone( TransformationReason reason )
		{
			return new Execution( parent );
		}

		public void interpreterExit()
		{}

		protected void runImpl()
			throws FaultException
		{
			InternalLink link = InternalLink.getById( linkId );
			try {
				link.signForMessage( this );
				synchronized( this ) {
					if( message == null && !Interpreter.getInstance().exiting() ) {
						ExecutionThread ethread = ExecutionThread.currentThread();
						ethread.setCanBeInterrupted( true );
						this.wait();
						ethread.setCanBeInterrupted( false );
					}
				}
			} catch( InterruptedException ie ) {
				link.cancelWaiting( this );
			}
		}

		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
		{
			this.message = message;
			this.notify();
			return true;
		}
		
		public boolean isKillable()
		{
			return true;
		}
	}
	
	final private String linkId;
	
	public LinkInProcess( String link )
	{
		this.linkId = link;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new LinkInProcess( linkId );
	}
	
	public InputHandler getInputHandler()
	{
		return InternalLink.getById( linkId  );
	}

	public void checkMessageType( CommMessage message )
	{}

	public void runBehaviour( CommChannel channel, CommMessage message )
	{}
	
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