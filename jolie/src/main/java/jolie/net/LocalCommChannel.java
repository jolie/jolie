/*
 * Copyright (C) 2008-2019 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import jolie.Interpreter;

/**
 * An in-memory channel that can be used to communicate directly with a specific
 * <code>Interpreter</code> instance.
 */
public class LocalCommChannel extends CommChannel implements PollableCommChannel {
	private static class CoLocalCommChannel extends CommChannel {
		private CommMessage request;
		private final long requestId;
		private final CompletableFuture< CommMessage > responseFut;

		private CoLocalCommChannel( CommMessage request, CompletableFuture< CommMessage > responseFut ) {
			this.request = request;
			this.responseFut = responseFut;
			this.requestId = request.requestId();
		}

		@Override
		protected CommMessage recvImpl()
			throws IOException {
			if( request == null ) {
				throw new IOException( "Unsupported operation" );
			}
			CommMessage r = request;
			request = null;
			return r;
		}

		@Override
		protected void sendImpl( CommMessage message )
			throws IOException {
			if( message.requestId() != requestId ) {
				throw new IOException( "Unexpected response message with id " + message.requestId() + " for operation "
					+ message.operationName() + " in local channel" );
			}
			responseFut.complete( message );
		}

		@Override
		public Future< CommMessage > recvResponseFor( CommMessage request )
			throws IOException {
			throw new IOException( "Unsupported operation" );
		}

		@Override
		protected void disposeForInputImpl()
			throws IOException {}

		@Override
		protected void closeImpl() {}
	}

	private final WeakReference< Interpreter > interpreter;
	private final CommListener listener;
	private final Map< Long, CompletableFuture< CommMessage > > responseWaiters = new ConcurrentHashMap<>();

	public LocalCommChannel( Interpreter interpreter, CommListener listener ) {
		this.interpreter = new WeakReference<>( interpreter );
		this.listener = listener;
	}

	@Override
	public CommChannel createDuplicate() {
		return new LocalCommChannel( interpreter.get(), listener );
	}

	public Interpreter interpreter() {
		return interpreter.get();
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException {
		throw new IOException( "Unsupported operation" );
	}

	@Override
	protected void sendImpl( CommMessage message ) throws IOException {
		Interpreter interpreter = interpreter();
		if( interpreter == null ) {
			throw new IOException( "Sending Channel is closed" );
		} else {
			CompletableFuture< CommMessage > f = new CompletableFuture<>();
			responseWaiters.put( message.requestId(), f );
			interpreter.commCore().scheduleReceive( new CoLocalCommChannel( message, f ), listener.inputPort() );
		}
	}

	@Override
	public Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException {
		return responseWaiters.remove( request.requestId() );
	}

	@Override
	public boolean isReady() {
		return responseWaiters.isEmpty() == false;
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException {
		Interpreter.getInstance().commCore().registerForPolling( this );
	}

	@Override
	protected void closeImpl() {}
}
