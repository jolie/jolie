package jolie;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import jolie.lang.parse.Scanner;
import jolie.runtime.correlation.CorrelationEngine;

public class InterpreterParameters {
	private Integer connectionsLimit = -1;
	private CorrelationEngine.Type correlationAlgorithm = CorrelationEngine.Type.SIMPLE;
	private final Deque< String > includePaths = new LinkedList<>();
	private final Deque< String > optionArgs = new LinkedList<>();
	private final Deque< URL > libURLs = new LinkedList<>();
	private final InputStream inputStream;
	private String charset;
	private final File programFilepath;
	private final Deque< String > arguments = new LinkedList<>();
	private final Map< String, Scanner.Token > constants = new HashMap<>();
	private JolieClassLoader jolieClassLoader;
	private boolean isProgramCompiled = false;
	private boolean typeCheck = false;
	private boolean tracer = false;
	private String tracerMode = "";
	private String tracerLevel = "";
	private boolean check = false;
	private long responseTimeout = 36000 * 1000; // 10 minutes;
	private boolean printStackTraces = false;
	private Level logLevel = Level.OFF;
	private File programDirectory = null;

	public InterpreterParameters( int connectionsLimit,
		CorrelationEngine.Type correlationAlgorithm,
		String[] includeList,
		String[] optionArgs,
		URL[] libUrls,
		InputStream inputStream,
		String charset,
		File programFilepath,
		String[] arguments,
		Map< String, Scanner.Token > constants,
		JolieClassLoader jolieClassLoader,
		boolean programCompiled,
		boolean typeCheck,
		boolean tracer,
		String tracerLevel,
		String tracerMode,
		boolean check,
		boolean printStackTraces,
		long responseTimeout,
		Level logLevel,
		File programDirectory ) throws IOException {

		this.connectionsLimit = connectionsLimit;
		this.correlationAlgorithm = correlationAlgorithm;
		Collections.addAll( this.includePaths, includeList );
		Collections.addAll( this.optionArgs, optionArgs );
		Collections.addAll( this.libURLs, libUrls );
		this.inputStream = inputStream;
		this.charset = charset;
		this.programFilepath = programFilepath;
		Collections.addAll( this.arguments, arguments );
		this.constants.putAll( constants );
		this.jolieClassLoader = jolieClassLoader;
		this.isProgramCompiled = programCompiled;
		this.typeCheck = typeCheck;
		this.tracer = tracer;
		this.tracerLevel = tracerLevel;
		this.tracerMode = tracerMode;
		this.check = check;
		this.printStackTraces = printStackTraces;
		this.responseTimeout = responseTimeout;
		this.logLevel = logLevel;
		this.programDirectory = programDirectory;
	}

	public InterpreterParameters( String[] optionArgs,
		String[] includeList,
		URL[] libUrls,
		File programFilepath,
		JolieClassLoader jolieClassLoader,
		InputStream inputStream ) throws IOException {
		Collections.addAll( this.optionArgs, optionArgs );
		Collections.addAll( this.includePaths, includeList );
		Collections.addAll( this.libURLs, libUrls );
		this.programFilepath = programFilepath;
		this.jolieClassLoader = jolieClassLoader;
		this.inputStream = inputStream;
	}


	/**
	 * Returns the connection limit parameter passed by command line with the -c option.
	 *
	 * @return the connection limit parameter passed by command line
	 */
	public Integer connectionsLimit() {
		return this.connectionsLimit;
	}

	/**
	 * Returns the type of correlation algorithm that has been specified.
	 *
	 * @return the type of correlation algorithm that has been specified.
	 * @see CorrelationEngine
	 */
	public CorrelationEngine.Type correlationAlgorithm() {
		return this.correlationAlgorithm;
	}

	/**
	 * Returns the include paths passed by command line with the -i option.
	 *
	 * @return the include paths passed by command line
	 */
	public String[] includePaths() {
		return includePaths.toArray( new String[] {} );
	}

	/**
	 * Returns the command line options passed to this command line parser. This does not include the
	 * name of the program.
	 *
	 * @return the command line options passed to this command line parser.
	 */
	public String[] optionArgs() {
		return optionArgs.toArray( new String[] {} );
	}

	/**
	 * Returns the library URLs passed by command line with the -l option.
	 *
	 * @return the library URLs passed by command line
	 */
	public URL[] libUrls() {
		return libURLs.toArray( new URL[] {} );
	}

	/**
	 * Returns an InputStream for the program code to execute.
	 *
	 * @return an InputStream for the program code to execute
	 */
	public InputStream inputStream() {
		return this.inputStream;
	}

	/**
	 * Returns the program's character encoding
	 *
	 * @return the program's character encoding
	 */
	public String charset() {
		return this.charset;
	}

	/**
	 * Returns the file path of the JOLIE program to execute.
	 *
	 * @return the file path of the JOLIE program to execute
	 */
	public File programFilepath() {
		return this.programFilepath;
	}

	/**
	 * Returns the arguments passed to the JOLIE program.
	 *
	 * @return the arguments passed to the JOLIE program.
	 */
	public String[] arguments() {
		return arguments.toArray( new String[] {} );
	}


	/**
	 * Returns a map containing the constants defined by command line.
	 *
	 * @return a map containing the constants defined by command line
	 */
	public Map< String, Scanner.Token > constants() {
		return this.constants;
	}

	/**
	 * Returns the classloader to use for the program.
	 *
	 * @return the classloader to use for the program.
	 */
	public JolieClassLoader jolieClassLoader() {
		return jolieClassLoader;
	}

	/**
	 * Returns {@code true} if the program is compiled, {@code false} otherwise.
	 *
	 * @return {@code true} if the program is compiled, {@code false} otherwise.
	 */
	public boolean isProgramCompiled() {
		return isProgramCompiled;
	}

	/**
	 * Returns the value of the --typecheck option.
	 *
	 * @return the value of the --typecheck option.
	 */
	public boolean typeCheck() {
		return this.typeCheck;
	}

	/**
	 * Returns <code>true</code> if the tracer option has been specified, false otherwise.
	 *
	 * @return <code>true</code> if the verbose option has been specified, false otherwise
	 */
	public boolean tracer() {
		return this.tracer;
	}

	/**
	 * Returns <code>true</code> if the tracer option has been specified, false otherwise.
	 *
	 * @return <code>true</code> if the verbose option has been specified, false otherwise
	 */
	public String tracerMode() {
		return tracerMode;
	}

	/**
	 * Returns the selected tracer level [all | comm | comp]
	 *
	 * all: all the traces comp: only computation traces comm: only communication traces
	 */
	public String tracerLevel() {
		return tracerLevel;
	}

	/**
	 * Returns <code>true</code> if the check option has been specified, false otherwise.
	 *
	 * @return <code>true</code> if the verbose option has been specified, false otherwise
	 */
	public boolean check() {
		return this.check;
	}

	/**
	 * Returns the response timeout parameter passed by command line with the --responseTimeout option.
	 *
	 * @return the response timeout parameter passed by command line
	 */
	public long responseTimeout() {
		return responseTimeout;
	}

	public boolean printStackTraces() {
		return printStackTraces;
	}

	/**
	 * Returns the {@link Level} of the logger of this interpreter.
	 *
	 * @return the {@link Level} of the logger of this interpreter.
	 */
	public Level logLevel() {
		return this.logLevel;
	}


	/**
	 * Returns the directory in which the main program is located.
	 *
	 * @return the directory in which the main program is located.
	 */
	public File programDirectory() {
		return programDirectory;
	}

	public void clear() {
		jolieClassLoader = null;
	}
}

