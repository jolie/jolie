package jolie.lang.parse.context;

import jolie.lang.Constants;
import java.io.Serializable;

/**
 * Implements the LSP specification of a Position.
 *
 * @param line The zero-based line of the Position.
 * @param character The zero-based character offset.
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#position">LSP
 *      position specification</a>
 */
public record Position(int line, int character) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	/**
	 * @throws IllegalArgumentException If line or character are negative.
	 */
	public Position {
		if( line < 0 || character < 0 )
			throw new IllegalArgumentException( "Position line and character must be non-negative." );
	}
}
