package jolie.lang.parse.module;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

/**
 * An interface description for the module finder of Jolie Module system
 */
public interface ModuleFinder {

	/*
	 * Default module name to look for when the import path points to a jolie package
	 */
	public static final String DEFAULT_MODULE_NAME = "main";

	/**
	 * finds module based on a passing module target string
	 *
	 * e.g. for finding module a.b, the receiving parameter should be ['a', 'b'], likewise, for a
	 * relative module .c.d, the passing parameter should be ['', 'c', 'd']
	 *
	 * @param importPath a class represent a importing target path
	 * @return Source Object which provides stream object for parser to use
	 */
	ModuleSource find( URI source, ImportPath importPath ) throws ModuleNotFoundException;

	/**
	 * Perform a lookup to a jap filename from basePath
	 *
	 * @param basePath base path for lookup
	 * @param filename a filename
	 * @return a new File of jap file, null if file is not found
	 */
	static Path japLookup( Path basePath, String filename ) throws FileNotFoundException {
		Path japPath = basePath.resolve( filename + ".jap" );
		if( Files.exists( japPath ) ) {
			return japPath;
		}
		throw new FileNotFoundException( japPath.toAbsolutePath().toString() );
	}

	/**
	 * Perform a lookup to a ol filename from basePath
	 *
	 * @param basePath base path for lookup
	 * @param filename a filename
	 * @return a new path of ol file,
	 * @throws FileNotFoundException if file is not found
	 */
	static Path olLookup( Path basePath, String filename ) throws FileNotFoundException {
		Path olPath = basePath.resolve( filename + ".ol" );
		if( Files.exists( olPath ) ) {
			return olPath;
		}
		throw new FileNotFoundException( olPath.toString() );
	}

}
