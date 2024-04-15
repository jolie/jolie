package joliex.java.parse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

import one.util.streamex.EntryStream;

public class TypeFactory {

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
            n -> CompletableFuture.supplyAsync( () -> parse( n, typeDefinition ) )
        );
    }

    private JolieType parse( String name, TypeDefinition typeDefinition ) { return parse( NameFormatter.typeClassNameSupplier( name ), typeDefinition, false ); }
    private JolieType parse( Supplier<String> nameSupplier, TypeDefinition typeDefinition, boolean isInner ) {
        return switch ( typeDefinition ) {

            case TypeDefinitionUndefined ud -> Undefined.getInstance();

            case TypeInlineDefinition id -> parseInline( nameSupplier, id );

            case TypeChoiceDefinition cd -> parseChoice( nameSupplier, cd );

            case TypeDefinitionLink ld -> isInner ? parseLink( ld ) : parseAlias( nameSupplier, ld );

            default -> throw new UnsupportedOperationException( "Parsing the given TypeDefinition isn't supported: " + typeDefinition.getClass().getName() );
        };
    }

    private JolieType parseInline( Supplier<String> nameSupplier, TypeInlineDefinition inlineDefinition ) {
        final Native type = Native.get( inlineDefinition.basicType().nativeType() );
        final NativeRefinement refinement = NativeRefinement.create( inlineDefinition.basicType().refinements() );

        if ( inlineDefinition.hasSubTypes() )
            return new Structure.Inline.Typed( nameSupplier.get(), type, refinement, createFields( inlineDefinition ) );

        if ( inlineDefinition.untypedSubTypes() )
            return new Structure.Inline.Untyped( nameSupplier.get(), type, refinement );

        if ( refinement != null )
            return new Basic.Inline( nameSupplier.get(), type, refinement );
        
        return type;
    }

    private JolieType parseChoice( Supplier<String> nameSupplier, TypeChoiceDefinition choiceDefinition ) {
        return new Choice.Inline( nameSupplier.get(), createOptions( choiceDefinition, new AtomicInteger() ) );
    }

    private JolieType parseLink( TypeDefinitionLink linkDefinition ) {
        final JolieType t = get( linkDefinition.linkedType() );
        return switch ( t ) {
            case Basic.Inline i -> new Basic.Link( i );
            case Structure.Inline i -> new Structure.Link( i );
            case Choice.Inline i -> new Choice.Link( i );
            default -> t;
        };
    }

    private JolieType parseAlias( Supplier<String> nameSupplier, TypeDefinitionLink linkDefinition ) {
        final JolieType t = get( unpackLink( linkDefinition ) );
        return switch ( t ) {
            case Basic.Inline b -> new Basic.Inline( nameSupplier.get(), b.nativeType(), b.refinement() );
            case Structure.Inline.Typed s -> new Structure.Inline.Typed( nameSupplier.get(), s.nativeType(), s.nativeRefinement(), s.fields() );
            case Structure.Inline.Untyped u -> new Structure.Inline.Untyped( nameSupplier.get(), u.nativeType(), u.nativeRefinement() );
            case Choice.Inline c -> new Choice.Inline( nameSupplier.get(), c.options() );
            default -> t;
        };
    }

    @SuppressWarnings("resource")
    private List<Structure.Field> createFields( TypeInlineDefinition inlineDefinition ) {
        return EntryStream.of( inlineDefinition.subTypes().stream() )
            .parallel()
            .mapKeys( NameFormatter::cleanFieldName )
            .chain( s -> EntryStream.of( s.grouping() ).parallel() )
            .flatMapKeyValue( (n, tds) -> tds.stream().map( td -> parseField( NameFormatter.getFieldName( n, td.name(), tds.size() == 1 ), td ) ) )
            .toList();
    }

    private Structure.Field parseField( String formattedName, TypeDefinition typeDefinition ) {
        return new Structure.Field(
            typeDefinition.name(),
            formattedName,
            CompletableFuture.supplyAsync( () -> parse( 
                NameFormatter.typeClassNameSupplier( formattedName ), 
                typeDefinition,
                true
            ) ),
            typeDefinition.cardinality().min(),
            typeDefinition.cardinality().max()
        );
    }

    private List<Choice.Option> createOptions( TypeChoiceDefinition choiceDefinition, AtomicInteger structureCount ) {
        return unfoldChoice( choiceDefinition )
            .sequential()
            .map( td -> switch ( td ) {
                case TypeInlineDefinition id -> {
                    final String n = id.hasSubTypes() || id.untypedSubTypes() ? "S" + structureCount.incrementAndGet() : null;
                    yield new Choice.Option( CompletableFuture.completedFuture( parseInline( () -> n, id ) ) );
                }
                case TypeDefinitionLink ld -> new Choice.Option( CompletableFuture.supplyAsync( () -> parseLink( ld ) ) );
                default -> throw new IllegalArgumentException( "Got a choice option of an unexpected type, class " + td.getClass().getName() + "." );
            } )
            .toList();
    }

    private static Stream<TypeDefinition> unfoldChoice( TypeChoiceDefinition choiceDefinition ) {
        return Stream.concat(
            unpackOption( choiceDefinition.left() ),
            unpackOption( choiceDefinition.right() )
        );
    }

    private static Stream<TypeDefinition> unpackOption( TypeDefinition optionDefinition ) {
        return optionDefinition instanceof TypeChoiceDefinition choiceDefinition
            ? unfoldChoice( choiceDefinition )
            : Stream.of( optionDefinition );
    }

    private static TypeDefinition unpackLink( TypeDefinitionLink linkDefinition ) { return unpackLink( linkDefinition, new HashSet<>() ); }
    private static TypeDefinition unpackLink( TypeDefinitionLink linkDefinition, Set<String> linkChain ) {
        if ( !linkChain.add( linkDefinition.name() ) )
            throw new IllegalArgumentException( "Trying to create link which never links to an actual type." );

        return linkDefinition.linkedType() instanceof TypeDefinitionLink ld
            ? unpackLink( ld, linkChain )
            : linkDefinition.linkedType();
    }
}
