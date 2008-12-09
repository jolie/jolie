/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package joliex.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jolie.net.CommMessage;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author fmontesi
 */
public class ZipUtils extends JavaService
{
	static private int BUFFER_SIZE = 1024;

	static private ByteArray inputStreamToByteArray( InputStream istream )
		throws IOException
	{
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		byte buffer[] = new byte[ BUFFER_SIZE ];
		int read;
		while( (read=istream.read( buffer )) >= 0 ) {
			ostream.write( buffer, 0, read );
		}
		return new ByteArray( ostream.toByteArray() );
	}

	public CommMessage readEntry( CommMessage request )
		throws FaultException
	{
		Value response = Value.create();
		try {
			ZipFile file = new ZipFile(
				request.value().getFirstChild( "filename" ).strValue()
			);
			ZipEntry entry = file.getEntry(
				request.value().getFirstChild( "entry" ).strValue()
			);
			if ( entry != null ) {
				response.setValue(
					inputStreamToByteArray( file.getInputStream( entry ) )
				);
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return CommMessage.createResponse( request, response );
	}

	public CommMessage zip( CommMessage request )
		throws FaultException
	{
		ByteArrayOutputStream bbstream = new ByteArrayOutputStream();
		try {
			ZipOutputStream zipStream = new ZipOutputStream( bbstream );
			ZipEntry zipEntry;
			byte[] bb;
			for( Entry< String, ValueVector > entry : request.value().children().entrySet() ) {
				zipEntry = new ZipEntry( entry.getKey() );
				zipStream.putNextEntry( zipEntry );
				bb = entry.getValue().first().byteArrayValue().getBytes();
				zipStream.write( bb, 0, bb.length );
				zipStream.closeEntry();
			}
			zipStream.close();
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return CommMessage.createResponse( request, Value.create( new ByteArray( bbstream.toByteArray() ) ) );
	}
}
