package jolie.runtime.expression;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import com.google.common.primitives.UnsignedInteger;

record Candidate(ValueVector vector, UnsignedInteger index, String path) {

	Candidate( ValueVector vector, String path ) {
		this( vector, UnsignedInteger.ZERO, path );
	}

	Value value() {
		return vector.get( index.intValue() );
	}
}
