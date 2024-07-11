package jolie.runtime.embedding.java.util;

import java.util.Objects;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.TypeValidationException;

@FunctionalInterface
public interface ConversionFunction< T, R > {
	R apply( T t ) throws TypeCheckingException;

	default R tryApply( T t ) {
		try {
			return apply( t );
		} catch( TypeCheckingException | TypeValidationException e ) {
			return null;
		}
	}

	default < V > ConversionFunction< T, V > andThen( ConversionFunction< ? super R, ? extends V > after ) {
		Objects.requireNonNull( after );
		return ( T t ) -> after.apply( apply( t ) );
	}
}
