package jolie.lang.parse.ast.expression;

import com.google.common.primitives.UnsignedInteger;

public sealed interface PathOperation {

	// .field
	record Field(String name) implements PathOperation {
	}

	// .*
	record FieldWildcard() implements PathOperation {
	}

	// [n]
	record ArrayIndex(UnsignedInteger index) implements PathOperation {
	}

	// [*]
	record ArrayWildcard() implements PathOperation {
	}

	// ..field
	record RecursiveField(String name) implements PathOperation {
	}

	// ..*
	record RecursiveWildcard() implements PathOperation {
	}
}
