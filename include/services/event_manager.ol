/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
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

execution { sequential }

inputPort EventPort {
Location: "local"
OneWay:
	/**
	 * Registers a location for receiving a certain event when it occurs.
	 * @request:void {
	 * 	.event:string the event name.
	 * 	.location:string the location to call back when the event occurs.
	 * 	.callbackData:? some callback data that will be sent back to the registered client.
	 * }
	 */
	register,
	/**
	 * Registers a location for receiving any event that may occur.
	 * @request:void {
	 * 	.location:string the location to call back when the event occurs.
	 * 	.callbackData:? some callback data that will be sent back to the registered client.
	 * }
	 */
	registerForAll,
	/**
	 * Unregisters a location for receiving an event.
	 * @request:void {
	 * 	.event:string the event name.
	 * 	.location:string the location to unregister.
	 * }
	 */
	unregister,
	/**
	 * Fires an event. This will cause every signed service to receive it.
	 * @request:string the event name {
	 * 	.*: additional data that will be sent to the waiting clients.
	 * }
	 */
	fireEvent,
	shutdown
}

outputPort Client {
Protocol: sodep
OneWay:
	receiveEvent
}

init
{
	Client.location[0] -> p.locations[i]
}

main
{
	[ register( request ) ] {
		global.clientsMap.(request.event).(request.location) = 1;
		global.clientsMap.(request.event).(request.location).callbackData << request.callbackData
	}

	[ registerForAll( request ) ] {
		global.clientsAll.(request.location) = 1;
		global.clientsAll.(request.location).callbackData << request.callbackData
	}

	[ unregister( request ) ] {
		undef( global.clientsMap.(request.event).(request.location) );
		undef( global.clientsAll.(request.location) )
	}

	[ fireEvent( event ) ] {
		foreach( location : global.clientsMap.(request.event) ) {
			Client.location = location;
			undef( event.callbackData );
			event.callbackData << global.clientsMap.(request.event).(location).callbackData;
			receiveEvent@Client( event )
		};
		foreach( location : global.clientsAll ) {
			// Do not send an event twice!
			if ( !is_defined( global.clientsMap.(request.event).(location) ) ) {
				Client.location = location;
				undef( event.callbackData );
				event.callbackData << global.clientsAll.(location).callbackData;
				receiveEvent@Client( event )
			}
		}
	}

	[ shutdown() ] {
		exit
	}
}
