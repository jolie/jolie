/*
 * Copyright (C) 2014-2016 Fabrizio Montesi <famontesi@gmail.com>
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommCore;
import jolie.net.CommListener;
import jolie.net.UnsupportedCommMediumException;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.runtime.AndJarDeps;
import jolie.util.Helpers;
import org.ini4j.Ini;

/**
 * Listener factory for the auto communication medium.
 * @author Fabrizio Montesi
 */
@AndJarDeps({"ini4j.jar"})
public class AutoListenerFactory extends CommListenerFactory
{
	public AutoListenerFactory( CommCore commCore )
	{
		super( commCore );
	}
	
	private IOException buildIOException( String message )
	{
		return new IOException( "autoconf: " + message );
	}
	
	private void throwIOException( String message )
		throws IOException
	{
		throw buildIOException( message );
	}
	
	private void assertIOException( boolean condition, String message )
		throws IOException
	{
		Helpers.condThrow( condition, buildIOException( message ) );
	}
	
	private String getLocationFromIni( String iniLocation )
		throws IOException
	{
		// Format: "/Section/Key:URL_to_ini"
		String[] ss = iniLocation.split( ":", 2 );
		assertIOException( ss.length < 2, "invalid ini location; the format is /Section/Key:URL_to_ini" );
		
		String[] iniPath = ss[0].split( "/", 3 );
		assertIOException( iniPath.length < 3, "path to ini content is not well-formed; the format is /Section/Key" );
		
		URL iniURL = new URL( ss[1] );
		Ini ini = new Ini( new InputStreamReader( iniURL.openStream() ) );
		
		Ini.Section section = ini.get( iniPath[1] );
		assertIOException( section == null, "could not find section " + iniPath[1] + " in ini" );
		
		String retLocation = section.get( iniPath[2] );
		assertIOException( retLocation == null, "could not find key " + iniPath[2] + " in section " + iniPath[1] + " in ini" );
		
		return retLocation;
	}

	public CommListener createListener(
					Interpreter interpreter,
					CommProtocolFactory protocolFactory,
					InputPort inputPort
				)
		throws IOException
	{
		// Format: "auto:autoconf_uri"
		URI locationURI = inputPort.location();

		String[] ss = locationURI.getSchemeSpecificPart().split( ":", 2 );
		String location = null;
		if ( "ini".equals( ss[0] ) ) {
			location = getLocationFromIni( ss[1] );
		} else {
			throwIOException( "unsupported scheme: " + locationURI.getScheme() );
		}

		assertIOException( location == null, "internal error: location is null" );
		assertIOException( location.equals( Constants.LOCAL_LOCATION_KEYWORD ), "autoconf does not support local locations" );
		
		try {
			URI uri = new URI( location );
			inputPort.setLocation( location );
			CommListenerFactory factory = interpreter.commCore().getCommListenerFactory( uri.getScheme() );
			Helpers.condThrow( factory == null, new UnsupportedCommMediumException( uri.getScheme() ) );
			return factory.createListener( interpreter, protocolFactory, inputPort );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
	}
}
