package joliex.java.generate.type;

import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public abstract class TypeClassBuilder extends JavaClassBuilder {

    protected final String className;
    protected final String packageName;
    protected final String typeFolder;

    protected TypeClassBuilder( String className, String packageName, String typeFolder ) {
        this.className = className;
        this.packageName = packageName;
        this.typeFolder = typeFolder;
    }

    public String className() { return className; }

    public void appendHeader() {
        builder.append( "package " ).append( packageName ).append( "." ).append( typeFolder ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.Value;" )
            .newlineAppend( "import jolie.runtime.ValueVector;" )
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.typing.TypeCheckingException;" )
            .newline()
            .newlineAppend( "import java.util.ArrayList;" )
            .newlineAppend( "import java.util.Map;" )
            .newlineAppend( "import java.util.SequencedCollection;" )
            .newlineAppend( "import java.util.List;" )
            .newlineAppend( "import java.util.Optional;" )
            .newlineAppend( "import java.util.Objects;" )
            .newlineAppend( "import java.util.function.Function;" )
            .newlineAppend( "import java.util.function.Predicate;" )
            .newlineAppend( "import java.util.function.UnaryOperator;" )
            .newlineAppend( "import java.util.function.BinaryOperator;" )
            .newlineAppend( "import java.util.stream.Stream;" )
            .newlineAppend( "import java.util.stream.Collectors;" )
            .newline()
            .newlineAppend( "import joliex.java.embedding.*;" )
            .newlineAppend( "import joliex.java.embedding.BasicType.*;" )
            .newlineAppend( "import joliex.java.embedding.util.*;" );
    }

    public void appendDefinition() { appendDefinition( false ); }

    public abstract void appendDefinition( boolean isInnerClass );

    private static TypeClassBuilder create( JolieType type, String packageName, String typeFolder, boolean listable ) {
        return switch ( type ) {
            case Basic.Inline b -> new BasicClassBuilder( b, packageName, typeFolder );
            case Structure.Inline s -> new StructureClassBuilder( s, packageName, typeFolder, listable );
            case Choice.Inline c -> new ChoiceClassBuilder( c, packageName, typeFolder, listable );
            default -> null;
        };
    }

    public static TypeClassBuilder create( JolieType type, String packageName, String typeFolder ) { return create( type, packageName, typeFolder, true ); }
    
    protected static TypeClassBuilder create( JolieType type, boolean listable ) { return create( type, null, null, listable ); }
}
