package joliex.wsdl;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

public class Jolie2WsdlCommandLineParser extends CommandLineParser {

    private String portName;
    private String tns;
    private String address;
    private String outputFile;

    public String getPortName() {
        return portName;
    }

    public String getAddress() {
        return address;
    }

    public String getTns() {
        return tns;
    }
    
    public String getOutputFile() {
        return outputFile;
    }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

        private String portName;
        private String tns;
        private String address;
        private String outputFile;
            
        public int onUnrecognizedArgument(List< String> argumentsList, int index)
                throws CommandLineException {
            if ("--tns".equals(argumentsList.get(index))) {
                index++;
                tns = argumentsList.get(index);
            } else if ("--pN".equals(argumentsList.get(index))) {
                index++;
                portName = argumentsList.get(index);
            } else if ("--addr".equals(argumentsList.get(index))) {
                index++;
                address = argumentsList.get(index);
            } else if ("--oF".equals(argumentsList.get(index))) {
                index++;
                outputFile = argumentsList.get(index);
            } else {
                throw new CommandLineException("Unrecognized command line option: " + argumentsList.get(index));
            }

            return index;
        }
    }

    public static Jolie2WsdlCommandLineParser create(String[] args, ClassLoader parentClassLoader)
            throws CommandLineException, IOException {
        return new Jolie2WsdlCommandLineParser(args, parentClassLoader, new JolieDummyArgumentHandler());
    }

    private Jolie2WsdlCommandLineParser(String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler)
            throws CommandLineException, IOException {
        super(args, parentClassLoader, argHandler);

        portName = argHandler.portName;
        address = argHandler.address;
        tns = argHandler.tns;
        outputFile = argHandler.outputFile;
    }

    
    @Override
    protected String getHelpString() {
        return "Usage: jolie2wsdl --tns target_name_space --pN name_of_the_port --addr address_string --oF output_filename file.ol";
    }
}