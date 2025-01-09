package jolie.lang.parse.module;

import java.util.List;

public class ImportPath {

	/**
	 * Dot-separated import target path from an import statement
	 */
	private final List< String > pathParts;

	protected ImportPath( List< String > pathParts ) {
		this.pathParts = pathParts;
	}

	protected List< String > pathParts() {
		return pathParts;
	}

	/**
	 * checks the format of target module path if it has a relative import format, by test if the first
	 * index is empty
	 *
	 * eg. .package.module or [ "", "package", "module" ] is a relative import while package.module or [
	 * "package", "module"] is not
	 *
	 * @return true if it has relative import format, false otherwise
	 *
	 */
	protected boolean isRelativeImport() {
		return pathParts.get( 0 ).isEmpty();
	}

	@Override
	public String toString() {
		return isRelativeImport() ? "." + String.join( ".", this.pathParts() ) : String.join( ".", this.pathParts() );
	}
}
