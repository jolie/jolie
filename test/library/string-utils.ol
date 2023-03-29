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
from string-utils import StringUtils

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed StringUtils as su

	main {
		test()() {
			if( fmt@su( "Hello {name}" { name = "Homer" } ) != "Hello Homer" )
				throw( TestFailed, "Hello Homer" )
			
			template = "The disk contains {files,number,integer} file(s)"
			if( fmt@su( template { files = 0 } ) != "The disk contains 0 file(s)" )
				throw( TestFailed, "The disk contains 0 file(s)" )
			
			if( fmt@su( { format = template, locale = "en-us", data.files = 12312332 } ) != "The disk contains 12,312,332 file(s)" )
				throw( TestFailed, "The disk contains 12,312,332 file(s)" )

			if( fmt@su( "Up to {pct,number,percent}" { pct = 0.6 } ) != "Up to 60%" )
				throw( TestFailed, "Up to 60%" )
		}
	}
}
