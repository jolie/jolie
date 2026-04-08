package jolie.runtime.expression;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import com.google.common.primitives.UnsignedInteger;

final class ValueNavigator {

	private ValueNavigator() {}

	/**
	 * Navigates value tree applying operations sequentially. Read-only: no vivification. Example:
	 * data.items[0].name="A", ops=[Field("items"), ArrayWildcard(), Field("name")] →
	 * [Candidate(Value("A"), "data.items[0].name"), Candidate(Value("B"), "data.items[1].name")]
	 * Extract: VALUES→.map(Candidate::value).toList() | PATHS→.map(Candidate::path).toList()
	 */
	public static List< Candidate > navigate( VariablePath from, List< PathOperation > ops ) {
		ValueVector rootVec = from.getValueVectorOrNull(); // SAFE: uses children().get(), not getChildren()
		if( rootVec == null || rootVec.isEmpty() )
			return List.of();

		List< Candidate > candidates = new ArrayList<>();
		candidates.add( new Candidate( rootVec, from.path()[ 0 ].key().evaluate().strValue() ) );

		for( PathOperation op : ops ) {
			List< Candidate > next = new ArrayList<>();
			candidates.forEach( c -> next.addAll( expand( c, op ) ) );
			candidates = next;
		}

		return candidates;
	}

	/** Navigates from a Candidate (for $ expressions in WHERE clauses) */
	public static List< Candidate > navigateFromCandidate( Candidate start, List< PathOperation > ops ) {
		List< Candidate > candidates = List.of( start );

		for( PathOperation op : ops )
			candidates = candidates.stream().flatMap( c -> expand( c, op ).stream() ).toList();

		return candidates;
	}

	private static List< Candidate > expand( Candidate c, PathOperation op ) {
		return switch( op ) {
		// .field
		case PathOperation.Field(String name) -> {
			if( !c.value().hasChildren( name ) )
				yield List.of(); // SAFE: checks via children.get() on AtomicRef, no map creation
			ValueVector vec = c.value().children().get( name ); // SAFE: map exists after hasChildren check
			yield vec.isEmpty() ? List.of() : List.of( new Candidate( vec, c.path() + "." + name ) );
		}

		// .*
		case PathOperation.FieldWildcard() -> {
			if( !c.value().hasChildren() )
				yield List.of(); // SAFE: checks via children.get() on AtomicRef, no map creation
			List< Candidate > results = new ArrayList<>();
			c.value().children().forEach( ( name, vec ) -> { // SAFE: map exists after hasChildren check
				if( !vec.isEmpty() )
					results.add( new Candidate( vec, c.path() + "." + name ) );
			} );
			yield results;
		}

		// [n]
		case PathOperation.ArrayIndex(UnsignedInteger idx) ->
			idx.intValue() < c.vector().size()
				? List.of( new Candidate( c.vector(), idx, c.path() + "[" + idx + "]" ) )
				: List.of();

		// [*]
		case PathOperation.ArrayWildcard() -> {
			List< Candidate > results = new ArrayList<>();
			for( int i = 0; i < c.vector().size(); i++ ) {
				results.add( new Candidate( c.vector(), UnsignedInteger.valueOf( i ), c.path() + "[" + i + "]" ) );
			}
			yield results;
		}

		// ..field
		case PathOperation.RecursiveField(String name) -> descendRecursive( c, fn -> fn.equals( name ) );

		// ..*
		case PathOperation.RecursiveWildcard() -> descendRecursive( c, fn -> true );
		};
	}

	/**
	 * BFS recursive descent matching fields via predicate. Example: "a" + (name=="id") → all a..id |
	 * "a" + (true) → all a..*
	 */
	private static List< Candidate > descendRecursive( Candidate start, Predicate< String > match ) {
		List< Candidate > results = new ArrayList<>();
		record Item(Value v, String p) {
		}
		List< Item > queue = new ArrayList<>();
		queue.add( new Item( start.value(), start.path() ) );

		while( !queue.isEmpty() ) {
			Item item = queue.removeFirst();
			if( !item.v.hasChildren() )
				continue; // SAFE: checks via children.get() on AtomicRef, no map creation

			item.v.children().forEach( ( name, vec ) -> { // SAFE: map exists after hasChildren check
				if( vec.isEmpty() )
					return;
				String fieldPath = item.p + "." + name;

				if( match.test( name ) )
					results.add( new Candidate( vec, fieldPath ) );

				for( int i = 0; i < vec.size(); i++ ) {
					String elementPath = fieldPath + "[" + i + "]";
					queue.add( new Item( vec.get( i ), elementPath ) );
				}
			} );
		}

		return results;
	}
}
