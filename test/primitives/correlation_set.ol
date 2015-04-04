/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

include "../AbstractTestUnit.iol"

include "private/cset_server.iol"
include "runtime.iol"

outputPort Server {
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/cset_server.ol" in Server
}

inputPort ClientInput {
Location: "local"
Interfaces: ClientInterface
}

define doTest
{
	getLocalLocation@Runtime()( r[0].clientLocation );
	r[1].clientLocation = r[0].clientLocation;
	r[2].clientLocation = r[0].clientLocation;
	{
		r[0].person.firstName = "John"; r[0].person.lastName = "Smith";
		startSession@Server( r[0] )( resp[0] );
		preEndSession@Server( resp[0] );
		endSession@Server( r[0].person )
		|
		r[1].person.firstName = "Donald"; r[1].person.lastName = "Duck";
		startSession@Server( r[1] )( resp[1] );
		preEndSession@Server( resp[1] );
		endSession@Server( r[1].person )
		|
		r[2].person.firstName = "Duffy"; r[2].person.lastName = "Duck";
		startSession@Server( r[2] )( resp[2] );
		preEndSession@Server( resp[2] );
		endSession@Server( r[2].person )
	};
	for( i = 0, i < #r, i++ ) {
		onSessionEnd( event );
		for( k = 0, k < #resp, k++ ) {
			if ( event.sid == resp[k].sid ) {
				if (
					event.person.firstName != r[k].person.firstName
					||
					event.person.lastName != r[k].person.lastName
				) {
					throw( TestFailed )
				}
			}
		}
	}
}

