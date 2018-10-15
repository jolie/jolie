/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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


type BluetoothInquiryResponse:void {
	.device[0,*]:void {
		.address:string
		.name:string
	}
	.service[0,*]:void {
		.location:string
	}
}

interface BluetoothInterface {
RequestResponse:
	/**!
	 * Sets the current Bluetooth device as discoverable or not discoverable
	 * @request: 0 if the device has to be set not discoverable, 1 if the device has to be set discoverable.
	 */
	setDiscoverable(int)(int),
	inquire(void)(BluetoothInquiryResponse)
	//discoveryServices
}

outputPort Bluetooth {
Interfaces: BluetoothInterface
}

embedded {
Java:
	"joliex.net.BluetoothService" in Bluetooth
}
