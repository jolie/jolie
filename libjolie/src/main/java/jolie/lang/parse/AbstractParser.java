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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.text.similarity.LevenshteinDistance;

import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;
import jolie.lang.CodeCheckMessage;
import jolie.lang.Keywords;


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
		if( scanner.line() == scanner.startLine() && scanner.line() == scanner.endLine() ) {
			return new URIParsingContext( scanner.source(), scanner.line(), scanner.line(), scanner.errorColumn(),
				scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else if( scanner.startLine() == scanner.endLine() && scanner.startLine() != scanner.line() ) {
			return new URIParsingContext( scanner.source(), scanner.line(), scanner.line(), scanner.errorColumn(),
				scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else {
			return new URIParsingContext( scanner.source(), scanner.startLine(), scanner.endLine(),
				scanner.errorColumn(),
				scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		}
	}

	/**
	 * Returns the current {@link ParsingContext} from the underlying {@link Scanner} taking start and
	 * end line numbers into account
	 * 
	 * @return the current {@link ParsingContext} from the underlying {@link Scanner}
	 */
	public final ParsingContext getContextDuringError() throws IOException {
		readLineAfterError();
		if( scanner.startLine() == 0 && scanner.endLine() == 0 ) { // If neither start nor end has been changed, then
																	// use currentline
			return new URIParsingContext( scanner.source(), scanner.line(), scanner.line(), scanner.errorColumn(),
				scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else if( scanner.line() == scanner.startLine() && scanner.line() == scanner.endLine() ) { // start, end and
																									// current the same
			return new URIParsingContext( scanner.source(), scanner.line(), scanner.line(), scanner.errorColumn(),
				scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else if( scanner.line() != scanner.startLine() && scanner.line() == scanner.endLine() ) { // start different
																									// than current and
																									// end
			return new URIParsingContext( scanner.source(), scanner.startLine(), scanner.endLine(),
				scanner.errorColumn(), scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else if( scanner.line() == scanner.startLine() && scanner.line() != scanner.endLine() ) {// end different than
																									// start and current
																									// (hopefully does
																									// not happen)
			return new URIParsingContext( scanner.source(), scanner.startLine(), scanner.endLine(),
				scanner.errorColumn(), scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		} else if( scanner.startLine() == scanner.endLine() && scanner.startLine() != scanner.line() ) { // current
																											// different
																											// from
																											// start and
																											// end
			return new URIParsingContext( scanner.source(), scanner.startLine(), scanner.endLine(),
				0,
				Integer.MAX_VALUE,
				scanner.codeLine() );
		} else if( scanner.errorColumn() < 0 ) { // The reader read to the next line, need to go back a line and fix
													// errorColumn
			int linenr = scanner.line() - 1;
			String correctLine = scanner.getAllCodeLines().get( linenr );
			return new URIParsingContext( scanner.source(), linenr, linenr, correctLine.length(),
				scanner.errorColumn() + token.content().length(),
				List.of( correctLine ) );
		} else {
			return new URIParsingContext( scanner.source(), scanner.startLine(), scanner.endLine(),
				scanner.errorColumn(), scanner.errorColumn() + token.content().length(),
				scanner.codeLine() );
		}
	}

	public final int line() {
		return scanner.line();
	}

	public final int startLine() {
		return scanner.startLine();
	}

	public void setStartLine() {
		scanner.setStartLine( line() );
	}

	public final int endLine() {
		return scanner.endLine();
	}

	public void setEndLine() {
		scanner.setEndLine( line() );
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

	protected final void eat( Scanner.TokenType type, String errorMessage, String scopeName, String scope )
		throws ParserException, IOException {
		assertToken( type, errorMessage, scopeName, scope );
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

	protected final void assertToken( Scanner.TokenType type, String errorMessage, String scopeName, String scope )
		throws ParserException, IOException {
		if( token.isNot( type ) ) {
			throwExceptionWithScope( errorMessage, scopeName, scope );
		}
	}

	/**
	 * Creates help message from the context, the token/term which was incorrect during parsing and a
	 * list of possible tokens/terms which can replace the current token. If the tokenContent is not
	 * empty, the possibleTokens is checked whether it contains keywords close in levenshteins distance
	 * to the tokenContent, and these words are added to the help message, such that the user can see
	 * correct keywords matching closely to their own token.
	 * 
	 * @param context context of the code, when parsing error occured
	 * @param tokenContent content of the token, which was wrong or missing(if empty)
	 * @param possibleTokens list of possible keywords, which the user could have meant or are missing
	 */
	public static String createHelpMessage( ParsingContext context, String tokenContent,
		List< String > possibleTokens ) {
		if( tokenContent.isEmpty() && possibleTokens.isEmpty() ) {
			return "";
		}
		if( tokenContent.isEmpty() ) { // if the token is missing, the user is given all
			// possible tokens that can be written
			StringBuilder help = new StringBuilder( "You are missing a keyword. Possible inputs are:\n" )
				.append( String.join( ", ", possibleTokens ) );
			return help.toString();
		}
		// if tokenContent not empty, look for closely related correct keywords
		LevenshteinDistance dist = new LevenshteinDistance();
		ArrayList< String > proposedWord = new ArrayList<>();
		for( String correctToken : possibleTokens ) {
			if( dist.apply( tokenContent, correctToken ) <= 2 ) {
				proposedWord.add( correctToken );
			}
		}
		if( proposedWord.isEmpty() && !tokenContent.isEmpty() ) { // If none of the correct terms match the written
																	// token
			StringBuilder help = new StringBuilder( "The term did not match possible terms. Possible inputs are:\n" )
				.append( String.join( ", ", possibleTokens ) );
			return help.toString();
		} else { // If one or more words are really close to the token, the suggested correct token will be presented
					// with the original code line, to show how to write it correctly
			StringBuilder help = new StringBuilder( "Your term is similar to what would be valid input: " )
				.append( String.join( ", ", proposedWord ) )
				.append( ". Perhaps you meant:\n" );
			int columnSpace = context.startColumn() + (context.startLine() + ":").length();
			help.append( context.enclosingCode().get( 0 ).substring( 0, columnSpace ) )
				.append( proposedWord.get( 0 ) )
				.append( context.enclosingCode().get( 0 ).substring( tokenContent.length() + columnSpace ) );
			if( !context.enclosingCode().get( 0 ).endsWith( "\n" ) ) {
				help.append( "\n" );
			}
			for( int j = 0; j < columnSpace; j++ ) {
				help.append( " " );
			}
			help.append( '^' );
			return help.toString();
		}
	}

	/**
	 * Creates the help message just like createHelpMessage, taking the code lines in the scope of the
	 * error into consideration
	 * 
	 * @param context
	 * @param tokenContent
	 * @param scope
	 * @return
	 */
	public static String createHelpMessageWithScope( ParsingContext context, String tokenContent,
		String scope ) {
		StringBuilder help = new StringBuilder();
		List< String > possibleTerms = Keywords.getKeywordsForScope( scope );
		if( tokenContent == null || tokenContent.isEmpty() ) {
			help.append( "A term is missing. Possible inputs are:\n" );
			for( String term : possibleTerms ) {
				help.append( term ).append( "\n" );
			}
		} else {
			LevenshteinDistance dist = new LevenshteinDistance();
			ArrayList< String > proposedWord = new ArrayList<>();
			for( String correctTerm : possibleTerms ) {
				if( dist.apply( tokenContent, correctTerm ) <= 2 ) {
					proposedWord.add( correctTerm );
				}

			}
			if( !proposedWord.isEmpty() ) {
				help.append( "\nYour term is similar to what would be valid input: " )
					.append( String.join( ", ", proposedWord ) )
					.append( ". Perhaps you meant:\n" );
				int numberSpaces = context.startColumn() + (":" + context.endLine()).length();
				for( String line : context.enclosingCodeWithLineNumbers() ) {
					if( line.contains( context.endLine() + ":" ) ) {
						help.append( line.substring( 0, numberSpaces ) )
							.append( proposedWord.get( 0 ) )
							.append( line.substring( numberSpaces + tokenContent.length() ) );
					} else {
						help.append( line );
					}
					if( !line.endsWith( "\n" ) ) {
						help.append( "\n" );
					}
				}
				for( int j = 0; j < numberSpaces; j++ ) {
					help.append( " " );
				}
				help.append( '^' );
			} else { // If none of the correct terms match the written token
				help = new StringBuilder( "The term did not match possible terms. Possible inputs are:\n" )
					.append( String.join( ", ", possibleTerms ) );
			}
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
		CodeCheckMessage exceptionMessage;
		ParsingContext context = getContextDuringError();
		if( !token.content().equals( "" ) ) {
			if( !mesg.equals( "" ) ) {
				mesg += ": " + token.content() + "\n";
			} else {
				mesg += ". Found term: " + token.content() + "\n";
			}
			String help = createHelpMessage( context, token.content(), Arrays.asList() );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
		} else {
			mesg += "\n";
			// I remove 1 from currentcolumn, because the message otherwise look as if the error is within the
			// curly bracket and not at/before the curly bracket
			// example, if service does not have a name
			context =
				new URIParsingContext( context.source(), context.startLine(), context.endLine(),
					context.startColumn() - 1, context.startColumn() - 1,
					context.enclosingCode() );
			exceptionMessage = CodeCheckMessage.withoutHelp( context, mesg );
		}
		throw new ParserException( exceptionMessage );
	}

	/**
	 * Method to find all lines of code of the scope with the name we are looking at during a parsing
	 * error
	 * 
	 * @param name The name of the scope, e.g. the inputPort name
	 * @param scope The scope we are in, e.g. an inputPort
	 */
	protected final List< String > getWholeScope( String name, String scope ) {
		List< String > allLines = scanner.getAllCodeLines();
		ArrayList< String > lines = new ArrayList<>();
		for( int i = startLine(); i <= endLine(); i++ ) {
			try {
				String currentLine = allLines.get( i );
				lines.add( currentLine );
			} catch( IndexOutOfBoundsException e ) {
				// do nothing
			}
		}
		return lines;
	}

	/**
	 * Shortcut to throw a correctly formed ParserException.
	 * 
	 * @param mesg The message to insert in the ParserException.
	 * @param scopeName The name of the scope, e.g. the inputPort name
	 * @param scope The scope we are in, e.g. an inputPort
	 * @throws ParserException Every time, as its the purpose of this method.
	 */
	protected final void throwExceptionWithScope( String mesg, String scopeName, String scope )
		throws ParserException, IOException {
		CodeCheckMessage exceptionMessage;
		ParsingContext context = getContextDuringError();
		if( !token.content().equals( "" ) ) {
			mesg += ". Found term: " + token.content() + "\n";
		} else {
			mesg += "\n";
		}
		String help;
		List< String > extralines;
		switch( scope ) {
		case Keywords.INPUT_PORT:
			extralines = getWholeScope( scopeName, scope );
			if( extralines.get( extralines.size() - 1 ).contains( "}" ) ) {
				// If a term is missing, it needs the column of the last curly bracket instead of where it
				// originally threw the error
				int columnNumber = extralines.get( extralines.size() - 1 ).lastIndexOf( "}" );
				context =
					new URIParsingContext( context.source(), context.startLine(), context.endLine(), columnNumber,
						columnNumber + token.content().length(),
						extralines );
			} else {
				context = new URIParsingContext( context.source(), context.startLine(), context.endLine(),
					context.startColumn(), context.startColumn() + token.content().length(), extralines );
			}
			help = createHelpMessageWithScope( context, token.content(), scope );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
			break;
		case Keywords.EXECUTION:
			extralines = getWholeScope( "", scope ); // look for line containing execution
			int startColumn = 0;
			int endColumn = 0;
			int startLine = 0;
			int endLine = 0;
			System.out.println( "get(0): " + extralines.get( 0 ) );
			if( extralines.get( 0 ).contains( "execution:" ) && extralines.get( 0 ).contains( token.content() ) ) {
				startColumn = extralines.get( 0 ).indexOf( token.content() );
				endColumn = startColumn + token.content().length();
				startLine = context.startLine();
				endLine = context.endLine();
			} else if( extralines.get( 0 ).contains( "execution:" )
				&& !extralines.get( 0 ).contains( token.content() ) ) {
				startColumn = extralines.get( 0 ).indexOf( "execution:" ) + "execution:".length();
				endColumn = startColumn;
				startLine = context.startLine();
				endLine = startLine;
			} else {
				startColumn = context.startColumn();
				endColumn = context.endColumn();
				startLine = context.startLine();
				endLine = context.endLine();
			}
			context = new URIParsingContext( context.source(), startLine, endLine,
				startColumn, endColumn, extralines );
			help = createHelpMessageWithScope( context, token.content(), scope );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
			break;
		case Keywords.SERVICE:
			// if the service is empty and it does not have an ending curlybracket
			if( mesg.contains( "unexpected term found inside service" ) && token.content().isEmpty() ) {
				extralines = getWholeScope( scopeName, scope );
				// Change column to where starting curly bracket is, as this is the start of the empty service
				int columnNumber = extralines.get( 0 ).lastIndexOf( '{' );
				context =
					new URIParsingContext( context.source(), context.startLine(), context.startLine(), columnNumber,
						columnNumber + token.content().length(),
						List.of( extralines.get( 0 ) ) );
				help = createHelpMessageWithScope( context, token.content(), scope );
				exceptionMessage = CodeCheckMessage.withHelp( context,
					"Service " + scopeName + " is empty and does not have an ending }\n", help );
				break;
			}
			help = createHelpMessageWithScope( context, token.content(), scope );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
			break;
		case Keywords.IMPORT:
			// extralines = getWholeScope( null, scope ); // find line where import is spelled wrong
			// Find the column where the spelled wrong import is
			// String[] tempSplit = extralines.get( 0 ).split( " ", 0 );
			// int columnNumber = (tempSplit[ 0 ] + " " + tempSplit[ 1 ] + " ").length();
			/*
			 * context = new URIParsingContext( context.source(), context.startLine(), context.endLine(),
			 * columnNumber, columnNumber + tempSplit[ 2 ].length(), extralines );
			 */
			// Put the wrongly spelled import as tokenContent, as the token.content() is empty, but we want the
			// error message to display the wrongly spelled word
			// help = createHelpMessageWithScope( context, tempSplit[ 2 ], scope );
			exceptionMessage = CodeCheckMessage.withoutHelp( context, mesg );
			break;
		case Keywords.OUTER:
			help = createHelpMessageWithScope( context, token.content(), scope );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
			break;
		case Keywords.INTERFACE:
			help = createHelpMessageWithScope( context, token.content(), scope );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );
			break;
		/*
		 * case Keywords.MAIN: help = createHelpMessageWithScope( context, token.content(), scope );
		 * exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help ); break;
		 */
		default:
			help = createHelpMessage( context, token.content(), List.of() );
			exceptionMessage = CodeCheckMessage.withHelp( context, mesg, help );

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
