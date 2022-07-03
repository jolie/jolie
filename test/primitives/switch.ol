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

service Test {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			if(
				!switch( 5 ) {
					5 -> true
					instanceof int -> false
					default -> false
				}
			)
				throw( TestFailed, "switch on 5" )
			
			if(
				!switch( "hello" ) {
					instanceof int -> false
					"hello" -> true
					default -> false
				}
			)
				throw( TestFailed, "switch on hello" )

			if(
				!switch( "hello" ) {
					6 -> false
					instanceof int -> false
					default -> true
				}
			)
				throw( TestFailed, "switch on hello, default case" )

			x = switch( { name = "Homer" } ) {
				instanceof int -> false
				{ name:string } -> "OK"
				default -> 2
			}
			if( x != "OK" )
				throw( TestFailed, "switch on Homer" )

			people << { items[0] = "Homer", items[1] = "Bart" }
			x = switch( people ) {
				instanceof int -> false
				instanceof { items*:string } when #people.items > 1 -> true
				default -> 2
			}
			if( !x )
				throw( TestFailed, "switch on people, type case" )

			x << switch( people ) {
				instanceof int -> {}
				{ items[0] = "Homer" } -> {}
				{ items[0] = "Homer", items[1] = "Bart" } -> { correct = true }
				default -> {}
			}
			if( !x.correct )
				throw( TestFailed, "switch on people, value case" )
		}
	}
}
