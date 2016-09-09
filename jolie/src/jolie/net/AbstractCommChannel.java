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
import jolie.ExecutionContext;
import jolie.Interpreter;
import jolie.StatefulContext;
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
	private final Map< Long, ExecutionContext> waiters = new HashMap<>();
	private final List< CommMessage> pendingGenericResponses = new LinkedList<>();

	private final Object responseRecvMutex = new Object();

	/* Handle messages received on OutputPort */
	@Override
	public CommMessage recvResponseFor( ExecutionContext ctx, CommMessage request )
		throws IOException
	{
		CommMessage response;
		
		synchronized( responseRecvMutex ) {
			response = pendingResponses.remove( request.id() );
			if ( response == null ) {
				if ( pendingGenericResponses.isEmpty() ) {
					assert (waiters.containsKey( request.id() ) == false);
					waiters.put( request.id(), ctx );
					//responseRecvMutex.notify();
				} else {
					response = pendingGenericResponses.remove( 0 );
				}
			}
		}
		
		return response;
//		if ( response == null ) {
//			synchronized( monitor ) {
//				if ( monitor.response == null ) {
//					try {
//						monitor.wait();
//					} catch( InterruptedException e ) {
//						Interpreter.getInstance().logSevere( e );
//					}
//				}
//				response = monitor.response;
//			}
//		}
//		return response;
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
		ExecutionContext waitingContext;
		// Add the message in any case, so that the message can be fetched from 
		// the queue by the ExecutionContext.
		pendingGenericResponses.add( response );
		if ( !waiters.isEmpty() ) {
			Entry< Long, ExecutionContext> entry 
				= waiters.entrySet().iterator().next();
			waitingContext = entry.getValue();
			waiters.remove( entry.getKey() );
			waitingContext.start();
//			synchronized( waitingContext ) {
//				monitor.response = new CommMessage(
//					entry.getKey(),
//					response.operationName(),
//					response.resourcePath(),
//					response.value(),
//					response.fault()
//				);
//				monitor.notify();
//			}
		}
	}

	private void handleMessage( CommMessage response )
	{
		ExecutionContext waitingContext;
		// Add to queue in any case. See handleGenericMessage.
		pendingResponses.put( response.id(), response );
		if ( (waitingContext = waiters.remove( response.id() )) != null ) {
			waitingContext.start();
		}
	}

	private void throwIOExceptionFault( IOException e )
	{
		if ( waiters.isEmpty() == false ) {
			ExecutionContext waitingContext;
			for( Entry< Long, ExecutionContext> entry : waiters.entrySet() ) {
				waitingContext = entry.getValue();
				pendingResponses.put( entry.getKey(),
					new CommMessage(
						entry.getKey(),
						"",
						Constants.ROOT_RESOURCE_PATH,
						Value.create(),
						new FaultException( "IOException", e )
					)
				);
				waitingContext.start();
			}
			waiters.clear();
		}
	}
	
	/* Handle messages received on InputPort */
	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );

	private void forwardResponse( StatefulContext ctx, CommMessage message )
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
				redirectionChannel().send( message, ctx );
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

	private void handleRedirectionInput( StatefulContext ctx, CommMessage message, String[] ss )
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
			ctx.interpreter().logWarning( error );
			throw new IOException( error );
		}
		try {
			CommChannel oChannel = oPort.getNewCommChannel( ctx );
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
			oChannel.send( rMessage, ctx );
			oChannel.setToBeClosed( false );
			oChannel.disposeForInput();
		} catch( IOException e ) {
			send( CommMessage.createFaultResponse( message, new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ), ctx );
			disposeForInput();
			throw e;
		}
	}

	private void handleAggregatedInput( StatefulContext ctx, CommMessage message, AggregatedOperation operation )
		throws IOException, URISyntaxException
	{
		operation.runAggregationBehaviour( ctx, message, this );
	}

	private void handleDirectMessage( StatefulContext ctx, CommMessage message )
		throws IOException
	{
		try {
			InputOperation operation
				= ctx.interpreter().getInputOperation( message.operationName() );
			try {
				operation.requestType().check( message.value() );
				ctx.interpreter().correlationEngine().onMessageReceive( message, this );
				if ( operation instanceof OneWayOperation ) {
					// We need to send the acknowledgement
					send( CommMessage.createEmptyResponse( message ), ctx );
					//channel.release();
				}
			} catch( TypeCheckingException e ) {
				ctx.interpreter().logWarning( "Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
				try {
					send( CommMessage.createFaultResponse( message, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ), ctx );
				} catch( IOException ioe ) {
					ctx.interpreter().logSevere( ioe );
				}
			} catch( CorrelationError e ) {
				ctx.interpreter().logWarning( "Received a non correlating message for operation " + message.operationName() + ". Sending CorrelationError to the caller." );
				send( CommMessage.createFaultResponse( message, new FaultException( "CorrelationError", "The message you sent can not be correlated with any session and can not be used to start a new session." ) ), ctx );
			}
		} catch( InvalidIdException e ) {
			ctx.interpreter().logWarning( "Received a message for undefined operation " + message.operationName() + ". Sending IOException to the caller." );
			send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ), ctx );
		} finally {
			disposeForInput();
		}
	}

	private final static Pattern pathSplitPattern = Pattern.compile( "/" );

	private void handleRecvMessage( StatefulContext ctx, CommMessage message )
		throws IOException
	{
		try {
			String[] ss = pathSplitPattern.split( message.resourcePath() );
			if ( ss.length > 1 ) {
				handleRedirectionInput( ctx, message, ss );
			} else if ( parentInputPort().canHandleInputOperationDirectly( message.operationName() ) ) {
				handleDirectMessage( ctx, message );
			} else {
				AggregatedOperation operation = parentInputPort().getAggregatedOperation( message.operationName() );
				if ( operation == null ) {
					Interpreter.getInstance().logWarning(
						"Received a message for operation " + message.operationName()
						+ ", not specified in the input port at the receiving service. Sending IOException to the caller."
					);
					try {
						send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ), ctx );
					} finally {
						disposeForInput();
					}
				} else {
					handleAggregatedInput( ctx, message, operation );
				}
			}
		} catch( URISyntaxException e ) {
			ctx.interpreter().logSevere( e );
		}
	}

	protected void messageRecv( StatefulContext ctx, CommMessage message )
	{
		assert (parentInputPort() != null);
		lock.lock();
		channelHandlersLock.readLock().lock();
		try {
			if ( redirectionChannel() == null ) {
				if ( message != null ) {
					handleRecvMessage( ctx, message );
				} else {
					disposeForInput();
				}
			} else {
				lock.unlock();
				CommMessage response = null;
				try {
					response = recvResponseFor( ctx, new CommMessage( redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, null ) );
				} finally {
					if ( response == null ) {
						response = new CommMessage( redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, new FaultException( "IOException", "Internal server error" ) );
					}
					forwardResponse( ctx, response );
				}
			}
		} catch( ChannelClosingException e ) {
			ctx.interpreter().logFine( e );
		} catch( IOException e ) {
			ctx.interpreter().logSevere( e );
			try {
				closeImpl();
			} catch( IOException e2 ) {
				ctx.interpreter().logSevere( e2 );
			}
		} finally {
			channelHandlersLock.readLock().unlock();
			if ( lock.isHeldByCurrentThread() ) {
				lock.unlock();
			}
		}
	}
}
