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
	.charset -> charset;
	.addHeader.header[0] -> headerAccept;
	.addHeader.header[1] -> headerCharset;
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

define checkResponse_formData
{
	// form data does not support "void" attribute values, they will be converted to "" ones
	if ( response.id != 123456789123456789L || response.firstName != "John" || response.lastName != "Döner" || response.age != 30 || response.size != 90.5 || response.male != true || response.unknown != "Hey" || response.unknown2 != "" ) {
		throw( TestFailed, "Data <=> Querystring value mismatch" )
	}
}

define checkResponse2
{
	if ( response2 != reqVal ) {
		throw( TestFailed, "Data (" + response2 + ") <=> Querystring (" + reqVal + ") value mismatch" )
	}
}

define test
{
	// utf-8
	charset = "utf-8";
	headerCharset << "Accept-Charset" { .value = "utf-8" };
	headerAccept << "Accept" { .value = "*/*" };
	format = undefined; // default
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "application/x-www-form-urlencoded" };
	format = "x-www-form-urlencoded"; // URL-encoded
	echoPerson@Server( person )( response );
	checkResponse_formData;
	headerAccept << "Accept" { .value = "text/xml" };
	format = "xml"; // XML
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "application/json" };
	format = "json"; // JSON
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "text/html" };
	format = "html"; // HTML
	identity@Server( reqVal )( response2 );
	checkResponse2;
	headerAccept << "Accept" { .value = "text/plain" };
	format = "raw"; // plain-text
	identity@Server( reqVal )( response2 );
	checkResponse2;
	headerAccept << "Accept" { .value = "application/octet-stream" };
	format = "binary"; // binary
	identity@Server( reqVal )( response2 );
	checkResponse2;

	// utf-16
	charset = "utf-16";
	headerCharset << "Accept-Charset" { .value = "utf-16" };
	headerAccept << "Accept" { .value = "*/*" };
	format = undefined; // default
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "application/x-www-form-urlencoded" };
	format = "x-www-form-urlencoded"; // URL-encoded
	echoPerson@Server( person )( response );
	checkResponse_formData;
	headerAccept << "Accept" { .value = "text/xml" };
	format = "xml"; // XML
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "application/json" };
	format = "json"; // JSON
	echoPerson@Server( person )( response );
	checkResponse;
	headerAccept << "Accept" { .value = "text/html" };
	format = "html"; // HTML
	identity@Server( reqVal )( response2 );
	checkResponse2;
	headerAccept << "Accept" { .value = "text/plain" };
	format = "raw"; // plain-text
	identity@Server( reqVal )( response2 );
	checkResponse2;
	headerAccept << "Accept" { .value = "application/octet-stream" };
	format = "binary"; // binary
	identity@Server( reqVal )( response2 );
	checkResponse2;

	// no response checking here, it just needs to pass
	charset = null;
	headerCharset << "Accept-Charset" { .value = "*" };
	headerAccept << "Accept" { .value = "*/*" };
	format = "html"; // HTML
	consume@Server( reqVal )( );
	format = "raw"; // plain-text
	consume@Server( reqVal )( );
	format = "binary"; // binary
	consume@Server( reqVal )( );
	format = "html"; // HTML
	consume2@Server( reqVal );
	format = "raw"; // plain-text
	consume2@Server( reqVal );
	format = "binary"; // binary
	consume2@Server( reqVal )
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
