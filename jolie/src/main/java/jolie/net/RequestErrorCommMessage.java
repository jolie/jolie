package jolie.net;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class RequestErrorCommMessage extends CommMessage {

	private final String template;

	/**
	 * Constructor
	 *
	 * @param requestId the identifier for the request
	 * @param operationName the operation name for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 */
	public RequestErrorCommMessage( long requestId, String operationName, String resourcePath, Value value,
		FaultException fault, String template ) {
		super( requestId, operationName, resourcePath, value, fault );
		this.template = template;
	}

	public String getTemplate() {
		return this.template;
	}
}
