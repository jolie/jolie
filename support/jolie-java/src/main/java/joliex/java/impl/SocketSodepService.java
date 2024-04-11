/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package joliex.java.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jolie.net.CommMessage;
import jolie.net.SodepProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.Value;
import jolie.util.Pair;
import joliex.java.Callback;
import joliex.java.Service;
import joliex.java.ServiceFactory;

/**
 * A {@link Service} using sockets as medium and sodep as protocol. Do not instantiate this class
 * directly, its API is due to change in the future! Use
 * {@link ServiceFactory#create(java.net.URI, java.lang.String, jolie.runtime.Value)} instead.
 * 
 * @author Fabrizio Montesi
 */
public class SocketSodepService extends Service {
	private final SocketChannel socketChannel;
	private final CommProtocol protocol;
	private final InputStream istream;
	private final OutputStream ostream;

	private final Lock lock = new ReentrantLock( true );

	@SuppressWarnings( "unchecked" )
	public SocketSodepService( ServiceFactory factory, URI location, Value protocolConfiguration )
		throws IOException {
		super( factory, location );
		socketChannel = SocketChannel.open( new InetSocketAddress( location.getHost(), location.getPort() ) );
		protocol = new SodepProtocol( new ClosedVariablePath( new Pair[ 0 ], protocolConfiguration ) );
		istream = Channels.newInputStream( socketChannel );
		ostream = Channels.newOutputStream( socketChannel );
	}

	@Override
	public void close()
		throws IOException {
		socketChannel.close();
	}

	@Override
	protected Runnable createRequestResponseRunnable( CommMessage request, Callback callback ) {
		return new RequestResponseRunnable( this, request, callback );
	}

	@Override
	protected Runnable createOneWayRunnable( CommMessage message, Callback callback ) {
		return new OneWayRunnable( this, message, callback );
	}

	private static class RequestResponseRunnable implements Runnable {
		private final SocketSodepService service;
		private final CommMessage request;
		private final Callback callback;

		private RequestResponseRunnable( SocketSodepService service, CommMessage request, Callback callback ) {
			this.service = service;
			this.request = request;
			this.callback = callback;
		}

		@Override
		public void run() {
			service.lock.lock();
			try {
				service.protocol.send( service.ostream, request, service.istream );
				service.ostream.flush();
			} catch( IOException e ) {
				callback.onError( e );
				return;
			} finally {
				service.lock.unlock();
			}

			service.lock.lock();
			try {
				CommMessage response = service.protocol.recv( service.istream, service.ostream );
				if( response.isFault() ) {
					callback.onFault( response.fault() );
				} else {
					callback.onSuccess( response.value() );
				}
			} catch( IOException e ) {
				callback.onError( e );
			} finally {
				service.lock.unlock();
			}
		}
	}

	private static class OneWayRunnable implements Runnable {
		private final SocketSodepService service;
		private final CommMessage request;
		private final Callback callback;

		private OneWayRunnable( SocketSodepService service, CommMessage request, Callback callback ) {
			this.service = service;
			this.request = request;
			this.callback = callback;
		}

		@Override
		public void run() {
			service.lock.lock();
			try {
				service.protocol.send( service.ostream, request, service.istream );
				service.ostream.flush();
				callback.onSuccess( Value.create() );
			} catch( IOException e ) {
				callback.onError( e );
			} finally {
				service.lock.unlock();
			}
		}
	}
}
