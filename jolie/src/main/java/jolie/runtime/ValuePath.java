package jolie.runtime;

/**
 * Represents a path to a value in a Jolie data structure. Examples: "data[0]",
 * "tree.field[2].subfield"
 */
public class ValuePath {
	private final String path;

	/**
	 * Creates a ValuePath from a path string.
	 *
	 * @param path the path string
	 * @param validate if true, validates the path format; if false, assumes path is already valid (use
	 *        false only with known-valid paths for performance)
	 * @throws IllegalArgumentException if validate is true and the path format is invalid
	 */
	public ValuePath( String path, boolean validate ) {
		if( validate ) {
			PvalHelper.parsePathString( path ); // Validates format
		}
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	@Override
	public boolean equals( Object other ) {
		if( !(other instanceof ValuePath) )
			return false;
		return path.equals( ((ValuePath) other).path );
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public String toString() {
		return path;
	}
}
