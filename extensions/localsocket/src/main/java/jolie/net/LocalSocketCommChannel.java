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

import cx.ath.matthew.unix.UnixSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;

public class LocalSocketCommChannel extends StreamingCommChannel implements PollableCommChannel {
	private final UnixSocket socket;
	private final PreBufferedInputStream bufferedInputStream;
	private final InputStream socketInputStream;
	private final OutputStream socketOutputStream;

	public LocalSocketCommChannel( UnixSocket socket, URI location, CommProtocol protocol )
		throws IOException {
		super( location, protocol );

		this.socket = socket;
		this.socketInputStream = socket.getInputStream();
		this.socketOutputStream = socket.getOutputStream();
		this.bufferedInputStream = new PreBufferedInputStream( socketInputStream );

		setToBeClosed( false ); // LocalSocket connections are kept open by default
	}

	@Override
	protected void sendImpl( CommMessage message )
		throws IOException {
		protocol().send( socketOutputStream, message, bufferedInputStream );
		socketOutputStream.flush();
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException {
		return protocol().recv( bufferedInputStream, socketOutputStream );
	}

	@Override
	protected void closeImpl()
		throws IOException {
		socket.close();
	}

	@Override
	public synchronized boolean isReady()
		throws IOException {
		boolean ret = false;

		if( bufferedInputStream.hasCachedData() ) {
			ret = true;
		} else {
			byte[] r = new byte[ 1 ];
			if( socketInputStream.read( r ) > 0 ) {
				bufferedInputStream.append( r[ 0 ] );
				ret = true;
			}
		}

		return ret;
	}

	@Override
	public void disposeForInputImpl()
		throws IOException {
		Interpreter.getInstance().commCore().registerForPolling( this );
	}
}
