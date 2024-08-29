/*
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
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
from .private.localsocket-server import LocalSocketServerInterface, LocalSocketServer
from file import File
from runtime import Runtime

service Main {
	embed File as file
	embed Runtime as runtime

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	outputPort server {
		interfaces: LocalSocketServerInterface
	}

	main {
		test()() {
			getServiceDirectory@file()( dir )
			getFileSeparator@file()( fs )
			localSocketServerFilename = dir + fs + "private" + fs + "localsocket-server.ol"

			servers[ 0 ].protocol = "sodep"
			servers[ 1 ].protocol = "http"

			for( i = 0, i < #servers, i++ ) {
				servers[ i ].location = "localsocket:" + dir + fs + "private" + fs + "tmp-" + i + ".socket"
			}

			for( serverConfig in servers ) {
				loadEmbeddedService@runtime( {
					filepath = localSocketServerFilename
					params << serverConfig
				} )()
				undef( server )
				server << serverConfig
				for( i = 0, i < 10, i++ ) {
					twice@server( i )( result )
					if( result != i * 2 ) {
						throw( TestFailed, "Expected " + i * 2 + ", got " + result + " with protocol " + server.protocol )
					}
				}
			}
		}
	}
}
