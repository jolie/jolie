/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author Fabrizio Montesi
 */
public class ZipUtils extends JavaService
{
	private static final int BUFFER_SIZE = 1024;

	private static ByteArray inputStreamToByteArray( InputStream istream )
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

	public Value readEntry( Value request )
		throws FaultException
	{
		Value response = Value.create();
		String entryName = request.getFirstChild( "entry" ).strValue();
		ZipEntry entry = null;
		try {
			if ( request.hasChildren( "filename" ) ) {
				ZipFile file = new ZipFile(
					request.getFirstChild( "filename" ).strValue()
				);
			
				entry = file.getEntry( entryName );
				if ( entry != null ) {
				response.setValue(
					inputStreamToByteArray( new BufferedInputStream( file.getInputStream( entry ) ) )
				);
			}
			} else {
				ByteArray archive = request.getFirstChild( "archive" ).byteArrayValue();
		
				byte[] archiveBytes = archive.getBytes();
				ZipInputStream zipInputStream = new ZipInputStream( new ByteArrayInputStream( archiveBytes ) );
				ZipEntry zipEntry = zipInputStream.getNextEntry();
				boolean found = false;
				while( zipEntry != null && !found ) {
					if ( zipEntry.getName().equals( entryName ) ) {
						found = true;
						byte[] buffer = new byte[ BUFFER_SIZE ];
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						int len;
						while ((len = zipInputStream.read(buffer)) > 0) {
								bos.write(buffer, 0, len);
						}
						response.setValue( new ByteArray( bos.toByteArray() ) );
					} else {
						zipEntry = zipInputStream.getNextEntry();
					}
				}
				
			}
			
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return response;
	}

	public ByteArray zip( Value request )
		throws FaultException
	{
		ByteArrayOutputStream bbstream = new ByteArrayOutputStream();
		try {
			ZipOutputStream zipStream = new ZipOutputStream( bbstream );
			ZipEntry zipEntry;
			byte[] bb;
			for( Entry< String, ValueVector > entry : request.children().entrySet() ) {
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
		return new ByteArray( bbstream.toByteArray() );
	}
	
	public  Value unzip( Value request ) throws FaultException {
		String targetPath = request.getFirstChild( "targetPath" ).strValue();
		String filename = request.getFirstChild( "filename" ).strValue();
		
		Value response = Value.create();
		
		byte[] buffer = new byte[ BUFFER_SIZE ];
		ZipInputStream zipInputStream;
		try {
			zipInputStream = new ZipInputStream(new FileInputStream( filename ));		
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			int entryCounter = 0;
			while( zipEntry != null ) {
				if ( !zipEntry.isDirectory() ) {
					String fileName = zipEntry.getName();
					response.getChildren( "entry" ).get( entryCounter ).setValue( fileName );
					entryCounter++;
					File newFile = new File(targetPath + File.separator + fileName);
					new File(newFile.getParent()).mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(newFile);             
					int len;
					while ((len = zipInputStream.read(buffer)) > 0) {
							fileOutputStream.write(buffer, 0, len);
					}
					fileOutputStream.close();   
				}
				zipEntry = zipInputStream.getNextEntry();				
			}
			zipInputStream.closeEntry();
			zipInputStream.close();
		} catch( FileNotFoundException ex ) {
			throw new FaultException("FileNotFound");
		} catch( IOException ex ) {
			throw new FaultException("IOException");
		}
		return response;
	}
	
	public Value listEntries( Value request ) throws FaultException {
		ByteArray archive = request.getFirstChild( "archive" ).byteArrayValue();
		Value response = Value.create();
		
		byte[] archiveBytes = archive.getBytes();
		ZipInputStream zipInputStream = new ZipInputStream( new ByteArrayInputStream( archiveBytes ) );
		try {
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			int entryCounter = 0;
			while( zipEntry != null ) {
				response.getChildren( "entry" ).get( entryCounter ).setValue( zipEntry.getName() );
				zipEntry = zipInputStream.getNextEntry();	
				entryCounter++;
			}
			zipInputStream.closeEntry();
			zipInputStream.close();
		} catch( IOException ex ) {
			throw new FaultException("IOException");
		}
		return response;
	}
}
