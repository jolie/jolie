/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

include "http_server.iol"

// New interface since Authorization header gets added on each request
type HeaderPerson:void {
	.id:long
	.firstName:string
	.lastName:string
	.age:int
	.size:double
	.male:bool
	.unknown:any
	.unknown2:undefined
	.Authorization:string
}

type HeaderIdentity:any {
	.Authorization:string
}

interface HeadersServerInterface {
OneWay:
	shutdown(undefined),
	consume2(HeaderIdentity)
RequestResponse:
	echoPerson(HeaderPerson)(undefined),
	identity(HeaderIdentity)(undefined),
	consume(HeaderIdentity)(void),

	illegalStatusCode(HeaderIdentity)(void),
	illegalRedirectNoLocation(HeaderIdentity)(void)
}

execution { single }

inputPort ServerInput {
Location: Location_HTTPServer
Protocol: http {
	.headers.Authorization = "Authorization";
	.statusCode -> statusCode;
	.keepAlive -> KeepAlive_HTTPServer
}
Interfaces: HeadersServerInterface
}

define handleRequest
{
	statusCode = 200;
	undef( response );
	if ( request.Authorization != "TOP_SECRET" ) {
		statusCode = 403 // Forbidden
	} else {
		response << request;
		undef( response.Authorization )
	}
}

main
{
	provide
		[ echoPerson( request )( response ) {
			handleRequest
		} ]
		[ identity( request )( response ) {
			handleRequest
		} ]
		[ consume( request )( ) {
			statusCode = 204;
			if ( request.Authorization != "TOP_SECRET" ) {
				statusCode = 403 // Forbidden
			}
		} ]
		[ consume2( request ) ]
		[ illegalStatusCode ( )( ) {
			statusCode = 0
		} ]
		[ illegalRedirectNoLocation ( )( ) {
			statusCode = 302
		} ]
	until
		[ shutdown() ]
}
