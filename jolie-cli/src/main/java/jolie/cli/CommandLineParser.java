/*
 * Copyright (C) 2008-2019 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.cli;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.stream.Collectors;
import jolie.Interpreter;
import jolie.JolieClassLoader;
import jolie.jap.JapURLConnection;
import jolie.lang.Constants;
import jolie.lang.parse.Scanner;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.util.UriUtils;

/**
 * A parser for JOLIE's command line arguments, providing methods for accessing them.
 * 
 * @author Fabrizio Montesi
 */
public class CommandLineParser implements AutoCloseable {
	private final static String OPTION_SEPARATOR = " ";

	private final int connectionsLimit;
	private final CorrelationEngine.Type correlationAlgorithmType;
	private final String[] includePaths;
	private final String[] packagePaths;
	private final String[] optionArgs;
	private final URL[] libURLs;
	private final InputStream programStream;
	private String charset = null;
	private final File programFilepath;
	private final String[] arguments;
	private final Map< String, Scanner.Token > constants = new HashMap<>();
	private final JolieClassLoader jolieClassLoader;
	private final boolean isProgramCompiled;
	private final boolean typeCheck;
	private final boolean tracer;
	private final String tracerMode;
	private final String tracerLevel;
	private final boolean check;
	private final long responseTimeout;
	private final boolean printStackTraces;
	private final Level logLevel;
	private final String executionTarget;
	private final Optional< Path > parametersFilepath;
	private File programDirectory = null;
	private int cellId = 0;

	/**
	 * Closes the underlying {@link InputStream} to the target Jolie program.
	 */
	@Override
	public void close()
		throws IOException {
		programStream.close();
	}

	private static String getOptionString( String option, String description ) {
		return ('\t' + option + "\t\t" + description + '\n');
	}

	private String getVersionString() {
		return ("Jolie " + Constants.VERSION + "  " + Constants.COPYRIGHT);
	}

	public int cellId() {
		return cellId;
	}

	/**
	 * Returns the usage help message of Jolie.
	 * 
	 * @return the usage help message of Jolie.
	 */
	protected String getHelpString() {
		return new StringBuilder()
			.append( "\n\n" )
			.append( Constants.ASCII_LOGO )
			.append( "\n\n\n" )
			.append( getVersionString() )
			.append( "\n\nGitHub: https://github.com/jolie/jolie" )
			.append( "\nTwitter: https://twitter.com/jolielang" )
			.append( "\nWebsite: https://www.jolie-lang.org/" )
			.append( "\n\nUsage: jolie [options] program_file [program arguments]\n\n" )
			.append( "Available options:\n" )
			.append(
				getOptionString( "-h, --help", "Display this help information" ) )
			// TODO include doc for -l and -i
			.append( getOptionString( "-C ConstantIdentifier=ConstantValue",
				"Sets constant ConstantIdentifier to ConstantValue before starting execution \n"
					+ "-C ConstantIdentifier=ConstantValue".replaceAll( "(.)", " " ) + "\t\t\t"
					+ "(under Windows use quotes or double-quotes, e.g., -C \"ConstantIdentifier=ConstantValue\" )" ) )
			.append(
				getOptionString( "--connlimit [number]", "Set the maximum number of active connection threads" ) )
			.append(
				getOptionString( "--conncache [number]",
					"Set the maximum number of cached persistent output connections" ) )
			.append(
				getOptionString( "--responseTimeout [number]",
					"Set the timeout for request-response invocations (in milliseconds)" ) )
			.append(
				getOptionString( "--correlationAlgorithm [simple|hash]",
					"Set the algorithm to use for message correlation" ) )
			.append(
				getOptionString( "--log [severe|warning|info|fine]", "Set the logging level (default: info)" ) )
			.append(
				getOptionString( "--stackTraces", "Activate the printing of Java stack traces (default: false)" ) )
			.append(
				getOptionString( "--typecheck [true|false]",
					"Check for correlation and other data related typing errors (default: false)" ) )
			.append(
				getOptionString( "--check", "Check for syntactic and semantic errors." ) )
			.append(
				getOptionString( "--trace [console|file]",
					"Activate tracer. console prints out in the console, file creates a json file" ) )
			.append(
				getOptionString( "--traceLevel [all|comm|comp]",
					"Defines tracer level: all - all the traces; comm - only communication traces; comp - only computation traces. Default is all. " ) )
			.append(
				getOptionString( "--charset [character encoding, e.g., UTF-8]",
					"Character encoding of the source *.ol/*.iol (default: system-dependent, on GNU/Linux UTF-8)" ) )
			.append(
				getOptionString( "-p PATH",
					"Add PATH to the set of paths where modules are looked up" ) )
			.append(
				getOptionString( "-s [service name], --service [service name]",
					"Specify a service in the module to execute (not necessary if the module contains only one service definition)" ) )
			.append(
				getOptionString( "--params json_file",
					"Use the contents of json_file as the argument of the service being executed." ) )
			.append(
				getOptionString( "--version", "Display this program version information" ) )
			.append(
				getOptionString( "--cellId",
					"set an integer as cell identifier, used for creating message ids. (max: "
						+ Integer.MAX_VALUE + ")" ) )
			.toString();
	}

