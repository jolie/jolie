include "../AbstractTestUnit.iol"

include "private/cset_server.iol"
include "runtime.iol"

outputPort Server {
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/cset_server.ol" in Server
}

inputPort ClientInput {
Location: "local"
Interfaces: ClientInterface
}

define doTest
{
	getLocalLocation@Runtime()( r[0].clientLocation );
	r[1].clientLocation = r[0].clientLocation;
	r[2].clientLocation = r[0].clientLocation;
	{
		r[0].person.firstName = "John"; r[0].person.lastName = "Smith";
		startSession@Server( r[0] )( sid[0] );
		endSession@Server( r[0].person )
		|
		r[1].person.firstName = "Donald"; r[1].person.lastName = "Duck";
		startSession@Server( r[1] )( sid[1] );
		endSession@Server( r[1].person )
		|
		r[2].person.firstName = "Duffy"; r[2].person.lastName = "Duck";
		startSession@Server( r[2] )( sid[2] );
		endSession@Server( r[2].person )
	};
	for( i = 0, i < #r, i++ ) {
		onSessionEnd( event );
		for( k = 0, k < #sid, k++ ) {
			if ( event.sid == sid[k] ) {
				if (
					event.person.firstName != r[k].person.firstName
					||
					event.person.lastName != r[k].person.lastName
				) {
					throw( TestFailed )
				}
			}
		}
	}
}

