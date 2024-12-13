package jolie.lang.parse.context;

/**
 * A text document range with zero-based start and end positions. Implements the LSP specification
 * of a Range.
 *
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#range">LSP
 *      range specification</a>
 * @param start The start Position of the Range.
 * @param end The end Position of the Range.
 */
public record Range(Position start, Position end) {
	public Range {
		if( start == null )
			throw new IllegalArgumentException( "Range cannot have a null start." );
		if( end == null )
			throw new IllegalArgumentException( "Range cannot have a null end." );
	}

}
