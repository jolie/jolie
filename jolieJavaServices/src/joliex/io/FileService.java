/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package joliex.io;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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
	
	public CommMessage writeFile( CommMessage request )
		throws FaultException
	{
		Value filenameValue = request.value().getFirstChild( "filename" );
		if ( !filenameValue.isString() ) {
			throw new FaultException( "FileNotFound" );
		}
		Value content = request.value().getFirstChild( "content" );
		try {
			if ( content.isByteArray() ) {
				FileOutputStream os = new FileOutputStream( filenameValue.strValue() );
				os.write( ((ByteArray)content.valueObject()).getBytes() );
				os.flush();
			} else {
				FileWriter writer = new FileWriter( filenameValue.strValue() );
				writer.write( content.strValue() );
				writer.flush();
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return CommMessage.createEmptyMessage();
	}
	
	public CommMessage delete( CommMessage request )
		throws FaultException
	{
		String filename = request.value().strValue();
		if ( new File( filename ).delete() == false ) {
			throw new FaultException( "IOException" );
		}
		return CommMessage.createEmptyMessage();
	}
	
	public CommMessage rename( CommMessage request )
		throws FaultException
	{
		String filename = request.value().getFirstChild( "filename" ).strValue();
		String toFilename = request.value().getFirstChild( "to" ).strValue();
		if ( new File( filename ).renameTo( new File( toFilename ) ) == false ) {
			throw new FaultException( "IOException" );
		}
		return CommMessage.createEmptyMessage();
	}
}
