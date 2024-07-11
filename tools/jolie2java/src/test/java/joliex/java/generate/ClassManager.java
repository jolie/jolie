package joliex.java.generate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.util.ProgramInspector;
import joliex.java.Jolie2Java;
import joliex.java.Jolie2JavaCommandLineParser;

public class ClassManager {

	public static final Path OUTPUT_DIRECTORY = Path.of( "./target", "generated-test-sources" );
	public static final String PACKAGE_NAME = "com.test";
	public static final String SOURCES_PACKAGE = ".spec";
	public static final String TYPE_PACKAGE = PACKAGE_NAME + SOURCES_PACKAGE + ".types";
	public static final String FAULT_PACKAGE = PACKAGE_NAME + SOURCES_PACKAGE + ".faults";
	private static final URLClassLoader CLASSLOADER;

	static {
		try { CLASSLOADER = URLClassLoader.newInstance( new URL[] { OUTPUT_DIRECTORY.toUri().toURL() } ); } 
		catch( MalformedURLException e ) { throw new RuntimeException( e ); }
	}

	public static void generateClasses() throws CommandLineException, IOException, CodeCheckException {
		deleteClasses();

		final Jolie2JavaCommandLineParser cmdParser = Jolie2JavaCommandLineParser.create( 
			new String[] { "src/test/resources/main.ol" },
			Jolie2Java.class.getClassLoader() );

		final ProgramInspector inspector = Jolie2Java.getInspector( cmdParser );

		final JavaDocumentCreator jdc = new JavaDocumentCreator( OUTPUT_DIRECTORY.toString(), null, false );

		jdc.translateServices( inspector );
	}

	public static void compileClasses() throws IOException {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		final OutputStream output = new OutputStream() {
			private final StringBuilder sb = new StringBuilder();

			@Override
			public void write( int b ) throws IOException {
				this.sb.append( (char) b );
			}

			@Override
			public String toString() {
				return this.sb.toString();
			}
		};
		final String[] filesToBeCompiled = Files.walk( OUTPUT_DIRECTORY )
			.filter( Files::isRegularFile )
			.map( Path::toString )
			.toArray( String[]::new );
			
		compiler.run( null, null, output, filesToBeCompiled );
		System.out.println( output );
	}

	public static void deleteClasses() throws IOException {
		Files.walkFileTree( OUTPUT_DIRECTORY, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			  	Files.deleteIfExists(file);
			  	return FileVisitResult.CONTINUE;
			}
		 
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			  	Files.deleteIfExists(file);
			  	return FileVisitResult.CONTINUE;
			}
		 
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null)
					throw exc;
				Files.deleteIfExists(dir);
			 	return FileVisitResult.CONTINUE;
			}
		} );
	}

	public static Class<?> getClass( String packageName, String className ) throws ClassNotFoundException {
		return Class.forName( packageName + "." + className, true, CLASSLOADER );
	}

	public static Class<?> getTypeClass( String className ) throws ClassNotFoundException {
		return getClass( TYPE_PACKAGE, className );
	}

	public static Class<?> getInterfaceClass( String className ) throws ClassNotFoundException {
		return getClass( PACKAGE_NAME + SOURCES_PACKAGE, className );
	}
}
