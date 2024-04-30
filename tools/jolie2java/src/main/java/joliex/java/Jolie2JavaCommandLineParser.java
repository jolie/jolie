package joliex.java;

import java.io.IOException;
import java.util.List;

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import joliex.java.generate.JavaDocumentCreator;

public class Jolie2JavaCommandLineParser extends CommandLineParser {
    
    private final String outputDirectory;
    private final String typePackage;
    private final String faultPackage;
    private final String interfacePackage;
    private final String serviceDirectory;
    private final String servicePackage;
    private final String serviceName;
    private final boolean overrideService;

    public String outputDirectory() { return outputDirectory; }
    public String typePackage() { return typePackage; }
    public String faultPackage() { return faultPackage; }
    public String interfacePackage() { return interfacePackage; }
    public String serviceDirectory() { return serviceDirectory; }
    public String servicePackage() { return servicePackage; }
    public String serviceName() { return serviceName; }
    public boolean overrideService() { return overrideService; }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private String outputDirectory = null;
        private String typePackage = null;
        private String faultPackage = null;
        private String interfacePackage = null;
        private String serviceDirectory = null;
        private String servicePackage = null;
        private String serviceName = null;
        private boolean overrideService = false;

        public int onUnrecognizedArgument( List<String> argumentsList, int index ) throws CommandLineException {
            
            switch( argumentsList.get( index ) ) {
                case "--outputDirectory" -> { index++; outputDirectory = argumentsList.get( index ); }
                case "--typePackage" -> { index++; typePackage = argumentsList.get( index ); }
                case "--faultPackage" -> { index++; faultPackage = argumentsList.get( index ); }
                case "--interfacePackage" -> { index++; interfacePackage = argumentsList.get( index ); }
                case "--serviceDirectory" -> { index++; serviceDirectory = argumentsList.get( index ); }
                case "--servicePackage" -> { index++; servicePackage = argumentsList.get( index ); }
                case "--serviceName" -> { index++; serviceName = argumentsList.get( index ); }
                case "--overrideService" -> { index++; overrideService = Boolean.valueOf( argumentsList.get( index ) ); }
                
                /* deprecated flags */
                case "--packageName" -> { index++; }
                case "--javaservice" -> { index++; }
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

        outputDirectory = argHandler.outputDirectory;
        typePackage = argHandler.typePackage;
        faultPackage = argHandler.faultPackage;
        interfacePackage = argHandler.interfacePackage;
        serviceDirectory = argHandler.serviceDirectory;
        servicePackage = argHandler.servicePackage;
        serviceName = argHandler.serviceName;
        overrideService = argHandler.overrideService;
    }

    @Override
    protected String getHelpString() {
        return new StringBuilder()
            .append( "Usage: jolie2java --outputDirectory <path>" ).append( "\n" )
            .append( "                  [ --typePackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_TYPE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --faultPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_FAULT ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --interfacePackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_INTERFACE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --serviceDirectory <path> (default=\"" ).append( JavaDocumentCreator.DEFAULT_DIRECTORY_SERVICE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --servicePackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_SERVICE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --serviceName <name> (default=\"" ).append( JavaDocumentCreator.DEFAULT_NAME_SERVICE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --overrideService <true|false> (default=false) ]" ).append( "\n" )
            .append( "                  <file>" ).toString();
    }
}
