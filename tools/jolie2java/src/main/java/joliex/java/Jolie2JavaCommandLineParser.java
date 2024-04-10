package joliex.java;

import java.io.IOException;
import java.util.List;

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import joliex.java.generate.JavaDocumentCreator;

public class Jolie2JavaCommandLineParser extends CommandLineParser {
    
    private final String packageName;
    private final String typesPackage;
    private final String faultsPackage;
    private final String interfacesPackage;
    private final String outputDirectory;
    private final boolean generateService;
    private final String serviceName;

    public String packageName() { return packageName; }
    public String typesPackage() { return typesPackage; }
    public String faultsPackage() { return faultsPackage; }
    public String interfacesPackage() { return interfacesPackage; }
    public String outputDirectory() { return outputDirectory; }
    public boolean generateService() { return generateService; }
    public String serviceName() { return serviceName; }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private String packageName = null;
        private String typesPackage = null;
        private String faultsPackage = null;
        private String interfacesPackage = null;
        private String outputDirectory = null;
        private boolean generateService = true;
        private String serviceName = null;

        public int onUnrecognizedArgument( List<String> argumentsList, int index ) throws CommandLineException {
            
            switch( argumentsList.get( index ) ) {
                case "--packageName" -> { index++; packageName = argumentsList.get( index ); }
                case "--typesPackage" -> { index++; typesPackage = argumentsList.get( index ); }
                case "--faultsPackage" -> { index++; faultsPackage = argumentsList.get( index ); }
                case "--interfacesPackage" -> { index++; interfacesPackage = argumentsList.get( index ); }
                case "--outputDirectory" -> { index++; outputDirectory = argumentsList.get( index ); }
                case "--generateService" -> { index++; generateService = Boolean.valueOf( argumentsList.get( index ) ); }
                case "--serviceName" -> { index++; serviceName = argumentsList.get( index ); }
                
                /* deprecated flags */
                case "--javaservice" -> { index++; generateService = Boolean.valueOf( argumentsList.get( index ) ); }
                case "--addSource" -> { index++; }
                case "--format" -> { index++; }
                case "--buildXml" -> { index++; }
                case "--targetPort" -> { index++; }

                default -> throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
            }

            return index;
        }
    }

    public static Jolie2JavaCommandLineParser create( String[] args, ClassLoader parentClassLoader )
    throws CommandLineException, IOException {
        return new Jolie2JavaCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
    }

    private Jolie2JavaCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler )
    throws CommandLineException, IOException {
        super( args, parentClassLoader, argHandler );

        packageName = argHandler.packageName;
        typesPackage = argHandler.typesPackage;
        faultsPackage = argHandler.faultsPackage;
        interfacesPackage = argHandler.interfacesPackage;
        outputDirectory = argHandler.outputDirectory;
        generateService = argHandler.generateService;
        serviceName = argHandler.serviceName;
    }

    @Override
    protected String getHelpString() {
        return new StringBuilder()
            .append( "Usage: jolie2java --packageName <package>" ).append( "\n" )
            .append( "                  [ --typesPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_TYPE_PACKAGE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --faultsPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_FAULT_PACKAGE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --interfacesPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_INTERFACE_PACKAGE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --outputDirectory <path> (default=\"" ).append( JavaDocumentCreator.DEFAULT_OUTPUT_DIRECTORY ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --generateService <true|false> (default=true) ]" ).append( "\n" )
            .append( "                  [ --serviceName <name> (default=\"" ).append( JavaDocumentCreator.DEFAULT_SERVICE_NAME ).append( "\") ]" ).append( "\n" )
            .append( "                  <file>" ).toString();
    }
}
