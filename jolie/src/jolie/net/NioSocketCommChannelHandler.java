package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
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

/**
 *
 * @author martin
 */
@ChannelHandler.Sharable
public class NioSocketCommChannelHandler extends SimpleChannelInboundHandler<CommMessage>
{

	private ChannelHandlerContext ctx;
	private final NioSocketCommChannel channel;
	private final Interpreter interpreter;

	NioSocketCommChannelHandler( NioSocketCommChannel channel )
	{
		this.channel = channel;
		this.interpreter = Interpreter.getInstance();
	}

	@Override
	public void channelRegistered( ChannelHandlerContext ctx ) throws Exception
	{
		super.channelRegistered( ctx );
		this.ctx = ctx;
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, CommMessage msg ) throws Exception
	{
		if(channel.parentPort() instanceof OutputPort) {
			this.channel.recievedResponse( msg );
		} else {
			messageRecv( msg );
		}
	}

	protected ChannelFuture write( CommMessage msg )
	{
		return this.ctx.writeAndFlush( msg );
	}

	protected ChannelFuture close()
	{
		return ctx.close();
	}

	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );

	private void forwardResponse( CommMessage message )
		throws IOException
	{
		message = new CommMessage(
			channel.redirectionMessageId(),
			message.operationName(),
			message.resourcePath(),
			message.value(),
			message.fault()
		);
		try {
			try {
				channel.redirectionChannel().send( message );
			} finally {
				try {
					if ( channel.redirectionChannel().toBeClosed() ) {
						channel.redirectionChannel().close();
					} else {
						channel.redirectionChannel().disposeForInput();
					}
				} finally {
					channel.setRedirectionChannel( null );
				}
			}
		} finally {
			channel.closeImpl();
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
		OutputPort oPort = channel.parentInputPort().redirectionMap().get( ss[ 1 ] );
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
			oChannel.setRedirectionChannel( channel );
			oChannel.setRedirectionMessageId( rMessage.id() );
			oChannel.send( rMessage );
			oChannel.setToBeClosed( false );
			oChannel.disposeForInput();
		} catch( IOException e ) {
			channel.send( CommMessage.createFaultResponse( message, new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ) );
			channel.disposeForInput();
			throw e;
		}
	}

	private void handleAggregatedInput( CommMessage message, AggregatedOperation operation )
		throws IOException, URISyntaxException
	{
		operation.runAggregationBehaviour( message, channel );
	}

	private void handleDirectMessage( CommMessage message )
		throws IOException
	{
		try {
			InputOperation operation
				= interpreter.getInputOperation( message.operationName() );
			try {
				operation.requestType().check( message.value() );
				interpreter.correlationEngine().onMessageReceive( message, channel );
				if ( operation instanceof OneWayOperation ) {
					// We need to send the acknowledgement
					channel.send( CommMessage.createEmptyResponse( message ) );
					//channel.release();
				}
			} catch( TypeCheckingException e ) {
				interpreter.logWarning( "Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
				try {
					channel.send( CommMessage.createFaultResponse( message, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
				} catch( IOException ioe ) {
					Interpreter.getInstance().logSevere( ioe );
				}
			} catch( CorrelationError e ) {
				interpreter.logWarning( "Received a non correlating message for operation " + message.operationName() + ". Sending CorrelationError to the caller." );
				channel.send( CommMessage.createFaultResponse( message, new FaultException( "CorrelationError", "The message you sent can not be correlated with any session and can not be used to start a new session." ) ) );
			}
		} catch( InvalidIdException e ) {
			interpreter.logWarning( "Received a message for undefined operation " + message.operationName() + ". Sending IOException to the caller." );
			channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
		} finally {
			channel.disposeForInput();
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
			} else if ( channel.parentInputPort().canHandleInputOperationDirectly( message.operationName() ) ) {
				handleDirectMessage( message );
			} else {
				AggregatedOperation operation = channel.parentInputPort().getAggregatedOperation( message.operationName() );
				if ( operation == null ) {
					interpreter.logWarning(
						"Received a message for operation " + message.operationName()
						+ ", not specified in the input port at the receiving service. Sending IOException to the caller."
					);
					try {
						channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
					} finally {
						channel.disposeForInput();
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
		channel.lock.lock();
		channelHandlersLock.readLock().lock();
		try {
			if ( channel.redirectionChannel() == null ) {
				assert (channel.parentInputPort() != null);
				if ( message != null ) {
					handleMessage( message );
				} else {
					channel.disposeForInput();
				}
			}
		} catch( ChannelClosingException e ) {
			interpreter.logFine( e );
		} catch( IOException e ) {
			interpreter.logSevere( e );
			try {
				channel.closeImpl();
			} catch( IOException e2 ) {
				interpreter.logSevere( e2 );
			}
		} finally {
			channelHandlersLock.readLock().unlock();
			if ( channel.lock.isHeldByCurrentThread() ) {
				channel.lock.unlock();
			}
		}
	}

};
