package joliex.java;

import java.io.IOException;
import java.util.List;

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import joliex.java.generate.JavaDocumentCreator;

public class Jolie2JavaCommandLineParser extends CommandLineParser {
    
    private final String outputDirectory;
    private final String packageName;
    private final String typePackage;
    private final String faultPackage;
    private final String interfacePackage;
    private final String serviceName;
    private final Integer generateService;

    public String outputDirectory() { return outputDirectory; }
    public String packageName() { return packageName; }
    public String typePackage() { return typePackage; }
    public String faultPackage() { return faultPackage; }
    public String interfacePackage() { return interfacePackage; }
    public String serviceName() { return serviceName; }
    public Integer generateService() { return generateService; }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private String outputDirectory = null;
        private String packageName = null;
        private String typePackage = null;
        private String faultPackage = null;
        private String interfacePackage = null;
        private String serviceName = null;
        private Integer generateService = 0;

        public int onUnrecognizedArgument( List<String> argumentsList, int index ) throws CommandLineException {
            
            switch( argumentsList.get( index ) ) {
                case "--outputDirectory" -> { index++; outputDirectory = argumentsList.get( index ); }
                case "--packageName" -> { index++; packageName = argumentsList.get( index ); }
                case "--typePackage" -> { index++; typePackage = argumentsList.get( index ); }
                case "--faultPackage" -> { index++; faultPackage = argumentsList.get( index ); }
                case "--interfacePackage" -> { index++; interfacePackage = argumentsList.get( index ); }
                case "--serviceName" -> { index++; serviceName = argumentsList.get( index ); }
                case "--generateService" -> { index++; generateService = Integer.parseInt( argumentsList.get( index ) ); }
                
                /* deprecated flags */
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
        packageName = argHandler.packageName;
        typePackage = argHandler.typePackage;
        faultPackage = argHandler.faultPackage;
        interfacePackage = argHandler.interfacePackage;
        serviceName = argHandler.serviceName;
        generateService = argHandler.generateService;
    }

    @Override
    protected String getHelpString() {
        return new StringBuilder()
            .append( "Usage: jolie2java --outputDirectory <path>" ).append( "\n" )
            .append( "                  [ --packageName <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_SERVICE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --typePackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_TYPE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --faultPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_FAULT ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --interfacePackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_PACKAGE_INTERFACE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --serviceName <name> (default=\"" ).append( JavaDocumentCreator.DEFAULT_NAME_SERVICE ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --generateService <0:if absent | 1:always | 2:never> (default=0) ]" ).append( "\n" )
            .append( "                  <file>" ).toString();
    }
}
