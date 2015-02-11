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

package joliex.security;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class MessageDigestService extends JavaService
{
	public String md5( Value request )
		throws FaultException
	{
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance( "MD5" );
			md.update( request.strValue().getBytes( "UTF8" ) );
		} catch( UnsupportedEncodingException e ) {
			throw new FaultException( "UnsupportedOperation", e );
		} catch( NoSuchAlgorithmException e ) {
			throw new FaultException( "UnsupportedOperation", e );
		}
		int radix;
		if ( (radix=request.getFirstChild( "radix" ).intValue()) < 2 ) {
			radix = 16;
		}

		String response = new BigInteger( 1, md.digest() ).toString( radix );
		if ( response.length() < 32 ) {
			int paddingLength = 32 - response.length();
			StringBuilder sb = new StringBuilder();
			for( int i = 0; i < paddingLength; i++ ) {
				sb.append( "0" );
			}
			sb.append( response );
			response = sb.toString();
		}
		return response;
	}
}
