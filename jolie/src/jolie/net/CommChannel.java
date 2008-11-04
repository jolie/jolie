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
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import jolie.ExecutionThread;

/**
 * A communication abstraction to send and receive messages.
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
abstract public class CommChannel implements Channel
{
	final private Map< Long, CommMessage > pendingResponses =
			new HashMap< Long, CommMessage >();
	final private Map< Long, ResponseContainer > waiters =
			new HashMap< Long, ResponseContainer >();
	final private List< CommMessage > pendingGenericResponses =
			new Vector< CommMessage >();

	private class ResponseContainer
	{
		private CommMessage response = null;
	}
	
	protected boolean toBeClosed = true;
	private CommListener listener = null;
	private boolean isOpen = true;
	
	private CommChannel redirectionChannel = null;
	
	public void setRedirectionChannel( CommChannel redirectionChannel )
	{
		this.redirectionChannel = redirectionChannel;
	}
	
	public CommChannel redirectionChannel()
	{
		return redirectionChannel;
	}
	
	public void refreshProtocol()
	{}
	
	public void setParentListener( CommListener listener )
	{
		this.listener = listener;
	}
	
	public CommListener parentListener()
	{
		return listener;
	}
	
	final public boolean isOpen()
	{
		return isOpen;
	}
	
	final public boolean canBeReused()
	{
		return isOpen && !toBeClosed;
	}
	
	/** Receives a message from the channel. */
	final synchronized public CommMessage recv()
		throws IOException
	{
		return recvImpl();
	}
	
	final public CommMessage recvResponseFor( CommMessage message )
		throws IOException
	{
		CommMessage response;
		ResponseContainer monitor = null;
		synchronized( this ) {
			response = pendingResponses.remove( message.id() );
			if ( response == null ) {
				if ( pendingGenericResponses.isEmpty() ) {
					assert( waiters.containsKey( message.id() ) == false );
					monitor = new ResponseContainer();
					waiters.put( message.id(), monitor );
				} else {
					response = pendingGenericResponses.remove( 0 );
				}
			}
		}
		if ( response == null ) {
			synchronized( monitor ) {
				if ( responseReceiver == null ) {
					responseReceiver = new ResponseReceiver( this, ExecutionThread.currentThread() );
					responseReceiver.start();
				}
				try {
					monitor.wait();
				} catch( InterruptedException e ) {}
				response = monitor.response;
			}
		}
		return response;
	}
	
	private ResponseReceiver responseReceiver = null;
	
	private class ResponseReceiver extends CommChannelHandler
	{
		private final CommChannel parent;
		
		private ResponseReceiver( CommChannel parent, ExecutionThread ethread )
		{
			super( jolie.Interpreter.getInstance() );
			setExecutionThread( ethread ); // TODO: this could be buggy!
			this.parent = parent;
		}
		
		private void handleGenericMessage( CommMessage response )
		{
			ResponseContainer monitor;
			if ( waiters.isEmpty() ) {
				pendingGenericResponses.add( response );
			} else {
				Entry< Long, ResponseContainer > entry =
					waiters.entrySet().iterator().next();
				monitor = entry.getValue();
				monitor.response = new CommMessage(
					entry.getKey(),
					response.operationName(),
					response.resourcePath(),
					response.value(),
					response.fault()
				);
				waiters.remove( entry.getKey() );
				synchronized( monitor ) {
					monitor.notify();
				}
			}
		}
		
		private void handleMessage( CommMessage response )
		{
			ResponseContainer monitor;
			if ( (monitor=waiters.remove( response.id() )) == null ) {
				pendingResponses.put( response.id(), response );
			} else {
				monitor.response = response;
				synchronized( monitor ) {
					monitor.notify();
				}
			}
		}
		
		@Override
		public void run()
		{
			CommMessage response;
			boolean keepRun = true;
			while( keepRun ) {
				synchronized( parent ) {
					try {
						response = recvImpl();
						if ( response.hasGenericId() ) {
							handleGenericMessage( response );
						} else {
							handleMessage( response );
						}
					} catch( IOException e ) {
						e.printStackTrace();
					}
					if ( waiters.isEmpty() ) {
						keepRun = false;
						responseReceiver = null;
					}
				}
			}
		}
	}
	
	/** Sends a message through the channel. */
	final synchronized public void send( CommMessage message )
		throws IOException
	{
		sendImpl( message );
	}
	
	abstract protected CommMessage recvImpl()
		throws IOException;
	
	abstract protected void sendImpl( CommMessage message )
		throws IOException;
	
	/** Closes the communication channel */
	final public void close()
		throws IOException
	{
		if ( toBeClosed ) {
			closeImpl();
			isOpen = false;
		}
	}
	
	final public void disposeForInput()
		throws IOException
	{
		if ( toBeClosed ) {
			closeImpl();
		} else {
			disposeForInputImpl();
		}
	}
	
	protected void disposeForInputImpl()
		throws IOException
	{
		closeImpl();
	}
	
	public void setToBeClosed( boolean toBeClosed )
	{
		this.toBeClosed = toBeClosed;
	}

	/** Implements the communication channel closing operation. */
	abstract protected void closeImpl()
		throws IOException;
}