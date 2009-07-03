/***************************************************************************
 *   Copyright (C) 2006-2008 by Fabrizio Montesi                           *
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

/**
 * CommChannel allows for the sending and receiving of CommMessage instances.
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
abstract public class CommChannel
{
	final private Map< Long, CommMessage > pendingResponses =
			new HashMap< Long, CommMessage >();
	final private Map< Long, ResponseContainer > waiters =
			new HashMap< Long, ResponseContainer >();
	final private List< CommMessage > pendingGenericResponses =
			new Vector< CommMessage >();
	
	final protected ReentrantLock lock = new ReentrantLock( true );
	final private Object responseRecvMutex = new Object();

	private class ResponseContainer
	{
		private CommMessage response = null;
	}
	
	private boolean toBeClosed = true;
	private CommListener listener = null;
	private boolean isOpen = true;

	private long redirectionMessageId = 0L;

	protected long redirectionMessageId()
	{
		return redirectionMessageId;
	}

	protected void setRedirectionMessageId( long id )
	{
		redirectionMessageId = id;
	}
	
	private CommChannel redirectionChannel = null;

    final public boolean toBeClosed()
    {
        return toBeClosed;
    }

	public void setRedirectionChannel( CommChannel redirectionChannel )
	{
		this.redirectionChannel = redirectionChannel;
	}
	
	public CommChannel redirectionChannel()
	{
		return redirectionChannel;
	}
	
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
		return isOpen && isOpenImpl();
	}

	protected boolean isOpenImpl()
	{
		return true;
	}

	protected boolean isThreadSafe()
	{
		return false;
	}
	
	/** Receives a message from the channel. */
	public CommMessage recv()
		throws IOException
	{
		CommMessage ret;
		if ( lock.isHeldByCurrentThread() ) {
			ret = recvImpl();
		} else {
			lock.lock();
			try {
				ret = recvImpl();
			} catch( IOException e ) {
				lock.unlock();
				throw e;
			}
			lock.unlock();
		}
		return ret;
	}

	final public CommMessage recvResponseFor( CommMessage message )
		throws IOException
	{
		CommMessage response;
		ResponseContainer monitor = null;
		synchronized( responseRecvMutex ) {
			response = pendingResponses.remove( message.id() );
			if ( response == null ) {
				if ( pendingGenericResponses.isEmpty() ) {
					assert( waiters.containsKey( message.id() ) == false );
					monitor = new ResponseContainer();
					waiters.put( message.id(), monitor );
					responseRecvMutex.notify();
				} else {
					response = pendingGenericResponses.remove( 0 );
				}
			}
		}
		if ( response == null ) {
			synchronized( responseRecvMutex ) {
				if ( responseReceiver == null ) {
					responseReceiver = new ResponseReceiver( this, ExecutionThread.currentThread() );
					Interpreter.getInstance().commCore().startCommChannelHandler( responseReceiver );
				}
			}
			synchronized( monitor ) {
				if ( monitor.response == null ) {
					try {
						monitor.wait();
					} catch( InterruptedException e ) {
						Interpreter.getInstance().logSevere( e );
					}
				}
				response = monitor.response;
			}
		}
		return response;
	}
	
	private ResponseReceiver responseReceiver = null;
	
	private static class ResponseReceiver implements Runnable
	{
		final private CommChannel parent;
		final private ExecutionThread ethread;
		
		private ResponseReceiver( CommChannel parent, ExecutionThread ethread )
		{
			this.ethread = ethread;
			this.parent = parent;
		}
		
		private void handleGenericMessage( CommMessage response )
		{
			ResponseContainer monitor;
			if ( parent.waiters.isEmpty() ) {
				parent.pendingGenericResponses.add( response );
			} else {
				Entry< Long, ResponseContainer > entry =
					parent.waiters.entrySet().iterator().next();
				monitor = entry.getValue();
				parent.waiters.remove( entry.getKey() );
				synchronized( monitor ) {
					monitor.response = new CommMessage(
						entry.getKey(),
						response.operationName(),
						response.resourcePath(),
						response.value(),
						response.fault()
					);
					monitor.notify();
				}
			}
		}
		
		private void handleMessage( CommMessage response )
		{
			ResponseContainer monitor;
			if ( (monitor=parent.waiters.remove( response.id() )) == null ) {
				parent.pendingResponses.put( response.id(), response );
			} else {
				synchronized( monitor ) {
					monitor.response = response;
					monitor.notify();
				}
			}
		}

		private void throwIOExceptionFault( IOException e )
		{
			if ( parent.waiters.isEmpty() == false ) {
				ResponseContainer monitor;
				for( Entry< Long, ResponseContainer > entry : parent.waiters.entrySet() ) {
					monitor = entry.getValue();
					synchronized( monitor ) {
						monitor.response = new CommMessage(
							entry.getKey(),
							"",
							"/",
							Value.create(),
							new FaultException( "IOException", e )
						);
						monitor.notify();
					}
				}
				parent.waiters.clear();
			}
		}
		
		public void run()
		{
			/*
			 * Warning: the following line implies that this
			 * whole thing is safe iff the CommChannel is used only for outputs,
			 * otherwise we are messing with correlation set checking.
			 */
			CommChannelHandler.currentThread().setExecutionThread( ethread ); // TODO: this is hacky..

			CommMessage response;
			boolean keepRun = true;
			while( keepRun ) {
				synchronized( parent.responseRecvMutex ) {
					if ( parent.waiters.isEmpty() ) {
						try {
							parent.responseRecvMutex.wait();
						} catch( InterruptedException e ) {
							Interpreter.getInstance().logSevere( e );
						}
					}
					try {
						response = parent.recv();
						if ( response.hasGenericId() ) {
							handleGenericMessage( response );
						} else {
							handleMessage( response );
						}
					} catch( IOException e ) {
						throwIOExceptionFault( e );
						keepRun = false;
					}
					if ( parent.waiters.isEmpty() ) {
						keepRun = false;
						parent.responseReceiver = null;
					}
				}
			}
		}
	}
	
	/** Sends a message through the channel. */
	public void send( CommMessage message )
		throws IOException
	{
		if ( lock.isHeldByCurrentThread() ) {
			sendImpl( message );
		} else {
			lock.lock();
			try {
				sendImpl( message );
			} catch( IOException e ) {
				lock.unlock();
				throw e;
			}
			lock.unlock();
		}
	}
	
	abstract protected CommMessage recvImpl()
		throws IOException;
	
	abstract protected void sendImpl( CommMessage message )
		throws IOException;
	
	/**
	 * Releases this CommChannel, making it available
	 * to other processes for sending data.
	 */
	final public void release()
		throws IOException
	{
		if ( toBeClosed ) {
			isOpen = false;
			close();
		} else {
			releaseImpl();
		}
	}

	protected void releaseImpl()
		throws IOException
	{
		isOpen = false;
		closeImpl();
	}

	final public void disposeForInput()
		throws IOException
	{
		disposeForInputImpl();
	}
	
	protected void disposeForInputImpl()
		throws IOException
	{}
	
	public void setToBeClosed( boolean toBeClosed )
	{
		this.toBeClosed = toBeClosed;
	}

	protected void close()
		throws IOException
	{

		closeImpl();
	}

	/** Implements the communication channel closing operation. */
	abstract protected void closeImpl()
		throws IOException;
}