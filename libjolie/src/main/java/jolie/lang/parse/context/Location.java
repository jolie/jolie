package jolie.lang.parse.context;

import java.net.URI;

/**
 * Implements of the LSP specification of a Location.
 *
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#location">LSP
 *      location specification</a>
 * @param documentUri The file of the Location.
 * @param range The Range in the document.
 */
public record Location(URI documentUri, Range range) {
	public Location( URI documentUri, int startLine, int endLine, int startCharacter, int endCharacter ) {
		this( documentUri,
			new Range( new Position( startLine, startCharacter ), new Position( endLine, endCharacter ) ) );
	}

	public Location {
		if( documentUri == null )
			throw new IllegalArgumentException( "documentUri cannot be null." );
		if( range == null )
			throw new IllegalArgumentException( "range cannot be null" );
	}

}
