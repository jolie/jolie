package joliex.java.parse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.NativeRefinement;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import joliex.java.parse.util.AnnotationParser;
import joliex.java.parse.util.NameFormatter;
import joliex.java.parse.util.NameSupplier;
import one.util.streamex.EntryStream;

public class TypeFactory {

    private static final boolean INLINE_LINK_DEFAULT = false;
    private static final boolean HAS_BUILDER_DEFAULT = true;
    private final ConcurrentMap<String, CompletableFuture<JolieType>> typeMap = new ConcurrentHashMap<>();

    public Stream<JolieType> getAll( TypeDefinition[] typeDefinitions ) {
        return Arrays.stream( typeDefinitions ).parallel().map( this::get );
    }

    public JolieType get( TypeDefinition typeDefinition ) {
        return create( typeDefinition ).join();
    } 

    private CompletableFuture<JolieType> create( TypeDefinition typeDefinition ) {
        return typeMap.computeIfAbsent( 
            typeDefinition.name(), 
            n -> CompletableFuture.supplyAsync( () -> parse( typeDefinition, n ) ) );
    }

    private JolieType parse( TypeDefinition typeDefinition, String name ) { 
        return parse( typeDefinition, NameSupplier.from( name, typeDefinition ), false ); 
    }

    private JolieType parse( TypeDefinition typeDefinition, NameSupplier nameSupplier, boolean isInner ) {
        return switch ( typeDefinition ) {

            case TypeDefinitionUndefined ud -> Undefined.getInstance();

            case TypeInlineDefinition id -> parseInline( id, nameSupplier, isInner );

            case TypeChoiceDefinition cd -> parseChoice( cd, nameSupplier );

            case TypeDefinitionLink ld -> isInner ? parseLink( ld ) : parseAlias( ld, nameSupplier );

            default -> throw new UnsupportedOperationException( "Couldn't parse the type \"" + typeDefinition.name() + "\", the given class was not supported: " + typeDefinition.getClass().getName() );
        };
    }

    private JolieType parseInline( TypeInlineDefinition inlineDefinition, NameSupplier nameSupplier, boolean isInner ) {
        final Native type = Native.get( inlineDefinition.basicType().nativeType() );
        final NativeRefinement refinement = NativeRefinement.create( inlineDefinition.basicType().refinements() );

        if ( inlineDefinition.hasSubTypes() )
            return new Structure.Inline.Typed( nameSupplier.get(), type, refinement, 
                CompletableFuture.supplyAsync( () -> createFields( inlineDefinition, nameSupplier ) ),
                hasBuilder( inlineDefinition ) );

        if ( inlineDefinition.untypedSubTypes() )
            return new Structure.Inline.Untyped( nameSupplier.get(), type, refinement, hasBuilder( inlineDefinition ) );

        if ( refinement != null )
            return new Basic.Inline( isInner ? null : nameSupplier.get(), type, refinement );
        
        return type;
    }

    private JolieType parseChoice( TypeChoiceDefinition choiceDefinition, NameSupplier nameSupplier ) {
        return new Choice.Inline( nameSupplier.get(), 
            CompletableFuture.supplyAsync( () -> createOptions( choiceDefinition, new AtomicInteger() ) ),
            hasBuilder( choiceDefinition ) );
    }

    private JolieType parseLink( TypeDefinitionLink linkDefinition ) {
        if ( inlineLink( linkDefinition ) )
            return get( unpackLink( linkDefinition ) );

        final JolieType t = get( linkDefinition.linkedType() );
        return switch ( t ) {
            case Basic.Inline i -> new Basic.Link( i );
            case Structure.Inline i -> new Structure.Link( i );
            case Choice.Inline i -> new Choice.Link( i );
            default -> t;
        };
    }

