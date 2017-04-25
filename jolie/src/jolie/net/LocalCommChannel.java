/*
 * Copyright (C) 2008-2015 Fabrizio Montesi <famontesi@gmail.com>
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

import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import jolie.ExecutionContext;
import jolie.Interpreter;

/**
 * An in-memory channel that can be used to communicate directly with a specific
 * <code>Interpreter</code> instance.
 */
public class LocalCommChannel extends AbstractCommChannel
{
	private static class CoLocalCommChannel extends AbstractCommChannel
	{
		private CommMessage request;
		private final LocalCommChannel senderChannel;

		private CoLocalCommChannel( LocalCommChannel senderChannel, CommMessage request )
		{
			this.senderChannel = senderChannel;
			setParentInputPort( senderChannel.listener.inputPort() );
			this.request = request;
		}

		@Override
		protected CommMessage recvImpl()
			throws IOException
		{
			throw new IOException( "Unsupported operation" );
		}

		@Override
		protected void sendImpl( StatefulMessage msg, Function<Void, Void> completionHandler )
			throws IOException
		{
			synchronized( senderChannel ) {
				ExecutionContext waitingContext = senderChannel.responseWaiters.get( msg.message().id() );
				senderChannel.pendingResponses.put( msg.message().id(), msg.message() );
				if (waitingContext != null)
					waitingContext.start();		
			}
			
			//if ( waitingContext == null ) {
				//throw new IOException( "Unexpected response message with id " + msg.message().id() + " for operation " + msg.message().operationName() + " in local channel" );
			//}
		}

		@Override
		public CommMessage recvResponseFor( ExecutionContext ctx, CommMessage request )
			throws IOException
		{
			throw new IOException( "Unsupported operation" );
		}

		@Override
		protected void disposeForInputImpl()
			throws IOException
		{
		}

		@Override
		protected void closeImpl()
		{
		}

		private void messageRecv( CommMessage message )
		{
			messageRecv( getContextFor( message.id(), message.isRequest() ), message );
		}
	}

	private final Interpreter interpreter;
	private final CommListener listener;
	private final Map< Long, ExecutionContext> responseWaiters = new ConcurrentHashMap<>();
	private final Map< Long,  CommMessage> pendingResponses = new ConcurrentHashMap<>();
	private final EventLoopGroup workerGroup;

	public LocalCommChannel( Interpreter interpreter, CommListener listener )
	{
		this.interpreter = interpreter;
		this.listener = listener;
		this.workerGroup = interpreter.commCore().getWorkerGroup();
	}

	@Override
	public CommChannel createDuplicate()
	{
		return new LocalCommChannel( interpreter, listener );
	}

	public Interpreter interpreter()
	{
		return interpreter;
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException
	{
		throw new IOException( "Unsupported operation" );
	}

	@Override
	protected void sendImpl( StatefulMessage msg, Function<Void, Void> completionHandler )
	{
		workerGroup.execute(new Runnable()
		{
			@Override
			public void run()
			{
				CoLocalCommChannel coChannel = new CoLocalCommChannel( LocalCommChannel.this, msg.message() );
				coChannel.messageRecv( msg.message() );
			}
		});
	}

	@Override
	public CommMessage recvResponseFor( ExecutionContext ctx, CommMessage request )
		throws IOException
	{
		CommMessage response = null;
		synchronized( this ) {
			response = pendingResponses.get( request.id() );
			if (response == null)
				responseWaiters.put( request.id(), ctx );
		}
		return response;
	}

	@Override
	protected void closeImpl()
	{
	}
}
