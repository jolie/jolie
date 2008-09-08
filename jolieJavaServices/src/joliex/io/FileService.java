package joliex.io;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import jolie.net.CommMessage;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class FileService extends JavaService
{
	private static void readBase64IntoValue( File file, Value value )
		throws IOException
	{
		FileInputStream fis = new FileInputStream( file );
		byte[] buffer = new byte[ (int)file.length() ];
		fis.read( buffer );
		fis.close();
		sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
		value.setValue( encoder.encode( buffer ) );
	}
	
	private static void readBinaryIntoValue( File file, Value value )
		throws IOException
	{
		FileInputStream fis = new FileInputStream( file );
		byte[] buffer = new byte[ (int)file.length() ];
		fis.read( buffer );
		fis.close();
		value.setValue( new ByteArray( buffer ) );
	}
	
	private static void readTextIntoValue( File file, Value value )
		throws IOException
	{
		String separator = System.getProperty( "line.separator" );
		StringBuffer buffer = new StringBuffer();
		String line;
		BufferedReader reader = new BufferedReader( new FileReader( file ) );
		while( (line=reader.readLine()) != null ) {
			buffer.append( line );
			buffer.append( separator );
		}
		reader.close();
		value.setValue( buffer.toString() );
	}
	
	public CommMessage readFile( CommMessage message )
		throws FaultException
	{
		
		Value filenameValue = message.value().getChildren( "filename" ).first();
		if ( !filenameValue.isString() ) {
			throw new FaultException( "FileNotFound" );
		}

		Value retValue = Value.create();
		String format = message.value().getFirstChild( "format" ).strValue();

		try {
			if ( "base64".equals( format ) ) {
				readBase64IntoValue( new File( filenameValue.strValue() ), retValue );
			} else if ( "binary".equals( format ) ) {
				readBinaryIntoValue( new File( filenameValue.strValue() ), retValue );
			} else {
				readTextIntoValue( new File( filenameValue.strValue() ), retValue );
			}			
		} catch( FileNotFoundException e ) {
			throw new FaultException( "FileNotFound", e );
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return new CommMessage( message.operationName(), "/", retValue );
	}
}
