/*
 * Copyright (C) 2025 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2025 Marco Peressotti <marco.peressotti@gmail.com>
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
from runtime import Runtime
from console import Console

/**
	A template for test units.
*/
service Main {
	embed Runtime as runtime
	embed Console as console

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
			/* Assumptions
			 * - key is the name of a system property that is not defined when the test starts and that the security manager allows to set it.
			 * - default, value1, and value2 are distinct, non-empty strings.
       */ 
			key = "jolie.temp" 
			default = "default"
			value1 = "value 1"
			value2 = "value 2"
			// reading an undefined property retuns void or the default value if one is provided
			getProperty@runtime(key)( res[1] )
			getProperty@runtime(key{ .default = default })( res[2] );
			if( res[1] != void || res[2] != default ) {
			 	throw( TestFailed, "getProperty, unexpected result" )
			}
			undef(res)
			// setProperty updates a property and returns the previous value
			setProperty@runtime({ key = key value = value1 })( res[0] );
			setProperty@runtime({ key = key value = value2 })( res[1] );
			getProperty@runtime(key)( res[2] );
			if( res[0] != void || res[1] != value1 || res[2] != value2 ) {
			 	throw( TestFailed, "setProperty, unexpected result" )
			}
			undef(res)
			// getProperties 
			getProperties@runtime()( props );
			foreach( k : props ) {
				getProperty@runtime(k)( prop );
				if( props.(k) != prop ) {
					throw( TestFailed, "getProperties, unexpected result" )
				}
			}
			// clearProperty clears a property and returns the previous value
			clearProperty@runtime(key)( res[0] )
			getProperty@runtime(key)( res[1] )
			if( res[0] != value2 || res[1] != void ) {
			 	throw( TestFailed, "clearProperty, unexpected result" )
			}
			// illegal keys are rejected as type mismatches on the client side
			failed = true
			scope(f) {
				install(IllegalArgumentException => nullProcess )
				install( TypeMismatch => failed = false )
				getProperty@runtime("")( )
			}
			if( failed ) {
				throw( TestFailed, "empty keys are not rejected" )
			}
		}
	}
}
