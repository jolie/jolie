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

include "private/http_server.iol"

outputPort Server {
Location: Location_HTTPServer
Protocol: http {
	.method = "post";
	.format -> format;
	.compression -> compression;
	.requestCompression -> requestCompression
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

define checkResponse2
{
	if ( response2 != reqVal ) {
		throw( TestFailed, "Data <=> Querystring value mismatch" )
	}
}

define test
{
	format = undefined; // default
	echoPerson@Server( person )( response );
	checkResponse;
	format = "x-www-form-urlencoded"; // URL-encoded
	echoPerson@Server( person )( response );
	checkResponse;
	format = "xml"; // XML
	echoPerson@Server( person )( response );
	checkResponse;
	format = "json"; // JSON
	echoPerson@Server( person )( response );
	checkResponse;
	/* format = "text/x-gwt-rpc"; // GWT-RPC
	echoPerson@Server( person )( response );
	checkResponse; */ // Currently unsupported
	format = "html"; // HTML
	identity@Server( reqVal )( response2 );
	checkResponse2;
	format = "raw"; // plain-text
	identity@Server( reqVal )( response2 );
	checkResponse2;
	format = "binary"; // binary
	identity@Server( reqVal )( response2 );
	checkResponse2
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
	reqVal = "Döner";
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

