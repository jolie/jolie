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

