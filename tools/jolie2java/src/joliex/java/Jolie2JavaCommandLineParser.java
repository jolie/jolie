package joliex.java;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import joliex.java.formatExeption;

public class Jolie2JavaCommandLineParser extends CommandLineParser {

    private String namespace;
    private String format;
    private String targetPort;

    public String getNameSpace()
            throws formatExeption {
        return namespace;
    }

    public String getFormat() {
        return format;
    }

    public String getTargetPort() {
        return targetPort;
    }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private String namespace;
        private String format;
        private String targetPort;

        public int onUnrecognizedArgument(List< String> argumentsList, int index)
                throws CommandLineException {
            if ("--namespace".equals(argumentsList.get(index))) {
                index++;

                namespace = argumentsList.get(index);
                //index++;
            } else if ("--format".equals(argumentsList.get(index))) {
                index++;
                format = argumentsList.get(index);
            } else if ("--targetPort".equals(argumentsList.get(index))) {
                index++;
                targetPort = argumentsList.get(index);
            } else {
                throw new CommandLineException("Unrecognized command line option: " + argumentsList.get(index));
            }

            return index;
        }
    }

    public static Jolie2JavaCommandLineParser create(String[] args, ClassLoader parentClassLoader)
            throws CommandLineException, IOException {
        return new Jolie2JavaCommandLineParser(args, parentClassLoader, new JolieDummyArgumentHandler());
    }

    private Jolie2JavaCommandLineParser(String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler)
            throws CommandLineException, IOException {
        super(args, parentClassLoader, argHandler);

        namespace = argHandler.namespace;
        format = argHandler.format;
        targetPort = argHandler.targetPort;
    }

    
    @Override
    protected String getHelpString() {
        return "Usage: jolie2java --format [java|gwt] --namespace package_namespace [--targetPort inputPort_to_be_encoded] file.ol";
    }
}