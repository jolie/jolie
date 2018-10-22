/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net.coap.message.options;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to provide constants for several content formats.
 *
 * @author Oliver Kleine
 */
public class ContentFormat
{

	/**
	 * Corresponds to number -1
	 */
	public static final long UNDEFINED = -1;

	/**
	 * Corresponds to number 0
	 */
	public static final long TEXT_PLAIN_UTF8 = 0;

	/**
	 * Corresponds to number 40
	 */
	public static final long APP_LINK_FORMAT = 40;

	/**
	 * Corresponds to number 41
	 */
	public static final long APP_XML = 41;

	/**
	 * Corresponds to number 42
	 */
	public static final long APP_OCTET_STREAM = 42;

	/**
	 * Corresponds to number 47
	 */
	public static final long APP_EXI = 47;

	/**
	 * Corresponds to number 50
	 */
	public static final long APP_JSON = 50;

	/**
	 * Map of Coap's Content Formats
	 */
	public static final Map<Long, String> CONTENT_FORMAT = new HashMap<>( 6 );

	static {
		CONTENT_FORMAT.put( TEXT_PLAIN_UTF8, "text/plain" );
		CONTENT_FORMAT.put( APP_LINK_FORMAT, "application/link-format" );
		CONTENT_FORMAT.put( APP_XML, "application/xml" );
		CONTENT_FORMAT.put( APP_OCTET_STREAM, "application/octet-stream" );
		CONTENT_FORMAT.put( APP_EXI, "application/exi" );
		CONTENT_FORMAT.put( APP_JSON, "application/json" );
	}

	/**
	 * Map of Joplie's allowed Content Formats
	 */
	public static final Map<String, Long> JOLIE_ALLOWED_CONTENT_FORMAT = new HashMap<>( 10 );

	static {
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "text/plain", TEXT_PLAIN_UTF8 );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "text", TEXT_PLAIN_UTF8 );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "utf8", TEXT_PLAIN_UTF8 );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "application/xml", APP_XML );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "xml", APP_XML );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "application/octet-stream", APP_OCTET_STREAM );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "octet-stream", APP_OCTET_STREAM );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "raw", APP_OCTET_STREAM );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "application/json", APP_JSON );
		JOLIE_ALLOWED_CONTENT_FORMAT.put( "json", APP_JSON );
	}

	public static String toString( long key )
	{
		return CONTENT_FORMAT.get( key );
	}
}
