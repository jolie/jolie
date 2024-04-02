package joliex.java.generate.operation;

import java.util.Collection;

import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieOperation;

public class InterfaceClassBuilder extends JavaClassBuilder {
    
    private final String className;
    private final Collection<JolieOperation> operations;
    private final String packageName;
    private final String typesFolder;
    private final String interfaceFolder;

    public InterfaceClassBuilder( String className, Collection<JolieOperation> operations, String packageName, String typesFolder, String interfaceFolder ) {
        this.className = className;
        this.operations = operations;
        this.packageName = packageName;
        this.typesFolder = typesFolder;
        this.interfaceFolder = interfaceFolder;
    }

    public String className() {
        return className;
    }

    public void appendHeader() {
        builder.append( "package " ).append( packageName ).append( "." ).append( interfaceFolder ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.FaultException;" )
            .newline()
            .newlineAppend( "import joliex.java.embedding.*;" );

        if ( typesFolder != null )
            builder.newlineAppend( "import " ).append( packageName ).append( "." ).append( typesFolder ).append( ".*;" );
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

        builder.newlineAppend( operation.responseType().orElse( "void" ) ).append( " " ).append( operation.name() ).append( operation.requestType().map( t -> "( " + t + " request )" ).orElse( "()" ) ).append( " throws FaultException;" );
    }
}
