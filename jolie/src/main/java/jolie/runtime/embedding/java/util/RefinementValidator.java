package jolie.runtime.embedding.java.util;

import java.util.Arrays;
import jolie.runtime.embedding.java.TypeValidationException;

public class RefinementValidator {
	
	public static <N extends Number & Comparable<N>> N ranges( N value, @SuppressWarnings( "unchecked" ) N... intervals ) {
		if ( value == null )
			return value;

		for ( int i = 0; i < intervals.length-1; i += 2 ) {
			if ( value.compareTo( intervals[i] ) >= 0 && value.compareTo( intervals[i+1] ) <= 0 )
				return value;
		}
		
		throw new TypeValidationException( "The provided number wasn't within any of the provided intervals." );
	}

	public static String length( String value, int min, int max ) {
		if ( value == null )
			return value;
			
		if ( value.length() >= min && value.length() <= max )
			return value;

		throw new TypeValidationException( "The provided string wasn't within the provided length interval." );
	}

	public static String regex( String value, String regex ) {
		if ( value == null )
			return value;
			
		if ( value.matches( regex ) )
			return value;

		throw new TypeValidationException( "The provided string didn't match the provided regex." );
	}

	public static String enumeration( String value, String... values ) {
		if ( value == null )
			return value;
			
		if ( Arrays.stream( values ).anyMatch( value::equals ) )
			return value;

		throw new TypeValidationException( "The provided string didn't match any of the provided values." );
	}
}
