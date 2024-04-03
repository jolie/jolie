/*
 * Copyright (C) 2015 Martin Wolf <mw@martinwolf.eu>
 * Copyright (C) 2016 Fabrizio Montesi <famontesi@gmail.com>
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
package jolie.net;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jolie.Interpreter;
import jolie.StaticUtils;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class LocalListenerFactory extends CommListenerFactory {
	// TODO (FM): I made this complicated StaticUtils instead of just having a static ConcurrentHashMap
	// here.. one day we should simplify this.
	static {
		StaticUtils.create(
			LocalListenerFactory.class,
			ConcurrentHashMap::new );
	}

	@SuppressWarnings( "unchecked" )
	private static Map< String, LocalListener > locationToListener() {
		return StaticUtils.retrieve( LocalListenerFactory.class, Map.class );
	}

	public LocalListenerFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException {
		if( inputPort.location() == null || inputPort.location().getHost() == null ) {
			throw new IOException( "No address given" );
		}
		if( LocalListenerFactory.getListener( inputPort.location().getHost() ) != null ) {
			throw new IOException( "Address already in use" );
		}

		LocalListener localListener = LocalListener.create( interpreter, inputPort );
		LocalListenerFactory.addListener( inputPort.location().getHost(), localListener );
		return localListener;
	}

	public static LocalListener getListener( String location ) {
		return locationToListener().get( location );
	}

	public static void addListener( String hostname, LocalListener listener ) {
		locationToListener().put( hostname, listener );
	}

}
