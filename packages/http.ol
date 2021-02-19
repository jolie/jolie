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

/// A stream identifier
type SID:string

type ConnectStreamRequest {
	uri:string //< The URI to connect to
	headers?:undefined //< HTTP headers, if any
	method?:string( enum(["get"]) ) //< HTTP method (default: get)
	format?:string( enum(["json"]) ) //< HTTP method (default: json)
}

interface HttpUtilsIface {
RequestResponse:
	/// Connects to an HTTP stream. Returns the id of the created stream.
	connectStream( ConnectStreamRequest )( SID ) throws URISyntaxException,
	close( SID )( void ) throws NotFound
}

type NextStreamMessage {
	sid:SID //< The stream's identifier
	data:undefined //< The message's content
}

type EndStreamMessage {
	sid:SID
}

interface StreamHandlerIface {
OneWay:
	next( NextStreamMessage ), end( EndStreamMessage )
}

service HttpUtils {
	inputPort Input {
		location: "local"
		interfaces: HttpUtilsIface
	}

	foreign java {
		class: "joliex.http.HttpUtils"
	}
}