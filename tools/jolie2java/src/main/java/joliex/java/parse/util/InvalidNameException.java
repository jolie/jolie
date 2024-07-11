package joliex.java.parse.util;

public class InvalidNameException extends RuntimeException {

	public InvalidNameException( String message ) {
		super(
			message + " Please either change the name or use the @JavaName(\"NAME\") annotation to fix the problem." );
	}
}
