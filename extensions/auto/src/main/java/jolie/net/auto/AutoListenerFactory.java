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
import java.net.URI;
import java.net.URISyntaxException;
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

/**
 * Listener factory for the auto communication medium.
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps( { "ini4j.jar", "jolie-js.jar", "json-simple.jar" } )
public class AutoListenerFactory extends CommListenerFactory {
	public AutoListenerFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException {
		// Format: "auto:autoconf_uri"
		URI locationURI = inputPort.location();

		String[] ss = locationURI.getSchemeSpecificPart().split( ":", 2 );
		String location = AutoHelper.getLocationFromUrl( ss[ 0 ], ss[ 1 ] );

		AutoHelper.assertIOException( location == null, "internal error: location is null" );

		if( Constants.LOCAL_LOCATION_KEYWORD.equals( location ) ) {
			interpreter.commCore().addLocalInputPort( inputPort );
			inputPort.setLocation( location );
			return interpreter.commCore().localListener();
		} else {
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
}
