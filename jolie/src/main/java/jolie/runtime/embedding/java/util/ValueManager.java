package jolie.runtime.embedding.java.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.UnaryOperator;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.TypeValidationException;

public class ValueManager {
	
	public static <T> T validated( String n, T t ) throws TypeValidationException { 
		if ( t == null )
			throw new TypeValidationException( "Mandatory field \"" + n + "\" was unset." );

		return t;
	}

	public static <T> T validated( String n, T t, List<Refinement<T>> rs ) throws TypeValidationException { 
		return Refinement.validated( validated( n, t ), rs );
	}

	public static <T> List<T> validated( String n, SequencedCollection<T> ts, int min, int max ) throws TypeValidationException {
		return validated( n, ts, min, max, UnaryOperator.identity() );
	}

	public static <T> List<T> validated( String n, SequencedCollection<T> ts, int min, int max, List<Refinement<T>> rs ) throws TypeValidationException {
		return validated( n, ts, min, max, t -> Refinement.validated( t, rs ) );
	}

	private static <T> List<T> validated( String n, SequencedCollection<T> ts, int min, int max, UnaryOperator<T> validator ) {
		final List<T> vts = ts == null 
			? List.of() 
			: ts.parallelStream()
				.filter( Objects::nonNull )
				.map( validator::apply )
				.toList();

		if ( vts.size() < min || vts.size() > max )
			throw new TypeValidationException( "Field \"" + n + "\"'s cardinality was expected to be in the range [" + min + ", " + max + "], received cardinality=" + vts.size() + "." );

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
            throw new TypeCheckingException( "The given Value contained the following set of unexpected children: " + 
				v.children().keySet().parallelStream().filter( k -> !superset.contains( k ) ).reduce( (s1,s2) -> s1 + ", " + s2 ).map( s -> "{ " + s + " }" ).orElse( "{}" ) );
	}
}
