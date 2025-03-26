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
		return this.pathParts;
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
		return this.pathParts.get( 0 ).isEmpty();
	}

	public String toRelativePathString() {
		if( !this.isRelativeImport() ) {
			throw new IllegalStateException( "This import path is targeting packages module" );
		}
		String joinedPath = String.join( "/", this.pathParts() );
		if( joinedPath.isEmpty() ) {
			throw new IllegalStateException( "This relative import path must not be empty at this state" );
		}
		return joinedPath.substring( 1 );
	}

	@Override
	public String toString() {
		return String.join( ".", this.pathParts() );
	}
}
