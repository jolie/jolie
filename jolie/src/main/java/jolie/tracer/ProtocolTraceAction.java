package jolie.tracer;

import jolie.lang.parse.context.ParsingContext;

public class ProtocolTraceAction implements TraceAction {

	public static enum Type {
		SOAP, HTTP
	}

	private final ProtocolTraceAction.Type type;
	private final String name;
	private final String description;
	private final String message;
	private final ParsingContext context;

	public ProtocolTraceAction( ProtocolTraceAction.Type type, String name, String description, String message,
		ParsingContext context ) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.message = message;
		this.context = context;
	}

	public ProtocolTraceAction.Type type() {
		return type;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public String message() {
		return message;
	}

	public ParsingContext context() {
		return context;
	}
}
