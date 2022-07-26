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
from .private.byref-server import ByRefServer
from .private.quicksort import Quicksort
from values import Values

service Test {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed ByRefServer as byRefServer
	embed Quicksort as quicksort
	embed Values as values

	main {
		test()() {
			x << {
				items[0] = "0"
				items[1] = "1"
				items[2] = "2"
			}
			y << &x
			if( is_defined( x ) )
				throw( TestFailed, "passing by reference did not undefine" )

			request.x = 1
			run@byRefServer( &request )()
			if( is_defined( request.x ) )
				throw( TestFailed, "passing by reference exposed a side-effect" )
			
			request.x = 1
			run@byRefServer( request )()
			if( request.x != 1 )
				throw( TestFailed, "passing a copy exposed a side-effect" )
			undef( request )
			
			request << {
				a = 1
				b = 3
				c << {
					c1 = 5
					c2 = 2
				}
			}
			originalRequest << request
			sumNodes@byRefServer( &request )( response )
			if( !equals@values( { fst << originalRequest, snd << response.data } ) )
				throw( TestFailed, "read-only navigation of a tree modified the tree" )
			
			if( response.result != 11 )
				throw( TestFailed, "wrong result in read-only tree navigation (expected 11, got " + response.result + ")" )
			
			// Create a reverse ordered vector
			for( i = 0, i < 100, i++ ) {
				vector.items[i] = 99 - i
			}
			sort@quicksort( &vector )( vector )
			for( i = 0, i < 100, i++ ) {
				if( vector.items[i] != string( i ) )
					throw( TestFailed, "sorting by reference does not work, item " + i + " has value " + vector.items[i] + " (expected " + i + ")" )
			}
		}
	}
}
