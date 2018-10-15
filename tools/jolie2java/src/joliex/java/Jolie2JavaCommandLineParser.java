package joliex.java;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

public class Jolie2JavaCommandLineParser extends CommandLineParser {

    private String packageName;
    private String format;
    private String targetPort;
    private boolean addSource = false;

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

    public void setAddSource(boolean addSource) {
	this.addSource = true;
    }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

	private String packageName;
	private String format;
	private String targetPort;
	private boolean addSource = false;

	public int onUnrecognizedArgument(List< String> argumentsList, int index)
		throws CommandLineException {
	    if ("--addSource".equals(argumentsList.get(index))) {
		index++;
		this.addSource = true;
	    } else if ("--packageName".equals(argumentsList.get(index))) {
		index++;
		packageName = argumentsList.get(index);
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

	packageName = argHandler.packageName;
	format = argHandler.format;
	targetPort = argHandler.targetPort;
	addSource = argHandler.addSource;
    }

    @Override
    protected String getHelpString() {
	return "Usage: jolie2java --addSource [true] --format [java|gwt] --packageName package_namespace [--targetPort inputPort_to_be_encoded] file.ol";
    }
}
