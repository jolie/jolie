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
 *   You should have received a copy of the GNU General Public             *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


include "console.iol"
include "runtime.iol"

constants {
	Location_PresenterService = "socket://192.168.1.20:9001",
	Debug = 1
}

execution { sequential }

include "presenter.iol"
include "viewer.iol"

inputPort PresenterInputPort {
OneWay:
	goToPage,
	openDocument,
	closeClientSession
RequestResponse:
	startClientSession
}

service PresenterBluetoothService
{
Location: "btl2cap://localhost:3B9FA89520078C303355AAA694238F07;name=Vision;encrypt=false;authenticate=false"
Protocol: sodep { .charset = "ISO8859-1"; .keepAlive = 1 }
Ports: PresenterInputPort
}

service PresenterService {
Location: Location_PresenterService
Protocol: sodep
Ports: PresenterInputPort
}

define initViewer
{
	install( CouldNotStartFault => println@Console( main.CouldNotStartFault ) );
	if ( #args < 1 ) {
		throw( CouldNotStartFault, "Syntax is: jolie presenter.ol kpdf|okular|previewer [presenter_uri]" )
	} else {
		embedInfo.filepath = "viewers/" + args[0] + ".ol";
		embedInfo.type = "Jolie";
		if ( args[0] != "kpdf" && args[0] != "okular" && args[0] != "previewer" ) {
			throw( CouldNotStartFault, "Unsupported viewer: " + args[0] )
		};

		install( RuntimeException => println@Console( main.RuntimeException.stackTrace ) );
		loadEmbeddedService@Runtime( embedInfo )( Viewer.location );
		if ( is_defined( args[1] ) ) {
			start@Viewer( startData )();
			req.location = Location_PresenterService;
			Presenter.location = args[1];
			startClientSession@Presenter( req )( resp );
			masterSid = resp.sid;
			openDocument@Viewer( resp );
			goToPage@Viewer( resp )
		} else {
			getLocalLocation@Runtime()( startData.presenterLocation );
			start@Viewer( startData )()
		}
	}
}


init
{
	currentPage -> global.currentPage;
	currentDocument -> global.currentDocument;
	clients -> global.clients;
	counter -> global.counter;
	masterSid -> global.masterSid;

	counter = 0;

	initViewer
}

main
{
	[ openDocument( request ) ] {
		currentDocument = request.documentUrl;
		req.documentUrl = request.documentUrl;
		req.local = 0;
		{
			foreach( sid : clients ) {
				Presenter.location = clients.(sid).location;
				openDocument@Presenter( req );
				if ( Debug ) {
					println@Console(
						"Sending openDocument(" + request.documentUrl + ") to " + Presenter.location
					)
				}
			}
			|
			if ( request.local == 0 ) {
				openDocument@Viewer( request )
			}
		}
	}

	[ goToPage( request ) ] {
		currentPage = request.pageNumber;
		req.pageNumber = request.pageNumber;
		req.local = 0;
		{
			foreach( sid : clients ) {
				Presenter.location = clients.(sid).location;
				goToPage@Presenter( req );
				if ( Debug ) {
					println@Console( "Sending goToPage(" + request.pageNumber + ") to " + Presenter.location )
				}
			}
			|
			if ( request.local == 0 ) {
				goToPage@Viewer( request )
			}
		}
	}

	[ startClientSession( request )( response ) {
		response.sid = counter++;
		clients.(response.sid).location = request.location;
		response.pageNumber = currentPage;
		response.documentUrl = currentDocument
	} ] {
		if ( Debug ) {
			println@Console( request.location + " is now following the presentation." )
		};
		Presenter.location = request.location;
		goToPage@Presenter( currentPage );
		openDocument@Presenter( currentDocument )
	}

	[ closeClientSession( sid ) ] {
		undef( clients.(sid) );
		if ( Debug ) {
			println@Console( clients.(sid).location + " stopped following the presentation." )
		}
	}
}
