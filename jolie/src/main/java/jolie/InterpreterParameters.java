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


	public void setConnectionsLimit( int connLimit ) {
		this.connectionsLimit = connLimit;
	}

	public boolean hasConnectionsLimit() {
		return this.connectionsLimit != null;
	}

	public Integer connectionsLimit() {
		return this.connectionsLimit;
	}

	public void setConnectionCache( int connCache ) {
		this.connectionCache = connCache;
	}

	public boolean hasConnectionCache() {
		return this.connectionCache != null;
	}

	public Integer setConnectionCache() {
		return this.connectionCache;
	}

	public void correlationAlgorithm( CorrelationEngine.Type correlationAlgorithm ) {
		this.correlationAlgorithm = correlationAlgorithm;
	}

	public boolean hasCorrelationAlgorithm() {
		return this.correlationAlgorithm != null;
	}

	public CorrelationEngine.Type correlationAlgorithm() {
		return this.correlationAlgorithm;
	}

	public void includePaths( String[] includeList ) {
		includePaths.clear();
		Collections.addAll( this.includePaths, includeList );
	}

	public void addIncludePathItem( String includeItem ) {
		this.includePaths.add( includeItem );
	}

	public String[] includePaths() {
		return includePaths.toArray( new String[] {} );
	}

	public boolean hasIncludePaths() {
		return !this.includePaths.isEmpty();
	}

	public void setOptionArgs( String[] optionArgs ) {
		Collections.addAll( this.optionArgs, optionArgs );
	}

	public void addOptionArgsItem( String optionArgs ) {
		this.optionArgs.add( optionArgs );
	}

	public String[] optionArgs() {
		return optionArgs.toArray( new String[] {} );
	}

	public boolean hasOptionArgs() {
		return !this.optionArgs.isEmpty();
	}

	public void setLibUrls( URL[] libUrls ) {
		libURLs.clear();
		Collections.addAll( this.libURLs, libUrls );
	}

	public void addLibUrlsItem( URL libUrlItem ) {
		this.libURLs.add( libUrlItem );
	}

	public URL[] libUrls() {
		return libURLs.toArray( new URL[] {} );
	}

	public boolean hasLibUrls() {
		return !this.libURLs.isEmpty();
	}

	public void setInputStream( InputStream inputStream ) {
		this.inputStream = inputStream;
	}

	public boolean hasInputStream() {
		if( inputStream == null ) {
			return false;
		} else {
			return true;
		}
	}

	public InputStream inputStream() {
		return this.inputStream;
	}

	public void setCharset( String charset ) {
		this.charset = charset;
	}

	public boolean hasCharset() {
		return this.charset != null;
	}

	public String charset() {
		return this.charset;
	}

	public void setProgramFilepath( File programFilepath ) {
		this.programFilepath = programFilepath;
	}

	public boolean hasProgramFilePath() {
		if( programFilepath == null ) {
			return false;
		} else {
			return true;
		}
	}

	public File programFilepath() {
		return this.programFilepath;
	}

	public void setArguments( String[] arguments ) {
		Collections.addAll( this.arguments, arguments );
	}

	public void addArgumentItem( String argument ) {
		this.arguments.add( argument );
	}

	public String[] arguments() {
		return arguments.toArray( new String[] {} );
	}

	public boolean hasArguments() {
		return !this.arguments.isEmpty();
	}

	public void setConstants( Map< String, Scanner.Token > constants ) {
		this.constants.putAll( constants );
	}

	public void addConstantItem( String id, Scanner.Token token ) {
		constants.put( id, token );
	}

	public boolean hasConstants() {
		return !this.constants.isEmpty();
	}

	public Map< String, Scanner.Token > constants() {
		return this.constants;
	}

	public void setJolieClassLoader( JolieClassLoader jolieClassLoader ) {
		this.jolieClassLoader = jolieClassLoader;
	}

	public boolean hasJolieClassLoader() {
		if( jolieClassLoader == null ) {
			return false;
		} else {
			return true;
		}
	}

	public JolieClassLoader jolieClassLoader() {
		return jolieClassLoader;
	}

	public void setProgramCompiled( boolean programCompiled ) {
		this.isProgramCompiled = programCompiled;
	}

	public boolean isProgramCompiled() {
		return isProgramCompiled;
	}

	public void setTypeCheck( boolean typeCheck ) {
		this.typeCheck = typeCheck;
	}

	public boolean typeCheck() {
		return this.typeCheck;
	}

	public void setTracer( boolean tracer ) {
		this.tracer = tracer;
	}

	public boolean tracer() {
		return this.tracer;
	}

	public void setTracerMode( String tracerMode ) {
		this.tracerMode = tracerMode;
	}

	public String tracerMode() {
		return tracerMode;
	}

	public void setTracerLevel( String tracerLevel ) {
		this.tracerLevel = tracerLevel;
	}

	public String tracerLevel() {
		return tracerLevel;
	}

	public void setCheck( boolean check ) {
		this.check = check;
	}

	public boolean check() {
		return this.check;
	}

	public void setResponseTimeout( long responseTimeout ) {
		this.responseTimeout = responseTimeout;
	}

	public long responseTimeout() {
		return responseTimeout;
	}

	public void setPrintStackTraces( boolean printStackTraces ) {
		this.printStackTraces = printStackTraces;
	}

	public boolean printStackTraces() {
		return printStackTraces;
	}

	public void setLogLevel( Level logLevel ) {
		this.logLevel = logLevel;
	}

	public Level logLevel() {
		return this.logLevel;
	}

	public void setProgramDirectory( File programDirectory ) {
		this.programDirectory = programDirectory;
	}

	public File programDirectory() {
		return programDirectory;
	}



}

