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
import java.util.Set;

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
	private static final Set< String > CONTENT_TYPE_FOR_CHARSET;

	static {
		Set< String > contentTypeForCharset;
		try {
			contentTypeForCharset = readContentTypeListForCharset();
		} catch( IOException e ) {
			e.printStackTrace();
			contentTypeForCharset = null;
		}
		CONTENT_TYPE_FOR_CHARSET = contentTypeForCharset;
	}

	public static boolean shouldHaveCharset( String contentType ) {
		return CONTENT_TYPE_FOR_CHARSET != null && CONTENT_TYPE_FOR_CHARSET.contains( contentType );
	}

	private static Set< String > readContentTypeListForCharset() throws IOException {
		Set< String > contentTypesForCharset = new HashSet<>();

		try( InputStream is =
			HttpProtocolConstants.class.getClassLoader().getResourceAsStream( "http-content-type-charset.txt" ) ) {
			if( is == null ) {
				throw new IOException(
					"Could not find http-contenttype-charset.txt. Your distribution of Jolie might be corrupted." );
			}

			try( BufferedReader r = new BufferedReader( new InputStreamReader( is ) ) ) {
				r.lines().forEach( contentTypesForCharset::add );
			}

			return contentTypesForCharset;
		}
	}

}
