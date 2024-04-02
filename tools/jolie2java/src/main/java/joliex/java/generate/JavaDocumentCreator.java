package joliex.java.generate;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import jolie.lang.Constants;
import jolie.lang.parse.util.ProgramInspector;

import joliex.java.generate.operation.ExceptionClassBuilder;
import joliex.java.generate.operation.InterfaceClassBuilder;
import joliex.java.generate.operation.ServiceClassBuilder;
import joliex.java.generate.type.TypeClassBuilder;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.OperationFactory;
import joliex.java.parse.TypeFactory;
import joliex.java.parse.ast.JolieOperation;

import one.util.streamex.EntryStream;

public class JavaDocumentCreator {

    public static final String DEFAULT_OUTPUT_DIRECTORY = "./generated";
    public static final String DEFAULT_SERVICE_NAME = "MainService";
    public static final String DEFAULT_TYPE_PACKAGE = "types";
    public static final String DEFAULT_FAULT_PACKAGE = "faults";
    public static final String DEFAULT_INTERFACE_PACKAGE = "interfaces";
    private final String packageName;
    private final String packageDirectory;
    private final String typesPackage;
    private final String typesDirectory;
    private final String faultsPackage;
    private final String faultsDirectory;
    private final String interfacesPackage;
    private final String interfacesDirectory;
    private final boolean generateService;
    private final String serviceName;

    public JavaDocumentCreator( String packageName, String typesPackage, String faultsPackage, String interfacesPackage, String outputDirectory, boolean generateService, String serviceName ) {
        final String od = Optional.ofNullable( outputDirectory )
            .map( d -> d.endsWith( Constants.FILE_SEPARATOR ) 
                ? d.substring( 0, d.length()-1 ) 
                : d 
            ).orElse( DEFAULT_OUTPUT_DIRECTORY );

        this.packageName = packageName.replaceAll( "-", "_" );
        packageDirectory = getDirectory( od, this.packageName );

        this.typesPackage = Optional.ofNullable( typesPackage ).map( n -> n.replaceAll( "-", "_" ) ).orElse( DEFAULT_TYPE_PACKAGE );
        this.typesDirectory = getDirectory( packageDirectory, this.typesPackage );

        this.faultsPackage = Optional.ofNullable( faultsPackage ).map( n -> n.replaceAll( "-", "_" ) ).orElse( DEFAULT_FAULT_PACKAGE );
        this.faultsDirectory = getDirectory( packageDirectory, this.faultsPackage );

        this.interfacesPackage = Optional.ofNullable( interfacesPackage ).map( n -> n.replaceAll( "-", "_" ) ).orElse( DEFAULT_INTERFACE_PACKAGE );
        this.interfacesDirectory = getDirectory( packageDirectory, this.interfacesPackage );

        this.generateService = generateService;
        this.serviceName = serviceName == null ? DEFAULT_SERVICE_NAME : serviceName;

        createDirectory( packageDirectory );
    }

    public void generateClasses( ProgramInspector inspector ) {
        final TypeFactory definitionFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( definitionFactory );

        final boolean generatedTypes = generateTypeClasses( definitionFactory.getAll( inspector.getTypes() ) );

        final var operationsMap = operationFactory.createOperationsMap( inspector.getInterfaces() );

        final boolean generatedExceptions = generateExceptionClasses( operationsMap );
        final boolean generatedInterfaces = generateInterfaceClasses( operationsMap, generatedTypes );

        if ( generateService && !operationsMap.isEmpty() )
            generateServiceClass( operationsMap, generatedTypes, generatedExceptions, generatedInterfaces );
    }

    private boolean generateTypeClasses( Stream<JolieType> typeDefinitions ) {
        final AtomicBoolean generated = new AtomicBoolean( false );

        typeDefinitions.parallel()
            .map( definition -> TypeClassBuilder.create( definition, packageName, typesPackage ) )
            .filter( Objects::nonNull )
            .forEach( builder -> {
                if ( !generated.getAndSet( true ) )
                    createDirectory( typesDirectory );

                JavaClassDirector.writeClass( builder, typesDirectory );
            } );
        
        return generated.get();
    }

    private boolean generateExceptionClasses( Map<String, Collection<JolieOperation>> operationsMap ) {
        final AtomicBoolean generated = new AtomicBoolean( false );

        operationsMap.values()
            .parallelStream()
            .flatMap( Collection::stream )
            .map( JolieOperation::faults )
            .flatMap( Collection::stream )
            .distinct()
            .map( fault -> ExceptionClassBuilder.create( fault, packageName, typesPackage, faultsPackage ) )
            .forEach( builder -> {
                if ( !generated.getAndSet( true ) )
                    createDirectory( faultsDirectory );

                JavaClassDirector.writeClass( builder, faultsDirectory );
            } );

        return generated.get();
    }

    private boolean generateInterfaceClasses( Map<String, Collection<JolieOperation>> operationsMap, boolean generatedTypes ) {
        final AtomicBoolean generated = new AtomicBoolean( false );

        EntryStream.of( operationsMap )
            .parallel()
            .mapKeyValue( (name, operations) -> new InterfaceClassBuilder( 
                name, 
                operations, 
                packageName, 
                generatedTypes ? typesPackage : null,
                interfacesPackage
            ) )
            .forEach( builder -> {
                if ( !generated.getAndSet( true ) )
                    createDirectory( interfacesDirectory );

                JavaClassDirector.writeClass( builder, interfacesDirectory );
            } );

        return generated.get();
    }

    private void generateServiceClass( Map<String, Collection<JolieOperation>> operationsMap, boolean generatedTypes, boolean generatedFaults, boolean generatedInterfaces ) {
        JavaClassDirector.writeClass(
            new ServiceClassBuilder( 
                serviceName, 
                operationsMap,
                packageName,
                generatedTypes ? typesPackage : null, 
                generatedFaults ? faultsPackage : null, 
                generatedInterfaces ? interfacesPackage : null
            ), 
            packageDirectory
        );
    }

    private String getDirectory( String baseDirectory, String packageName ) {
        return baseDirectory + Arrays.stream( packageName.split( "\\." ) )
            .map( s -> File.separator + s )
            .reduce( (s1,s2) -> s1 + s2 )
            .orElse( "" );
    }

    private void createDirectory( String directory ) {
        File f = new File( directory );
        f.mkdirs();
    }
}
