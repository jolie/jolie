package jolie;

import jolie.lang.parse.Scanner;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class InterpreterParameters {
	private Integer connectionsLimit = -1;
	private Integer connectionCache = 100;
	private CorrelationEngine.Type correlationAlgorithm = CorrelationEngine.Type.SIMPLE;
	private final Deque< String > includePaths = new LinkedList<>();
	private final Deque< String > optionArgs = new LinkedList<>();
	private final Deque< URL > libURLs = new LinkedList<>();
	private InputStream inputStream = null;
	private String charset;
	private File programFilepath = null;
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


	public InterpreterParameters() throws IOException {
		String pwd = UriUtils.normalizeWindowsPath( new File( "" ).getCanonicalPath() );
		includePaths.add( pwd );
		includePaths.add( "include" );
	}

	/**
	 *
	 * @param connectionsLimit: the connection limit parameter passed by command line with the -c
	 *        option.
	 * @param connectionCache: the connection cache parameter passed by command line with the
	 *        --conncache option.
	 * @param correlationAlgorithm: the type of correlation algorithm that has been specified.
	 * @param includeList: the include paths passed by command line with the -i option.
	 * @param optionArgs: the command line options passed to this command line parser. This does not
	 *        include the name of the program.
	 * @param libUrls: the library URLs passed by command line with the -l option.
	 * @param inputStream: an InputStream for the program code to execute.
	 * @param charset: the program's character encoding
	 * @param programFilepath: the file path of the JOLIE program to execute.
	 * @param arguments: the arguments passed to the JOLIE program.
	 * @param constants: a map containing the constants defined by command line.
	 * @param jolieClassLoader: the classloader to use for the program.
	 * @param programCompiled: {@code true} if the program is compiled, {@code false} otherwise.
	 * @param typeCheck: the value of the --typecheck option.
	 * @param tracer: <code>true</code> if the tracer option has been specified, false otherwise.
	 * @param tracerLevel: the selected tracer level [all | comm | comp] all: all the traces comp: only
	 *        computation traces comm: only communication traces
	 * @param tracerMode: <code>true</code> if the tracer option has been specified, false otherwise.
	 * @param check: <code>true</code> if the check option has been specified, false otherwise.
	 * @param responseTimeout: the response timeout parameter passed by command line with the
	 *        --responseTimeout option.
	 * @param logLevel: the {@link Level} of the logger of this interpreter.
	 * @param programDirectory: the directory in which the main program is located.
	 * @throws IOException
	 */

	public InterpreterParameters( int connectionsLimit,
		int connectionCache,
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
		long responseTimeout,
		Level logLevel,
		File programDirectory ) throws IOException {

		super();
		this.connectionsLimit = connectionsLimit;
		this.connectionCache = connectionCache;
		this.correlationAlgorithm = correlationAlgorithm;
		includePaths.clear();
		Collections.addAll( this.includePaths, includeList );
		Collections.addAll( this.optionArgs, optionArgs );
		libURLs.clear();
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

		super();
		Collections.addAll( this.optionArgs, optionArgs );
		includePaths.clear();
		Collections.addAll( this.includePaths, includeList );
		libURLs.clear();
		Collections.addAll( this.libURLs, libUrls );
		this.programFilepath = programFilepath;
		this.jolieClassLoader = jolieClassLoader;
		this.inputStream = inputStream;
	}



	public Integer connectionsLimit() {
		return this.connectionsLimit;
	}

	public CorrelationEngine.Type correlationAlgorithm() {
		return this.correlationAlgorithm;
	}

	public String[] includePaths() {
		return includePaths.toArray( new String[] {} );
	}

	public String[] optionArgs() {
		return optionArgs.toArray( new String[] {} );
	}

	public URL[] libUrls() {
		return libURLs.toArray( new URL[] {} );
	}

	public InputStream inputStream() {
		return this.inputStream;
	}

	public String charset() {
		return this.charset;
	}

	public File programFilepath() {
		return this.programFilepath;
	}

	public String[] arguments() {
		return arguments.toArray( new String[] {} );
	}

	public Map< String, Scanner.Token > constants() {
		return this.constants;
	}

	public JolieClassLoader jolieClassLoader() {
		return jolieClassLoader;
	}

	public boolean isProgramCompiled() {
		return isProgramCompiled;
	}

	public boolean typeCheck() {
		return this.typeCheck;
	}

	public boolean tracer() {
		return this.tracer;
	}

	public String tracerMode() {
		return tracerMode;
	}

	public String tracerLevel() {
		return tracerLevel;
	}

	public boolean check() {
		return this.check;
	}

	public long responseTimeout() {
		return responseTimeout;
	}

	public boolean printStackTraces() {
		return printStackTraces;
	}

	public Level logLevel() {
		return this.logLevel;
	}

	public File programDirectory() {
		return programDirectory;
	}

	public void clear() {
		jolieClassLoader = null;
	}



}

