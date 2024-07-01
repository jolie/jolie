package joliex.java.generate;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.ServiceNodeJava;
import jolie.lang.parse.util.ProgramInspector;
import joliex.java.generate.operation.ExceptionClassBuilder;
import joliex.java.generate.operation.InterfaceClassBuilder;
import joliex.java.generate.operation.ServiceClassBuilder;
import joliex.java.generate.type.TypeClassBuilder;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.OperationFactory;
import joliex.java.parse.TypeFactory;
import joliex.java.parse.ast.JolieOperation;

public class JavaDocumentCreator {

    public static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of( ".", "src", "main", "java" );
    public static final String DEFAULT_SOURCES_PACKAGE = ".spec";
    private static final String TYPE_PACKAGE = ".types";
    private static final String FAULT_PACKAGE = ".faults";

    private final Path outputDirectory;
    private final String sourcesPackage;
    private final Boolean overwriteServices;

    public JavaDocumentCreator( String outputDirectory, String sourcesPackage, Boolean overwriteServices ) {
        this.outputDirectory = outputDirectory == null ? DEFAULT_OUTPUT_DIRECTORY : Path.of( outputDirectory );
        this.sourcesPackage = sourcesPackage == null ? DEFAULT_SOURCES_PACKAGE : sourcesPackage;
        this.overwriteServices = overwriteServices;
    }

    public void translateServices( ProgramInspector inspector ) {
        List<ServiceNodeJava> nodes = Arrays.stream( inspector.getServiceNodes() )
            .map( n -> n instanceof ServiceNodeJava j ? j : null )
            .filter( Objects::nonNull )
            .toList();
        
        if ( nodes.isEmpty() )
            throw new IllegalArgumentException( "No Java service nodes were found in the given Jolie file." );

        nodes.forEach( this::generateServiceClass );
    }

    public void translateInterfaces( ProgramInspector inspector ) {
        if ( inspector.getInterfaces().length == 0 )
            throw new IllegalArgumentException( "No interfaces were found in the given Jolie file." );

        if ( sourcesPackage.startsWith( "." ) )
            throw new IllegalArgumentException( "When translating interfaces the source package cannot be relative (starting with \".\"), please specify an absolute package name." );

        final String interfacePackage = requireValidPackage( sourcesPackage );
        final String typePackage = sourcesPackage + TYPE_PACKAGE;
        final String faultPackage = sourcesPackage + FAULT_PACKAGE;

        final TypeFactory typeFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( typeFactory );

        final Map<String, Collection<JolieOperation>> operationsMap = Arrays.stream( inspector.getInterfaces() )
            .parallel()
            .collect( Collectors.toConcurrentMap(
                InterfaceDefinition::name, 
                id -> id.operationsMap().values()
                    .parallelStream()
                    .map( operationFactory::createOperation )
                    .toList() ) );
        generateInterfaceClasses( operationsMap, typePackage, faultPackage, interfacePackage );
        generateTypeClasses( typeFactory.getAll(), typePackage );
    }

    public void translateTypes( ProgramInspector inspector ) {
        if ( inspector.getTypes().length == 0 )
            throw new IllegalArgumentException( "No types were found in the given Jolie file." );

        if ( sourcesPackage.startsWith( "." ) )
            throw new IllegalArgumentException( "When translating types the source package cannot be relative (i.e. start with \".\"), please specify an absolute package name." );

        final TypeFactory typeFactory = new TypeFactory();
        generateTypeClasses( 
            Arrays.stream( inspector.getTypes() ).parallel().map( typeFactory::get ), 
            sourcesPackage );
    }

    private void generateServiceClass( ServiceNodeJava serviceNode ) {
        final int lastSep = serviceNode.classPath().lastIndexOf( "." );
        final String servicePackage = requireValidPackage( serviceNode.classPath() ).substring( 0, lastSep );
        final String serviceName = serviceNode.classPath().substring( lastSep+1 );

        final String interfacePackage = getAbsolutePackage( servicePackage, sourcesPackage );
        final String typePackage = getAbsolutePackage( servicePackage, sourcesPackage + TYPE_PACKAGE );
        final String faultPackage = getAbsolutePackage( servicePackage, sourcesPackage + FAULT_PACKAGE );

        final TypeFactory typeFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( typeFactory );

        final Map<String, Collection<JolieOperation>> operationsMap = serviceNode.inputPortInfo()
            .getInterfaceList()
            .parallelStream()
            .collect( Collectors.toConcurrentMap(
                InterfaceDefinition::name, 
                id -> id.operationsMap().values()
                    .parallelStream()
                    .map( operationFactory::createOperation )
                    .toList() ) );

        if ( !operationsMap.isEmpty() ) {
            generateInterfaceClasses( operationsMap, typePackage, faultPackage, interfacePackage );
            generateTypeClasses( typeFactory.getAll(), typePackage );
            JavaClassDirector.writeClass( 
                outputPath( servicePackage ),
                new ServiceClassBuilder( 
                    operationsMap, serviceName, servicePackage, 
                    typePackage, faultPackage, interfacePackage ),
                overwriteServices );
        }
    }

    private void generateInterfaceClasses( Map<String, Collection<JolieOperation>> operationsMap, String typePackage, String faultPackage, String interfacePackage ) {
        operationsMap.forEach( (name, operations) -> 
            JavaClassDirector.writeClass( 
                outputPath( interfacePackage ), 
                new InterfaceClassBuilder( operations, name, interfacePackage, typePackage, faultPackage ),
                true ) );

        generateFaultClasses( 
            operationsMap.values()
                .parallelStream()
                .flatMap( Collection::stream ), 
            typePackage, faultPackage );
    }

    private void generateFaultClasses( Stream<JolieOperation> operations, String typePackage, String faultPackage ) {
        operations.parallel()
            .map( JolieOperation::faults )
            .flatMap( Collection::stream )
            .distinct()
            .map( fault -> new ExceptionClassBuilder( fault, faultPackage, typePackage ) )
            .forEach( builder -> JavaClassDirector.writeClass( outputPath( faultPackage ), builder, true ) );
    }

    private void generateTypeClasses( Stream<JolieType> typeDefinitions, String typePackage ) {
        typeDefinitions.parallel()
            .map( definition -> TypeClassBuilder.create( definition, typePackage ) )
            .filter( Objects::nonNull )
            .forEach( builder -> JavaClassDirector.writeClass( outputPath( typePackage ), builder, true ) );
    }

    private Path outputPath( String packageName ) {
        return outputDirectory.resolve( packageName.replace( '.', '/' ) );
    }

    private static String getAbsolutePackage( String basePackage, String packageName ) {
        if ( !packageName.startsWith( "." ) )
            return packageName;

        return packageName.length() == 1
            ? basePackage
            : basePackage + packageName;
    }

    private static String requireValidPackage( String packageName ) {
        if ( !SourceVersion.isName( packageName ) )
            throw new IllegalArgumentException( "The name of a package must be a syntactically valid qualified name, got=\"" + packageName + "\"." );

        return packageName;
    }
}
