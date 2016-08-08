/**
 * *************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com> * * This program
 * is free software; you can redistribute it and/or modify * it under the terms
 * of the GNU Library General Public License as * published by the Free Software
 * Foundation; either version 2 of the * License, or (at your option) any later
 * version. * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General
 * Public License for more details. * * You should have received a copy of the
 * GNU Library General Public * License along with this program; if not, write
 * to the * Free Software Foundation, Inc., * 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *************************************************************************
 */
package jolie.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.ports.OutputPort;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.Value;
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.typing.TypeCheckingException;

public abstract class AbstractCommChannel extends CommChannel
{
	private static final long RECEIVER_KEEP_ALIVE = 20000; // msecs

	private final Map< Long, CommMessage> pendingResponses = new HashMap<>();
	private final Map< Long, ResponseContainer> waiters = new HashMap<>();
	private final List< CommMessage> pendingGenericResponses = new LinkedList<>();

	private final Object responseRecvMutex = new Object();

	private static class ResponseContainer
	{
		private ResponseContainer()
		{
		}
		private CommMessage response = null;
	}

	/* Handle messages received on OutputPort */
	@Override
	public CommMessage recvResponseFor( CommMessage request )
		throws IOException
	{
		CommMessage response;
		ResponseContainer monitor = null;
		synchronized( responseRecvMutex ) {
			response = pendingResponses.remove( request.id() );
			if ( response == null ) {
				if ( pendingGenericResponses.isEmpty() ) {
					assert (waiters.containsKey( request.id() ) == false);
					monitor = new ResponseContainer();
					waiters.put( request.id(), monitor );
					//responseRecvMutex.notify();
				} else {
					response = pendingGenericResponses.remove( 0 );
				}
			}
		}
		if ( response == null ) {
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

	protected void recievedResponse( CommMessage response )
	{
		if ( response.hasGenericId() ) {
			handleGenericMessage( response );
		} else {
			handleMessage( response );
		}
	}

	private void handleGenericMessage( CommMessage response )
	{
		ResponseContainer monitor;
		if ( waiters.isEmpty() ) {
			pendingGenericResponses.add( response );
		} else {
			Entry< Long, ResponseContainer> entry
				= waiters.entrySet().iterator().next();
			monitor = entry.getValue();
			waiters.remove( entry.getKey() );
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
		if ( (monitor = waiters.remove( response.id() )) == null ) {
			pendingResponses.put( response.id(), response );
		} else {
			synchronized( monitor ) {
				monitor.response = response;
				monitor.notify();
			}
		}
	}

	private void throwIOExceptionFault( IOException e )
	{
		if ( waiters.isEmpty() == false ) {
			ResponseContainer monitor;
			for( Entry< Long, ResponseContainer> entry : waiters.entrySet() ) {
				monitor = entry.getValue();
				synchronized( monitor ) {
					monitor.response = new CommMessage(
						entry.getKey(),
						"",
						Constants.ROOT_RESOURCE_PATH,
						Value.create(),
						new FaultException( "IOException", e )
					);
					monitor.notify();
				}
			}
			waiters.clear();
		}
	}
	
	/* Handle messages received on InputPort */
	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );

	private void forwardResponse( CommMessage message )
		throws IOException
	{
		message = new CommMessage(
			redirectionMessageId(),
			message.operationName(),
			message.resourcePath(),
			message.value(),
			message.fault()
		);
		try {
			try {
				redirectionChannel().send( message );
			} finally {
				try {
					if ( redirectionChannel().toBeClosed() ) {
						redirectionChannel().close();
					} else {
						redirectionChannel().disposeForInput();
					}
				} finally {
					setRedirectionChannel( null );
				}
			}
		} finally {
			closeImpl();
		}
	}

	private void handleRedirectionInput( CommMessage message, String[] ss )
		throws IOException, URISyntaxException
	{
		// Redirection
		String rPath;
		if ( ss.length <= 2 ) {
			rPath = "/";
		} else {
			StringBuilder builder = new StringBuilder();
			for( int i = 2; i < ss.length; i++ ) {
				builder.append( '/' );
				builder.append( ss[ i ] );
			}
			rPath = builder.toString();
		}
		OutputPort oPort = parentInputPort().redirectionMap().get( ss[ 1 ] );
		if ( oPort == null ) {
			String error = "Discarded a message for resource " + ss[ 1 ]
				+ ", not specified in the appropriate redirection table.";
			Interpreter.getInstance().logWarning( error );
			throw new IOException( error );
		}
		try {
			CommChannel oChannel = oPort.getNewCommChannel();
			CommMessage rMessage
				= new CommMessage(
					message.id(),
					message.operationName(),
					rPath,
					message.value(),
					message.fault()
				);
			oChannel.setRedirectionChannel( this );
			oChannel.setRedirectionMessageId( rMessage.id() );
			oChannel.send( rMessage );
			oChannel.setToBeClosed( false );
			oChannel.disposeForInput();
		} catch( IOException e ) {
			send( CommMessage.createFaultResponse( message, new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ) );
			disposeForInput();
			throw e;
		}
	}

	private void handleAggregatedInput( CommMessage message, AggregatedOperation operation )
		throws IOException, URISyntaxException
	{
		operation.runAggregationBehaviour( message, this );
	}

	private void handleDirectMessage( CommMessage message )
		throws IOException
	{
		try {
			InputOperation operation
				= Interpreter.getInstance().getInputOperation( message.operationName() );
			try {
				operation.requestType().check( message.value() );
				Interpreter.getInstance().correlationEngine().onMessageReceive( message, this );
				if ( operation instanceof OneWayOperation ) {
					// We need to send the acknowledgement
					send( CommMessage.createEmptyResponse( message ) );
					//channel.release();
				}
			} catch( TypeCheckingException e ) {
				Interpreter.getInstance().logWarning( "Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
				try {
					send( CommMessage.createFaultResponse( message, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
				} catch( IOException ioe ) {
					Interpreter.getInstance().logSevere( ioe );
				}
			} catch( CorrelationError e ) {
				Interpreter.getInstance().logWarning( "Received a non correlating message for operation " + message.operationName() + ". Sending CorrelationError to the caller." );
				send( CommMessage.createFaultResponse( message, new FaultException( "CorrelationError", "The message you sent can not be correlated with any session and can not be used to start a new session." ) ) );
			}
		} catch( InvalidIdException e ) {
			Interpreter.getInstance().logWarning( "Received a message for undefined operation " + message.operationName() + ". Sending IOException to the caller." );
			send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
		} finally {
			disposeForInput();
		}
	}

	private final static Pattern pathSplitPattern = Pattern.compile( "/" );

	private void handleRecvMessage( CommMessage message )
		throws IOException
	{
		try {
			String[] ss = pathSplitPattern.split( message.resourcePath() );
			if ( ss.length > 1 ) {
				handleRedirectionInput( message, ss );
			} else if ( parentInputPort().canHandleInputOperationDirectly( message.operationName() ) ) {
				handleDirectMessage( message );
			} else {
				AggregatedOperation operation = parentInputPort().getAggregatedOperation( message.operationName() );
				if ( operation == null ) {
					Interpreter.getInstance().logWarning(
						"Received a message for operation " + message.operationName()
						+ ", not specified in the input port at the receiving service. Sending IOException to the caller."
					);
					try {
						send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
					} finally {
						disposeForInput();
					}
				} else {
					handleAggregatedInput( message, operation );
				}
			}
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		}
	}

	protected void messageRecv( CommMessage message )
	{
		assert (parentInputPort() != null);
		lock.lock();
		channelHandlersLock.readLock().lock();
		try {
			if ( redirectionChannel() == null ) {
				if ( message != null ) {
					handleRecvMessage( message );
				} else {
					disposeForInput();
				}
			} else {
				lock.unlock();
				CommMessage response = null;
				try {
					response = recvResponseFor( new CommMessage( redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, null ) );
				} finally {
					if ( response == null ) {
						response = new CommMessage( redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, new FaultException( "IOException", "Internal server error" ) );
					}
					forwardResponse( response );
				}
			}
		} catch( ChannelClosingException e ) {
			Interpreter.getInstance().logFine( e );
		} catch( IOException e ) {
			Interpreter.getInstance().logSevere( e );
			try {
				closeImpl();
			} catch( IOException e2 ) {
				Interpreter.getInstance().logSevere( e2 );
			}
		} finally {
			channelHandlersLock.readLock().unlock();
			if ( lock.isHeldByCurrentThread() ) {
				lock.unlock();
			}
		}
	}
}
