package joliex.wsdl;

import java.io.IOException;
import java.util.List;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;

public class Jolie2WsdlCommandLineParser extends CommandLineParser {

	private final String portName;
	private final String namespace;
	private final String address;
	private final String outputFile;

	public String getPortName() {
		return portName;
	}

	public String getAddress() {
		return address;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getOutputFile() {
		return outputFile;
	}

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

		private String portName;
		private String namespace;
		private String address;
		private String outputFile;

		@Override
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException {
			if( "--namespace".equals( argumentsList.get( index ) ) ) {
				index++;
				namespace = argumentsList.get( index );
			} else if( "--portName".equals( argumentsList.get( index ) ) ) {
				index++;
				portName = argumentsList.get( index );
			} else if( "--portAddr".equals( argumentsList.get( index ) ) ) {
				index++;
				address = argumentsList.get( index );
			} else if( "--outputFile".equals( argumentsList.get( index ) ) ) {
				index++;
				outputFile = argumentsList.get( index );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static Jolie2WsdlCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException {
		return new Jolie2WsdlCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private Jolie2WsdlCommandLineParser( String[] args, ClassLoader parentClassLoader,
		JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException {
		super( args, parentClassLoader, argHandler );

		portName = argHandler.portName;
		address = argHandler.address;
		namespace = argHandler.namespace;
		outputFile = argHandler.outputFile;
	}


	@Override
	protected String getHelpString() {
		return "Usage: jolie2wsdl --namespace target_name_space --portName name_of_the_port --portAddr address_string --outputFile output_filename file.ol";
	}
}
