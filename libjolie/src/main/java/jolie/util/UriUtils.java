/*
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jolie.lang.Constants;

/** Utilities to handle paths to locate files. */
public class UriUtils {
	private final static String JAR_PREFIX = "jar:";
	private final static String JAP_PREFIX = "jap:";
	private final static String JAP_FILE_PREFIX = "jap:file:";

	public static String normalizeJolieUri( String uri )
		throws URISyntaxException {
		final String normalizedUrl;
		if( uri.startsWith( JAP_PREFIX ) || uri.startsWith( JAR_PREFIX ) ) {
			final String prefix = uri.substring( 0, 4 );
			final String[] parts = uri.substring( 4 ).split( "!/", 2 );
			normalizedUrl = prefix
				+ new URI( parts[ 0 ] ).normalize().toString()
				+ (parts.length > 1 ? "!/" + new URI( parts[ 1 ] ).normalize().toString() : "");
		} else {
			// URI normalizedUri = URI.create( uri );
			// if ( normalizedUri != null ) {
			// normalizedUrl = normalizedUri.normalize().toString();
			// } else {
			normalizedUrl = uri;
			// }
		}
		return normalizedUrl;
	}

	public static String resolve( String context, String target ) {
		if( context.isEmpty() ) {
			return target;
		}

		String result = null;

		if( target.startsWith( JAP_FILE_PREFIX ) ) {
			if( context.startsWith( JAP_FILE_PREFIX ) ) {
				result = context + "/" + target.substring( JAP_FILE_PREFIX.length() );
			} else if( Files.exists( Paths.get( context ) ) ) {
				result = JAP_FILE_PREFIX + context + "/" + target.substring( JAP_FILE_PREFIX.length() );
			}
		}

		if( result == null ) {
			if( !context.endsWith( Constants.FILE_SEPARATOR ) ) {
				context += Constants.FILE_SEPARATOR;
			}
			result = context + target;
		}

		return result;
	}

	public static String normalizeWindowsPath( String path ) {
		return Helpers.ifWindowsOrElse(
			() -> {
				String result = path.replace( "\\", "/" );
				if( result.charAt( 1 ) == ':' ) {
					// Remove the drive name if present
					result = result.substring( 2 );
				}
				return result;
			},
			() -> path );
	}
}
