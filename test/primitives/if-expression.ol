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

/**
	A template for test units.
*/
service Test {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			x = if( 1 > 0 ) "then" else "else"
			if( x != "then" )
				throw( TestFailed, "if expression (1 > 0)" )

			x = if( 0 > 1 ) "then" else "else"
			if( x != "else" )
				throw( TestFailed, "if expression (0 > 1)" )
		}
	}
}
