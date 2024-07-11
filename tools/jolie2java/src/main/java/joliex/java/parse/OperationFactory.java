package joliex.java.parse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import joliex.java.parse.util.NameFormatter;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieOperation.OneWay;
import joliex.java.parse.ast.JolieOperation.RequestResponse;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;
import joliex.java.parse.ast.JolieType;
import one.util.streamex.EntryStream;

public class OperationFactory {

	private final TypeFactory typeFactory;
	private final ConcurrentHashMap< String, Fault > faultMap = new ConcurrentHashMap<>();

	public OperationFactory( TypeFactory typeFactory ) {
		this.typeFactory = typeFactory;
	}

	public JolieOperation createOperation( OperationDeclaration operationDeclaration ) {
		return switch( operationDeclaration ) {
		case OneWayOperationDeclaration ow -> new OneWay(
			ow.id(),
			typeFactory.get( ow.requestType() ),
			ow.getDocumentation().map( s -> s.isEmpty() ? null : s ).orElse( null ) );

		case RequestResponseOperationDeclaration rr -> new RequestResponse(
			rr.id(),
			typeFactory.get( rr.requestType() ),
			typeFactory.get( rr.responseType() ),
			EntryStream.of( rr.faults() )
				.parallel()
				.mapKeyValue( this::createFault )
				.toList(),
			rr.getDocumentation().map( s -> s.isEmpty() ? null : s ).orElse( null ) );

		default -> throw new UnsupportedOperationException( "Got unexpected operation declaration." );
		};
	}

	private Fault createFault( String name, TypeDefinition typeDefinition ) {
		final JolieType type = typeFactory.get( typeDefinition );
		return faultMap.compute( name, ( n, f ) -> {
			if( f == null )
				return new Fault( n, type, NameFormatter.requireValidClassName( n, Set.of() ) );

			if( f.type().equals( type ) )
				return f;

			throw new IllegalArgumentException(
				"Having multiple faults with the same name but different types is not allowed." );
		} );
	}
}
