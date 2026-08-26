package jolie.tracer;

import jolie.runtime.expression.Expression;
import jolie.util.Pair;

public final class TracerUtils {

	public enum TracerLevels {
		ALL, COMM, COMP
	}

	public static String getVarPathString( Pair< Expression, Expression >[] path ) {
		StringBuilder stringBuilder = new StringBuilder();
		for( int p = 0; p < path.length; p++ ) {
			if( p > 0 ) {
				stringBuilder.append( "." );
			}
			stringBuilder.append( path[ p ].key().evaluate().strValue() );
			if( path[ p ].value() != null ) {
				stringBuilder.append( "[" ).append( path[ p ].value().evaluate().strValue() ).append( "]" );
			}
		}
		return stringBuilder.toString();
	}

	public static String sanitizeForConsole( String input ) {
		StringBuilder result = new StringBuilder( input.length() );

		for( int i = 0; i < input.length(); i++ ) {
			char c = input.charAt( i );

			if( Character.isHighSurrogate( c ) ) {
				if( i + 1 < input.length()
					&& Character.isLowSurrogate( input.charAt( i + 1 ) ) ) {
					result.append( c );
					result.append( input.charAt( ++i ) );
				} else {
					result.append( '\uFFFD' );
				}
			} else if( Character.isLowSurrogate( c ) ) {
				result.append( '\uFFFD' );
			} else if( Character.isISOControl( c )
				&& c != '\n'
				&& c != '\r'
				&& c != '\t' ) {
				result.append( '\uFFFD' );
			} else {
				result.append( c );
			}
		}

		return result.toString();
	}
}
