package jolie.tracer;

import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.Value;

public class AssignmentTraceAction implements TraceAction {
	public enum Type {
		ASSIGNMENT, POINTER, DEEPCOPY
	}

	private final Type type;
	private final String name;
	private final String description;
	private final Value value;
	private final ParsingContext context;

	public AssignmentTraceAction( Type type, String name, String description, Value value, ParsingContext context ) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.value = value;
		this.context = context;
	}

	public Type type() {
		return type;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Value value() {
		return value;
	}

	public ParsingContext context() {
		return context;
	}
}
