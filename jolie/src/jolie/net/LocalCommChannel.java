/**
 * *************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 **************************************************************************
 */
package jolie.net;

import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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
			if ( request == null ) {
				throw new IOException( "Unsupported operation" );
			}
			CommMessage r = request;
			request = null;
			return r;
		}

		@Override
		protected void sendImpl( StatefulMessage msg, Function<Void, Void> completionHandler )
			throws IOException
		{
			CompletableFuture< CommMessage> f = senderChannel.responseWaiters.get( msg.message().id() );
			if ( f == null ) {
				throw new IOException( "Unexpected response message with id " + msg.message().id() + " for operation " + msg.message().operationName() + " in local channel" );
			}
			f.complete( msg.message() );
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
			messageRecv( getContextFor( message.id()), message );
		}
	}

	private final Interpreter interpreter;
	private final CommListener listener;
	private final Map< Long, CompletableFuture< CommMessage>> responseWaiters = new ConcurrentHashMap<>();
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
		System.out.println( "Sending message over LocalCommChannel " + msg.context().interpreter().logPrefix() + " -->> " + interpreter.logPrefix() );
		responseWaiters.put( msg.message().id(), new CompletableFuture<>() );
		workerGroup.execute(new Runnable()
		{
			@Override
			public void run()
			{
				CoLocalCommChannel coChannel = new CoLocalCommChannel( LocalCommChannel.this, msg.message() );
				coChannel.messageRecv( msg.message() );
			}
		});
		//interpreter.commCore().scheduleReceive( new CoLocalCommChannel( this, message ), listener.inputPort() );
	}

	@Override
	public CommMessage recvResponseFor( ExecutionContext ctx, CommMessage request )
		throws IOException
	{
		final CompletableFuture< CommMessage> f = responseWaiters.get( request.id() );
		final CommMessage m;

		try {
			m = f.get();
		} catch( ExecutionException | InterruptedException e ) {
			throw new IOException( e );
		} finally {
			responseWaiters.remove( request.id() );
		}

		return m;
	}

	@Override
	protected void closeImpl()
	{
	}
}