	private void parseCommandLineConstant( String input )
		throws IOException {
		try {
			// for command line options use the system's default charset (null)
			Scanner scanner =
				new Scanner( new ByteArrayInputStream( input.getBytes() ), new URI( "urn:CommandLine" ), null );
			Scanner.Token token = scanner.getToken();
			if( token.is( Scanner.TokenType.ID ) ) {
				String id = token.content();
				token = scanner.getToken();
				if( token.isNot( Scanner.TokenType.ASSIGN ) ) {
					throw new IOException(
						"expected = after constant identifier " + id + ", found token type " + token.type() );
				}
				token = scanner.getToken();
				if( token.isValidConstant() == false ) {
					throw new IOException( "expected constant value for constant identifier " + id
						+ ", found token type " + token.type() );
				}
				constants.put( id, token );
			} else {
				throw new IOException( "expected constant identifier, found token type " + token.type() );
			}
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
	}

	/**
	 * Constructor
	 * 
	 * @param args the command line arguments
	 * @param parentClassLoader the ClassLoader to use for finding resources
	 * @throws CommandLineException if the command line is not valid or asks for simple information.
	 *         (like --help and --version)
	 * @throws java.io.IOException
	 */
	public CommandLineParser( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException {
		this( args, parentClassLoader, ArgumentHandler.DEFAULT_ARGUMENT_HANDLER );
	}

	/**
	 * Constructor
	 * 
	 * @param args the command line arguments
	 * @param parentClassLoader the ClassLoader to use for finding resources
	 * @param argHandler
	 * @throws CommandLineException
	 * @throws IOException
	 */
	public CommandLineParser( String[] args, ClassLoader parentClassLoader, ArgumentHandler argHandler )
		throws CommandLineException, IOException {
		this( args, parentClassLoader, argHandler, false );
	}

	/**
	 * Constructor
	 * 
	 * @param args the command line arguments
	 * @param parentClassLoader the ClassLoader to use for finding resources
	 * @param ignoreFile do not open file that is given as parameter (used for internal services)
	 * @throws CommandLineException
	 * @throws IOException
	 */
	public CommandLineParser( String[] args, ClassLoader parentClassLoader, boolean ignoreFile )
		throws CommandLineException, IOException {
		this( args, parentClassLoader, ArgumentHandler.DEFAULT_ARGUMENT_HANDLER, ignoreFile );
	}

	/**
	 * Constructor
	 * 
	 * @param args the command line arguments
	 * @param parentClassLoader the ClassLoader to use for finding resources
	 * @param argHandler
	 * @param ignoreFile do not open file that is given as parameter (used for internal services)
	 * @throws CommandLineException
	 * @throws IOException
	 */
	public CommandLineParser( String[] args, ClassLoader parentClassLoader, ArgumentHandler argHandler,
		boolean ignoreFile )
		throws CommandLineException, IOException {
		List< String > argsList = Arrays.asList( args );

		String csetAlgorithmName = "simple";
		Deque< String > optionsList = new LinkedList<>();
		boolean bTracer = false;
		boolean bStackTraces = false;
		boolean bCheck = false;
		boolean bTypeCheck = false; // Default for typecheck
		Level lLogLevel = Level.INFO;
		String tMode = "console";
		String tLevel = "all";
		List< String > programArgumentsList = new ArrayList<>();
		Deque< String > includeList = new ArrayDeque<>();
		Deque< String > libList = new ArrayDeque<>();
		Deque< String > packagesList = new ArrayDeque<>();
		int cLimit = -1;
		long rTimeout = 60 * 1000; // 1 minute, in milliseconds
		String pwd = UriUtils.normalizeWindowsPath( new File( "" ).getCanonicalPath() );
		String tService = null;
		Path tParams = null;
		includeList.add( pwd );
		includeList.add( "include" );
		libList.add( pwd );
		libList.add( "ext" );
		libList.add( "lib" );
		String olFilepath = null;
		String japUrl = null;
		int i = 0;
		// First parse Jolie arguments with the Jolie program argument
		for( ; i < argsList.size() && olFilepath == null; i++ ) {
			if( "--help".equals( argsList.get( i ) ) || "-h".equals( argsList.get( i ) ) ) {
				throw new CommandLineException( getHelpString() );
			} else if( "-C".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				try {
					parseCommandLineConstant( argsList.get( i ) );
				} catch( IOException e ) {
					throw new CommandLineException( "Invalid constant definition, reason: " + e.getMessage() );
				}
				optionsList.add( argsList.get( i ) );
			} else if( "-i".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				if( japUrl != null ) {
					argsList.set( i, argsList.get( i ).replace( "$JAP$", japUrl ) );
				}
				Collections.addAll( includeList, argsList.get( i ).split( jolie.lang.Constants.PATH_SEPARATOR ) );
				optionsList.add( argsList.get( i ) );
			} else if( "-l".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				if( japUrl != null ) {
					argsList.set( i, argsList.get( i ).replace( "$JAP$", japUrl ) );
				}
				String[] tmp = argsList.get( i ).split( jolie.lang.Constants.PATH_SEPARATOR );
				for( String libPath : tmp ) {
					Optional< String > path = findLibPath( libPath, includeList, parentClassLoader );
					path.ifPresent( libList::add );
					// else {
					// throw new IOException( "Could not locate library: " + libPath );
					// }
				}
				optionsList.add( argsList.get( i ) );
			} else if( "-p".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				Collections.addAll( packagesList, argsList.get( i ).split( jolie.lang.Constants.PATH_SEPARATOR ) );
				optionsList.add( argsList.get( i ) );
			} else if( "--connlimit".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				cLimit = Integer.parseInt( argsList.get( i ) );
				optionsList.add( argsList.get( i ) );
			} else if( "--responseTimeout".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				rTimeout = Long.parseLong( argsList.get( i ) );
				optionsList.add( argsList.get( i ) );
			} else if( "--correlationAlgorithm".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				csetAlgorithmName = argsList.get( i );
				optionsList.add( argsList.get( i ) );
			} else if( "--typecheck".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				String typeCheckStr = argsList.get( i );
				optionsList.add( argsList.get( i ) );
				if( "false".equals( typeCheckStr ) ) {
					bTypeCheck = false;
				} else if( "true".equals( typeCheckStr ) ) {
					bTypeCheck = true;
				}
			} else if( "--stackTraces".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				bStackTraces = true;
			} else if( "--check".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				bCheck = true;
			} else if( "--trace".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				bTracer = true;
				switch( argsList.get( i + 1 ) ) {
				case "console":
					tMode = "console";
					i++;
					optionsList.add( argsList.get( i ) );
					break;
				case "file":
					tMode = "file";
					i++;
					optionsList.add( argsList.get( i ) );
					break;
				}
			} else if( "--traceLevel".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				switch( argsList.get( i + 1 ) ) {
				case "all":
					tLevel = "all";
					i++;
					optionsList.add( argsList.get( i ) );
					break;
				case "comm":
					tLevel = "comm";
					i++;
					optionsList.add( argsList.get( i ) );
					break;
				case "comp":
					tLevel = "comp";
					i++;
					optionsList.add( argsList.get( i ) );
					break;
				}
			} else if( "--log".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				String level = argsList.get( i );
				switch( level ) {
				case "severe":
					lLogLevel = Level.SEVERE;
					break;
				case "warning":
					lLogLevel = Level.WARNING;
					break;
				case "fine":
					lLogLevel = Level.FINE;
					break;
				case "info":
					lLogLevel = Level.INFO;
					break;
				}
				optionsList.add( argsList.get( i ) );
			} else if( "--charset".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				charset = argsList.get( i );
				optionsList.add( argsList.get( i ) );
			} else if( "--cellId".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				try {
					cellId = Integer.parseInt( argsList.get( i ) );
				} catch( Exception e ) {
					System.out
						.println(
							"The number specified for cellId (" + argsList.get( i ) + ") is not allowed. Set to 0" );
				}
				optionsList.add( argsList.get( i ) );
			} else if( "--service".equals( argsList.get( i ) ) || "-s".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				if( tService != null ) {
					throw new CommandLineException( "Execution service is already defined" );
				}
				tService = argsList.get( i );
				optionsList.add( argsList.get( i ) );
			} else if( "--params".equals( argsList.get( i ) ) ) {
				optionsList.add( argsList.get( i ) );
				i++;
				if( tParams != null ) {
					throw new CommandLineException( "Service parameters file already specified" );
				}
				tParams = Paths.get( argsList.get( i ) );
				optionsList.add( argsList.get( i ) );
				if( !Files.exists( tParams ) ) {
					throw new FileNotFoundException( argsList.get( i ) );
				}
			} else if( "--version".equals( argsList.get( i ) ) ) {
				throw new CommandLineException( getVersionString() );
			} else if( olFilepath == null && !argsList.get( i ).startsWith( "-" ) ) {
				final String path = argsList.get( i );
				if( path.endsWith( ".jap" ) ) {
					for( String includePath : prepend( "", includeList ) ) {
						try {
							String japFilename = UriUtils.normalizeJolieUri(
								UriUtils.normalizeWindowsPath( UriUtils.resolve( includePath, path ) ) );
							if( Files.exists( Paths.get( japFilename ) ) ) {
								try( JarFile japFile = new JarFile( japFilename ) ) {
									Manifest manifest = japFile.getManifest();
									olFilepath = UriUtils
										.normalizeWindowsPath( parseJapManifestForMainProgram( manifest, japFile ) );
									libList.add( japFilename );
									Collection< String > japOptions = parseJapManifestForOptions( manifest );
									argsList.addAll( i + 1, japOptions );
									japUrl = japFilename + "!";
									programDirectory = new File( japFilename ).getParentFile();
								}
								break;
							}
						} catch( URISyntaxException | InvalidPathException e ) {
						}
					}
					if( olFilepath == null ) {
						throw new IOException( "Could not locate " + path );
					}
				} else {
					olFilepath = path;
				}
			} else { // FIXME: Dead code?
				// It's an unrecognized argument
				int newIndex = argHandler.onUnrecognizedArgument( argsList, i );
				if( newIndex == i ) {
					// The handler didn't change the index.
					// We abort so to avoid infinite looping.
					throw new CommandLineException( "Unrecognized command line option: " + argsList.get( i ) );
				}
				i = newIndex;
			}
		}
		// Now parse the command line arguments for the Jolie program
		for( ; i < argsList.size() && olFilepath != null; i++ ) {
			programArgumentsList.add( argsList.get( i ) );
		}

		typeCheck = bTypeCheck;
		logLevel = lLogLevel;
		tracerMode = tMode;
		tracerLevel = tLevel;
		printStackTraces = bStackTraces;
		executionTarget = tService;
		parametersFilepath = Optional.ofNullable( tParams );

		correlationAlgorithmType = CorrelationEngine.Type.fromString( csetAlgorithmName );
		if( correlationAlgorithmType == null ) {
			throw new CommandLineException( "Unrecognized correlation algorithm: " + csetAlgorithmName );
		}
		arguments = programArgumentsList.toArray( new String[ 0 ] );
		// whitepages = whitepageList.toArray( new String[ whitepageList.size() ] );

		if( olFilepath == null ) {
			throw new CommandLineException( "Input file not specified." );
		}

		connectionsLimit = cLimit;
		responseTimeout = rTimeout;

		List< URL > urls = new ArrayList<>();
		for( String pathInList : libList ) {
			String path = pathInList;
			if( path.contains( "!/" ) && !path.startsWith( "jap:" ) && !path.startsWith( "jar:" ) ) {
				path = "jap:file:" + path;
			}
			if( path.endsWith( ".jar" ) || path.endsWith( ".jap" ) ) {
				if( path.startsWith( "jap:" ) ) {
					urls.add( new URL( path + "!/" ) );
				} else {
					urls.add( new URL( "jap:file:" + path + "!/" ) );
				}
			} else if( new File( path ).isDirectory() ) {
				urls.add( new URL( "file:" + path + "/" ) );
			} else if( path.endsWith( "/*" ) ) {
				Path dir;
				if( path.startsWith( "file:" ) ) {
					dir = Paths.get( path.substring( 5, path.length() - 2 ) );
				} else {
					dir = Paths.get( path.substring( 0, path.length() - 2 ) );
				}

				if( Files.isDirectory( dir ) ) {
					dir = dir.toRealPath();
					List< String > archives = Files.list( dir ).map( Path::toString )
						.filter( p -> p.endsWith( ".jar" ) || p.endsWith( ".jap" ) ).collect( Collectors.toList() );
					for( String archive : archives ) {
						String scheme = archive.substring( archive.length() - 3 ); // "jap" or "jar"
						urls.add( new URL( scheme + ":" + Paths.get( archive ).toUri().toString() + "!/" ) );
					}
				}
			} else if( path.contains( ":" ) ) { // Try to avoid unnecessary MalformedURLExceptions, filling up the stack
												// trace eats time.
				try {
					urls.add( new URL( path ) );
				} catch( MalformedURLException e ) {
				}
			}
		}
		urls.add( new URL( "file:/" ) );
		libURLs = urls.toArray( new URL[ 0 ] );
		jolieClassLoader = new JolieClassLoader( libURLs, parentClassLoader );

		for( URL url : libURLs ) {
			if( url.getProtocol().startsWith( "jap" ) ) {
				includeList.add( url.toString() );
			}
		}

		GetOLStreamResult olResult =
			getOLStream( ignoreFile, olFilepath, includeList, libList, optionsList, jolieClassLoader );

		if( olResult.stream == null ) {
			if( ignoreFile ) {
				olResult.source = olFilepath;
				olResult.stream = new ByteArrayInputStream( new byte[] {} );
			} else if( olFilepath.endsWith( ".ol" ) ) {
				// try to read the compiled version of the ol file
				olFilepath += "c";
				olResult = getOLStream( ignoreFile, olFilepath, includeList, libList, optionsList,
					jolieClassLoader );
				if( olResult.stream == null ) {
					throw new FileNotFoundException( olFilepath );
				}
			} else {
				throw new FileNotFoundException( olFilepath );
			}
		}

		isProgramCompiled = olFilepath.endsWith( ".olc" );
		tracer = bTracer && !isProgramCompiled;
		check = bCheck && !isProgramCompiled;
		programFilepath = new File( olResult.source );
		programStream = olResult.stream;

		includePaths = new LinkedHashSet<>( includeList ).toArray( new String[ 0 ] );
		packagePaths = new LinkedHashSet<>( packagesList ).toArray( new String[ 0 ] );
		optionArgs = optionsList.toArray( new String[ 0 ] );
	}

	/**
	 * Adds the standard include and library subdirectories of the program to the classloader paths.
	 */
	/*
	 * private void addProgramDirectories( List< String > includeList, List< String > libList, String
	 * olFilepath ) { File olFile = new File( olFilepath ); if ( olFile.exists() ) { File parent =
	 * olFile.getParentFile(); if ( parent != null && parent.isDirectory() ) { String parentPath =
	 * parent.getAbsolutePath(); includeList.add( parentPath ); includeList.add( parentPath + "/include"
	 * ); libList.add( parentPath ); libList.add( parentPath + "/lib" ); } } }
	 */

	private static String parseJapManifestForMainProgram( Manifest manifest, JarFile japFile ) {
		String filepath = null;
		if( manifest != null ) { // See if a main program is defined through a Manifest attribute
			Attributes attrs = manifest.getMainAttributes();
			filepath = attrs.getValue( Constants.Manifest.MAIN_PROGRAM );
		}

		if( filepath == null ) { // Main program not defined, we make <japName>.ol and <japName>.olc guesses
			String name = new File( japFile.getName() ).getName();
			filepath = new StringBuilder()
				.append( name.subSequence( 0, name.lastIndexOf( ".jap" ) ) )
				.append( ".ol" )
				.toString();
			if( japFile.getEntry( filepath ) == null ) {
				filepath = null;
				filepath = filepath + 'c';
				if( japFile.getEntry( filepath ) == null ) {
					filepath = null;
				}
			}
		}

		if( filepath != null ) {
			filepath = new StringBuilder()
				.append( "jap:file:" )
				.append( UriUtils.normalizeWindowsPath( japFile.getName() ) )
				.append( "!/" )
				.append( filepath )
				.toString();
		}
		return filepath;
	}

	private static Collection< String > parseJapManifestForOptions( Manifest manifest )
		throws IOException {
		Collection< String > optionList = new ArrayList<>();
		if( manifest != null ) {
			Attributes attrs = manifest.getMainAttributes();
			String options = attrs.getValue( Constants.Manifest.OPTIONS );
			if( options != null ) {
				String[] tmp = options.split( OPTION_SEPARATOR );
				Collections.addAll( optionList, tmp );
			}
		}
		return optionList;
	}

	private static class GetOLStreamResult {
		private String source;
		private InputStream stream;
	}

	private GetOLStreamResult getOLStream( boolean ignoreFile, String olFilepath, Deque< String > includePaths,
		Deque< String > libPaths,
		Deque< String > optionsList, ClassLoader classLoader )
		throws IOException {
		GetOLStreamResult result = new GetOLStreamResult();
		if( ignoreFile ) {
			return result;
		}

		URL olURL = null;
		File f = new File( olFilepath ).getAbsoluteFile();
		if( f.exists() ) {
			result.stream = new FileInputStream( f );
			result.source = f.toURI().getSchemeSpecificPart();
			programDirectory = f.getParentFile();
		} else {
			for( String includePath : includePaths ) {
				if( includePath.startsWith( "jap:" ) ) {
					try {
						olURL = new URL( UriUtils.normalizeJolieUri(
							UriUtils.normalizeWindowsPath( UriUtils.resolve( includePath, olFilepath ) ) ) );
						result.stream = olURL.openStream();
						result.source = olURL.toString();
						break;
					} catch( URISyntaxException | IOException e ) {
					}
				} else {
					f = new File(
						includePath +
							jolie.lang.Constants.FILE_SEPARATOR +
							olFilepath );
					if( f.exists() ) {
						f = f.getAbsoluteFile();
						result.stream = new FileInputStream( f );
						result.source = f.toURI().getSchemeSpecificPart();
						programDirectory = f.getParentFile();
						break;
					}
				}
			}

			if( result.stream == null ) {
				try {
					olURL = new URL( olFilepath );
					result.stream = olURL.openStream();
					result.source = olFilepath;
					if( result.stream == null ) {
						throw new MalformedURLException();
					}
				} catch( MalformedURLException e ) {
					olURL = classLoader.getResource( olFilepath );
					if( olURL != null ) {
						result.stream = olURL.openStream();
						result.source = olURL.toString();
					}
				}
				if( programDirectory == null && olURL != null && olURL.getPath() != null ) {
					// Try to extract the parent directory of the JAP/JAR library file
					try {
						File urlFile = new File( JapURLConnection.NESTING_SEPARATION_PATTERN
							.split( new URI( olURL.getPath() ).getSchemeSpecificPart() )[ 0 ] ).getAbsoluteFile();
						if( urlFile.exists() ) {
							programDirectory = urlFile.getParentFile();
						}
					} catch( URISyntaxException e ) {
					}
				}
			}
		}
		if( result.stream != null ) {
			final Optional< String > parent;
			if( f.exists() && f.getParent() != null ) {
				parent = Optional.of( f.getParent() );
				libPaths.addFirst( parent.get() + File.separator + "lib/*" );
			} else if( olURL != null ) {
				String urlString = olURL.toString();
				parent = Optional.of( urlString.substring( 0, urlString.lastIndexOf( '/' ) + 1 ) );
			} else {
				parent = Optional.empty();
			}
			parent.ifPresent( path -> {
				includePaths.addFirst( parent.get() );
				optionsList.addFirst( parent.get() );
				optionsList.addFirst( "-l" );
			} );
			result.stream = new BufferedInputStream( result.stream );
		}
		return result;
	}

	private static Optional< String > findLibPath( String libPath, Deque< String > includePaths,
		ClassLoader classLoader ) {
		if( libPath.endsWith( "*" ) ) {
			return findLibPath( libPath.substring( 0, libPath.length() - 1 ), includePaths, classLoader )
				.map( p -> p + "*" );
		}

		for( String context : prepend( "", includePaths ) ) {
			try {
				String path = UriUtils.normalizeJolieUri(
					UriUtils.normalizeWindowsPath(
						UriUtils.resolve(
							context,
							libPath ) ) );

				if( Files.exists( Paths.get( path ) ) ) {
					return Optional.of( path );
				} else {
					URL url = classLoader.getResource( path );
					if( url != null ) {
						return Optional.of( url.toString() );
					}
				}
			} catch( URISyntaxException | InvalidPathException e ) {
			}
		}

		return Optional.empty();
	}

	private static < T > List< T > prepend( T element, Collection< T > collection ) {
		List< T > result = new ArrayList<>( collection.size() + 1 );
		result.add( element );
		result.addAll( collection );
		return result;
	}

	public Interpreter.Configuration getInterpreterConfiguration() throws CommandLineException, IOException {
		return Interpreter.Configuration.create(
			connectionsLimit,
			cellId,
			correlationAlgorithmType,
			includePaths,
			optionArgs,
			libURLs,
			programStream,
			charset,
			programFilepath,
			arguments,
			constants,
			jolieClassLoader,
			isProgramCompiled,
			typeCheck,
			tracer,
			tracerLevel,
			tracerMode,
			check,
			printStackTraces,
			responseTimeout,
			logLevel,
			programDirectory,
			packagePaths,
			executionTarget,
			parametersFilepath );

	}

	/**
	 * A handler for unrecognized arguments, meant to be implemented by classes that wants to extend the
	 * behaviour of {@link CommandLineParser}.
	 * 
	 * @author Fabrizio Montesi
	 */
	public interface ArgumentHandler {
		/**
		 * Called when {@link CommandLineParser} cannot recognize a command line argument.
		 * 
		 * @param argumentsList the argument list.
		 * @param index the index at which the unrecognized argument has been found in the list.
		 * @return the new index at which the {@link CommandLineParser} should continue parsing the
		 *         arguments.
		 * @throws CommandLineException if the argument is invalid or not recognized.
		 */
		int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException;

		/**
		 * Default {@link ArgumentHandler}. It just throws a {@link CommandLineException} when it finds an
		 * unrecognised option.
		 */
		ArgumentHandler DEFAULT_ARGUMENT_HANDLER =
			( List< String > argumentsList, int index ) -> {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			};
	}
}
