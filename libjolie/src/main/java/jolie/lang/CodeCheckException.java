package jolie.lang;

import java.util.List;
import java.util.Iterator;

public class CodeCheckException extends Exception {
	private final List< CodeCheckMessage > messageList;

	public CodeCheckException( List< CodeCheckMessage > messageList ) {
		this.messageList = messageList;
	}

	public List< CodeCheckMessage > messages() {
		return messageList;
	}

	public String getMessage() {
		Iterator< CodeCheckMessage > iterator = messageList.iterator();
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
			if( currentMessage.help().isPresent() ) { // help can be null, so check to make sure message is made
														// correctly
				messageString
					.append( "help: " )
					.append( currentMessage.help().get() )
					.append( "\n" );
			}
		}
		return messageString.toString();
	}


}
