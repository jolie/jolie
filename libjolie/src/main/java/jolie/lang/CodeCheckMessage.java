package jolie.lang;

import jolie.lang.parse.context.ParsingContext;

import java.security.InvalidParameterException;
import java.util.Optional;

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

	public static CodeCheckMessage withoutHelp( ParsingContext context, String description ) {
		return new CodeCheckMessage( context, description, null );
	}

	public String getMessage() {
		if( context == null || description == null ) {
			return "No message was made.";
		} else if( help != null ) {
			return new StringBuilder()
				.append( context.sourceName() )
				.append( ':' )
				.append( context.line() )
				.append( ": error: " )
				.append( description )
				.append( "\n" )
				.append( "help: " )
				.append( help )
				.toString();
		} else {
			return new StringBuilder()
				.append( context.sourceName() )
				.append( ':' )
				.append( context.line() )
				.append( ": error: " )
				.append( description )
				.toString();
		}
	}

	public void setHelp( String help ) {
		this.help = help;
	}

	public Optional< String > help() {
		return Optional.ofNullable( help );
	}

	public ParsingContext context() {
		return context;
	}

	public String description() {
		return description;
	}
}
