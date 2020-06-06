package jolie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

class JolieRunner {

	public static Interpreter interpreter;

	public static void run( String[] args, ClassLoader parentClassLoader, File programDirectory )
		throws CommandLineException, FileNotFoundException, IOException, InterpreterException {
		interpreter = new Interpreter( args, parentClassLoader, null );
		interpreter.run();

		Runtime.getRuntime().addShutdownHook( new Thread() {
			@Override
			public void run() {
				interpreter.exit( -1 );
			}
		} );
	}

	public static void stop() {
		interpreter.exit( -1 );
	}


}
