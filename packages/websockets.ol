/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
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

/// A websocket identifier
type WID:string

type ConnectRequest {
	id:WID //< the id that should be assigned to the websocket
	uri:string //< the websocket URI to connect to
	corrData?:undefined //< correlation data for the notifications received from the utilities, if any
}

type CloseRequest {
	id:WID //< The websocket id
}

type SendRequest {
	id:WID //< The websocket id
	message:string //< The message
}

interface WebSocketUtilsInterface {
RequestResponse:
	/// Opens a websocket connection. Returns the id of the created websocket handler.
	connect( ConnectRequest )( void ) throws URISyntaxException,
	/// Sends a message over the specified websocket
	send( SendRequest )( void ) throws NotFound(void)
OneWay:
	/// Closes a websocket connection
	close( CloseRequest )
}

type OnOpenMesg {
	id:WID
	corrData?:undefined
}

type OnCloseMesg {
	id:WID
	corrData?:undefined
	code:int
	reason:string
	remote:bool
}

type OnMessageMesg {
	id:WID
	corrData?:undefined
	message:string
}

type OnErrorMesg {
	id:WID
	corrData?:undefined
	error:string
}

interface WebSocketHandlerInterface {
OneWay:
	onOpen( OnOpenMesg ),
	onMessage( OnMessageMesg ),
	onClose( OnCloseMesg ),
	onError( OnErrorMesg )
}

service WebSocketUtils {
	inputPort Input {
		location: "local"
		interfaces: WebSocketUtilsInterface
	}

	foreign java {
		class: "joliex.websocket.WebSocketUtils"
	}
}