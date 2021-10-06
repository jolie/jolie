package jolie.lang;

import jolie.lang.parse.context.ParsingContext;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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

	public static List< CodeCheckMessage > errorToMessage( Collection< CodeCheckingError > errors ) {
		List< CodeCheckMessage > messageList = new ArrayList<>();
		Iterator< CodeCheckingError > iter = errors.iterator();
		while( iter.hasNext() ) {
			CodeCheckingError error = (CodeCheckingError) iter.next();
			messageList.add( CodeCheckMessage.withoutHelp( error.context(), error.message() ) );
		}
		return messageList;
	}
}
