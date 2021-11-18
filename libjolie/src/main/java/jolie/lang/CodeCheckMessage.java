/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021 Vicki Mixen <vicki@mixen.dk>
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
import jolie.lang.parse.context.URIParsingContext;

public class CodeCheckMessage {
	private final URIParsingContext context;
	private final String description;
	private final String help;

	private CodeCheckMessage( URIParsingContext context, String description, String help ) {
		this.context = context;
		this.description = description;
		this.help = help;
	}

	public static CodeCheckMessage withHelp( URIParsingContext context, String description, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw new InvalidParameterException( "Parameter help cannot be null." );
		}
		return new CodeCheckMessage( context, description, help );
	}

	public static CodeCheckMessage buildWithHelp( OLSyntaxNode node, String message, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw new InvalidParameterException( "Parameter help cannot be null." );
		}
		return new CodeCheckMessage(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message, null );
	}

	public static CodeCheckMessage withoutHelp( URIParsingContext context, String description ) {
		return new CodeCheckMessage( context, description, null );
	}

	public static CodeCheckMessage buildWithoutHelp( OLSyntaxNode node, String message ) {
		return new CodeCheckMessage(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message, null );
	}

	public String toString() {
		StringBuilder messageBuilder = new StringBuilder();
		if( context != null ) {
			URIParsingContext myContext = context;
			messageBuilder.append( myContext.sourceName() ).append( ":" ).append( myContext.line() )
				.append( ": error: " );
			if( description != null ) {
				messageBuilder.append( description ).append( '\n' );
			} else {
				messageBuilder.append( "No descriptive error message found.\n" );
			}
			if( myContext.lineString().contains( myContext.line() + ":" ) ) {
				messageBuilder.append( myContext.lineString() ).append( '\n' );
			} else if( (myContext.lineString().split( "\n" )).length > 1 ) {
				messageBuilder.append( myContext.lineString() ).append( '\n' );
			} else {
				messageBuilder.append( myContext.line() ).append( ':' ).append( myContext.lineString() ).append( '\n' );
			}
			for( int i = 0; i < myContext.column() + (" " + myContext.line()).length(); i++ ) {
				messageBuilder.append( " " );
			}
			messageBuilder.append( "^\n" );
		} else {
			messageBuilder.append( ": error: " );
			if( description != null ) {
				messageBuilder.append( description ).append( '\n' );
			} else {
				messageBuilder.append( "No descriptive error message found.\n" );
			}
		}
		if( help != null ) {
			messageBuilder.append( help );
		}
		return messageBuilder.toString();
	}

	public Optional< String > help() {
		return Optional.ofNullable( help );
	}

	public Optional< URIParsingContext > context() {
		return Optional.ofNullable( context );
	}

	public Optional< String > description() {
		return Optional.ofNullable( description );
	}
}
