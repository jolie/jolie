/*
 * Copyright (C) 2014-2019 Fabrizio Montesi <famontesi@gmail.com>
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
package jolie.net.auto;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import jolie.js.JsUtils;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;
import jolie.util.Helpers;
import org.ini4j.Ini;

/**
 * @author Claudio Guidi, Fabrizio Montesi
 */
public class AutoHelper {
	public static IOException buildIOException( String message ) {
		return new IOException( "autoconf: " + message );
	}

	public static void throwIOException( String message )
		throws IOException {
		throw buildIOException( message );
	}

	public static void assertIOException( boolean condition, String message )
		throws IOException {
		Helpers.condThrow( condition, buildIOException( message ) );
	}

	public static String getLocationFromIni( String iniLocation )
		throws IOException {
		// Format: "/Section/Key:URL_to_ini"
		String[] ss = iniLocation.split( ":", 2 );
		assertIOException( ss.length < 2, "invalid ini location; the format is /Section/Key:URL_to_ini" );

		String[] iniPath = ss[ 0 ].split( "/", 3 );
		assertIOException( iniPath.length < 3, "path to ini content is not well-formed; the format is /Section/Key" );

		URL iniURL = new URL( ss[ 1 ] );

		try( Reader reader = new InputStreamReader( iniURL.openStream() ) ) {
			Ini ini = new Ini( reader );

			Ini.Section section = ini.get( iniPath[ 1 ] );
			assertIOException( section == null, "could not find section " + iniPath[ 1 ] + " in ini" );

			String retLocation = section.get( iniPath[ 2 ] );
			assertIOException( retLocation == null,
				"could not find key " + iniPath[ 2 ] + " in section " + iniPath[ 1 ] + " in ini" );

			return retLocation;
		}
	}

	public static String getLocationFromJson( String jsonLocation )
		throws IOException {
		// Format: "path:URL_to_json"
		String[] ss = jsonLocation.split( ":", 2 );
		assertIOException( ss.length < 2, "invalid JSON location; the format is path.to.config.node:URL_to_JSON" );
		String[] pathComponents = ss[ 0 ].split( "\\." );
		VariablePathBuilder builder = new VariablePathBuilder( false );
		for( String component : pathComponents ) {
			builder.add( component, 0 );
		}

		URL jsonURL = new URL( ss[ 1 ] );
		try( Reader reader = new InputStreamReader( jsonURL.openStream() ) ) {
			Value jsonValue = Value.create();
			JsUtils.parseJsonIntoValue( reader, jsonValue, false );
			return builder.toClosedVariablePath( jsonValue ).getValue().strValue();
		}
	}

	public static String getLocationFromUrl( String type, String url )
		throws IOException {
		switch( type ) {
		case "ini":
			return getLocationFromIni( url );
		case "json":
			return getLocationFromJson( url );
		}
		AutoHelper.throwIOException( "unsupported url type: " + type );
		return null; // To appease the compiler..
	}
}
