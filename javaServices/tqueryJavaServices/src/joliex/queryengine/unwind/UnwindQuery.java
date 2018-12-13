package joliex.queryengine.unwind;

import java.util.ArrayList;
import java.util.Arrays;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Utils;

public final class UnwindQuery
{

	public static ValueVector unwind( Value unwindRequest )
	{
		String query = unwindRequest.getChildren( "query" ).first().strValue();
		Value data = unwindRequest.getChildren( "data" ).first();

		return unwindPath( data, query );
	}

	private static ValueVector unwindPath( Value data, String path )
	{
		ArrayList<String> pathList = new ArrayList<>( Arrays.asList( path.split( "\\." ) ) );
		return unwindPathRec( data, pathList );
	}

	private static ValueVector unwindPathRec( Value data, ArrayList<String> pathList )
	{
		String head = pathList.remove( 0 );
		ValueVector children = data.getChildren( head );
		ValueVector values = ValueVector.create();
		if ( pathList.isEmpty() ) {
			for( Value child : children ) {
				values.add( Utils.flatChildren( data, head, child ) );
			}
		} else {
			for( Value child : children ) {
				ValueVector unwinded = unwindPathRec( child, pathList );
				for( Value value : unwinded ) {
					values.add( Utils.flatChildren( data, head, value ) );
				}
			}
		}

		return values;
	}

}
