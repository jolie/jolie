/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

include "server.iol"

execution { single }

inputPort ServerInput {
Location: Location_HTTPServer
Protocol: http
Interfaces: ServerInterface
}

// Explicit test for http service aggregation on the TLS endpoint:
// ServerInputS -> ServerForward -> ServerInput

outputPort ServerForward {
Location: Location_HTTPServer
Protocol: http
Interfaces: ServerInterface
}

inputPort ServerInputS {
Location: Location_HTTPsServer
Protocol: https {
	.ssl.keyStore = "extensions/private/keystore.jks";
	.ssl.keyStorePassword = KeystorePassword
}
Aggregates: ServerForward
}

main
{
	provide
		[ echoPerson( request )( response ) {
			undef( response );
			response << request
		} ]
		[ identity( request )( response ) {
			undef( response );
			response << request
		} ]
		[ consume( request )( ) {
			nullProcess
		} ]
		[ consume2( request ) ]
	until
		[ shutdown() ]
}
