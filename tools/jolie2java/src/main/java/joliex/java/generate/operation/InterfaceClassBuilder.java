package joliex.java.generate.operation;

import java.util.Collection;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieType.Native;

public class InterfaceClassBuilder extends JavaClassBuilder {

	private final Collection< JolieOperation > operations;
	private final String className;
	private final String packageName;
	private final String typesPackage;
	private final String faultsPackage;

	public InterfaceClassBuilder( Collection< JolieOperation > operations, String className, String packageName,
		String typesPackage, String faultsPackage ) {
		this.operations = operations;
		this.className = className;
		this.packageName = packageName;
		this.typesPackage = typesPackage;
		this.faultsPackage = faultsPackage;
	}

	@Override
	public String className() {
		return className;
	}

	@Override
	public void appendPackage() {
		builder.append( "package " ).append( packageName ).append( ";" );
	}

	@Override
	public void appendDefinition() {
		builder.newNewlineAppend( "public interface " ).append( className() )
			.body( () -> operations.forEach( this::appendMethod ) );
	}

	private void appendMethod( JolieOperation operation ) {
		builder.newline();

		operation.possibleDocumentation().ifPresent( s -> builder.commentBlock( () -> builder.newlineAppend( s ) ) );

		if( operation instanceof JolieOperation.RequestResponse && operation.response() == Native.VOID )
			builder.newlineAppend( "@" ).append( ClassPath.REQUESTRESPONSE );

		builder.newlineAppend( operation.responseType( typesPackage ) ).append( " " ).append( operation.name() )
			.append( operation.requestType( typesPackage ).map( t -> "( " + t + " request )" ).orElse( "()" ) )
			.append( operation.faults().parallelStream().map( f -> faultsPackage + "." + f.className() )
				.reduce( ( n1, n2 ) -> n1 + ", " + n2 ).map( s -> " throws " + s ).orElse( "" ) )
			.append( ";" );
	}
}
