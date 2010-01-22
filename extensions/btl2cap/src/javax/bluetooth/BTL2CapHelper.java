/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package javax.bluetooth;

import java.io.IOException;
import java.net.URI;
import jolie.net.BTL2CapChannelFactory;
import jolie.net.BTServiceDiscoveryListener;

/**
 *
 * @author Fabrizio Montesi
 */
public class BTL2CapHelper
{
	public static String getConnectionURL( URI uri, BTL2CapChannelFactory factory )
		throws BluetoothStateException, IOException
	{
		String[] ss = uri.getSchemeSpecificPart().split( ":" );
		String uuidStr = ss[1].split( "/" )[0];
		String btAddr = ss[0].substring( 2 );
		ServiceRecord record = factory.getFromServiceCache( btAddr, uuidStr );

		if ( record == null ) {
			UUID uuid = new UUID( uuidStr, false );
			BTServiceDiscoveryListener listener = new BTServiceDiscoveryListener( uuid );
			LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices( null, new UUID[] { uuid }, new RemoteDevice( btAddr ), listener );
			record = listener.getResult();
			factory.putInServiceCache( btAddr, uuidStr, record );
		}

		if ( record == null ) {
			throw new IOException( "Service not found" );
		}

		String url = record.getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false );
		return url;
	}
}
