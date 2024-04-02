package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Basic;

public class BasicClassBuilder extends TypeClassBuilder {

    private final Basic basic;

    public BasicClassBuilder( Basic basic, String packageName, String typeFolder ) {
        super( basic.name(), packageName, typeFolder );
        this.basic = basic; 
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "record " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " rootValue ) implements StructureType" )
            .body( this::appendDefinitionBody );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is a {@link StructureType} which can be described as follows:" )
            .newline()
            .codeBlock( () -> builder
                .newlineAppend( "root: {@link " ).append( basic.nativeType().valueName() ).append( "}( " ).append( basic.refinement().definitionString() ).append( " )" )
            )
            .newline()
            .newlineAppend( "@see JolieType" )
            .newlineAppend( "@see StructureType" )
            .newlineAppend( "@see BasicType" )
            .newlineAppend( "@see #create(" ).append( basic.nativeType().valueName() ).append( ")" );
    }

    private void appendDefinitionBody() {
        appendAttributes();
        appendMethods();
        appendConstructors();
        appendStaticMethods();
    }

    private void appendAttributes() {
        builder.newNewlineAppend( "private static final List<Refinement<" ).append( basic.nativeType().valueName() ).append( ">> refinements = " ).append( basic.refinement().createString() ).append( ";" );
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " rootValue )" )
            .body( () -> builder.newlineAppend( "this.rootValue = Refinement.validated( rootValue, refinements );" ) );
    }

    private void appendMethods() {
        builder.newNewlineAppend( "public " ).append( basic.nativeType().wrapperName() ).append( " root() { return new " ).append( basic.nativeType().wrapperName() ).append("( rootValue ); }" );
        builder.newNewlineAppend( "public Map<String, List<StructureType>> children() { return Map.of(); }" );
    }

    private void appendStaticMethods() {
        builder.newline()
            .newlineAppend( "public static " ).append( className ).append( " create( " ).append( basic.nativeType().valueName() ).append( " v ) throws TypeValidationException { return new " ).append( basic.name() ).append( "( v ); }" )
            .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieType t ) throws TypeValidationException { return new " ).append( basic.name() ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".createFrom( t ).value() ); }" )
            .newline()
            .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" )
            .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException { return new " ).append( basic.name() ).append( "( " + basic.nativeType().wrapperName() + ".fromValue( value ).value() ); }" );;
    }
}
