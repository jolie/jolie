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
include "exec.iol"
include "runtime.iol"
include "string_utils.iol"
include "viewer.iol"
include "ui/swing_ui.iol"

execution { sequential }

inputPort ViewerInputPort {
Location: "local"
Interfaces: ViewerInterface
}

include "presenter.iol"
include "poller.iol"

define startPoller
{
	install( RuntimeException => println@Console( main.RuntimeException.stackTrace )() );
	println@Console("Poller 1")();
	pollerData.type = "Jolie";
	pollerData.filepath = "poller.ol";
	loadEmbeddedService@Runtime( pollerData )( Poller.location );
	getLocalLocation@Runtime()( pollerStartData.viewerLocation );
	pollerStartData.presenterLocation = Presenter.location;
	start@Poller( pollerStartData )
}

define initDocumentViewer
{
	exec@Exec( "qdbus org.kde.okular*" )( cmdResult );
	if ( !is_string( cmdResult ) ) {
		throw( ViewerFault, "Could not query Okular" )
	} else {
		cmdStr = cmdResult
	};
	trim@StringUtils( cmdStr )( cmdStr );
	cmdStr.regex = "\n";
	split@StringUtils( cmdStr )( ss );
	if ( #ss.result < 1 ) {
		throw( CouldNotStartFault, "Could not find a running viewer" )
	};

	if ( #ss.result > 1 ) {
		range = " (1.." + (#ss.result) + ")"
	};
	choiceText = "Choose a viewer instance" + range;
	// println@Console( "Choose a viewer instance" + range )();
	for( i = 0, i < #ss.result, i++ ) {
		// We display numbers starting by 1
		exec@Exec( "qdbus " + ss.result[i] + " /okular currentDocument" )( cDoc );
		doc = cDoc;
		trim@StringUtils( doc )( doc );
		choiceText += "\n" + (i+1) + ") " + ss.result[i] + " - Currently displaying: " + doc
		// println@Console( (i+1) + ") " + ss.result[i] + " - Currently displaying: " + doc )()
	};
	selected = -1;
	// registerForInput@Console()();
	while( selected < 1 || selected > #ss.result ) {
		showInputDialog@SwingUI( choiceText )( selected );
		// print@Console( "> " )();
		// in( selected );
		selected = int(selected)
	};
	selected--;

	cmdStr = "qdbus " + ss.result[selected] + " "
}

init
{
	cmdStr -> global.cmdStr;

	start( startData )() {
		initDocumentViewer;
		if ( is_defined( startData.presenterLocation ) ) {
			Presenter.location = startData.presenterLocation;
			startPoller
		}
	}
}

main
{
	[ goToPage( request ) ] {
		exec@Exec( cmdStr + "/okular goToPage " + request.pageNumber )()
	}

	[ openDocument( request ) ] {
		exec@Exec( cmdStr + "/okular openDocument " + request.documentUrl )()
	}

	[ close( request ) ] {
		exec@Exec( cmdStr + "/MainApplication quit" )()
	}

	[ currentPage()( response ) {
		exec@Exec( cmdStr + "/okular currentPage" )( r );
		response = r;
		trim@StringUtils( response )( response )
	} ] { nullProcess }

	[ currentDocument()( response ) {
		exec@Exec( cmdStr + "/okular currentDocument" )( r );
		response = r;
		trim@StringUtils( response )( response )
	} ] { nullProcess }
}
