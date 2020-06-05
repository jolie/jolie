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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import jolie.lang.Constants;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author Fabrizio Montesi
 */
public class ZipUtils extends JavaService {
	private static final int BUFFER_SIZE = 1024;

	public Value readEntry( Value request )
		throws FaultException {
		if( !request.hasChildren( "filename" ) && !request.hasChildren( "archive" ) ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, "missing filename or archive" );
		}

		Value response = Value.create();
		String entryName = request.getFirstChild( "entry" ).strValue();

		try( ZipInputStream zIStream =
			(request.hasChildren( "filename" ))
				? new ZipInputStream( new FileInputStream( request.getFirstChild( "filename" ).strValue() ) )
				: new ZipInputStream(
					new ByteArrayInputStream( request.getFirstChild( "archive" ).byteArrayValue().getBytes() ) ) ) {
			boolean found = false;
			ZipEntry zipEntry = zIStream.getNextEntry();
			while( zipEntry != null && !found ) {
				if( zipEntry.getName().equals( entryName ) ) {
					found = true;
				} else {
					zipEntry = zIStream.getNextEntry();
				}
			}

			if( found ) {
				byte[] buffer = new byte[ BUFFER_SIZE ];
				int size;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while( (size = zIStream.read( buffer, 0, buffer.length )) != -1 ) {
					baos.write( buffer, 0, size );
				}
				response.setValue( new ByteArray( baos.toByteArray() ) );
			} else {
				throw new FaultException( "EntryNotFound" );
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return response;
	}

	public Value listEntries( Value request )
		throws FaultException {
		if( !request.hasChildren( "filename" ) && !request.hasChildren( "archive" ) ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, "missing filename or archive" );
		}

		Value response = Value.create();
		try(
			InputStream is =
				(request.hasChildren( "filename" ))
					? new FileInputStream( request.getFirstChild( "filename" ).strValue() )
					: new ByteArrayInputStream( request.getFirstChild( "archive" ).byteArrayValue().getBytes() );
			ZipInputStream zIStream = new ZipInputStream( is ) ) {
			ZipEntry zipEntry = zIStream.getNextEntry();
			int count = 0;
			while( zipEntry != null ) {
				response.getChildren( "entry" ).get( count ).setValue( zipEntry.getName() );
				zipEntry = zIStream.getNextEntry();
				count++;
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return response;
	}

	public ByteArray zip( Value request )
		throws FaultException {
		ByteArrayOutputStream bbstream = new ByteArrayOutputStream();
		try( ZipOutputStream zipStream = new ZipOutputStream( bbstream ) ) {
			byte[] bb;
			ZipEntry zipEntry;
			for( Entry< String, ValueVector > entry : request.children().entrySet() ) {
				try {
					zipEntry = new ZipEntry( entry.getKey() );
					zipStream.putNextEntry( zipEntry );
					bb = entry.getValue().first().byteArrayValue().getBytes();
					zipStream.write( bb, 0, bb.length );
				} finally {
					zipStream.closeEntry();
				}
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}
		return new ByteArray( bbstream.toByteArray() );
	}

	public Value unzip( Value request )
		throws FaultException {
		Path targetPath = Paths.get( request.getFirstChild( "targetPath" ).strValue() );
		String filename = request.getFirstChild( "filename" ).strValue();

		Value response = Value.create();

		byte[] buffer = new byte[ BUFFER_SIZE ];
		try(
			FileInputStream fis = new FileInputStream( filename );
			ZipInputStream zipInputStream = new ZipInputStream( fis ) ) {
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			int entryCounter = 0;
			while( zipEntry != null ) {
				if( !zipEntry.isDirectory() ) {
					String targetFilename = zipEntry.getName();
					File newFile = new File( targetPath + File.separator + targetFilename );
					if( !newFile.toPath().normalize().startsWith( targetPath.normalize() ) ) {
						throw new IOException( "Bad zip entry: " + targetFilename );
					}
					response.getChildren( "entry" ).get( entryCounter ).setValue( targetFilename );
					entryCounter++;

					new File( newFile.getParent() ).mkdirs();
					try( FileOutputStream fileOutputStream = new FileOutputStream( newFile ) ) {
						int len;
						while( (len = zipInputStream.read( buffer )) > 0 ) {
							fileOutputStream.write( buffer, 0, len );
						}
					}
				}
				zipInputStream.closeEntry();
				zipEntry = zipInputStream.getNextEntry();
			}
			zipInputStream.closeEntry();
		} catch( FileNotFoundException e ) {
			throw new FaultException( "FileNotFound", e );
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		}
		return response;
	}
}
