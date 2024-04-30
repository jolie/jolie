package joliex.java.generate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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

    public static final String DEFAULT_PACKAGE_TYPE = "types";
    public static final String DEFAULT_PACKAGE_FAULT = "faults";
    public static final String DEFAULT_PACKAGE_INTERFACE = "interfaces";
    public static final String DEFAULT_DIRECTORY_SERVICE = "./src/main/java";
    public static final String DEFAULT_PACKAGE_SERVICE = "joliex";
    public static final String DEFAULT_NAME_SERVICE = "MainService";

    private final String typePackage;
    private final Path typeDirectory;
    private final String faultPackage;
    private final Path faultDirectory;
    private final String interfacePackage;
    private final Path interfaceDirectory;
    private final String servicePackage;
    private final Path serviceDirectory;
    private final String serviceName;
    private final boolean overrideService;

    public JavaDocumentCreator( String outputDirectory, String typePackage, String faultPackage, String interfacePackage, String serviceDirectory, String servicePackage, String serviceName, boolean overrideService ) {
        final Path od = Path.of( outputDirectory );
        
        this.typePackage = formatPackageName( typePackage == null ? DEFAULT_PACKAGE_TYPE : typePackage );
        typeDirectory = getPackagePath( od, this.typePackage );

        this.faultPackage = formatPackageName( faultPackage == null ? DEFAULT_PACKAGE_FAULT : faultPackage );
        faultDirectory = getPackagePath( od, this.faultPackage );

        this.interfacePackage = formatPackageName( interfacePackage == null ? DEFAULT_PACKAGE_INTERFACE : interfacePackage );
        interfaceDirectory = getPackagePath( od, this.interfacePackage );

        this.servicePackage = formatPackageName( servicePackage == null ? DEFAULT_PACKAGE_SERVICE : servicePackage );
        this.serviceDirectory = getPackagePath( Path.of( serviceDirectory == null ? DEFAULT_DIRECTORY_SERVICE : serviceDirectory ), this.servicePackage );
        this.serviceName = serviceName == null ? DEFAULT_NAME_SERVICE : serviceName;
        this.overrideService = overrideService;
    }

    public void generateClasses( ProgramInspector inspector ) {
        final TypeFactory definitionFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( definitionFactory );

        generateTypeClasses( definitionFactory.getAll( inspector.getTypes() ) );

        final Map<String, Collection<JolieOperation>> operationsMap = 
            operationFactory.createOperationsMap( inspector.getInterfaces() );

        generateExceptionClasses( operationsMap );
        generateInterfaceClasses( operationsMap );

        if ( !operationsMap.isEmpty() )
            generateServiceClass( operationsMap );
    }

    private void generateTypeClasses( Stream<JolieType> typeDefinitions ) {
        typeDefinitions.parallel()
            .map( definition -> TypeClassBuilder.create( definition, typePackage ) )
            .filter( Objects::nonNull )
            .forEach( builder -> JavaClassDirector.writeClass( typeDirectory, builder, true ) );
    }

    private void generateExceptionClasses( Map<String, Collection<JolieOperation>> operationsMap ) {
        operationsMap.values()
            .parallelStream()
            .flatMap( Collection::stream )
            .map( JolieOperation::faults )
            .flatMap( Collection::stream )
            .distinct()
            .map( fault -> new ExceptionClassBuilder( fault, faultPackage, typePackage ) )
            .forEach( builder -> JavaClassDirector.writeClass( faultDirectory, builder, true ) );
    }

    private void generateInterfaceClasses( Map<String, Collection<JolieOperation>> operationsMap ) {
        EntryStream.of( operationsMap )
            .parallel()
            .mapKeyValue( (name, operations) -> new InterfaceClassBuilder( 
                name, 
                operations, 
                interfacePackage,
                Files.exists( typeDirectory ) ? typePackage : null ) )
            .forEach( builder -> JavaClassDirector.writeClass( interfaceDirectory, builder, true ) );
    }

    private void generateServiceClass( Map<String, Collection<JolieOperation>> operationsMap ) {
        JavaClassDirector.writeClass( 
            serviceDirectory, 
            new ServiceClassBuilder( 
                serviceName, 
                operationsMap,
                servicePackage,
                Files.exists( typeDirectory ) ? typePackage : null, 
                Files.exists( faultDirectory ) ? faultPackage : null, 
                Files.exists( interfaceDirectory ) ? interfacePackage : null ),
            overrideService );
    }

    private static String formatPackageName( String name ) {
        return name.replaceAll( "-", "_" );
    }

    private static Path getPackagePath( Path dir, String packagePath ) {
        return Path.of( dir.toString(), packagePath.split( "\\." ) );
    }
}
