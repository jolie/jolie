include "console.iol"
include "exec.iol"
include "string_utils.iol"
include "time.iol"

constants {
	PollingInterval = 1000
}

outputPort EventManager {
Notification:
	fireEvent
}

outputPort Amarok {
Protocol: sodep
SolicitResponse:
	getPlaylist,
	getNowPlaying, getVolume
}

inputPort PollerPort {
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

