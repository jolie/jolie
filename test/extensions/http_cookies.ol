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

include "private/http_cookies_server.iol"

outputPort Server {
Location: Location_HTTPServer
Protocol: http {
	.cookies.first_name = "firstName";
	.cookies.last_name = "lastName";
	.cookies.age = "age";
	.cookies.age.type = "int"
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/http_cookies_server.ol"
}

define doTest
{
	with( person ) {
		..firstName = "John";
		..lastName = "Smith";
		..age = 30
	};
	scope( s ) {
		install( TypeMismatch => throw( TestFailed, s.TypeMismatch ) );
		echoPerson@Server( person )( response );
		if ( response.firstName != "John" ) {
			throw( TestFailed, "Data <=> Cookie value mismatch" )
		}
	}
}

