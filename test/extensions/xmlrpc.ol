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

include "../AbstractTestUnit.iol"
include "security_utils.iol"

include "private/xmlrpc_server.iol"

outputPort Server {
Location: Location_XMLRPCServer
Protocol: xmlrpc {
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort ServerS {
Location: Location_XMLRPCsServer
Protocol: xmlrpcs {
	.compression -> compression;
	.requestCompression -> requestCompression;
	.ssl.trustStore = "extensions/private/truststore.jks";
	.ssl.trustStorePassword = KeystorePassword
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/xmlrpc_server.ol"
}

define test
{
	request.param[0] = 1;
	request.param[1] = 1;
	request.param[2].array.value.array[0] = 1;
	request.param[2].array.value.array[1] = 1;
	request.param[2].array.value.array[2] = 1;
	request.param[2].array.value.array[3] = 1;
	sum@Server( request )( response );
	if ( response.param != 6 ) {
		throw ( TestFailed, "Wrong result" )
	};
	sum@ServerS( request )( response );
	if ( response.param != 6 ) {
		throw ( TestFailed, "Wrong result" )
	};

	req2.param = true; // bool
	identity@Server( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};
	identity@ServerS( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};

	req2.param = 10; // int
	identity@Server( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};
	identity@ServerS( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};

	req2.param = 10.0; // double
	identity@Server( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};
	identity@ServerS( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};

	req2.param = "Döner"; // string
	identity@Server( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};
	identity@ServerS( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	};

	secReq.size = 50; // raw
	secureRandom@SecurityUtils( secReq )( req2.param );
	identity@Server( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	}
	identity@ServerS( req2 )( response );
	if ( req2.param != response.param ) {
		throw ( TestFailed, "Wrong result" )
	}
}

define doTest
{
	scope( s ) {
		install( TypeMismatch => throw( TestFailed, s.TypeMismatch ) );

		// compression on (default), but no request compression
		test;
		// request compression
		requestCompression = "deflate";
		test;
		requestCompression = "gzip";
		test;
		// no compression at all
		compression = false;
		test;

		shutdown@Server()
	}
}
