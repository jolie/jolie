package joliex.java.parse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;

import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import joliex.java.parse.util.NameFormatter;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieOperation.OneWay;
import joliex.java.parse.ast.JolieOperation.RequestResponse;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;

import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;

public class OperationFactory {

    private final TypeFactory typeFactory;

    public OperationFactory( TypeFactory typeFactory ) { this.typeFactory = typeFactory; }

    public Map<String, Collection<JolieOperation>> createOperationsMap( InterfaceDefinition[] interfaceDefinitions ) {
        return Arrays.stream( interfaceDefinitions )
            .parallel()
            .collect( Collectors.groupingByConcurrent(
                InterfaceDefinition::name,
                Collectors.flatMapping( 
                    id -> id.operationsMap().values().stream(),
                    Collectors.teeing(
                        Collectors.toList(),
                        faultCollector(),
                        ( declarations, faultMap ) -> declarations.parallelStream()
                            .map( d -> createOperation( d, faultMap ) )
                            .toList() ) ) ) );
    }

    private JolieOperation createOperation( OperationDeclaration operationDeclaration, Map<String, ? extends Map<String, Fault>> faultMap ) {
        return switch ( operationDeclaration ) {
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
                    .mapKeyValue( (n, td) -> faultMap.get( n ).get( td.name() ) )
                    .toList(),
                rr.getDocumentation().map( s -> s.isEmpty() ? null : s ).orElse( null ) );

            default -> throw new UnsupportedOperationException( "Got unexpected operation declaration." );
        };
    }

    private Collector<OperationDeclaration, ?, ConcurrentMap<String, ConcurrentMap<String, Fault>>> faultCollector() {
        return Collectors.flatMapping(
            od -> od instanceof RequestResponseOperationDeclaration rr ? rr.faults().entrySet().stream() : Stream.empty(),
            Collectors.groupingByConcurrent( 
                Map.Entry::getKey,
                Collectors.collectingAndThen(
                    MoreCollectors.distinctBy( e -> e.getValue().name() ),
                    ls -> {
                        final String name = ls.getFirst().getKey();
                        final boolean isUnique = ls.size() == 1;
                        return ls.parallelStream()
                            .map( Map.Entry::getValue )
                            .collect( Collectors.toConcurrentMap(
                                TypeDefinition::name, 
                                td -> {
                                    final JolieType type = typeFactory.get( td );
                                    final String className = name + (isUnique ? "" : switch ( type ) {
                                        case Native n -> n.toString();
                                        case Undefined u -> "Undefined";
                                        case Definition d -> d.name();
                                    });
                                    return new Fault( name, type, NameFormatter.requireValidClassName( className ) );
                                } ) );
                    } ) ) );
    }
}