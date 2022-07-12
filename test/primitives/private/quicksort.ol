/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) Saverio Giallorenzo
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

from runtime import Runtime

type QSType {
	items*:int { ? }
}

interface QuicksortInterface {
RequestResponse: sort( QSType )( QSType )
}

service Quicksort {
	execution: concurrent

	inputPort QuicksortInput {
		location: "local"
		interfaces: QuicksortInterface
	}

	outputPort self {
		interfaces: QuicksortInterface
	}

	embed Runtime as runtime

	init {
		getLocalLocation@runtime()( self.location )
	}

	main {
		sort( req )( &res ) {
			if( #req.items <= 1 ) {
				res << &req
			} else {
				pivot = (#req.items)/2;
				pivIt << req.items[ pivot ];
				for ( i=0, i < #req.items, i++) {
					if( i != pivot ) {
						if( req.items[ i ] < pivIt ) {
							left.items[ #left.items ] << req.items[ i ]
						} else {
							right.items[ #right.items ] << req.items[ i ]
						}
					}
				}
				if( #left.items > 0 ) {
					sort@self( left )( qsLeft )
				}
				if( #right.items > 0 ) {
					sort@self( right )( qsRight )
				}
				qsLeft.items[ #qsLeft.items ] << &pivIt;
				for( i = 0, i < #qsRight.items, i++ ) {
					qsLeft.items[ #qsLeft.items ] << qsRight.items[ i ]
				}
				res << &qsLeft
			}
		}
	}
}