include "console.iol"
include "file.iol"
include "string_utils.iol"

include "config.iol"

constants {
	Location_EventClientService = "socket://192.168.1.20:10111"
}

cset {
	Amarok.location
}

execution { concurrent }

inputPort __HttpPort {
RequestResponse:
	default
}

inputPort EventClientPort {
OneWay:
	receiveEvent
}

/**
 * Put your custom RR operations here.
 */
inputPort JAmaroidPort {
RequestResponse:
	play, pause,
	previous, next,
	waitForStateChange, getLyrics,
	playByIndex, getState, setVolume
}

outputPort Amarok {
Protocol: sodep
Notification:
	play, pause, previous,
	next, playByIndex, setVolume
SolicitResponse:
	getLyrics, getPlaylist,
	getNowPlaying, getVolume
}

inputPort ClientHandlingPort {
OneWay:
	fireClientUpdate
}

outputPort Myself {
Location: Location_EventClientService
Protocol: sodep
Notification:
	fireClientUpdate
}

outputPort EventManager {
Protocol: sodep
Notification:
	register, unregister, registerForAll
}

service EventClientService {
Protocol: sodep
Location: Location_EventClientService
Ports: EventClientPort, ClientHandlingPort
}

/**
 * You can add more custom input ports by adding them here.
 */
service HttpService {
Protocol: http {
	.format = "html"; .default = "default";
	.debug = DebugHttp; .debug.showContent = DebugHttpContent;
	.keepAlive = 0
}
Location: Location_HttpService
Ports: __HttpPort, JAmaroidPort
}

include "checkFileExtension.iol"

define buildState
{
	getNowPlaying@Amarok()( tmp.nowPlaying );
	getVolume@Amarok()( tmp.volume );
	getPlaylist@Amarok()( tmp.playlist );
	synchronized( lock ) {
		undef( state );
		state[0] << tmp[0]
	}
}

init
{
	file.format = "text";
	Amarok.location -> request.location;

	logicalClock -> global.logicalClockMap.(Amarok.location);
	state -> global.stateMap.(Amarok.location);
	eventRegistrationMap -> global.eventRegistrationMap;
	waiters -> global.waitersMap.(Amarok.location)
}

main
{
	// Do _not_ modify the behaviour of the default operation.
	[ default( request )( response ) {
		scope( s ) {
			install( FileNotFound => println@Console( "File not found: " + file.filename ) );
			file.filename = DocumentRootDirectory + request.operation;
			checkFileExtension;
			readFile@File( file )( response );
			if ( is_defined( type ) ) {
				response.("@ContentType") = type
			};
			if ( is_defined( encoding ) ) {
				response.("@ContentTransferEncoding") = encoding
			};
			if ( is_defined( format ) ) {
				response.("@Format") = format
			}
		}
	} ] { nullProcess }

	// Add your custom code in the main non deterministic choice.
	[ play( request )() {
		play@Amarok()
	} ] { nullProcess }
	
	[ pause( request )() {
		pause@Amarok()
	} ] { nullProcess }

	[ previous( request )() {
		previous@Amarok()
	} ] { nullProcess }

	[ next( request )() {
		next@Amarok()
	} ] { nullProcess }

	[ setVolume( request )() {
		setVolume@Amarok( request )
	} ] { nullProcess }

	[ getLyrics( request )( response ) {
		getLyrics@Amarok()( response )
	} ] { nullProcess }

	[ playByIndex( request )() {
		playByIndex@Amarok( request )
	} ] { nullProcess }

	[ waitForStateChange( request )( response ) {
		synchronized( lock ) {
			//undef( response );
			if ( logicalClock > request.logicalClock ) {
				response[0] << state[0];
				response.logicalClock = logicalClock
			}
		};
		if ( !is_defined( response ) ) {
			synchronized( lock ) {
				waiters++
			};
			fireClientUpdate( Amarok.location );
			synchronized( lock ) {
				waiters--;
				response[0] << state[0];
				response.logicalClock = logicalClock
			}
		}
	} ] { nullProcess }

	[ getState( request )( response ) {
		// Register us to get events if not already done before.
		if ( !is_defined( eventRegistrationMap.(Amarok.location) ) ) {
			EventManager.location = Amarok.location + "/EventManager";
			eventRegistrationMap.(Amarok.location) = 1;
			eventRegistration.location = Location_EventClientService;
			eventRegistration.callbackData = Amarok.location;
			registerForAll@EventManager( eventRegistration );
			buildState
		};
		synchronized( lock ) {
			response[0] << state[0];
			response.logicalClock = logicalClock
		}
	} ] { nullProcess }

	[ receiveEvent( event ) ] {
		Amarok.location = event.callbackData;
		synchronized( lock ) {
			logicalClock++;
			foreach( change : event.data ) {
				undef( state.(change) );
				state.(change) << event.data.(change)
			};
			w = waiters
		};
		for( i = 0, i < w, i++ ) {
			fireClientUpdate@Myself( Amarok.location )
		}
	}
}
