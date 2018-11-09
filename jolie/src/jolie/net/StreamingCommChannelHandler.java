/*******************************************************************************
 *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk>     *
 *   Copyright (C) 2017 by Fabrizio Montesi <famontesi@gmail.com>              *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *																																						 *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/
package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.net.URISyntaxException;
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
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.typing.TypeCheckingException;

public class StreamingCommChannelHandler
	extends SimpleChannelInboundHandler<CommMessage>
{

	private ChannelHandlerContext ctx;
	private StreamingCommChannel outChannel;  // TOWARDS THE NETWORK
	private StreamingCommChannel inChannel;   // TOWARDS JOLIE
	private final Interpreter interpreter;

	public StreamingCommChannelHandler( StreamingCommChannel channel )
	{
		this.inChannel = channel;
		this.outChannel = channel;
		this.interpreter = Interpreter.getInstance();
	}

	public void setOutChannel( StreamingCommChannel c )
	{
		this.outChannel = c;
	}

	public void setInChannel( StreamingCommChannel c )
	{
		this.inChannel = c;
	}

	public StreamingCommChannel getOutChannel()
	{
		return this.outChannel;
	}

	public StreamingCommChannel getInChannel()
	{
		return this.inChannel;
	}

	@Override
	public void channelRegistered( ChannelHandlerContext ctx ) throws Exception
	{
		super.channelRegistered( ctx );
		this.ctx = ctx;
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, CommMessage msg )
		throws Exception
	{
		if ( inChannel.parentPort() instanceof OutputPort ) {
			Interpreter.getInstance().commCore().removeRequestExecutionThread( msg.id() );
			Interpreter.getInstance().commCore().removeRequestExecutionThread( inChannel );
			Interpreter.getInstance().commCore().receiveResponse( msg );
			//this.inChannel.receiveResponse( msg );
		} else {
			messageRecv( msg );
		}
	}

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
		throws Exception
	{
		ctx.close();
		throw new Exception( cause );
	}

	public ChannelFuture write( CommMessage msg )
		throws InterruptedException
	{
		return this.ctx.writeAndFlush( msg );
	}

	public ChannelFuture close()
	{
		return this.ctx.close();
	}

	private final ReadWriteLock channelHandlersLock
		= new ReentrantReadWriteLock( true );

	private void forwardResponse( CommMessage message )
		throws IOException
	{
		message = new CommMessage(
			inChannel.redirectionMessageId(),
			message.operationName(),
			message.resourcePath(),
			message.value(),
			message.fault()
		);
		try {
			try {
				inChannel.redirectionChannel().send( message );
			} finally {
				try {
					if ( inChannel.redirectionChannel().toBeClosed() ) {
						inChannel.redirectionChannel().close();
					} else {
						inChannel.redirectionChannel().disposeForInput();
					}
				} finally {
					inChannel.setRedirectionChannel( null );
				}
			}
		} finally {
			inChannel.closeImpl();
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
		OutputPort oPort = inChannel.parentInputPort().redirectionMap().get( ss[ 1 ] );
		if ( oPort == null ) {
			String error = "Discarded a message for resource " + ss[ 1 ]
				+ ", not specified in the appropriate redirection table.";
			interpreter.logWarning( error );
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
			oChannel.setRedirectionChannel( inChannel );
			oChannel.setRedirectionMessageId( rMessage.id() );
			oChannel.send( rMessage );
			oChannel.setToBeClosed( false );
			oChannel.disposeForInput();
		} catch( IOException e ) {
			outChannel.send( CommMessage.createFaultResponse( message,
				new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ) );
			outChannel.disposeForInput();
			throw e;
		}
	}

	private void handleAggregatedInput( CommMessage message,
		AggregatedOperation operation )
		throws IOException, URISyntaxException
	{
		operation.runAggregationBehaviour( message, inChannel );
	}

	private void handleDirectMessage( CommMessage message )
		throws IOException
	{
		try {
			InputOperation operation
				= interpreter.getInputOperation( message.operationName() );
			try {
				operation.requestType().check( message.value() );
				interpreter.correlationEngine().onMessageReceive( message, inChannel );
				if ( operation instanceof OneWayOperation ) {
					// We need to send the acknowledgement
					outChannel.send( CommMessage.createEmptyResponse( message ) );
					//outChannel.release();
				}
			} catch( TypeCheckingException e ) {
				interpreter.logWarning( "Received message TypeMismatch (input operation "
					+ operation.id() + "): " + e.getMessage() );
				try {
					outChannel.send( CommMessage.createFaultResponse( message,
						new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME,
							e.getMessage() ) ) );
				} catch( IOException ioe ) {
					Interpreter.getInstance().logSevere( ioe );
				}
			} catch( CorrelationError e ) {
				interpreter.logWarning( "Received a non correlating message "
					+ "for operation " + message.operationName() + ". Sending "
					+ "CorrelationError to the caller." );
				outChannel.send( CommMessage.createFaultResponse( message,
					new FaultException( "CorrelationError", "The message you sent "
						+ "can not be correlated with any session and can not be "
						+ "used to start a new session." ) ) );
			}
		} catch( InvalidIdException e ) {
			interpreter.logWarning( "Received a message for undefined operation "
				+ message.operationName() + ". Sending IOException to the caller." );
			outChannel.send( CommMessage.createFaultResponse( message,
				new FaultException( "IOException", "Invalid operation: "
					+ message.operationName() ) ) );
		} finally {
			outChannel.disposeForInput();
		}
	}

	private final static Pattern pathSplitPattern = Pattern.compile( "/" );

	private void handleMessage( CommMessage message )
		throws IOException
	{
		try {
			String[] ss = pathSplitPattern.split( message.resourcePath() );
			if ( ss.length > 1 ) {
				handleRedirectionInput( message, ss );
			} else if ( inChannel.parentInputPort().canHandleInputOperationDirectly(
				message.operationName() ) ) {
				handleDirectMessage( message );
			} else {
				AggregatedOperation operation
					= inChannel.parentInputPort().getAggregatedOperation(
						message.operationName() );
				if ( operation == null ) {
					interpreter.logWarning(
						"Received a message for operation " + message.operationName()
						+ ", not specified in the input port at the receiving service. "
						+ "Sending IOException to the caller."
					);
					try {
						outChannel.send( CommMessage.createFaultResponse( message,
							new FaultException( "IOException", "Invalid operation: "
								+ message.operationName() ) ) );
					} finally {
						outChannel.disposeForInput();
					}
				} else {
					handleAggregatedInput( message, operation );
				}
			}
		} catch( URISyntaxException e ) {
			interpreter.logSevere( e );
		}
	}

	private void messageRecv( CommMessage message )
	{
		inChannel.lock.lock();
		channelHandlersLock.readLock().lock();
		try {
			if ( inChannel.redirectionChannel() == null ) {
				assert (inChannel.parentInputPort() != null);
				if ( message != null ) {
					handleMessage( message );
				} else {
					inChannel.disposeForInput();
				}
			}
		} catch( ChannelClosingException e ) {
			interpreter.logFine( e );
		} catch( IOException e ) {
			interpreter.logSevere( e );
			try {
				inChannel.closeImpl();
			} catch( IOException e2 ) {
				interpreter.logSevere( e2 );
			}
		} finally {
			channelHandlersLock.readLock().unlock();
			if ( inChannel.lock.isHeldByCurrentThread() ) {
				inChannel.lock.unlock();
			}
		}
	}
}
