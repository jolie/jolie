/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.lang.parse;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.text.similarity.LevenshteinDistance;

import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;
import jolie.lang.CodeCheckMessage;


/**
 * Skeleton implementation of a parser based on {@link jolie.lang.parse.Scanner}. Note that the
 * parsing process is not re-entrant.
 * 
 * @author Fabrizio Montesi
 * @see Scanner
 */
public abstract class AbstractParser {
	private Scanner scanner; // Input scanner.
	protected Scanner.Token token; /// < The current token.
	private final List< Scanner.Token > tokens = new ArrayList<>();
	private final StringBuilder stringBuilder = new StringBuilder( 256 );
	private boolean backup = false;
	private final List< Scanner.Token > backupTokens = new ArrayList<>();
	private boolean metNewline = false;

	protected final String build( String... args ) {
		stringBuilder.setLength( 0 );
		for( String s : args ) {
			stringBuilder.append( s );
		}
		return stringBuilder.toString();
	}

	/**
	 * Constructor
	 * 
	 * @param scanner The scanner to use during the parsing procedure.
	 */
	public AbstractParser( Scanner scanner ) {
		this.scanner = scanner;
	}

	protected final void addTokens( Collection< Scanner.Token > tokens ) {
		this.tokens.addAll( tokens );
	}

	protected final void addToken( Scanner.Token token ) {
		this.tokens.add( token );
	}

	protected final void prependToken( Scanner.Token prefixToken ) {
		addToken( prefixToken );
		addToken( token );
	}

	private void readToken()
		throws IOException {
		if( tokens.isEmpty() ) {
			token = scanner.getToken();
		} else {
			token = tokens.remove( 0 );
		}
	}

	/**
	 * Gets a new token. Whitespace tokens are ignored, but the metNewline flag is set.
	 * 
	 * @throws IOException If the internal scanner raises one.
	 */
	protected final void nextToken()
		throws IOException {
		metNewline = false;
		boolean run;
		do {
			readToken();
			run = token.is( Scanner.TokenType.NEWLINE );
			metNewline = metNewline || run;
		} while( run );
		if( backup ) {
			backupTokens.add( token );
		}
	}

	/**
	 * Used to read the rest of the line, after an error has occured.
	 */
	public final void readLineAfterError() throws IOException {
		try {
			scanner.readLineAfterError();
		} catch( Exception e ) {
			// Maybe add text : "Error occured while creating parserException."
			throw new IOException( e );
		}
	}

	protected final boolean hasMetNewline() {
		return metNewline;
	}

	/** Recovers the backed up tokens. */
	protected final void recoverBackup()
		throws IOException {
		backup = false;
		if( !backupTokens.isEmpty() ) {
			addTokens( backupTokens );
			backupTokens.clear();
			nextToken();
		}
	}

	/** Discards the backed up tokens. */
	protected final void discardBackup() {
		backup = false;
		backupTokens.clear();
	}

	protected void startBackup() {
		if( token != null ) {
			backupTokens.add( token );
		}
		backup = true;
	}


	/**
	 * Gets a new token, and throws an {@link EOFException} if such token is of type
	 * {@code jolie.lang.parse.Scanner.TokenType.EOF}.
	 * 
	 * @throws IOException If the internal scanner raises one.
	 * @throws EOFException If the next token is of type {@code jolie.lang.parse.Scanner.Token.EOF}
	 */
	protected final void nextTokenNotEOF()
		throws IOException, EOFException {
		nextToken();
		if( token.isEOF() ) {
			throw new EOFException();
		}
	}

	/**
	 * Returns the Scanner object used by this parser.
	 * 
	 * @return The Scanner used by this parser.
	 */
	public final Scanner scanner() {
		return scanner;
	}

	protected final void setScanner( Scanner scanner ) {
		this.scanner = scanner;
	}


	/**
	 * Returns the current {@link ParsingContext} from the underlying {@link Scanner}
	 * 
	 * @return the current {@link ParsingContext} from the underlying {@link Scanner}
	 */
	public final ParsingContext getContext() {
		return new URIParsingContext( scanner.source(), scanner.line(), scanner.errorColumn(), scanner.lineString() );
	}

