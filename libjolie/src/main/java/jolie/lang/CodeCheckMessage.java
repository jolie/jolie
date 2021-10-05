package jolie.lang;

import jolie.lang.parse.context.ParsingContext;

import java.util.Optional;

public class CodeCheckMessage {
	private final ParsingContext context;
	private final String description;
	private String help = null;

	public CodeCheckMessage( ParsingContext context, String description, String help ) {
		this.context = context;
		this.description = description;
		if( help != null ) {
			this.help = help;
		} else {
			this.help = null;
		}
	}

	public String getMessage() {
		if( context == null || description == null ) {
			return "No message was made.";// super.getMessage();
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
