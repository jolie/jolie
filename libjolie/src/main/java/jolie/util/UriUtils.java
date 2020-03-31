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
public class UriUtils
{
	private final static String JAR_PREFIX = "jar:";
	private final static String JAP_PREFIX = "jap:";
	private final static String JAP_FILE_PREFIX = "jap:file:";

	public static String normalizeJolieUri( String uri )
		throws URISyntaxException
	{
		final String normalizedUrl;
		if ( uri.startsWith( JAP_PREFIX ) || uri.startsWith( JAR_PREFIX ) ) {
			normalizedUrl = uri.substring( 0, 4 ) +
				new URI( uri.substring( 4 ) ).normalize().toString();
		} else {
			normalizedUrl = uri;
		}
		return normalizedUrl;
	}

	public static String resolve( String context, String target )
	{
		String result = null;
		
		if ( target.startsWith( JAP_FILE_PREFIX ) ) {
			if ( Files.exists( Paths.get( context ) ) ) {
				result = JAP_FILE_PREFIX + context + Constants.fileSeparator + target.substring( JAP_FILE_PREFIX.length() );
			} else if ( context.startsWith( JAP_FILE_PREFIX ) ) {
				result = context + Constants.fileSeparator + target.substring( JAP_FILE_PREFIX.length() );
			}
		}

		if ( result == null ) {
			if ( !"".equals( context ) ) {
				context += Constants.fileSeparator;
			}
			result = context + target;
		}

		return result;
	}

	public static String normalizeWindowsPath( String path )
	{
		String result = path;
		if ( Helpers.getOperatingSystemType() == Helpers.OSType.Windows ) {
			result = result.replace( "\\", "/" );
			if ( result.charAt( 1 ) == ':' ) {
				// Remove the drive name if present
				result = result.substring( 2 );
			}
		}
		return result;
	}
}