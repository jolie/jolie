package joliex.queryengine.common;

import java.util.ArrayList;
import java.util.Arrays;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class Utils
{

	public static ValueVector evaluatePath( Value data, String path )
	{
		ArrayList<String> paths = new ArrayList<>( Arrays.asList( path.split( "\\." ) ) );
		return evaluatePathRec( data, paths, ValueVector.create() );
	}

	private static ValueVector evaluatePathRec( Value data, ArrayList<String> paths, ValueVector values )
	{
		ValueVector children = data.getChildren( paths.remove( 0 ) );
		if ( paths.isEmpty() ) {
			for( Value child : children ) {
				if ( child.hasChildren() ) //for situations when data="awards.award", path="awards"
				{
					return ValueVector.create();
				} else {
					values.add( child );
				}
			}
		} else {
			children.forEach( child -> evaluatePathRec( child, paths, values ) );
		}
		return values;
	}

	public static Value flatChildren( Value data, String head, Value child )
	{
		Value dataCopy = Value.create();
		dataCopy.deepCopy( data );
		dataCopy.children().remove( head );
		ValueVector newChild = ValueVector.create();
		newChild.add( child );
		dataCopy.children().put( head, newChild );
		return dataCopy;
	}

	public static boolean isGreater( Value first, String second )
	{
		if ( isNumeric( first.strValue() ) && isNumeric( second ) ) {
			return first.doubleValue() > Double.parseDouble( second );
		} else {
			return first.strValue().length() > second.length();
		}
	}

	public static boolean isNumeric( String str )
	{
		return str.matches( "-?\\d+(\\.\\d+)?" );
	}

	public static class Constants
	{
		public static String right = "right";
		public static String left = "left";
		public static String and = "and";
		public static String or = "or";
		public static String not = "not";
		public static String equal = "equal";
		public static String greaterThen = "greaterThen";
		public static String lowerThen = "lowerThen";
		public static String exists = "exists";
		public static String path = "path";
		public static String val = "value";
	}
}
