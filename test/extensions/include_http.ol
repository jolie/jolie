/***************************************************************************
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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

include "runtime.iol"

outputPort IncludeClient {
Interfaces: TestUnitInterface
}

interface HttpServerInterface {
RequestResponse:
	getIncludeFile(void)(string)
}

outputPort HttpServer {
Interfaces: HttpServerInterface
}

embedded {
Jolie:
	"private/include_http_server.ol" in HttpServer
}

define doTest
{
	scope( s ) {
		install( default => throw( TestFailed, "Could not include file from HTTP URL" ) );
		srv.type = "Jolie";
		srv.filepath = "private/include_http_client.ol";
		loadEmbeddedService@Runtime( srv )( IncludeClient.location );
		test@IncludeClient()();
		callExit@Runtime( HttpServer.location )()
	}
}

