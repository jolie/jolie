package jolie.runtime.embedding.java.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.TypeValidationException;

public class ValueManager {
	
	public static <T> T validated( String n, T t ) throws TypeValidationException { 
		if ( t == null )
			throw new TypeValidationException( "Mandatory field \"" + n + "\" cannot be null." );

		return t;
	}

	public static <T> List<T> validated( String n, SequencedCollection<T> ts, int min, int max, UnaryOperator<T> op ) throws TypeValidationException {
		final List<T> vts = ts == null 
			? List.of()
			: ts.parallelStream()
				.map( op::apply )
				.map( Objects::requireNonNull )
				.toList();

		if ( vts.size() < min || vts.size() > max )
			throw new TypeValidationException( "The field \"" + n + "\"'s cardinality was expected to be in the range [" + min + ", " + max + "], actual cardinality=" + vts.size() + "." );

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

	public static <T> List<T> vectorFieldFrom( Value v, String k, ConversionFunction<Value, T> mapper ) throws TypeCheckingException {
		return v.hasChildren( k ) 
			? v.getChildren( k )
				.stream().parallel()
				.map( mapper::tryApply )
				.filter( Objects::nonNull )
				.toList()
			: List.of();
	}

	public static <T> T singleFieldFrom( Value v, String k, ConversionFunction<Value, T> mapper ) throws TypeCheckingException {
		final ValueVector child;
		if ( !v.hasChildren( k ) || (child = v.getChildren(k)).size() == 0 )
			return null;

		if ( child.size() == 1 )
			return mapper.apply( child.first() );
			
		throw new TypeCheckingException( "Expected child \"" + k + "\" to contain at most 1 element, but found " + child.size() + " elements." );
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

	public static Map<String, List<JolieValue>> childrenFrom( Value v ) {
		return v.children()
			.entrySet()
			.parallelStream()
			.collect( Collectors.toUnmodifiableMap(
				Map.Entry::getKey,
				e -> e.getValue().stream().map( JolieValue::fromValue ).toList()
			) );
	}

	public static <U,V extends JolieValue> ConversionFunction<U,V> castFunc( ConversionFunction<U,V> f ) { return f; }

	public static void requireChildren( Value v, Set<String> superset ) throws TypeCheckingException { 
		if ( !superset.containsAll( v.children().keySet() ) )
            throw new TypeCheckingException( "The given Value contained the following set of unexpected children: " + 
				v.children().keySet().parallelStream().filter( k -> !superset.contains( k ) ).reduce( (s1,s2) -> s1 + ", " + s2 ).map( s -> "{ " + s + " }" ).orElse( "{}" ) );
	}
}