	/**
	 * Returns the current {@link ParsingContext} from the underlying {@link Scanner}
	 * 
	 * @return the current {@link ParsingContext} from the underlying {@link Scanner}
	 */
	public final ParsingContext getContextDuringError() throws IOException {
		int linenr = scanner.line();
		readLineAfterError();
		if( linenr < scanner.line() ) {
			return new URIParsingContext( scanner.source(), linenr, scanner.errorColumn(),
				scanner.lineString().replace( '\n', ' ' ).stripTrailing() );
		} else {
			return new URIParsingContext( scanner.source(), scanner.line(), scanner.errorColumn(),
				scanner.lineString() );
		}
	}

	/**
	 * Eats the current token, asserting its type. Calling eat( type, errorMessage ) is equivalent to
	 * call subsequently tokenAssert( type, errorMessage ) and getToken().
	 * 
	 * @param type The type of the token to eat.
	 * @param errorMessage The error message to display in case of a wrong token type.
	 * @throws ParserException If the token type is wrong.
	 * @throws IOException If the internal scanner raises one.
	 */
	protected final void eat( Scanner.TokenType type, String errorMessage )
		throws ParserException, IOException {
		assertToken( type, errorMessage );
		nextToken();
	}

	protected final void eat( Scanner.TokenType type, String errorMessage, String[] possibleTerms )
		throws ParserException, IOException {
		assertToken( type, errorMessage, possibleTerms );
		nextToken();
	}

	protected final void maybeEat( Scanner.TokenType... types )
		throws ParserException, IOException {
		for( Scanner.TokenType type : types ) {
			if( token.is( type ) ) {
				nextToken();
				break;
			}
		}
	}

	protected final void eatKeyword( String keyword, String errorMessage )
		throws ParserException, IOException {
		assertToken( Scanner.TokenType.ID, errorMessage );
		if( !token.content().equals( keyword ) ) {
			throwException( errorMessage );
		}
		nextToken();
	}

	/**
	 * Eats the current token, asserting that it is an identifier (or an unreserved keyword). Calling
	 * eatIdentifier( errorMessage ) is equivalent to call subsequently assertIdentifier( errorMessage )
	 * and getToken().
	 * 
	 * @param errorMessage The error message to throw as a {@link ParserException} in case the current
	 *        token is not an identifier.
	 * @throws ParserException If the current token is not an identifier.
	 * @throws IOException If the internal scanner cannot read the next token.
	 */
	protected final void eatIdentifier( String errorMessage )
		throws ParserException, IOException {
		assertIdentifier( errorMessage );
		nextToken();
	}

	protected final void eatIdentifier( String errorMessage, String[] possibleTerms )
		throws ParserException, IOException {
		assertIdentifier( errorMessage, possibleTerms );
		nextToken();
	}

	/**
	 * Asserts that the current token is an identifier (or an unreserved keyword).
	 * 
	 * @param errorMessage the error message to throw as a {@link ParserException} if the current token
	 *        is not an identifier.
	 * @throws ParserException if the current token is not an identifier.
	 */
	protected final void assertIdentifier( String errorMessage )
		throws ParserException, IOException {
		if( !token.isIdentifier() ) {
			throwException( errorMessage );
		}
	}

	protected final void assertIdentifier( String errorMessage, String[] possibleTerms )
		throws ParserException, IOException {
		if( !token.isIdentifier() ) {
			throwException( errorMessage, possibleTerms );
		}
	}

	/**
	 * Asserts the current token type.
	 * 
	 * @param type The token type to assert.
	 * @param errorMessage The error message to display in case of a wrong token type.
	 * @throws ParserException If the token type is wrong.
	 */
	protected final void assertToken( Scanner.TokenType type, String errorMessage )
		throws ParserException, IOException {
		if( token.isNot( type ) ) {
			throwException( errorMessage );
		}
	}

	protected final void assertToken( Scanner.TokenType type, String errorMessage, String[] possibleTerms )
		throws ParserException, IOException {
		if( token.isNot( type ) ) {
			throwException( errorMessage, possibleTerms );
		}
	}

