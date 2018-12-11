/**********************************************************************************
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>    *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                              *
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>                 *  
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

include "../AbstractTestUnit.iol"
include "private/iot_server.iol"

outputPort Broker {
	Location: Location_MQTTBrokerRemote
	Protocol: mqtt {
		// .debug = true;
		.osc.echoPerson << {
			.alias = "echoPerson",
			.QoS = 1,
			.format = "json"
		};
		.osc.identity << {
			.alias = "identity",
			.QoS = 1,
			.format = "json"
		}
	}
	Interfaces: IoTServerInterface
}

outputPort CoAPServer {
	Location: Location_CoAPServer
	Protocol: coap {
		.osc.shutdown.messageType = "CON"
	}
	Interfaces: IoTServerInterface
}

embedded {
Jolie:
	"private/mqtt_server.ol",
	"private/coap_server.ol"
}

define checkResponse
{
	if ( response.id != 123456789123456789L 
		|| response.firstName != "John" 
		|| response.lastName != "Döner" 
		|| response.age != 30 
		|| response.size != 90.5 
		|| response.male != true 
		|| response.unknown != "Hey" 
		|| response.unknown2 != void 
		|| #response.array != 3 
		|| response.array[0] != 0 
		|| response.array[1] != "Ho" 
		|| response.array[2] != 3.14 
		|| response.object.data != 10L ) {
		throw( TestFailed, "Data <=> Query value mismatch" )
	};
	if ( response2 != reqVal ) {
		throw( TestFailed, "Data <=> Query value mismatch" )
	}
}

define test
{
	echoPerson@Broker( person )( response );
	identity@Broker( reqVal )( response2 );
	checkResponse;

	echoPerson@CoAPServer( person )( response );
	identity@CoAPServer( reqVal )( response2 );
	checkResponse
}

define shutdown
{
	shutdown@Broker()
	|
	shutdown@CoAPServer()
}

// main
define doTest
{
	with( person ) {
		.id = 123456789123456789L;
		.firstName = "John";
		.lastName = "Döner";
		.age = 30;
		.size = 90.5;
		.male = true;
		.unknown = "Hey";
		.unknown2 = void;
		.array[0] = 0;
		.array[1] = "Ho";
		.array[2] = 3.14;
		.object.data = 10L
	};
	reqVal = "Döner";
	scope( s ) {
		install( TypeMismatch => shutdown; throw( TestFailed, s.TypeMismatch ) );
		test;
		shutdown
	}
}
