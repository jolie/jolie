package jolie.runtime.expression;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import com.google.common.primitives.UnsignedInteger;

/**
 * Represents a value location during PATHS/VALUES traversal. All values are uniformly represented
 * as vector[index], exploiting Jolie's a = a[0] equivalence. The path accumulates the traversal
 * route (e.g., "data[0].child[2]") for PATHS result generation.
 */
record Candidate(ValueVector vector, UnsignedInteger index, String path) {

	Candidate( ValueVector vector, String path ) {
		this( vector, UnsignedInteger.ZERO, path );
	}

	Value value() {
		return vector.get( index.intValue() );
	}
}
