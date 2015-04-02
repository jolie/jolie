/***************************************************************************
 *   Copyright (C) by Claudio Guidi	                                       *
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

package joliex.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author claudio
 */
public class NetworkService extends JavaService  {

	public Value getNetworkInterfaceNames()
		throws FaultException {

		Value response = Value.create();
		ValueVector interfaces = response.getChildren( "interfaceName");

		try {
			Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
			int index = 0;
			while (list.hasMoreElements()) {
				NetworkInterface n = list.nextElement();
				interfaces.get( index ).setValue( n.getName() );
                                if ( n.getDisplayName() == null ) {
                                        interfaces.get( index ).getFirstChild("displayName").setValue( "" );
                                } else {
                                        interfaces.get( index ).getFirstChild("displayName").setValue( n.getDisplayName() );
                                }
				index++;
			}
		} catch( SocketException e ) {
			throw new FaultException( e );
		}
		return response;
	}

	public Value getIPAddresses( Value request )
		throws FaultException {

		Value response = Value.create();

		try {
			Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
			boolean found = false;
			while (list.hasMoreElements()) {
				NetworkInterface n = list.nextElement();
				
				if ( n.getName().equals( request.getFirstChild("interfaceName").strValue())) {
					found = true;
		
					Enumeration<InetAddress> ad = n.getInetAddresses();
					while( ad.hasMoreElements() ) {
						InetAddress ia = ad.nextElement();
						
						if ( ia.getHostName().contains( "." ) ) {
							// it is an IP4 address
							response.getFirstChild( "ip4").setValue( ia.getHostName() );
						} else {
							// it is an IP6
							response.getFirstChild( "ip6").setValue( ia.getHostName() );
						}

					}
				}

			}
			if ( !found ) {
				
				throw new FaultException("InterfaceNotFound", new Exception() );
			}
		} catch( SocketException e ) {
			throw new FaultException( e );
		}
		return response;

	}
}
