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

include "string_utils.iol"
include "console.iol"

outputPort Server {
Location: Location_HTTPServer
Protocol: http {
	.method = "post";
	.addHeader.header[0] -> header;
	.statusCode -> statusCode;
	.keepAlive -> keepAlive
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/http_features_server.ol"
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
	statusCode = 0; // important: initialise statusCode, otherwise it does not get set

	keepAlive = true; // first test with a persistent HTTP connection
	while ( keepAlive != null ) {
		header << "Authorization" { value = "TOP_SECRET" };
		scope( s ) {
			install( TypeMismatch => throw( TestFailed, s.TypeMismatch ) );
			echoPerson@Server( person )( response );
			checkResponse;
			if ( statusCode != 200 ) { // OK
				throw( TestFailed, "Wrong HTTP status code" )
			};
			identity@Server( reqVal )( response2 );
			checkResponse2;
			if ( statusCode != 200 ) { // OK
				throw( TestFailed, "Wrong HTTP status code" )
			};
			consume@Server( reqVal )( );
			if ( statusCode != 204 ) { // No Content
				throw( TestFailed, "Wrong HTTP status code" )
			};
			consume2@Server( reqVal );
			if ( statusCode != 204 ) { // No Content
				throw( TestFailed, "Wrong HTTP status code" )
			}
		};

		header << "Authorization" { .value = "WRONG_KEY" };
		scope( s ) {
			install( TypeMismatch => nullProcess );
			echoPerson@Server( person )( response );
			if ( !(response instanceof void) ) {
				throw( TestFailed, "Should not return data" )
			}
		};
		if ( statusCode != 403 ) { // Forbidden
			throw( TestFailed, "Wrong HTTP status code" )
		};
		scope( s ) {
			install( TypeMismatch => nullProcess );
			identity@Server( reqVal )( response2 );
			if ( !(response2 instanceof void) ) {
				throw( TestFailed, "Should not return data" )
			}
		};
		if ( statusCode != 403 ) { // Forbidden
			throw( TestFailed, "Wrong HTTP status code" )
		};
		scope( s ) {
			install( TypeMismatch => nullProcess );
			consume@Server( reqVal )( )
		};
		if ( statusCode != 403 ) { // Forbidden
			throw( TestFailed, "Wrong HTTP status code" )
		};
		/* not possible yet since for One-Way operations we may not change the return status code
		scope( s ) {
			install( TypeMismatch => nullProcess );
			consume2@Server( reqVal )
		};
		if ( statusCode != 403 ) { // Forbidden
			throw( TestFailed, "Wrong HTTP status code" )
		};
		*/
		scope( s ) {
			install( TypeMismatch => nullProcess );
			illegalStatusCode@Server( )( )
		};
		if ( statusCode != 500 ) { // Internal Server Error
			throw( TestFailed, "Wrong HTTP status code" )
		};
		scope( s ) {
			install( TypeMismatch => nullProcess );
			illegalRedirectNoLocation@Server( )( )
		};
		if ( statusCode != 500 ) { // Internal Server Error
			throw( TestFailed, "Wrong HTTP status code" )
		};

		if ( keepAlive ) {
			keepAlive = false // now re-test with on-demand connections
		} else {
			keepAlive = null
		}
	};

	shutdown@Server()
}
