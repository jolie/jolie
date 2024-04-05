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
import jolie.runtime.InternalLink;

public class LinkInProcess implements Process {
	public static class Execution {
		private CommMessage message = null;
		private final LinkInProcess parent;

		public Execution( LinkInProcess parent ) {
			this.parent = parent;
		}

		private void run()
			throws FaultException {
			InternalLink link = InternalLink.getById( parent.linkId );
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

		public synchronized boolean recvMessage( CommChannel channel, CommMessage message ) {
			this.message = message;
			this.notify();
			return true;
		}

		public boolean isKillable() {
			return true;
		}
	}

	private final String linkId;

	public LinkInProcess( String link ) {
		this.linkId = link;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new LinkInProcess( linkId );
	}

	@Override
	public void run()
		throws FaultException, ExitingException {
		if( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		(new Execution( this )).run();
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
