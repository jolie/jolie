package jolie.lang;

import jolie.lang.parse.context.ParsingContext;

import java.security.InvalidParameterException;
import java.util.Optional;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.URIParsingContext;

public class CodeCheckMessage {
	private final ParsingContext context;
	private final String description;
	private String help;

	private CodeCheckMessage( ParsingContext context, String description, String help ) {
		this.context = context;
		this.description = description;
		this.help = help;
	}

	public static CodeCheckMessage withHelp( ParsingContext context, String description, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw (new InvalidParameterException( "Parameter help cannot be null." ));
		}
		return new CodeCheckMessage( context, description, help );
	}

	public static CodeCheckMessage buildWithHelp( OLSyntaxNode node, String message, String help )
		throws InvalidParameterException {
		if( help == null ) {
			throw (new InvalidParameterException( "Parameter help cannot be null." ));
		}
		return new CodeCheckMessage(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message, null );
	}

	public static CodeCheckMessage withoutHelp( ParsingContext context, String description ) {
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
			messageBuilder
				.append( context.sourceName() )
				.append( ':' )
				.append( context.line() );
		}
		messageBuilder.append( ": error: " );
		if( description != null ) {
			messageBuilder
				.append( description );
		}
		if( help != null ) {
			messageBuilder
				.append( "\n" )
				.append( "help: " )
				.append( help )
				.toString();
		}
		return messageBuilder.toString();
	}

	public void setHelp( String help ) {
		this.help = help;
	}

	public Optional< String > help() {
		return Optional.ofNullable( help );
	}

	public Optional< ParsingContext > context() {
		return Optional.ofNullable( context );
	}

	public Optional< String > description() {
		return Optional.ofNullable( description );
	}
}
