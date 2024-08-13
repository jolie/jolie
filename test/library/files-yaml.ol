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
from file import File
from assertions import Assertions

/**
	A template for test units.
*/
service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed File as file
	embed Assertions as assertions

	main {
		test()() {
			getServiceDirectory@file()( dir )
			getFileSeparator@file()( fs )

			yamlFilename = dir + fs + "private" + fs + "yaml-test.yaml"

			readFile@file( { filename = yamlFilename, format = "yaml" } )( yamlDoc )

			scope( yamlChecks ) {
				install( AssertionError => throw TestFailed( yamlChecks.AssertionError ) )

				equals@assertions( {
					actual << yamlDoc
					expected << {
						news[0] << {
							title = "News 1"
							date = "2024-01-01"
							content = "Content 1"
						}
						news[1] << {
							title = "News 2"
							date = "2024-02-02"
							content = "Content 2"
						}
					}
				} )()
			}
		}
	}
}
