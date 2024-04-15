package joliex.java.embedding.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.UnaryOperator;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCheckingException;
import joliex.java.embedding.JolieValue;
import joliex.java.embedding.TypeValidationException;

public class ValueManager {
	
	public static <T> T validated( T t ) throws TypeValidationException { 
		if ( t == null )
			throw new TypeValidationException( "Field was unexpectedly unset." );

		return t;
	}

	public static <T> T validated( T t, List<Refinement<T>> rs ) throws TypeValidationException { 
		return Refinement.validated( validated( t ), rs );
	}

	public static <T> List<T> validated( SequencedCollection<T> ts, int min, int max ) throws TypeValidationException {
		return validated( ts, min, max, UnaryOperator.identity() );
	}

	public static <T> List<T> validated( SequencedCollection<T> ts, int min, int max, List<Refinement<T>> rs ) throws TypeValidationException {
		return validated( ts, min, max, t -> Refinement.validated( t, rs ) );
	}

	private static <T> List<T> validated( SequencedCollection<T> ts, int min, int max, UnaryOperator<T> validator ) {
		final List<T> vts = validated( ts )
			.parallelStream()
			.filter( Objects::nonNull )
			.map( validator::apply )
			.toList();

		if ( vts.size() < min || vts.size() > max )
			throw new TypeValidationException( "Field cardinality was incorrect." );

		return vts;
	}

	public static <T> List<T> fieldFrom( List<JolieValue> child, ConversionFunction<JolieValue,T> mapper ) {
		return child.parallelStream()
			.map( mapper::tryApply )
			.filter( Objects::nonNull )
			.toList();
	}

	public static <T> T fieldFrom( Optional<JolieValue> firstChild, ConversionFunction<JolieValue,T> mapper ) {
		return firstChild.map( mapper::tryApply ).orElse( null );
	}

	public static <T> List<T> fieldFrom( ValueVector child, ConversionFunction<Value, T> mapper ) throws TypeCheckingException {
		return child.stream()
			.parallel()
			.map( mapper::tryApply )
			.filter( Objects::nonNull )
			.toList();
	}

	public static <T> T fieldFrom( Value firstChild, ConversionFunction<Value, T> mapper ) throws TypeCheckingException {
		return Optional.ofNullable( firstChild ).map( mapper::tryApply ).orElse( null );
	}

	public static <T> T choiceFrom( JolieValue j, SequencedCollection<ConversionFunction<JolieValue, T>> fs ) throws TypeValidationException {
		return fs.parallelStream()
			.map( f -> f.tryApply( j ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElseThrow( () -> new TypeValidationException( "The given JolieValue couldn't be converted to any of the option types." ) );
	}

	public static <T> T choiceFrom( Value v, SequencedCollection<ConversionFunction<Value, T>> fs ) throws TypeCheckingException {
		return fs.parallelStream()
			.map( f -> f.tryApply( v ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElseThrow( () -> new TypeCheckingException( "The given Value couldn't be converted to any of the option types." ) );
	}

	public static <U,V extends JolieValue> ConversionFunction<U,V> castFunc( ConversionFunction<U,V> f ) { return f; }

	public static void requireChildren( Value v, Set<String> superset ) throws TypeCheckingException { 
		if ( !superset.containsAll( v.children().keySet() ) )
            throw new TypeCheckingException( "The given Value contained unexpected children." );
	}
}
