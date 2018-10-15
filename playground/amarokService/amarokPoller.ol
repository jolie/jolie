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
include "time.iol"

constants {
	PollingInterval = 1000
}

outputPort EventManager {
OneWay:
	fireEvent
}

outputPort Amarok {
Protocol: sodep
RequestResponse:
	getPlaylist,
	getNowPlaying, getVolume
}

inputPort PollerPort {
Location: "local"
OneWay:
	setEventManagerLocation,
	start,
	setAmarokServiceLocation
}

define checkForStateChanges
{
	undef( changes );
	if ( state.nowPlaying != oldState.nowPlaying ) {
		changes.nowPlaying -> state.nowPlaying
	};
	if ( state.volume != oldState.volume ) {
		changes.volume -> state.volume
	};
	b = 0;
	if ( #state.playlist.song != #oldState.playlist.song ) {
		b = 1
	} else {
		for( i = 0, i < #state.playlist.song && b == 0, i++ ) {
			if ( state.playlist.song[i] != oldState.playlist.song[i] ) {
				b = 1
			}
		}
	};
	if ( b == 1 ) {
		changes.playlist -> state.playlist
	}
}

define buildState
{
	//undef( state );
	getNowPlaying@Amarok()( state.nowPlaying );
	getVolume@Amarok()( state.volume );
	getPlaylist@Amarok()( state.playlist )
}

main
{
	{ setEventManagerLocation( EventManager.location ) | setAmarokServiceLocation( Amarok.location ) };
	start();

	buildState;
	oldState << state;

	while( 1 ) {
		sleep@Time( PollingInterval );
		undef( event );
		buildState;
		checkForStateChanges;
		undef( oldState );
		oldState << state;
		foreach( change : changes ) {
			event = change;
			event.data.(change) << changes.(change);
			fireEvent@EventManager( event )
		}
	}
}

