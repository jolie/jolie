/***************************************************************************
 *   Copyright (C) 2008-2009 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import jolie.net.ports.OutputPort;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.bluetooth.BTL2CapHelper;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import jolie.net.ext.CommChannelFactory;
import jolie.runtime.AndJarDeps;

@AndJarDeps( { "bluetooth.jar" } )
public class BTL2CapChannelFactory extends CommChannelFactory {
	private final static int CACHE_LIMIT = 1000; // Must be > 0
	private final Map< String, Map< String, ServiceRecord > > serviceCache =
		new HashMap<>();

	public BTL2CapChannelFactory( CommCore commCore ) {
		super( commCore );
	}

	public ServiceRecord getFromServiceCache( String btAddr, String uuidStr ) {
		ServiceRecord r = null;
		Map< String, ServiceRecord > m = serviceCache.get( btAddr );
		if( m != null ) {
			r = serviceCache.get( btAddr ).get( uuidStr );
		}
		return r;
	}

	public void putInServiceCache( String btAddr, String uuidStr, ServiceRecord record ) {
		if( serviceCache.size() > CACHE_LIMIT ) {
			serviceCache.remove( serviceCache.keySet().iterator().next() );
		}
		Map< String, ServiceRecord > map = serviceCache.computeIfAbsent( btAddr, k -> new HashMap<>() );
		if( map.size() > CACHE_LIMIT ) {
			map.remove( map.keySet().iterator().next() );
		}
		map.put( uuidStr, record );
	}

	@Override
	public CommChannel createChannel( URI uri, OutputPort port )
		throws IOException {
		if( uri.getHost() != null && uri.getHost().equals( "localhost" ) ) {
			throw new IOException( "Malformed output btl2cap location: " + uri.toString() );
		}
		try {
			String connectionURL = BTL2CapHelper.getConnectionURL( uri, this );
			L2CAPConnection conn = (L2CAPConnection) Connector.open( connectionURL );
			return new BTL2CapCommChannel( conn, uri, port.getProtocol() );
		} catch( ClassCastException e ) {
			throw new IOException( "CastException: malformed output btl2cap location: " + uri.toString() );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
	}
}
