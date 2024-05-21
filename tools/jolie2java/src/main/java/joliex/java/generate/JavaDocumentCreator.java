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

    public static final String DEFAULT_PACKAGE_SERVICE = "joliex";
    public static final String DEFAULT_PACKAGE_TYPE = ".spec.types";
    public static final String DEFAULT_PACKAGE_FAULT = ".spec.faults";
    public static final String DEFAULT_PACKAGE_INTERFACE = ".spec.interfaces";
    public static final String DEFAULT_NAME_SERVICE = "MainService";

    private final String servicePackage;
    private final Path serviceDirectory;
    private final String typePackage;
    private final Path typeDirectory;
    private final String faultPackage;
    private final Path faultDirectory;
    private final String interfacePackage;
    private final Path interfaceDirectory;
    private final String serviceName;
    private final GenerateService generateService;

    public JavaDocumentCreator( String outputDirectory, String packageName, String typePackage, String faultPackage, String interfacePackage, String serviceName, GenerateService generateService ) {
        final Path od = Path.of( outputDirectory );

        servicePackage = getPackageName( null, packageName, DEFAULT_PACKAGE_SERVICE );
        serviceDirectory = getPackagePath( od, servicePackage );
        
        this.typePackage = getPackageName( servicePackage, typePackage, DEFAULT_PACKAGE_TYPE );
        typeDirectory = getPackagePath( od, this.typePackage );

        this.faultPackage = getPackageName( servicePackage, faultPackage, DEFAULT_PACKAGE_FAULT );
        faultDirectory = getPackagePath( od, this.faultPackage );

        this.interfacePackage = getPackageName( servicePackage, interfacePackage, DEFAULT_PACKAGE_INTERFACE );
        interfaceDirectory = getPackagePath( od, this.interfacePackage );

        this.serviceName = serviceName == null ? DEFAULT_NAME_SERVICE : serviceName;
        this.generateService = generateService;
    }

    public void generateClasses( ProgramInspector inspector ) {
        final TypeFactory definitionFactory = new TypeFactory();
        final OperationFactory operationFactory = new OperationFactory( definitionFactory );

        generateTypeClasses( definitionFactory.getAll( inspector.getTypes() ) );

        final Map<String, Collection<JolieOperation>> operationsMap = 
            operationFactory.createOperationsMap( inspector.getInterfaces() );

        generateExceptionClasses( operationsMap );
        generateInterfaceClasses( operationsMap );

        if ( !operationsMap.isEmpty() && generateService != GenerateService.NEVER )
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
            generateService == GenerateService.ALWAYS );
    }

    private static String getAbsoluteName( String baseName, String relativeName ) {
        if ( !relativeName.startsWith( "." ) )
            return relativeName;

        if ( baseName == null )
            throw new IllegalArgumentException( "Can't use a relative package name for the base package." );

        return relativeName.length() == 1
            ? baseName
            : baseName + relativeName;
    }

    private static String getPackageName( String basePackage, String packageName, String defaultName ) {
        if ( packageName == null )
            return getAbsoluteName( basePackage, defaultName );

        final String name = getAbsoluteName( basePackage, packageName );

        if ( !name.matches( "\\w+(\\.\\w+)*" ) )
            throw new IllegalArgumentException( "The absolute name of a package must match the regex \"\\w+(\\.\\w+)*\", got=\"" + name + "\"." );

        return name;
    }

    private static Path getPackagePath( Path dir, String packagePath ) {
        return Path.of( dir.toString(), packagePath.split( "\\." ) );
    }
}
