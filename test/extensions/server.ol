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

include "../AbstractTestUnit.iol"

include "private/server.iol"

outputPort SODEPServer {
Location: Location_SODEPServer
Protocol: sodep
Interfaces: ServerInterface
}

outputPort SODEPSServer {
Location: Location_SODEPSServer
Protocol: sodeps {
	.ssl.trustStore = "extensions/private/truststore.jks";
	.ssl.trustStorePassword = KeystorePassword
}
Interfaces: ServerInterface
}

outputPort SOAPServer {
Location: Location_SOAPServer
Protocol: soap {
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort JSONRPCServer {
Location: Location_JSONRPCServer
Protocol: jsonrpc {
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort HTTPServer {
Location: Location_HTTPServer
Protocol: http {
	.method -> method;
	.method.queryFormat = "json";
	.format -> format;
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort HTTPSServer {
Location: Location_HTTPSServer
Protocol: https {
	.method -> method;
	.method.queryFormat = "json";
	.format -> format;
	.compression -> compression;
	.requestCompression -> requestCompression;
	.ssl.trustStore = "extensions/private/truststore.jks";
	.ssl.trustStorePassword = KeystorePassword
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/sodep_server.ol",
	"private/sodeps_server.ol",
	"private/soap_server.ol",
	"private/jsonrpc_server.ol",
	"private/http_server2.ol",
	"private/https_server.ol"
}

define checkResponse
{
	if ( response.id != 123456789123456789L
		|| response.firstName != "John"
		|| response.lastName != "Döner"
		|| response.age != 30
		|| response.size != 90.5
		|| response.male != true
		|| response.unknown != "Hey"
		|| response.unknown2 != void
		|| #response.array != 3
		|| response.array[0] != 0
		|| response.array[1] != "Ho"
		|| response.array[2] != 3.14
		|| response.object.data != 10L ) {
		throw( TestFailed, "compression:" + compression + ", requestCompression:" + requestCompression + "\n" +current_protocol + "\nCheckResponse: Data <=> Query value mismatch\n"
		+ "\t.id:" + (response.id != 123456789123456789L) +"\n"
		+ "\t.firstName:" + (response.firstName != "John") +"\n"
		+ "\t.lastName:" + (response.lastName != "Döner") +"\n"
		+ "\t.age:" + (response.age != 30) +"\n"
		+ "\t.size:" + (response.size != 90.5) +"\n"
		+ "\t.male:" + (response.male != true) +"\n"
		+ "\t.unknown:" + (response.unknown != "Hey") +"\n"
		+ "\t.unknown2:" + (response.unknown2 != void) +"\n"
		+ "\t.array:" + (#response.array != 3) +"\n"
		+ "\t.array[0]:" + (response.array[0] != 0) +"\n"
		+ "\t.array[1]:" + (response.array[1] != "Ho") +"\n"
		+ "\t.array[2]:" + (response.array[2] != 3.14) +"\n"
		+ "\t.object:" + (response.object.data != 10L) +"\n"
		)
	};
	if ( response2 != reqVal ) {
		throw( TestFailed, "Data <=> Query value mismatch" )
	}
}

define test
{
	echoPerson@SODEPServer( person )( response );
	identity@SODEPServer( reqVal )( response2 );
	consume@SODEPServer( reqVal )( );
	consume2@SODEPServer( reqVal );
	checkResponse;
	echoPerson@SODEPSServer( person )( response );
	identity@SODEPSServer( reqVal )( response2 );
	consume@SODEPSServer( reqVal )( );
	consume2@SODEPSServer( reqVal );
	checkResponse;

	echoPerson@SOAPServer( person )( response );
	identity@SOAPServer( reqVal )( response2 );
	consume@SOAPServer( reqVal )( );
	consume2@SOAPServer( reqVal );
	checkResponse;

	echoPerson@JSONRPCServer( person )( response );
	identity@JSONRPCServer( reqVal )( response2 );
	consume@JSONRPCServer( reqVal )( );
	consume2@JSONRPCServer( reqVal );
	checkResponse;

	method = "post";
	format = "xml";
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	consume@HTTPServer( reqVal )( );
	consume2@HTTPServer( reqVal );
	checkResponse;
	echoPerson@HTTPSServer( person )( response );
	identity@HTTPSServer( reqVal )( response2 );
	consume@HTTPSServer( reqVal )( );
	consume2@HTTPSServer( reqVal );
	checkResponse;
	format = "json";
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	consume@HTTPServer( reqVal )( );
	consume2@HTTPServer( reqVal );
	checkResponse;
	echoPerson@HTTPSServer( person )( response );
	identity@HTTPSServer( reqVal )( response2 );
	checkResponse;
	method = "get"; // JSON-ified
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	consume@HTTPServer( reqVal )( );
	consume2@HTTPServer( reqVal );
	checkResponse;
	echoPerson@HTTPSServer( person )( response );
	identity@HTTPSServer( reqVal )( response2 );
	consume@HTTPSServer( reqVal )( );
	consume2@HTTPSServer( reqVal );
	checkResponse
}

define shutdown
{
	shutdown@SODEPServer()
	|
	shutdown@SODEPSServer()
	|
	shutdown@SOAPServer()
	|
	shutdown@JSONRPCServer()
	|
	shutdown@HTTPServer()
	|
	shutdown@HTTPSServer()
}

define doTest
{
	with( person ) {
		.id = 123456789123456789L;
		.firstName = "John";
		.lastName = "Döner";
		.age = 30;
		.size = 90.5;
		.male = true;
		.unknown = "Hey";
		.unknown2 = void;
		.array[0] = 0;
		.array[1] = "Ho";
		.array[2] = 3.14;
		.object.data = 10L
	};
	reqVal = "Döner";
	scope( s ) {
		install( TypeMismatch => shutdown; throw( TestFailed, s.TypeMismatch ) );

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

		shutdown
	}
}
