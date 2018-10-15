include "console.iol"
include "file.iol"
include "string_utils.iol"

include "config.iol"

constants {
	//Location_EventClientService = "socket://192.168.1.20:10111"
	Location_EventClientService = "socket://localhost:10111"
}

cset {
	Amarok.location, sid
}

execution { concurrent }

interface __HttpInterface {
RequestResponse:
	default
}

interface EventClientInterface {
OneWay:
	receiveEvent
}

/**
 * Put your custom RR operations here.
 */
interface EchoesInterface {
RequestResponse:
	play, pause,
	previous, next,
	waitForStateChange, getLyrics,
	playByIndex, setVolume,
	startClientSession,
	closeClientSession
}

outputPort Amarok {
Protocol: sodep
OneWay:
	play, pause, previous,
	next, playByIndex, setVolume
RequestResponse:
	getLyrics, getPlaylist,
	getNowPlaying, getVolume
}

interface ClientHandlingInterface {
OneWay:
	fireClientUpdate, cancelWaitForEvent
}

outputPort Myself {
Location: Location_EventClientService
Protocol: sodep
OneWay:
	fireClientUpdate, cancelWaitForEvent
}

outputPort EventManager {
Protocol: sodep
OneWay:
	register, unregister, registerForAll
}

inputPort EventClientService {
Protocol: sodep
Location: Location_EventClientService
Interfaces: EventClientInterface, ClientHandlingInterface
}

/**
 * You can add more custom input ports by adding them here.
 */
inputPort HttpService {
Protocol: http {
	.format = "html"; .default = "default";
	.debug = DebugHttp; .debug.showContent = DebugHttpContent;
	.keepAlive = 0
}
Location: Location_HttpService
Interfaces: __HttpInterface, EchoesInterface
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
	waiters -> global.waitersMap.(Amarok.location);
	counter -> global.counter;

	counter = 0
}

main
{
	// Do _not_ modify the behaviour of the default operation.
	[ default( request )( response ) {
		scope( s ) {
			install( FileNotFound => println@Console( "File not found: " + file.filename )() );

			s = request.operation;
			s.regex = "\\?";
			split@StringUtils( s )( ss );
			file.filename = DocumentRootDirectory + ss.result[0];
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
			};
			if ( is_defined( charset ) ) {
				response.("@Charset") = charset
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
			if ( logicalClock > request.logicalClock ) {
				response.state[0] << state[0];
				response.logicalClock = logicalClock
			}
		};
		if ( !is_defined( response ) ) {
			synchronized( lock ) {
				waiters++
			};
			[ fireClientUpdate( Amarok.location ) ] {
				synchronized( lock ) {
					response.state[0] << state[0];
					response.logicalClock = logicalClock
				}
			}
			[ cancelWaitForEvent( sid ) ] { nullProcess }
			;
			synchronized( lock ) {
				waiters--
			}
		}
	} ] { nullProcess }

	[ closeClientSession( request )( response ) {
		if ( --eventRegistrationMap.(Amarok.location) < 1 ) {
			undef( eventRegistrationMap.(Amarok.location) );
			EventManager.location = Amarok.location + "/EventManager";
			eventRegistration.location = Location_EventClientService;
			unregister@EventManager( eventRegistration )
		}
		|
		cancelWaitForEvent@Myself( request.sid )
	} ] { nullProcess }

	[ startClientSession( request )( response ) {
		synchronized( lock ) {
			if ( !is_defined( eventRegistrationMap.(Amarok.location) ) ) {
				register = 1;
				eventRegistrationMap.(Amarok.location) = 1
			} else {
				register = 0;
				++eventRegistrationMap.(Amarok.location)
			};
			response.sid = counter++
		};
		// Register us to get events if not already done before.
		if ( register ) {
			EventManager.location = Amarok.location + "/EventManager";
			eventRegistration.location = Location_EventClientService;
			eventRegistration.callbackData = Amarok.location;
			registerForAll@EventManager( eventRegistration );
			buildState
		};
		synchronized( lock ) {
			response.state[0] << state[0];
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