    private JolieType parseAlias( TypeDefinitionLink linkDefinition, NameSupplier nameSupplier ) {
        final JolieType t = get( unpackLink( linkDefinition ) );
        return inlineLink( linkDefinition ) ? t : switch ( t ) {
            case Basic.Inline b -> new Basic.Inline( nameSupplier.get(), b.nativeType(), b.refinement() );
            case Structure.Inline.Typed s -> new Structure.Inline.Typed( nameSupplier.get(), s.nativeType(), s.nativeRefinement(), s.fieldsFuture(), s.hasBuilder() );
            case Structure.Inline.Untyped u -> new Structure.Inline.Untyped( nameSupplier.get(), u.nativeType(), u.nativeRefinement(), u.hasBuilder() );
            case Choice.Inline c -> new Choice.Inline( nameSupplier.get(), c.optionsFuture(), c.hasBuilder() );
            default -> t;
        };
    }

    private List<Structure.Field> createFields( TypeInlineDefinition inlineDefinition, NameSupplier nameSupplier ) {
        return EntryStream.of( inlineDefinition.subTypes().stream() )
            .parallel()
            .mapToKey( NameFormatter::getJavaName )
            .mapKeys( NameFormatter::requireValidFieldName )
            .mapKeyValue( (n,td) -> parseField( td, n, nameSupplier ) )
            .toList();
    }

    private Structure.Field parseField( TypeDefinition typeDefinition, String javaName, NameSupplier nameSupplier ) {
        return new Structure.Field(
            typeDefinition.name(),
            javaName,
            typeDefinition.cardinality().min(),
            typeDefinition.cardinality().max(),
            parse( typeDefinition, nameSupplier.resolve( () -> StringUtils.capitalize( javaName ) ), true ) );
    }

    private List<JolieType> createOptions( TypeChoiceDefinition choiceDefinition, AtomicInteger structureCount ) {
        return unfoldChoice( choiceDefinition )
            .map( td -> switch ( td ) {
                case TypeInlineDefinition id -> parseInline( id, new NameSupplier( () -> "S" + structureCount.incrementAndGet() ), true );
                case TypeDefinitionLink ld -> parseLink( ld );
                default -> throw new IllegalArgumentException( choiceDefinition.name() + " contained an option of an unexpected type, class=" + td.getClass().getName() + "." );
            } )
            .toList();
    }

    private static Stream<TypeDefinition> unfoldChoice( TypeChoiceDefinition choiceDefinition ) {
        return Stream.concat(
            unpackOption( choiceDefinition.left() ),
            unpackOption( choiceDefinition.right() ) );
    }

    private static Stream<TypeDefinition> unpackOption( TypeDefinition optionDefinition ) {
        return switch ( optionDefinition ) {
            case TypeChoiceDefinition choiceDefinition -> unfoldChoice( choiceDefinition ); 
            case TypeDefinitionLink linkDefinition when inlineLink( linkDefinition ) -> unpackOption( linkDefinition.linkedType() ); // TODO: make this resistent against looping choice types
            default -> Stream.of( optionDefinition );
        };
    }

    private static TypeDefinition unpackLink( TypeDefinitionLink linkDefinition ) { return unpackLink( linkDefinition, new HashSet<>() ); }
    private static TypeDefinition unpackLink( TypeDefinitionLink linkDefinition, Set<String> linkChain ) {
        if ( !linkChain.add( linkDefinition.name() ) )
            throw new IllegalArgumentException( "Found loop while parsing link, looping set: " + linkChain.parallelStream().reduce( (s1, s2) -> s1 + ", " + s2 ).map( s -> "{ " + s + ", " + linkDefinition.name() + " }" ).orElseThrow() );

        return linkDefinition.linkedType() instanceof TypeDefinitionLink ld
            ? unpackLink( ld, linkChain )
            : linkDefinition.linkedType();
    }

    private static boolean inlineLink( TypeDefinitionLink linkDefinition ) {
        return AnnotationParser.parseInlineLink( linkDefinition ).orElse( INLINE_LINK_DEFAULT );
    }

    private static boolean hasBuilder( TypeDefinition typeDefinition ) {
        return AnnotationParser.parseGenerateBuilder( typeDefinition ).orElse( HAS_BUILDER_DEFAULT );
    }
}
