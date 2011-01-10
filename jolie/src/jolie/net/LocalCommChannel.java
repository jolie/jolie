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
import java.util.HashMap;
import java.util.Map;
import jolie.Interpreter;

/**
 * An in-memory channel that can be used to communicate directly with a specific <code>Interpreter</code> instance.
 */
public class LocalCommChannel extends CommChannel implements PollableCommChannel
{
	private static class CoLocalCommChannel extends CommChannel
	{
		private CommMessage request;
		private final LocalCommChannel senderChannel;

		private CoLocalCommChannel( LocalCommChannel senderChannel, CommMessage request )
		{
			this.senderChannel = senderChannel;
			this.request = request;
		}

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

		protected void sendImpl( CommMessage message )
		{
			synchronized( senderChannel.messages ) {
				senderChannel.messages.put( message.id(), message );
				senderChannel.messages.notifyAll();
			}
		}

		public CommMessage recvResponseFor( CommMessage request )
			throws IOException
		{
			throw new IOException( "Unsupported operation" );
		}

		@Override
		protected void disposeForInputImpl()
			throws IOException
		{}

		protected void closeImpl()
		{}
	}

	private final Interpreter interpreter;
	private final CommListener listener;
	private final Map< Long, CommMessage > messages = new HashMap< Long, CommMessage >();
	
	public LocalCommChannel( Interpreter interpreter, CommListener listener )
	{
		this.interpreter = interpreter;
		this.listener = listener;
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

	protected CommMessage recvImpl()
		throws IOException
	{
		throw new IOException( "Unsupported operation" );
	}

	protected void sendImpl( CommMessage message )
	{
		interpreter.commCore().scheduleReceive( new CoLocalCommChannel( this, message ), listener.inputPort() );
	}

	public CommMessage recvResponseFor( CommMessage request )
	{
		boolean keepRun = true;
		CommMessage ret = null;
		synchronized( messages ) {
			while( keepRun ) {
				if ( (ret=messages.remove( request.id() )) == null ) {
					try {
						messages.wait();
					} catch( InterruptedException e ) {}
				} else {
					keepRun = false;
				}
			}
		}
		return ret;
	}
	
	public boolean isReady()
	{
		boolean isReady;
		synchronized( messages ) {
			isReady = messages.isEmpty() == false;
		}
		return isReady;
	}
	
	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForPolling( this );
	}

	protected void closeImpl()
	{}
}
