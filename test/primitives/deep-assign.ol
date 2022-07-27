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

service Test {
	embed Values as values

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			a = 1
			a.b = 2
			a.b.c1 = 3
			a.b.c2 = 4

			x := 1 { b := 2 { c1 = 3, c2 = 4 } }
			// x << 1 { b << 2 { c1 = 3, c2 = 4 } }

			if( !equals@values( { fst := a, snd := x } ) )
				throw( TestFailed, "tree created step by step is not equal to inline tree" )
		}
	}
}
