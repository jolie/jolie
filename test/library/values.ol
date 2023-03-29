/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
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
from values import Values

/**
	A template for test units.
*/
service Main {
	embed Values as values

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			person << {
				name = "Homer"
			   	age = 42
				interests[0] = "beer"
				interests[1] = "hockey"
			}
			if( !equals@values( {
				fst << person
				snd << person
			} ) ) {
				throw( TestFailed, "value equality does not match expected result" )
			}

			if( !equals@values( {
				fst -> person
				snd << person
			} ) ) {
				throw( TestFailed, "value equality does not match expected result" )
			}

			if( equals@values( {
				fst << person
				snd << {
					name = "Homer"
					age = 42
					interests[0] = "beer"
				}
			} ) ) {
				throw( TestFailed, "value equality does not match expected result" )
			}

			if( equals@values( {
				fst << person
				snd << {
					name = "Homer"
					age = 42
					interests[0] = "hockey"
					interests[1] = "beer"
				}
			} ) ) {
				throw( TestFailed, "value equality does not match expected result" )
			}
		}
	}
}