	public String createHelpMessage( URIParsingContext context, String tokenContent, String[] possibleTokens ) {
		LevenshteinDistance dist = new LevenshteinDistance();
		ArrayList< String > proposedWord = new ArrayList<>();
		for( String correctToken : possibleTokens ) {
			if( dist.apply( tokenContent, correctToken ) <= 2 ) {
				proposedWord.add( correctToken );
			}

		}
		if( proposedWord.isEmpty() && tokenContent.isEmpty() ) {
			StringBuilder help =
				new StringBuilder(
					"You are missing a keyword. Possible inputs are:\n" );
			for( String correctToken : possibleTokens ) {
				help.append( correctToken ).append( ", " );
			}
			help.delete( help.length() - 2, help.length() );
			return help.toString();
		} else if( proposedWord.isEmpty() && !tokenContent.isEmpty() ) {
			StringBuilder help =
				new StringBuilder(
					"The term did not match possible terms. Possible inputs are:\n" );
			for( String correctToken : possibleTokens ) {
				help.append( correctToken ).append( ", " );
			}
			help.delete( help.length() - 2, help.length() );
			return help.toString();
		} else {
			StringBuilder help = new StringBuilder( "Your term is similar to what would be valid input: " );
			for( String word : proposedWord ) {
				if( word != null ) {
					help.append( word ).append( ", " );

				}
			}
			help.delete( help.length() - 2, help.length() );

			help.append( ". Perhaps you meant:\n" ).append( context.line() ).append( ':' );
			int numberSpaces;
			if( context.column() != 0 ) {
				help.append( context.lineString().substring( 0, context.column() ) )
					.append( proposedWord.get( 0 ) )
					.append( context.lineString().substring( context.column() + tokenContent.length() ) )
					.append( '\n' );
				numberSpaces = context.column() + (":" + context.line()).length();
			} else {
				help.append( proposedWord.get( 0 ) )
					.append( context.lineString().substring( context.column() + tokenContent.length() ) )
					.append( '\n' );
				numberSpaces = context.column() + (":" + context.line()).length();

			}
			for( int j = 0; j < numberSpaces; j++ ) {
				help.append( " " );
			}
			help.append( '^' );
			return help.toString();
		}
	}

	// Should have more booleans, when more cases are covered, where extra lines of code are necessary,
	// fx with execution modality
	public String createHelpMessageWithScope( URIParsingContext context, String tokenContent, String extraLines,
		boolean InputPort ) {
		StringBuilder help =
			new StringBuilder( extraLines ).append( context.line() ).append( ':' ).append( context.lineString() )
				.append( '\n' );
		if( tokenContent.isEmpty() && InputPort ) {
			help.append(
				"A term is missing. Possible inputs are: location, protocol, interfaces, aggregates, redirects.\n" );
		} else {
			help.append( "The term: " ).append( tokenContent ).append( " is missing.\n" );
		}
		return help.toString();
	}

	/**
	 * Shortcut to throw a correctly formed ParserException.
	 * 
	 * @param mesg The message to insert in the ParserException.
	 * @throws ParserException Every time, as its the purpose of this method.
	 */
	protected final void throwException( String mesg )
		throws ParserException, IOException {
		String m = mesg;
		CodeCheckMessage exceptionMessage;
		URIParsingContext context = (URIParsingContext) getContextDuringError();
		if( !token.content().equals( "" ) ) {
			if( !m.equals( "" ) ) {
				m += ": " + token.content();
			} else {
				m += ". Found term: " + token.content();
			}
			String[] possibleTokens = { "These error messages have yet to get possible tokens!" };
			String help = createHelpMessage( context, token.content(), possibleTokens );
			exceptionMessage = CodeCheckMessage.withHelp( context, m, help );
		} else {
			// I remove 1 from currentcolumn, because the message otherwise look as if the error is within the
			// curly bracket and not at/before the curly bracket
			// example, if service does not have a name
			context = new URIParsingContext( context.source(), context.line(), context.column() - 1,
				context.lineString() );
			exceptionMessage = CodeCheckMessage.withoutHelp( context, m );
		}
		throw new ParserException( exceptionMessage );
	}

