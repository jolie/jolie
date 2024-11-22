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

type BAResponse: B | A
type ABResponse: A | B

type A {
    anArray*: undefined
    sameName: string
}

type B {
    sameName?: A
}

interface Iface {
    RequestResponse: AB(void)(BAResponse)
	RequestResponse: BA(void)(ABResponse)
}

define makeResponse
{
    //sameName is required to have the same name to test that the correct type is found when converting the type to json
	Response << {
		sameName << {
			anArray[0] = "Should be wrapped in an array"
			sameName = ""
		}
	}
} 

service json_server {
    execution: concurrent
    inputPort ip {
        location: "socket://localhost:12345"
        protocol: http {
            format = "json"
		    .contentType = "text/plain" //< overwrite the output format to 
        }
        interfaces: Iface
    }
    main {
        [AB()(Response) {
            makeResponse
         }]
		[BA()(Response) {
            makeResponse
         }] 
    }
}

