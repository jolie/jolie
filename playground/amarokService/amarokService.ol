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
include "exec.iol"
include "string_utils.iol"
include "runtime.iol"

constants {
	DCOPPrefix = "dcop amarok ",
	Location_AmarokService = "socket://localhost:10100"
}

execution { sequential }

interface AmarokInterface {
OneWay:
	play, pause, previous,
	next, playByIndex, setVolume
RequestResponse:
	getLyrics, getPlaylist,
	getNowPlaying, getVolume
}

outputPort EventManager {
OneWay:
	register,
	unregister,
	registerForAll,
	fireEvent
}

outputPort Poller {
OneWay:
	setEventManagerLocation,
	start, setAmarokServiceLocation
}

embedded {
Jolie:
	"services/EventManager.ol" in EventManager,
	"amarokPoller.ol" in Poller
}

inputPort AmarokService {
Location: Location_AmarokService
Protocol: sodep
Interfaces: AmarokInterface
Redirects:
	EventManager => EventManager
}

init
{
	getLocalLocation@Runtime()( myLocalLocation );
	setEventManagerLocation@Poller( EventManager.location );
	setAmarokServiceLocation@Poller( myLocalLocation );
	undef( myLocalLocation );
	start@Poller()
}

main
{
	[ play() ] {
// EVENT HERE!
		exec@Exec( DCOPPrefix + "player play" )()
	}
	
	[ pause() ] {
		exec@Exec( DCOPPrefix + "player pause" )()
	}

	[ previous() ] {
		exec@Exec( DCOPPrefix + "player prev" )()
	}

	[ next() ] {
		exec@Exec( DCOPPrefix + "player next" )()
	}

	[ getVolume()( response ) {
		exec@Exec( DCOPPrefix + "player getVolume" )( response );
		trim@StringUtils( response )( response )
	} ] { nullProcess }

	[ setVolume( request ) ] {
		exec@Exec( DCOPPrefix + "player setVolume " + request )()
	}

	[ getNowPlaying( request )( response ) {
		exec@Exec( DCOPPrefix + "player isPlaying" )( isPlaying );
		trim@StringUtils( isPlaying )( isPlaying );
		if ( isPlaying == "false" ) {
			response = "Paused"
		} else {
			exec@Exec( DCOPPrefix + "player nowPlaying" )( response );
			trim@StringUtils( response )( response )
		}
	} ] { nullProcess }

	[ getPlaylist( request )( response ) {
		exec@Exec( DCOPPrefix + "playlist filenames" )( s );
		s.regex = "\n";
		split@StringUtils( s )( ss );
		for( i = 0, i < #ss.result, i++ ) {
			response.song[i] = ss.result[i]
		}
	} ] { nullProcess }

	[ getLyrics( request )( response ) {
		exec@Exec( DCOPPrefix + "player lyrics" )( response )
	} ] { nullProcess }

	[ playByIndex( request ) ] {
		exec@Exec( DCOPPrefix + "playlist playByIndex " + request.index )()
	}
}

