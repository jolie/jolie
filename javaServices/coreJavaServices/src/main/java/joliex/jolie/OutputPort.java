package joliex.jolie;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class OutputPort {
    private Interpreter interpreter;

    public OutputPort( Interpreter targetInterpreter ) {
        this.interpreter = targetInterpreter;
    }

    public Value callRequestResponse( CommMessage request ) throws FaultException {
        LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
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

    public void callOneWay( CommMessage message ) throws FaultException {
        LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
        try {
            c.send( message );
        } catch( IOException e ) {
            // This should never happen
            e.printStackTrace();
        }
    }
}
