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
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;

/**
 *
 * @author Fabrizio Montesi
 */
public class RemoteBasicChannelImpl implements RemoteBasicChannel {
	final private LocalCommChannel channel;
	final private JolieRemoteImpl parent;

	public RemoteBasicChannelImpl( LocalCommChannel channel, JolieRemoteImpl parent ) {
		this.channel = channel;
		this.parent = parent;
	}

	@Override
	public void send( CommMessage message )
		throws IOException {
		channel.send( message );
	}

	@Override
	public boolean isReady() {
		return channel.isReady();
	}

	@Override
	public void close() {
		parent.disposeOf( this );
	}

	@Override
	public CommMessage recv()
		throws IOException {
		throw new IOException( "Unsupported operation" );
		// return channel.recv();
	}

	@Override
	public Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException {
		return channel.recvResponseFor( request );
	}
}
