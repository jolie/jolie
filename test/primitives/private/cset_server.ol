include "cset_server.iol"

execution { concurrent }

cset {
	firstName: request.person.firstName person.firstName,
	lastName: request.person.lastName person.lastName
}

inputPort ServerInput {
Location: "local"
Interfaces: ServerInterface
}

outputPort Client {
Interfaces: ClientInterface
}

main
{
	startSession( request )( sid ) {
		synchronized( Lock ) {
			sid = global.sid++
		};
		Client.location = request.clientLocation
	};
	endSession( person );
	event.person -> person;
	event.sid -> sid;
	onSessionEnd@Client( event )
}
