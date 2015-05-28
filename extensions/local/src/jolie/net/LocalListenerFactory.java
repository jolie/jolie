/*
 * Copyright (C) 2015 Martin Wolf <mw@martinwolf.eu>
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
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class LocalListenerFactory extends CommListenerFactory
{
	private final Map<String, LocalListener> locationToListener = new ConcurrentHashMap<String, LocalListener>();

	public LocalListenerFactory( CommCore commCore )
	{
		super( commCore );
	}

	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort)
		throws IOException
	{
		System.out.println("createListener");
		if (inputPort.location() == null || inputPort.location().getHost() == null) {
			throw new IOException( "No address given" );
		}
		
		if ( this.locationToListener.containsKey( inputPort.location().getHost() ) ) {
			throw new IOException( "Address already in use" );
		}
		
		LocalListener localListener = LocalListener.create( interpreter, inputPort );
		System.out.println(localListener.inputPort().getInterface().oneWayOperations().isEmpty());
		
		System.out.println("_:_Port: "+ inputPort.name());
		for ( String key: inputPort.getInterface().oneWayOperations().keySet() ) {
			System.out.println("__    Key: "+key);
		}
		
//		public void addRedirections( Map< String, OutputPort > redirectionMap )
//	
//		public void addAggregations( Map< String, AggregatedOperation > aggregationMap )
//	
		this.locationToListener.put( inputPort.location().getHost(), localListener);

		return localListener;
	}
	
	public LocalListener getListener(String location)
	{
		return this.locationToListener.get( location);
	}
	
}
