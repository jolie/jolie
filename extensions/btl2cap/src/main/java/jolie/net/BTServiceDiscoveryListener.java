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

import java.util.Enumeration;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BTServiceDiscoveryListener implements DiscoveryListener {
	private final UUID uuid;
	private boolean completed = false;
	private ServiceRecord serviceRecord = null;

	public BTServiceDiscoveryListener( UUID uuid ) {
		this.uuid = uuid;
	}

	@Override
	public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) {}

	public ServiceRecord getResult() {
		synchronized( this ) {
			while( !completed ) {
				try {
					this.wait();
				} catch( InterruptedException e ) {
				}
			}
		}
		return serviceRecord;
	}

	@Override
	public void inquiryCompleted( int discType ) {}

	@Override
	@SuppressWarnings( "unchecked" )
	public void servicesDiscovered( int transID, ServiceRecord[] serviceRecords ) {
		DataElement e;
		ServiceRecord r;
		Enumeration< DataElement > en;
		boolean keepRun = true;
		for( int i = 0; i < serviceRecords.length && keepRun; i++ ) {
			r = serviceRecords[ i ];
			// Search for the desired UUID
			if( (e = r.getAttributeValue( 0x0001 )) != null ) {
				if( e.getDataType() == DataElement.DATSEQ ) {
					en = (Enumeration< DataElement >) e.getValue();
					Object o;
					while( en.hasMoreElements() ) {
						o = en.nextElement().getValue();
						if( o instanceof UUID ) {
							if( o.equals( uuid ) ) {
								serviceRecord = r;
								keepRun = false;
							}
						}
					}
				} else if( e.getDataType() == DataElement.UUID ) {
					if( e.getValue().equals( uuid ) ) {
						serviceRecord = r;
						keepRun = false;
					}
				}
			}
		}
	}

	@Override
	public void serviceSearchCompleted( int transID, int respCode ) {
		synchronized( this ) {
			completed = true;
			this.notify();
		}
	}
}
