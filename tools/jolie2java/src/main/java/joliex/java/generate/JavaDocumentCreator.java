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

    public static final String DEFAULT_OUTPUT_DIRECTORY = "./generated";
    public static final String DEFAULT_SERVICE_NAME = "MainService";
    public static final String DEFAULT_TYPE_PACKAGE = ".types";
    public static final String DEFAULT_FAULT_PACKAGE = ".faults";
    public static final String DEFAULT_INTERFACE_PACKAGE = ".interfaces";
    private final String packageName;
    private final Path packageDirectory;
    private final String typesPackage;
    private final Path typesDirectory;
    private final String faultsPackage;
    private final Path faultsDirectory;
    private final String interfacesPackage;
    private final Path interfacesDirectory;
    private final boolean generateService;
    private final String serviceName;

    public JavaDocumentCreator( String packageName, String typesPackage, String faultsPackage, String interfacesPackage, String outputDirectory, boolean generateService, String serviceName ) {
        final Path outputPath = Path.of( outputDirectory == null ? DEFAULT_OUTPUT_DIRECTORY : outputDirectory );

        this.packageName = formatPackageName( packageName );
        packageDirectory = getPackagePath( outputPath, this.packageName );

        this.typesPackage = getPackage( this.packageName, typesPackage == null ? DEFAULT_TYPE_PACKAGE : typesPackage );
        typesDirectory = getPackagePath( outputPath, this.typesPackage );

        this.faultsPackage = getPackage( this.packageName, faultsPackage == null ? DEFAULT_FAULT_PACKAGE : faultsPackage );
        faultsDirectory = getPackagePath( outputPath, this.faultsPackage );

        this.interfacesPackage = getPackage( this.packageName, interfacesPackage == null ? DEFAULT_INTERFACE_PACKAGE : interfacesPackage );
        interfacesDirectory = getPackagePath( outputPath, this.interfacesPackage );

        this.generateService = generateService;
        this.serviceName = serviceName == null ? DEFAULT_SERVICE_NAME : serviceName;
    }

    public void generateClasses( ProgramInspector inspector ) {
        final TypeFactory definitionFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( definitionFactory );

        generateTypeClasses( definitionFactory.getAll( inspector.getTypes() ) );

        final Map<String, Collection<JolieOperation>> operationsMap = 
            operationFactory.createOperationsMap( inspector.getInterfaces() );

        generateExceptionClasses( operationsMap );
        generateInterfaceClasses( operationsMap );

        if ( generateService && !operationsMap.isEmpty() )
            generateServiceClass( operationsMap );
    }

    private void generateTypeClasses( Stream<JolieType> typeDefinitions ) {
        typeDefinitions.parallel()
            .map( definition -> TypeClassBuilder.create( definition, typesPackage ) )
            .filter( Objects::nonNull )
            .forEach( builder -> JavaClassDirector.writeClass( typesDirectory, builder ) );
    }

    private void generateExceptionClasses( Map<String, Collection<JolieOperation>> operationsMap ) {
        operationsMap.values()
            .parallelStream()
            .flatMap( Collection::stream )
            .map( JolieOperation::faults )
            .flatMap( Collection::stream )
            .distinct()
            .map( fault -> new ExceptionClassBuilder( fault, faultsPackage, typesPackage ) )
            .forEach( builder -> JavaClassDirector.writeClass( faultsDirectory, builder ) );
    }

    private void generateInterfaceClasses( Map<String, Collection<JolieOperation>> operationsMap ) {
        EntryStream.of( operationsMap )
            .parallel()
            .mapKeyValue( (name, operations) -> new InterfaceClassBuilder( 
                name, 
                operations, 
                interfacesPackage,
                Files.exists( typesDirectory ) ? typesPackage : null ) )
            .forEach( builder -> JavaClassDirector.writeClass( interfacesDirectory, builder ) );
    }

    private void generateServiceClass( Map<String, Collection<JolieOperation>> operationsMap ) {
        JavaClassDirector.writeClass( packageDirectory, new ServiceClassBuilder( 
            serviceName, 
            operationsMap,
            packageName,
            Files.exists( typesDirectory ) ? typesPackage : null, 
            Files.exists( faultsDirectory ) ? faultsPackage : null, 
            Files.exists( interfacesDirectory ) ? interfacesPackage : null ) );
    }

    private static String formatPackageName( String name ) {
        return name.replaceAll( "-", "_" );
    }

    private static String getPackage( String basePackage, String packageName ) {
        if ( packageName.startsWith( "." ) )
            return packageName.length() == 1 ? basePackage : basePackage + formatPackageName( packageName );
        return formatPackageName( packageName );
    }

    private static Path getPackagePath( Path dir, String packagePath ) {
        return Path.of( dir.toString(), packagePath.split( "\\." ) );
    }
}
