/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang;

import java.security.InvalidParameterException;
import java.util.Optional;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;

public class CodeCheckMessage {
	private final ParsingContext context;
	private final String description;
	private final String help;

	/**
	 * Private contructer of codeCheckMessage
	 * 
	 * @param context
	 * @param description
	 * @param help
	 */
	private CodeCheckMessage( ParsingContext context, String description, String help ) {
		this.context = context;
		this.description = description;
		this.help = help;
	}

	/**
	 * public contructer of CodeCheckMessage, for when the message includes a help message
	 * 
	 * @param context
	 * @param description
	 * @param help
	 * @return
	 * @throws InvalidParameterException
	 */
	public static CodeCheckMessage withHelp( ParsingContext context, String description, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw new InvalidParameterException( "Parameter help cannot be null." );
		}
		return new CodeCheckMessage( context, description, help );
	}

	/**
	 * Public contructer of CodeCheckMessage, when information is in a OLSyntaxNode, help message
	 * included
	 * 
	 * @param node
	 * @param message
	 * @param help
	 * @return
	 * @throws InvalidParameterException
	 */
	public static CodeCheckMessage buildWithHelp( OLSyntaxNode node, String message, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw new InvalidParameterException( "Parameter help cannot be null." );
		}
		return new CodeCheckMessage(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message, null );
	}

	/**
	 * Public contructer of CodeCheckMessage, which du not include help in the message
	 * 
	 * @param context
	 * @param description
	 * @return
	 */
	public static CodeCheckMessage withoutHelp( ParsingContext context, String description ) {
		return new CodeCheckMessage( context, description, null );
	}

	/**
	 * Public contructer of CodeCheckMessage, when information is in a OLSyntaxNode, no help message
	 * included
	 * 
	 * @param node
	 * @param message
	 * @return
	 */
	public static CodeCheckMessage buildWithoutHelp( OLSyntaxNode node, String message ) {
		return new CodeCheckMessage(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message, null );
	}

	/**
	 * Returns a string, containing the information from the CodeCheckMessage
	 */
	@Override
	public String toString() {
		StringBuilder messageBuilder = new StringBuilder();
		if( context != null ) {
			// Add context
			messageBuilder.append( context.sourceName() ).append( ":" ).append( context.startLine() + 1 )
				.append( ": error: " );
			// Add description
			if( description != null ) {
				messageBuilder.append( description ).append( '\n' );
				if( !description.endsWith( "\n" ) ) {
					messageBuilder.append( '\n' );
				}
			} else {
				messageBuilder.append( "No descriptive error message found.\n\n" );
			}
			if( !context.enclosingCode().isEmpty() ) {
				// Appends all lines of code involved with the error
				messageBuilder.append( String.join( "", context.enclosingCodeWithLineNumbers() ) );
				if( !context.enclosingCode().get( context.enclosingCode().size() - 1 ).endsWith( "\n" ) ) {
					messageBuilder.append( "\n" );
				}
				// Add the extra line with the upwards arrow, to the startcolumn
				for( int i = 0; i < context.startColumn() + (":" + (context.endLine() + 1)).length(); i++ ) {
					messageBuilder.append( " " );
				}
				messageBuilder.append( "^\n" );
			}
		} else {
			// no context, simply write error
			messageBuilder.append( ": error: " );
			// Add description
			if( description != null ) {
				messageBuilder.append( description ).append( '\n' );
			} else {
				messageBuilder.append( "No descriptive error message found.\n" );
			}
		}
		if( help != null ) { // Add help message
			messageBuilder.append( '\n' ).append( help );
		}
		return messageBuilder.toString();
	}

	/**
	 * Get an optional of help from the CodeCheckMessage
	 * 
	 * @return
	 */
	public Optional< String > help() {
		return Optional.ofNullable( help );
	}

	/**
	 * Get an optional of the context from the CodeCheckMessage
	 * 
	 * @return
	 */
	public Optional< ParsingContext > context() {
		return Optional.ofNullable( context );
	}

	/**
	 * Get an optional of the description from the CodeCheckMessage
	 * 
	 * @return
	 */
	public Optional< String > description() {
		return Optional.ofNullable( description );
	}
}
