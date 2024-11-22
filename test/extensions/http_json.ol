/*
 * Copyright (C) 2024 Niels Erik Jepsen
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

include "../AbstractTestUnit.iol"

interface Iface {
    RequestResponse: AB(void)(string)
	RequestResponse: BA(void)(string)
}

outputPort Server {
	Location: "socket://localhost:12345"
	Protocol: http {
		format = "json"
	}
	Interfaces: Iface
}

embedded {
Jolie:
	"private/http_json_server.ol"
}

constants {
	correctResponce = "{\"sameName\":{\"anArray\":[\"Should be wrapped in an array\"],\"sameName\":\"\"}}"
}

define doTest
{
	AB@Server()( ABResponse )
	BA@Server()( BAResponse ) 

	if ( ABResponse != correctResponce) {
		if ( BAResponse != correctResponce) {
			throw( TestFailed, "Both type AB and BA does not fit correct response." )
		} else {
			throw( TestFailed, "Type AB does not fit correct response." )
		}
	}

	if ( BAResponse != correctResponce) {
		throw( TestFailed, "Type BA does not fit correct response." )
	}
	
	
}
