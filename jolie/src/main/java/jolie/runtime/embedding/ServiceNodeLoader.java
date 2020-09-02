package jolie.runtime.embedding;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.BasicType;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class ServiceNodeLoader extends EmbeddedServiceLoader {
	private final Interpreter currInterpreter;
	private final ServiceNode serviceNode;
	private final Expression passingParameter;
	private final static AtomicLong SERVICE_LOADER_COUNTER = new AtomicLong();

	protected ServiceNodeLoader( Expression channelDest, Interpreter currInterpreter,
		ServiceNode serviceNode, Expression passingParameter ) throws IOException, CommandLineException {
		super( channelDest );
		this.currInterpreter = currInterpreter;
		this.serviceNode = serviceNode;
		this.passingParameter = passingParameter;
	}

	@Override
	public void load() throws EmbeddedServiceLoadingException {

		Interpreter interpreter = null;
		try {
			Value passingValue = passingParameter == null ? Value.create() : passingParameter.evaluate();
			Value pathValue = Value.create();
			if( this.serviceNode.parameterType().isPresent() ) {
				Type acceptedType = buildType( this.serviceNode.parameterType().get() );
				acceptedType.check( passingValue );
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

	private Type buildType( TypeDefinition typeDefinition ) {
		if( typeDefinition instanceof TypeDefinitionLink ) {
			return buildType( (TypeDefinitionLink) typeDefinition );
		} else if( typeDefinition instanceof TypeInlineDefinition ) {
			return buildType( (TypeInlineDefinition) typeDefinition );
		} else if( typeDefinition instanceof TypeChoiceDefinition ) {
			return buildType( (TypeChoiceDefinition) typeDefinition );
		}
		return null; // dead code
	}

	private Type buildType( TypeDefinitionLink n ) {
		return buildType( n.linkedType() );
	}


	private Type buildType( TypeInlineDefinition n ) {
		Type t;
		if( n.untypedSubTypes() ) {
			t = Type.create( BasicType.fromBasicTypeDefinition( n.basicType() ), n.cardinality(), true, null );
		} else {
			Map< String, Type > subTypes = new HashMap<>();
			if( n.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
					subTypes.put( entry.getKey(), buildType( entry.getValue() ) );
				}
			}
			t = Type.create( BasicType.fromBasicTypeDefinition( n.basicType() ), n.cardinality(), false, subTypes );
		}
		return t;
	}

	private Type buildType( TypeChoiceDefinition n ) {
		return Type.createChoice( n.cardinality(), buildType( n.left() ), buildType( n.right() ) );
	}

}
