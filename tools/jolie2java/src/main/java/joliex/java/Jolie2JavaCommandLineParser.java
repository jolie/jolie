package joliex.java;

import java.io.IOException;
import java.util.List;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;

public class Jolie2JavaCommandLineParser extends CommandLineParser {

	private final String packageName;
	private final String format;
	private final String targetPort;
	private boolean addSource = false;
	private final String outputDirectory;
	private final boolean buildXml;
	private final boolean javaservice;

	public String getPackageName() {
		return packageName;
	}

	public String getFormat() {
		return format;
	}

	public String getTargetPort() {
		return targetPort;
	}

	public boolean isAddSource() {
		return addSource;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setAddSource( boolean addSource ) {
		this.addSource = true;
	}

	public boolean isBuildXmlenabled() {
		return buildXml;
	}

	public boolean javaService() {
		return javaservice;
	}

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

		private String packageName = null;
		private String format = null;
		private String targetPort;
		private Boolean addSource = false;
		private String outputDirectory = null;
		private Boolean buildXml = true;
		private Boolean javaservice = false;

		@Override
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException {
			if( "--addSource".equals( argumentsList.get( index ) ) ) {
				index++;
				this.addSource = Boolean.valueOf( argumentsList.get( index ) );
			} else if( "--packageName".equals( argumentsList.get( index ) ) ) {
				index++;
				packageName = argumentsList.get( index );
			} else if( "--format".equals( argumentsList.get( index ) ) ) {
				index++;
				format = argumentsList.get( index );
			} else if( "--targetPort".equals( argumentsList.get( index ) ) ) {
				index++;
				targetPort = argumentsList.get( index );
			} else if( "--outputDirectory".equals( argumentsList.get( index ) ) ) {
				index++;
				outputDirectory = argumentsList.get( index );
			} else if( "--javaservice".equals( argumentsList.get( index ) ) ) {
				index++;
				javaservice = Boolean.valueOf( argumentsList.get( index ) );
			} else if( "--buildXml".equals( argumentsList.get( index ) ) ) {
				index++;
				buildXml = Boolean.valueOf( argumentsList.get( index ) );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static Jolie2JavaCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException {
		return new Jolie2JavaCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private Jolie2JavaCommandLineParser( String[] args, ClassLoader parentClassLoader,
		JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException {
		super( args, parentClassLoader, argHandler );

		packageName = argHandler.packageName;
		format = argHandler.format;
		targetPort = argHandler.targetPort;
		addSource = argHandler.addSource;
		outputDirectory = argHandler.outputDirectory;
		buildXml = argHandler.buildXml;
		javaservice = argHandler.javaservice;
	}

	@Override
	protected String getHelpString() {
		return "Usage: jolie2java --format [java|gwt] --packageName package_namespace [--javaservice produce files for javaservice implememtation] [--targetPort outputPort_to_be_encoded] [ --outputDirectory outputDirectory ] [--buildXml true|false] [--addSource true|false] file.ol";
	}
}
