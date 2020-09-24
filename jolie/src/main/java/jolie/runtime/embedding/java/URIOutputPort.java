package jolie.runtime.embedding.java;

import java.io.IOException;
import java.net.URI;
import jolie.Interpreter;
import jolie.net.CommChannel;

public class URIOutputPort extends OutputPort {
	private final URI location;

	public URIOutputPort( Interpreter targetInterpreter, URI location ) {
		super( targetInterpreter );
		this.location = location;
	}

	@Override
	public CommChannel commChannel() {
		try {
			CommChannel commChannel = super.targetInterpreter().commCore().createCommChannel( location, null );
			return commChannel;
		} catch( IOException e ) {
			// this should never happen, location is validated at service creation
			e.printStackTrace();
			return null;
		}
	}
}
