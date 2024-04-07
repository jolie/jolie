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

package joliex.rmi;

import java.io.IOException;
import java.util.concurrent.Future;
import jolie.Interpreter;
import jolie.net.AbstractCommChannel;
import jolie.net.CommMessage;
import jolie.net.PollableCommChannel;

public class RMICommChannel extends AbstractCommChannel implements PollableCommChannel {
	final private RemoteBasicChannel remoteChannel;

	public RMICommChannel( RemoteBasicChannel remoteChannel ) {
		super();
		this.remoteChannel = remoteChannel;
	}

	@Override
	protected void sendImpl( CommMessage message )
		throws IOException {
		remoteChannel.send( message );
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException {
		throw new IOException( "Unsupported operation" );
		// return remoteChannel.recv();
	}

	@Override
	public Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException {
		return remoteChannel.recvResponseFor( request );
	}

	@Override
	public boolean isReady()
		throws IOException {
		return remoteChannel.isReady();
	}

	@Override
	protected void closeImpl()
		throws IOException {
		remoteChannel.close();
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException {
		Interpreter.getInstance().commCore().registerForPolling( this );
	}
}
