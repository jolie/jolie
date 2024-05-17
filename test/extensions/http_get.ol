/*
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
 */

include "../AbstractTestUnit.iol"

include "private/http_server.iol"

outputPort Server {
Location: Location_HTTPServer
Protocol: http {
	.concurrent = true;
	.method = "get";
	.method.queryFormat -> queryFormat;
	.compression -> compression;
	.requestCompression -> requestCompression;
	.keepAlive -> keepAlive
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/http_server.ol"
}

define checkResponse
{
	if ( response.id != 123456789123456789L || response.firstName != "John" || response.lastName != "Döner" || response.age != 30 || response.size != 90.5 || response.male != true || response.unknown != "Hey" || response.unknown2 != void ) {
		throw( TestFailed, "Data <=> Querystring value mismatch" )
	}
}

define test
{
	queryFormat = undefined; // URL-encoded
	echoPerson@Server( person )( response );
	checkResponse;
	queryFormat = "json"; // JSON
	echoPerson@Server( person )( response );
	checkResponse
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
		.unknown2 = void
	};
	scope( s ) {
		install( TypeMismatch => throw( TestFailed, s.TypeMismatch ) );

		keepAlive = true; // first test with a persistent HTTP connection
		while ( keepAlive != null ) {
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

			if ( keepAlive ) {
				keepAlive = false // now re-test with on-demand connections
			} else {
				keepAlive = null
			}
		};

		shutdown@Server()
	}
}

