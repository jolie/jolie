/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
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

include "console.iol"
include "../../metaservice.iol"

outputPort Factorial {
Protocol: sodep
RequestResponse:
	calcFactorial
}

outputPort MetaService {
Location: "socket://localhost:8000/"
Protocol: sodep
Interfaces: MetaServiceAdministration, MetaServiceConsultation
}

main
{
	// We ask MetaService to set up a new service
	with( embedInfo ) {
		.resourcePrefix = "Factorial";
		.filepath = "examples/factorial/factorialServer.ol"
	};
	loadEmbeddedJolieService@MetaService( embedInfo )( resourceName );
	Factorial.location = MetaService.location + resourceName;

	// We call the service
	calcFactorial@Factorial( 5 )( result );
	println@Console( result )
}