	/**
	 * Shortcut to throw a correctly formed ParserException.
	 * 
	 * @param mesg The message to insert in the ParserException.
	 * @throws ParserException Every time, as its the purpose of this method.
	 */
	protected final void throwException( String mesg, String[] possibleTokens )
		throws ParserException, IOException {
		String m = mesg;
		CodeCheckMessage exceptionMessage;
		URIParsingContext context = (URIParsingContext) getContextDuringError();
		String help;
		if( !token.content().equals( "" ) ) {
			if( !m.equals( "" ) ) {
				m += ". Found term: " + token.content();
			} else {
				m += ". Found term: " + token.content();
			}

			help = createHelpMessage( context, token.content(), possibleTokens );
			exceptionMessage = CodeCheckMessage.withHelp( context, m, help );
		} else {
			if( !m.equals( "" ) ) {
				m += ". Could not be found.";
			}
			// I remove 1 from currentcolumn, because the message otherwise look as if the error is within the
			// curly bracket and not at/before the curly bracket
			// example, if service does not have a name
			context = new URIParsingContext( context.source(), context.line(), context.column() - 1,
				context.lineString() );
			if( possibleTokens.length > 0 ) {
				help = createHelpMessage( context, token.content(), possibleTokens );
				exceptionMessage = CodeCheckMessage.withHelp( context, m, help );
			} else {
				exceptionMessage = CodeCheckMessage.withoutHelp( context, m );
			}
		}
		throw new ParserException( exceptionMessage );
	}

	protected final String getWholeScope( String name, String scope ) {
		ArrayList< String > allLines = scanner.getAllLines();
		int startLine = 0;
		for( int i = 0; i < allLines.size(); i++ ) {
			String currentLine = allLines.get( i );
			if( currentLine.contains( name ) && currentLine.contains( scope ) ) {
				startLine = i;
			}
		}
		StringBuilder lines = new StringBuilder();
		int leftCurlies = 0;
		int rightCurlies = 0;
		for( int j = startLine; j < allLines.size(); j++ ) {
			String currentLine = allLines.get( j );
			lines.append( j ).append( ':' ).append( currentLine );
			if( currentLine.contains( "{" ) ) {
				char[] lineChars = currentLine.toCharArray();
				for( char c : lineChars ) {
					if( c == '{' ) {
						leftCurlies++;
					}
				}
			}
			if( currentLine.contains( "}" ) ) {
				char[] lineChars = currentLine.toCharArray();
				for( char c : lineChars ) {
					if( c == '}' ) {
						rightCurlies++;
					}
				}
			}
			if( leftCurlies == rightCurlies ) {
				break;
			}
		}
		return lines.toString();
	}

	protected final void throwExceptionInputPort( String mesg, String inputPortName )
		throws ParserException, IOException {
		String m = mesg;
		CodeCheckMessage exceptionMessage;
		URIParsingContext context = (URIParsingContext) getContextDuringError();
		if( !token.content().equals( "" ) ) {
			if( !m.equals( "" ) ) {
				m += ": " + token.content();
			} else {
				m += ". Found term: " + token.content();
			}
			String[] possibleTokens =
				{ "location", "protocol", "interfaces", "aggregates", "redirects", "RequestResponse", "OneWay" };
			String help = createHelpMessage( context, token.content(), possibleTokens );
			exceptionMessage = CodeCheckMessage.withHelp( context, m, help );
		} else {
			String help;
			String extralines = getWholeScope( inputPortName, "inputPort" );
			context = new URIParsingContext( context.source(), context.line(), context.column() - 1,
				context.lineString() );
			if( m.contains( "location URI" ) ) {
				help = createHelpMessageWithScope( context, "location", extralines, true );
			} else if( m.contains( "protocol" ) ) {
				help = createHelpMessageWithScope( context, "protocol", extralines, true );
			} else {
				help = createHelpMessageWithScope( context, token.content(), extralines, true );
			}
			exceptionMessage = CodeCheckMessage.withHelp( context, m, help );
		}
		throw new ParserException( exceptionMessage );
	}

	/**
	 * Shortcut to throw a correctly formed ParserException, getting the message from an existing
	 * exception.
	 * 
	 * @param exception The exception to get the message from.
	 * @throws ParserException Every time, as its the purpose of this method.
	 */
	protected final void throwException( Exception exception )
		throws ParserException {
		ParsingContext context = getContext();
		CodeCheckMessage exceptionMessage = CodeCheckMessage.withoutHelp( context, exception.getMessage() );
		throw new ParserException( exceptionMessage );
	}
}
