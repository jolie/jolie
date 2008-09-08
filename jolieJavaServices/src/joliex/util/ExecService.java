package joliex.util;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;

import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class ExecService extends JavaService
{
	public CommMessage exec( CommMessage message )
		throws FaultException
	{
		Vector< String > command = new Vector< String >();
		String[] str = message.value().strValue().split( " " );
		for( int i = 0; i < str.length; i++ )
			command.add( str[i] );
		for( Value v : message.value().getChildren( "args" ) )
			command.add( v.strValue() );
		//String input = null;
		ProcessBuilder builder = new ProcessBuilder( command );
		//builder.redirectErrorStream( true );
		try {
			Value response = Value.create();
			Process p = builder.start();
			ValueVector waitFor = message.value().children().get( "waitFor" );
			if ( waitFor == null || waitFor.first().intValue() > 0 ) {
				int exitCode = p.waitFor();
				response.getNewChild( "exitCode" ).setValue( exitCode );
				int len = p.getInputStream().available();
				if ( len > 0 ) {
					char[] buffer = new char[ len ];
					BufferedReader reader = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
					reader.read( buffer, 0, len );
					response.setValue( new String( buffer ) );
				}
			}
			return new CommMessage( null, "/", response ); 
		} catch( Exception e ) {
			throw new FaultException( e );
		}
	}
}
