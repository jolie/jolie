package jolie.lang.parse.context;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jolie.lang.Constants;

/**
 *
 * @param textLocation
 * @param enclosingCode
 */
public record ParsingContext(Location textLocation, List< String > enclosingCode) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	public static final ParsingContext DEFAULT =
		new ParsingContext( URI.create( "urn:undefined" ), 0, 0, 0, 0, List.of() );

	public ParsingContext( URI uri, int startLine, int endLine, int startCharacter, int endCharacter,
		List< String > enclosingCode ) {
		this( new Location( uri, startLine, endLine, startCharacter, endCharacter ), enclosingCode );
	}

	/**
	 *
	 * @return The URI of the file containing the code
	 */
	public URI source() {
		return textLocation().documentUri();
	}

	/**
	 * @return The name of the file containing the code.
	 */
	public String sourceName() {
		try {
			Path path = Paths.get( source() );
			return path.toString();
		} catch( InvalidPathException | FileSystemNotFoundException e ) {
			return source().toString();
		}
	}

	/**
	 * @return The zero-based starting line of the code range.
	 */
	public int startLine() {
		return textLocation().range().start().line();
	}

	/**
	 * @return The zero-based ending line of the code range.
	 */
	public int endLine() {
		return textLocation().range().end().line();
	}

	/**
	 *
	 * @return The zero-based starting character column of the code range.
	 */
	public int startColumn() {
		return textLocation().range().start().character();
	}

	/**
	 *
	 * @return The zero-based starting character column of the code range.
	 */
	public int endColumn() {
		return textLocation().range().end().character();
	}

	/**
	 *
	 * @return the code which the ParsingContext points at as a List of strings, and has the correct
	 *         line numbers on each line as well
	 */
	public List< String > enclosingCodeWithLineNumbers() {
		int i = textLocation().range().start().line();
		List< String > linesWithNumbers = new ArrayList<>();
		for( String line : enclosingCode() ) {
			String newLine = i + ":" + line;
			linesWithNumbers.add( newLine );
			i++;
		}
		return linesWithNumbers;
	}
}
