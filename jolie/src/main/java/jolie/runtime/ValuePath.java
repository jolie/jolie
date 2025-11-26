package jolie.runtime;

/**
 * Represents a path to a value in a Jolie data structure. Examples: "data[0]",
 * "tree.field[2].subfield"
 */
public class ValuePath {
	private final String path;

	public ValuePath( String path ) {
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
