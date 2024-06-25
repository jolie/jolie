package joliex.java;

import java.io.IOException;
import java.util.List;

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import joliex.java.generate.JavaDocumentCreator;

public class Jolie2JavaCommandLineParser extends CommandLineParser {
    
    private final Integer translationTarget;
    private final Boolean overwriteServices;
    private final String outputDirectory;
    private final String sourcesPackage;

    public Integer translationTarget() { return translationTarget; }
    public Boolean overwriteServices() { return overwriteServices; }
    public String outputDirectory() { return outputDirectory; }
    public String sourcesPackage() { return sourcesPackage; }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private Integer translationTarget = 0;
        private Boolean overwriteServices = false;
        private String outputDirectory = null;
        private String sourcesPackage = null;

        public int onUnrecognizedArgument( List<String> argumentsList, int index ) throws CommandLineException {
            
            switch( argumentsList.get( index ) ) {
                case "--translationTarget" -> { index++; translationTarget = Integer.parseInt( argumentsList.get( index ) ); }
                case "--overwriteServices" -> { index++; overwriteServices = Boolean.parseBoolean( argumentsList.get( index ) ); }
                case "--outputDirectory" -> { index++; outputDirectory = argumentsList.get( index ); }
                case "--sourcesPackage" -> { index++; sourcesPackage = argumentsList.get( index ); }
                
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

        translationTarget = argHandler.translationTarget;
        overwriteServices = argHandler.overwriteServices;
        outputDirectory = argHandler.outputDirectory;
        sourcesPackage = argHandler.sourcesPackage;
    }

    @Override
    protected String getHelpString() {
        return new StringBuilder()
            .append( "Usage: jolie2java" ).append( "\n" )
            .append( "                  [ --translationTarget <0:services | 1:interfaces | 2:types> (default=0) ]" ).append( "\n" )
            .append( "                  [ --overwriteServices <true|false> (default=false) ]" ).append( "\n" )
            .append( "                  [ --outputDirectory <path> (default=\"" ).append( JavaDocumentCreator.DEFAULT_OUTPUT_DIRECTORY ).append( "\") ]" ).append( "\n" )
            .append( "                  [ --sourcesPackage <package> (default=\"" ).append( JavaDocumentCreator.DEFAULT_SOURCES_PACKAGE ).append( "\") ]" ).append( "\n" )
            .append( "                  <file>" ).toString();
    }
}
