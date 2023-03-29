/*
 * Copyright (C) 2021 Mauro Sgarzi <sgarzi.mauro@gmail.com>
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
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

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			for( i = 0, i < 10, i++ ) {
				global.array[i] = i
			}
			i = 0
			for( item in global.array ) {
				if( item != i ) {
					throw( TestFailed, "global.array[" + i + "] = " + item + "instead of " + i )
				}
				i++
			}
		}
	}
}
