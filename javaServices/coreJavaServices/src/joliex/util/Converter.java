/***************************************************************************
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

import java.io.IOException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.RequestResponse;

public class Converter extends JavaService
{
	@RequestResponse
	public String rawToBase64( Value value )
	{
		byte[] buffer = value.byteArrayValue().getBytes();
		sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
		return encoder.encode( buffer );
	}

	@RequestResponse
	public ByteArray base64ToRaw( Value value )
		throws FaultException
	{
		ByteArray returnValue = null;
		try {
			String stringValue = value.strValue();
			sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
			byte[] supportArray = decoder.decodeBuffer( stringValue );
			returnValue = new ByteArray( supportArray );
			return returnValue;
		} catch( IOException ex ) {
			throw new FaultException( "IOException", ex );
		}
	}

	@RequestResponse
	public String rawToString( Value value )
		throws FaultException
	{
		byte[] buffer = value.byteArrayValue().getBytes();

		String charset = null;
		if ( value.hasChildren( "charset" ) ) {
			charset = value.getFirstChild( "charset" ).strValue();
		}
		try {
			if ( charset != null ) {
				return new String( buffer, charset );
			} else {
				return new String( buffer );
			}
		} catch ( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public ByteArray stringToRaw( Value value )
		throws FaultException
	{
		String str = value.strValue();

		String charset = null;
		if ( value.hasChildren( "charset" ) ) {
			charset = value.getFirstChild( "charset" ).strValue();
		}
		try {
			if ( charset != null ) {
				return new ByteArray( str.getBytes( charset ) );
			} else {
				return new ByteArray( str.getBytes() );
			}
		} catch ( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}
}
