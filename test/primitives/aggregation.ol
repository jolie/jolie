/*
 * Copyright (C) 2025 Claudio Guidi <guidiclaudio@gmail.com>
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

from ..test-unit import TestUnitInterface
from ..primitives.private.aggregation-test import test 

service Main {

    outputPort http {
        location: "socket://localhost:55555"
        protocol: http {
            debug = true
            debug.showContent = true
            addHeader.header[0] << "Accept" {
                value = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
            }
        }
        RequestResponse: testFr
    }

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed test as testService

	main {
		test()() {
            testFr@http( void )( response )
            if ( response.a.b.c != "hello" )
                throw( TestFailed, "aggregation primitive error" )
        }
    }
}