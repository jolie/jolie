/*
 * Copyright (C) 2023 Fabrizio Montesi <famontesi@gmail.com>
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

interface IdInterface {
RequestResponse: id(undefined)(undefined), id2(undefined)(undefined)
}

service ConcService {
	execution: concurrent

	inputPort Input {
		location: "local"
		interfaces: IdInterface
	}

	main {
		[ id(x)(x) ]
		[ id2(x)(x) ]
	}
}

service SeqService {
	execution: sequential

	inputPort Input {
		location: "local"
		interfaces: IdInterface
	}

	main {
		[ id(x)(x) ]
		[ id2(x)(x) ]
	}
}

service RecService {
	execution: single

	inputPort Input {
		location: "local"
		interfaces: IdInterface
	}

	define X {
		{
			[ id(x)(x) ]
			[ id2(x)(x) ]
		}
		X
	}
	main {
		X
	}
}

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed ConcService as conc
	embed RecService as rec
	embed SeqService as seq

	main {
		test()() {
			MAX = 1000
			while( i++ < MAX ) {
				if( id@conc( i ) != i ) {
					throw TestFailed( "concurrent identity operation returned an unexpected result" )
				}
			}
			i = 0
			while( i++ < MAX ) {
				if( id@rec( i ) != i ) {
					throw TestFailed( "recursive identity operation returned an unexpected result" )
				}
			}
			i = 0
			while( i++ < MAX ) {
				if( id@seq( i ) != i ) {
					throw TestFailed( "sequential identity operation returned an unexpected result" )
				}
			}
		}
	}
}
