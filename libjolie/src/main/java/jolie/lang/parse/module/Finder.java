package jolie.lang.parse.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

/**
 * An interface description for the module finder of Jolie Module system
 */
public interface Finder {

	/**
	 * finds module based on a passing module target string
	 * 
	 * e.g. for finding module a.b, the receiving parameter should be ['a', 'b'], likewise, for a
	 * relative module .c.d, the passing parameter should be ['', 'c', 'd']
	 * 
	 * @param moduleTargets list of dot separated string resemble a module target path
	 * @return Source Object which provides stream object for parser to use
	 */
	Source find( List< String > moduleTargetTokens, URI source ) throws ModuleNotFoundException;

	/**
	 * Returns an array of considering paths for resolving an absolute import of the finder.
	 * 
	 * @return an array of considering paths for resolving an absolute import
	 */
	Path[] packagePaths();


	/**
	 * checks the format of a dot separated string list if it has a relative import format or not. by
	 * test if the first index is empty
	 * 
	 * eg. .package.module or [ "", "package", "module" ] is a relative import while package.module or [
	 * "package", "module"] is not
	 * 
	 * @param moduleTargetTokens dot separated string from import statement
	 * 
	 * @return true if it has relative import format, false otherwise
	 * 
	 */
	static boolean isRelativeImport( List< String > moduleTargetTokens ) {
		return moduleTargetTokens.get( 0 ).isEmpty() ? true : false;
	}


	/**
	 * Perform a lookup to a jap filename from basePath
	 * 
	 * @param basePath base path for lookup
	 * @param filename a filename
	 * @return a new File of jap file, null if file is not found
	 */
	static File japLookup( Path basePath, String filename ) throws FileNotFoundException {
		Path japPath = basePath.resolve( filename + ".jap" );
		if( Files.exists( japPath ) ) {
			return japPath.toFile();
		}
		throw new FileNotFoundException( japPath.toAbsolutePath().toString() );
	}

	/**
	 * Perform a lookup to a ol filename from basePath
	 * 
	 * @param basePath base path for lookup
	 * @param filename a filename
	 * @return a new path of ol file, null if file is not found
	 */
	static File olLookup( Path basePath, String filename ) throws FileNotFoundException {
		Path olPath = basePath.resolve( filename + ".ol" );
		if( Files.exists( olPath ) ) {
			return olPath.toFile();
		}
		throw new FileNotFoundException( olPath.toString() );
	}

}


