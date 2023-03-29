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
from vectors import Vectors

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed Vectors as vectors

	main {
		test()() {
			vec << {
				items[0] = "0"
				items[1] = "1"
			}
			if( !equals@vectors( { fst << vec, snd << vec } ) )
				throw( TestFailed, "equals@vectors" )
			
			if( !equals@vectors( {
				fst << {
					items[0] = "0"
					items[1] = "Hi"
					items[2] = "1"
				}
				snd << insert@vectors( { 
					vector -> vec
					index = 1
					item = "Hi"
				} )
			} ) )
				throw( TestFailed, "insert@vectors" )

			if( !equals@vectors( {
				fst << {
					items[0] = "0"
					items[1] = "1"
					items[2] = "2"
				}
				snd << add@vectors( { 
					vector -> vec
					item = "2"
				} )
			} ) )
				throw( TestFailed, "add@vectors" )

			if( !equals@vectors( {
				fst << {
					items[0] = "1"
					items[1] = "2"
				}
				snd << slice@vectors( { 
					vector << {
						items[0] = "0"
						items[1] = "1"
						items[2] = "2"
					}
					from = 1
					to = 3
				} )
			} ) )
				throw( TestFailed, "slice@vectors" )
			
			if( !equals@vectors( {
				fst << {
					items[0] = "0"
					items[1] = "1"
					items[2] = "2"
					items[3] = "3"
				}
				snd << concat@vectors( { 
					fst << {
						items[0] = "0"
						items[1] = "1"
					}
					snd << {
						items[0] = "2"
						items[1] = "3"
					}
				} )
			} ) )
				throw( TestFailed, "concat@vectors" )
		}
	}
}
