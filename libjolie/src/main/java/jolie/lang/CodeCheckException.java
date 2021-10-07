package jolie.lang;

import java.util.List;
import java.util.Iterator;

public class CodeCheckException extends Exception {
	private static final long serialVersionUID = Constants.serialVersionUID();

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
				.append( currentMessage.toString() );
		}
		return messageString.toString();
	}


}
