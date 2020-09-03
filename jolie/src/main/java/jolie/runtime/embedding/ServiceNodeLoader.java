package jolie.runtime.embedding;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.lang.parse.ast.ServiceNode;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class ServiceNodeLoader extends EmbeddedServiceLoader {
	private final Interpreter currInterpreter;
	private final ServiceNode serviceNode;
	private final Expression passingParameter;
	private final Type acceptingType;
	private final static AtomicLong SERVICE_LOADER_COUNTER = new AtomicLong();

	protected ServiceNodeLoader( Expression channelDest, Interpreter currInterpreter,
		ServiceNode serviceNode, Expression passingParameter, Type acceptingType )
		throws IOException, CommandLineException {
		super( channelDest );
		this.currInterpreter = currInterpreter;
		this.serviceNode = serviceNode;
		this.passingParameter = passingParameter;
		this.acceptingType = acceptingType;
	}

	@Override
	public void load() throws EmbeddedServiceLoadingException {

		Interpreter interpreter = null;
		try {
			Value passingValue = passingParameter == null ? Value.create() : passingParameter.evaluate();
			Value pathValue = Value.create();
			if( this.serviceNode.parameterType().isPresent() ) {
				this.acceptingType.check( passingValue );
				pathValue.getChildren( this.serviceNode.parameterPath().get() ).first()
					.deepCopy( passingValue );
			}

			Interpreter.Configuration configuration = Interpreter.Configuration.create(
				currInterpreter.configuration(),
				new File( "#service_node_" + SERVICE_LOADER_COUNTER.getAndIncrement() ),
				new ByteArrayInputStream( "".getBytes() ) );

			interpreter = new Interpreter(
				configuration,
				currInterpreter.programDirectory(),
				currInterpreter,
				serviceNode.program(),
				pathValue );

			Future< Exception > f = interpreter.start();
			Exception e = f.get();
			if( e == null ) {
				setChannel( interpreter.commCore().getLocalCommChannel() );
			} else {
				throw new EmbeddedServiceLoadingException( e );
			}
		} catch( IOException | InterruptedException | ExecutionException
			| EmbeddedServiceLoadingException | TypeCheckingException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

	public String serviceName() {
		return serviceNode.name();
	}

}
