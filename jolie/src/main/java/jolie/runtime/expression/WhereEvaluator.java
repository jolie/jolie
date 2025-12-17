package jolie.runtime.expression;

import jolie.runtime.ValueVector;
import java.util.List;

/**
 * Evaluates WHERE clauses with existential semantics for $ expressions. Handles composable path
 * operations like $..field[*].* by trying all possible bindings.
 */
public class WhereEvaluator {
	public static boolean matches( Expression where, Candidate candidate ) {
		bind( where, candidate );
		return eval( where );
	}

	static void bind( Expression expr, Candidate candidate ) {
		switch( expr ) {
		// $
		case CurrentValueExpression cve -> cve.setCurrentCandidate( candidate );
		// #
		case ValueVectorSizeExpression vvse -> vvse.bind( candidate );
		// ==, !=, <, >, <=, >=
		case CompareCondition cmp -> {
			bind( cmp.leftExpression, candidate );
			bind( cmp.rightExpression, candidate );
		}
		// &&
		case AndCondition and -> {
			for( var c : and.children )
				bind( c, candidate );
		}
		// ||
		case OrCondition or -> {
			for( var c : or.children )
				bind( c, candidate );
		}
		// !
		case NotExpression not -> bind( not.expression, candidate );
		// Arithmetic: +, -, *, /, %
		case SumExpression sum -> {
			for( var operand : sum.children )
				bind( operand.expression(), candidate );
		}
		case ProductExpression prod -> {
			for( var operand : prod.children )
				bind( operand.expression(), candidate );
		}
		default -> {
		}
		}
	}

	private static boolean eval( Expression expr ) {
		return switch( expr ) {
		// $
		case CurrentValueExpression cve -> {
			for( var c : ValueNavigator.navigateFromCandidate( cve.currentCandidate, cve.operations ) )
				if( c.value().boolValue() )
					yield true;
			yield false;
		}
		// ==, !=, <, >, <=, >=
		case CompareCondition cmp -> {
			for( var l : candidates( cmp.leftExpression ) )
				for( var r : candidates( cmp.rightExpression ) )
					if( compare( cmp, l, r ) )
						yield true;
			yield false;
		}
		// &&
		case AndCondition and -> {
			for( var c : and.children )
				if( !eval( c ) )
					yield false;
			yield true;
		}
		// ||
		case OrCondition or -> {
			for( var c : or.children )
				if( eval( c ) )
					yield true;
			yield false;
		}
		// !
		case NotExpression not -> !eval( not.expression );
		default -> expr.evaluate().boolValue();
		};
	}

	private static List< Candidate > candidates( Expression expr ) {
		return switch( expr ) {
		// $
		case CurrentValueExpression cve -> ValueNavigator.navigateFromCandidate( cve.currentCandidate, cve.operations );
		// Non-$ expressions: wrap value in Candidate for type uniformity (candidate is ignored by
		// compare())
		default -> {
			ValueVector vec = ValueVector.create();
			vec.add( expr.evaluate() );
			yield List.of( new Candidate( vec, "42" ) );
		}
		};
	}

	private static boolean compare( CompareCondition cmp, Candidate left, Candidate right ) {
		switch( cmp.leftExpression ) {
		// $
		case CurrentValueExpression cve -> cve.setCurrentCandidate( left );
		default -> {
		}
		}
		switch( cmp.rightExpression ) {
		// $
		case CurrentValueExpression cve -> cve.setCurrentCandidate( right );
		default -> {
		}
		}
		return cmp.evaluate().boolValue();
	}
}
