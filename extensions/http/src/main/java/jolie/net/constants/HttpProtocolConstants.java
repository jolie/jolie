/*
 * Copyright (C) 2024 Claudio Guidi <guidiclaudio@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net.constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * Http Global constants.
 * 
 * @author Claudio Guidi
 */
public final class HttpProtocolConstants {
	/*
	 * charset string will be encoded to Content-Type header only if contentType starst with text/ or if
	 * it is contained in this list
	 */
	public static final HashSet< String > CONTENTYPE_FOR_CHARSET;

	static {
		HashSet< String > contentTypeForCharset;
		try {
			contentTypeForCharset = readContentTypeListForCharset();
		} catch( HttpProtocolConstantsException e ) {
			System.out.println( e.getMessage() );
			contentTypeForCharset = null;
		}
		CONTENTYPE_FOR_CHARSET = contentTypeForCharset;
	}

	private static HashSet< String > readContentTypeListForCharset() throws HttpProtocolConstantsException {
		HashSet< String > contentTypesForCharset = new HashSet<>();

		try( InputStream is =
			HttpProtocolConstants.class.getClassLoader().getResourceAsStream( "http-contenttype-charset.txt" ) ) {
			if( is == null ) {
				throw new HttpProtocolConstantsException(
					"ERROR: could not find http-contenttype-charset.txt. Your distribution of Jolie might be corrupted." );
			}
			BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
			String line;
			while( (line = reader.readLine()) != null ) {
				contentTypesForCharset.add( line );
			}

			return contentTypesForCharset;
		} catch( IOException e ) {
			throw new HttpProtocolConstantsException(
				"ERROR: could not read from http-contenttype-charset.txt correctly." );
		}
	}

}
