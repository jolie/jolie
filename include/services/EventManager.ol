execution { sequential }

inputPort EventPort {
OneWay:
	/**
	 * Registers a location for receiving a certain event when it occurs.
	 * @request:void {
	 * 	.event:string the event name.
	 * 	.location:string the location to call back when the event occurs.
	 * 	.callbackData:? some callback data that will be sent back to the registered client.
	 * }
	 */
	register,
	/**
	 * Registers a location for receiving any event that may occur.
	 * @request:void {
	 * 	.location:string the location to call back when the event occurs.
	 * 	.callbackData:? some callback data that will be sent back to the registered client.
	 * }
	 */
	registerForAll,
	/**
	 * Unregisters a location for receiving an event.
	 * @request:void {
	 * 	.event:string the event name.
	 * 	.location:string the location to unregister.
	 * }
	 */
	unregister,
	/**
	 * Fires an event. This will cause every signed service to receive it.
	 * @request:string the event name {
	 * 	.*: additional data that will be sent to the waiting clients.
	 * }
	 */
	fireEvent
}

outputPort Client {
Protocol: sodep
Notification:
	receiveEvent
}

init
{
	Client.location[0] -> p.locations[i]
}

main
{
	[ register( request ) ] {
		global.clientsMap.(request.event).(request.location) = 1;
		global.clientsMap.(request.event).(request.location).callbackData << request.callbackData
	}

	[ registerForAll( request ) ] {
		global.clientsAll.(request.location) = 1;
		global.clientsAll.(request.location).callbackData << request.callbackData
	}

	[ unregister( request ) ] {
		undef( global.clientsMap.(request.event).(request.location) );
		undef( global.clientsAll.(request.location) )
	}

	[ fireEvent( event ) ] {
		foreach( location : global.clientsMap.(request.event) ) {
			Client.location = location;
			undef( event.callbackData );
			event.callbackData << global.clientsMap.(request.event).(location).callbackData;
			receiveEvent@Client( event )
		};
		foreach( location : global.clientsAll ) {
			// Do not send an event twice!
			if ( !is_defined( global.clientsMap.(request.event).(location) ) ) {
				Client.location = location;
				undef( event.callbackData );
				event.callbackData << global.clientsAll.(location).callbackData;
				receiveEvent@Client( event )
			}
		}
	}
}
