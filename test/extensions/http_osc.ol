/*
 *   Copyright (C) 2022 by Claudio Guidi <cguidi@italianasoftware.com>     *
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
 */

include "../AbstractTestUnit.iol"

include "string_utils.iol"
include "console.iol"

interface HTTPInterface {
RequestResponse:
	default( undefined )( undefined ) throws NotFound
}


outputPort Server {
Location:  "socket://localhost:10101"
Protocol: http {
	.concurrent = true
	.responseHeaders="@header"
}
Interfaces: HTTPInterface
}

// outputPort ServerExternal {
// Location: "socket://api.restful-api.dev:443/objects"
// Protocol: https {
// 	.method = "get"
// 	.osc.default.alias = ""
// 	.osc.default.outHeaders.("Accept")= "accept"
// 	.responseHeaders="@header"
// }
// Interfaces: HTTPInterface
// }

embedded {
Jolie:
	"private/http_server_osc.ol"
}


define doTest
{
	default@Server()( response ); // first call: 200 Ok/204 No Content
	if ( response.( "@header" ).statusCode != "200"
		&& response.( "@header" ).statusCode != "204" ) {
		throw( TestFailed, "Status code 200 or 204 expected: " + response.( "@header" ).statusCode )
	};

	default@Server()( response ); // second call: 404 Not Found
	if ( response.( "@header" ).statusCode != "404" ) {
		throw( TestFailed, "Status code 404 expected: " + response.( "@header" ).statusCode )
	}

	// default@ServerExternal({ accept = "application/json" })( response ) // third call to the outside: should not crash
	// if ( response.( "@header" ).statusCode != "200" ) {
	// 	throw( TestFailed, "Status code 200 expected: " + response.( "@header" ).statusCode )
	// }
}