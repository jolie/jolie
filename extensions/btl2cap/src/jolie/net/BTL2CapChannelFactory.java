/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/
package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.Identifier;
import jolie.runtime.AndJarDeps;

@Identifier("btl2cap")
@AndJarDeps({"bluetooth.jar"})
public class BTL2CapChannelFactory extends CommChannelFactory
{
	final static int cacheLimit = 1000; // Must be > 0
	final private Map< String, Map< String, ServiceRecord > > serviceCache =
						new HashMap< String, Map< String, ServiceRecord > > ();

	public BTL2CapChannelFactory( CommCore commCore )
	{
		super( commCore );
	}

	private ServiceRecord getFromServiceCache( String btAddr, String uuidStr )
	{
		ServiceRecord r = null;
		try {
			r = serviceCache.get( btAddr ).get( uuidStr );
		} catch( NullPointerException e ) {}
		return r;
	}
	
	private void putInServiceCache( String btAddr, String uuidStr, ServiceRecord record )
	{
		if ( serviceCache.size() > cacheLimit ) {
			serviceCache.remove( serviceCache.keySet().iterator().next() );
		}
		Map< String, ServiceRecord > map = serviceCache.get( btAddr );
		if ( map == null ) {
			map = new HashMap< String, ServiceRecord > ();
			serviceCache.put( btAddr, map );
		}
		if ( map.size() > cacheLimit ) {
			map.remove( map.keySet().iterator().next() );
		}
		map.put( uuidStr, record);
	}
	
	private String getConnectionURL( URI uri )
		throws BluetoothStateException, IOException
	{
		String[] ss = uri.getSchemeSpecificPart().split( ":" );
		String uuidStr = ss[1].split( "/" )[0];
		String btAddr = ss[0].substring( 2 );
		ServiceRecord record = getFromServiceCache( btAddr, uuidStr );
		
		if ( record == null ) {
			UUID uuid = new UUID( uuidStr, false );
			BTServiceDiscoveryListener listener = new BTServiceDiscoveryListener( uuid );
			LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices( null, new UUID[] { uuid }, new RemoteDevice( btAddr ), listener );
			record = listener.getResult();
			putInServiceCache( btAddr, uuidStr, record );
		}
		
		if ( record == null ) {
			throw new IOException( "Service not found" );
		}
		
		String url = record.getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false );
		return url;
	}
	
	public CommChannel createChannel( URI uri, OutputPort port )
		throws IOException
	{
		if ( uri.getHost() != null && uri.getHost().equals( "localhost" ) ) {
			throw new IOException( "Malformed output btl2cap location: " + uri.toString() );
		}
		try {
			String connectionURL = getConnectionURL( uri );
			L2CAPConnection conn = (L2CAPConnection)Connector.open( connectionURL );
			return new BTL2CapCommChannel( conn, uri, port.getProtocol() );
		} catch( ClassCastException e ) {
			throw new IOException( "CastException: malformed output btl2cap location: " + uri.toString() );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		} catch( NullPointerException e ) {
			throw new IOException( e );
		} 
	}
}
