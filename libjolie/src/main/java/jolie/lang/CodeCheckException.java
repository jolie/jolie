package jolie.lang;

import java.util.ArrayList;
import java.util.Iterator;

public class CodeCheckException extends Exception {
	ArrayList< CodeCheckMessage > messages;

	// For ParserException
	public CodeCheckException( CodeCheckMessage message ) {
		this.messages = new ArrayList<>();
		this.messages.add( message );
	}

	public ArrayList< CodeCheckMessage > messages() {
		return messages;
	}

	public String getMessage() {
		Iterator< CodeCheckMessage > iterator = messages.iterator();
		StringBuilder messageString = new StringBuilder();
		while( iterator.hasNext() ) {
			CodeCheckMessage currentMessage = (CodeCheckMessage) iterator.next();
			messageString
				.append( currentMessage.context().sourceName() )
				.append( ':' )
				.append( currentMessage.context().line() )
				.append( ": error: " )
				.append( currentMessage.description() )
				.append( "\n" );
			if( currentMessage.help().isPresent() ) {
				messageString
					.append( "help: " )
					.append( currentMessage.help().get() )
					.append( "\n" );
			}
		}
		return messageString.toString();
	}


}
