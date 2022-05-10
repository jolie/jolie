package jolie.runtime.expression;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.monitoring.events.OperationCallEvent;
import jolie.monitoring.events.OperationReplyEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.process.TransformationReason;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class SolicitResponseExpression implements Expression {
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression;
	private final RequestResponseTypeDescription types;

	public SolicitResponseExpression( String operationId, OutputPort outputPort, Expression outputExpression,
		RequestResponseTypeDescription types ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.types = types;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new SolicitResponseExpression( operationId, outputPort, outputExpression, types );
	}

	@Override
	public Value evaluate() throws FaultException {
		// TODO implement all exception cases
		try {
			CommMessage message =
				CommMessage.createRequest( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );


			if( types.requestType() != null ) {
				try {
					types.requestType().check( message.value() );
				} catch( TypeCheckingException e ) {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( message.id() ), OperationCallEvent.FAULT, "TypeMismatch",
								outputPort.id(), message.value() ) );
					}
					throw (e);
				}
			}

			CommChannel channel = outputPort.getCommChannel();
			channel.send( message );

			if( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance().fireMonitorEvent(
					new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(),
						Long.toString( message.id() ),
						OperationCallEvent.SUCCESS, "", outputPort.id(), message.value() ) );
			}

			CommMessage response = null;
			while( response == null ) {
				try {
					response = channel.recvResponseFor( message ).get( Interpreter.getInstance().responseTimeout(),
						TimeUnit.MILLISECONDS );
				} catch( InterruptedException e ) {
					throw new IOException( e );
				} catch( ExecutionException e ) {
					if( e.getCause() instanceof IOException ) {
						throw (IOException) e.getCause();
					} else {
						throw new IOException( e.getCause() );
					}
				}
			}

			// Check to see if it there is Fault in response

			if( response.isFault() ) {
				Type faultType = types.getFaultType( response.fault().faultName() );
				if( faultType != null ) {
					try {
						faultType.check( response.fault().value() );
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent(
								new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.id() ), OperationReplyEvent.FAULT,
									response.fault().faultName(), outputPort.id(), response.value() ) );
						}
					} catch( TypeCheckingException e ) {
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent(
								new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.id() ), OperationReplyEvent.FAULT,
									"TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(),
									outputPort.id(), response.fault().value() ) );
						}
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
							"Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@"
								+ outputPort.id() + "): " + e.getMessage() );
					}
				} else {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( response.id() ), OperationReplyEvent.FAULT, response.fault().faultName(),
								outputPort.id(), response.fault().value() ) );
					}
				}
				throw response.fault();
			}


			return response.value();
		} catch( IOException e ) {
			throw new FaultException( Constants.TIMEOUT_EXCEPTION_FAULT_NAME );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TimeoutException e ) {
			throw new FaultException( Constants.TIMEOUT_EXCEPTION_FAULT_NAME );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
				"Output message TypeMismatch (" + operationId + "@" + outputPort.id() + ") " + e.getMessage() );
		}
		return null;
	}
}