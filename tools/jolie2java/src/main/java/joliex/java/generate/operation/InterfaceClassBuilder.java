package joliex.java.generate.operation;

import java.util.Collection;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieOperation;

public class InterfaceClassBuilder extends JavaClassBuilder {
    
    private final String className;
    private final Collection<JolieOperation> operations;
    private final String interfacesPackage;
    private final String typesPackage;

    public InterfaceClassBuilder( String className, Collection<JolieOperation> operations, String interfacesPackage, String typesPackage ) {
        this.className = className;
        this.operations = operations;
        this.interfacesPackage = interfacesPackage;
        this.typesPackage = typesPackage;
    }

    public String className() { return className; }

    public void appendPackage() {
        builder.append( "package " ).append( interfacesPackage ).append( ";" );
    }

    public void appendDefinition() {
        builder.newNewlineAppend( "public interface " ).append( className )
            .body( () -> operations.forEach( this::appendMethod ) );
    }

    private void appendMethod( JolieOperation operation ) {
        builder.newline();

        operation.possibleDocumentation().ifPresent( s -> 
            builder.commentBlock( () -> builder.newlineAppend( s ) )
        );

        builder.newlineAppend( operation.responseType( typesPackage ) ).append( " " ).append( operation.name() ).append( operation.requestType( typesPackage ).map( t -> "( " + t + " request )" ).orElse( "()" ) ).append( " throws " ).append( ClassPath.FAULTEXCEPTION ).append( ";" );
    }
}
