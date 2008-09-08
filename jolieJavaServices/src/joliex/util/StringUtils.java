package joliex.util;


import java.util.regex.Pattern;
import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class StringUtils extends JavaService
{
	public CommMessage replaceAll( CommMessage message )
	{
		String regex = message.value().getChildren( "regex" ).first().strValue();
		String replacement = message.value().getChildren( "replacement" ).first().strValue();
		return new CommMessage(
				"replaceAll", "/",
				Value.create( message.value().strValue().replaceAll( regex, replacement ) )
						);
	}
	
	public CommMessage trim( CommMessage message )
	{
		return new CommMessage( "trim", "/", Value.create( message.value().strValue().trim() ) );
	}
	
	public CommMessage split( CommMessage message )
	{
		String str = message.value().strValue();
		int limit = 0;
		Value lValue = message.value().getFirstChild( "limit" );
		if ( lValue.isDefined() ) {
			limit = lValue.intValue();
		}
		String[] ss = str.split(
				message.value().getFirstChild( "regex" ).strValue(),
				limit
			);
		Value value = Value.create();
		for( int i = 0; i < ss.length; i++ ) {
			value.getNewChild( "result" ).add( Value.create( ss[ i ] ) );
		}

		return new CommMessage( "split", "/", value );
	}
	
	public CommMessage match( CommMessage message )
	{
		String str = message.value().strValue();
		String regex = message.value().getFirstChild( "regex" ).strValue();
		int ret = ( Pattern.matches( regex, str ) ) ? 1 : 0;
		return new CommMessage( "match", "/", Value.create( ret ) );
	}
}
