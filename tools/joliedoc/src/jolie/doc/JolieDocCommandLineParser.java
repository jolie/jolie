/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.doc;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

/**
 *
 * @author claudio guidi
 */
public class JolieDocCommandLineParser extends CommandLineParser {
    
    private boolean outputPortEnabled;
    private String inputPortName;
    
    public boolean getOutputPortEnabled() {
        return outputPortEnabled;
    }
    
    public String getInputPortName () {
        return inputPortName;
    }
    
    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {
        
        private boolean outputPortEnabled = false;
        private String inputPortName = "";

        public int onUnrecognizedArgument(List< String> argumentsList, int index)
                throws CommandLineException {
            if ("--outputPortEnabled".equals(argumentsList.get(index))) {
                index++;

                outputPortEnabled = new Boolean( argumentsList.get( index ));
                //index++;
            } else if ("--port".equals(argumentsList.get(index))) {
                index++;
                inputPortName = argumentsList.get(index);
            } else {
                throw new CommandLineException("Unrecognized command line option: " + argumentsList.get(index));
            }

            return index;
        }
    }
    
    public static JolieDocCommandLineParser create(String[] args, ClassLoader parentClassLoader)
            throws CommandLineException, IOException {
        return new JolieDocCommandLineParser(args, parentClassLoader, new JolieDummyArgumentHandler());
    }
    
    private JolieDocCommandLineParser(String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler)
            throws CommandLineException, IOException {
        super(args, parentClassLoader, argHandler);
        inputPortName = argHandler.inputPortName;
        outputPortEnabled = argHandler.outputPortEnabled;
        
    }

    
    @Override
    protected String getHelpString() {
        return "Usage: joliedoc filename.ol [ --outputPortEnabled true|false ] [ --port portname ]";
    }
   
    
}
