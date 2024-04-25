/***************************************************************************
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

include "xmlrpc_server.iol"

execution { single }

inputPort ServerInput {
Location: Location_XMLRPCServer
Protocol: xmlrpc
Interfaces: ServerInterface
}

inputPort ServerInputS {
Location: Location_XMLRPCsServer
Protocol: xmlrpcs {
	.ssl.keyStore = "extensions/private/keystore.jks";
	.ssl.keyStorePassword = KeystorePassword
}
Interfaces: ServerInterface
}

main
{
	provide
		[ sum( request )( response ) {
			undef( response );
			response.param = request.param[0] + request.param[1];
			for (i = 0, i < #request.param[2].array.value.array, i++)
				response.param += request.param[2].array.value.array[i]
		}]
		[ identity( request )( response ) {
			undef( response );
			response << request
		}]
	until
		[ shutdown() ]
}
