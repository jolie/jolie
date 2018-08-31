/***************************************************************************
 *   Copyright (C) 2008-09-10 by Fabrizio Montesi <famontesi@gmail.com>    *
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
include "time.iol"
include "string_utils.iol"

constants {
	PollingInterval = 500
}

include "presenter.iol"
include "viewer.iol"

inputPort PollerInputPort {
Location: "local"
OneWay:
	start
}

main
{
	start( startData );
	Viewer.location = startData.viewerLocation;
	Presenter.location = startData.presenterLocation;
	oldCurrentPage = -1;
	currentDocument = "";
	while( 1 ) {
		sleep@Time( PollingInterval )();
		currentPage@Viewer()( currentPage );
		if ( currentPage != oldCurrentPage ) {
			oldCurrentPage = currentPage;
			undef( request );
			request.pageNumber = int( currentPage );
			request.local = 1;
			goToPage@Presenter( request )
		};
		currentDocument@Viewer()( currentDocument );
		if ( currentDocument != oldCurrentDocument ) {
			oldCurrentDocument = currentDocument;
			undef( request );
			request.documentUrl = currentDocument;
			request.local = 1;
			openDocument@Presenter( request )
		}
	}
}
