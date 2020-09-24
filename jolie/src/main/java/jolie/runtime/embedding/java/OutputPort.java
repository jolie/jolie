package jolie.runtime.embedding.java;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class OutputPort {
	private final Interpreter targetInterpreter;

	public OutputPort( Interpreter targetInterpreter ) {
		this.targetInterpreter = targetInterpreter;
	}

	public Interpreter targetInterpreter() {
		return targetInterpreter;
	}

	public CommChannel commChannel() {
		return this.targetInterpreter.commCore().getLocalCommChannel();
	}

	public void callOneWay( CommMessage message ) {
		try {
			CommChannel c = this.commChannel();

			c.send( message );
		} catch( IOException e ) {
			// This should never happen
			e.printStackTrace();
		}
	}

	public Value callRequestResponse( CommMessage request )
		throws FaultException {
		CommChannel c = this.commChannel();
		try {
			c.send( request );
			CommMessage response = c.recvResponseFor( request ).get();
			if( response.isFault() ) {
				throw response.fault();
			}
			return response.value();
		} catch( ExecutionException | InterruptedException | IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		}
	}

	public static OutputPort create( Interpreter interpreter, URI location ) {
		if( location == null ) {
			return new OutputPort( interpreter );
		}
		return new URIOutputPort( interpreter, location );
	}
}
