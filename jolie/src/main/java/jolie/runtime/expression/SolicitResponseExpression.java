package jolie.runtime.expression;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.process.TransformationReason;
import jolie.runtime.Value;

public class SolicitResponseExpression implements Expression {
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression;

	public SolicitResponseExpression( String operationId, OutputPort outputPort, Expression outputExpression ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
	}

	@Override
	public Value evaluate() {
		// TODO implement all exception cases
		try {
			CommMessage message =
				CommMessage.createRequest( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );

			CommChannel channel = outputPort.getCommChannel();
			channel.send( message );

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
			/*
			 * if( response.isFault() ) { throw response.fault(); }
			 */



			return response.value();
		} catch( IOException e ) {

		} catch( URISyntaxException e ) {

		} catch( TimeoutException e ) {

		}
		return null;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new SolicitResponseExpression( operationId, outputPort, outputExpression );
	}

}
