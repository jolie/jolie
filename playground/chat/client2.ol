/*
   Copyright 2015 Fabrizio Montesi <famontesi@gmail.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

include "chat.iol"
include "console.iol"

outputPort Chat {
Location: "socket://localhost:8000/"
Protocol: sodep
Interfaces: ChatInterface
}

inputPort MyInput {
Location: "socket://localhost:8002/"
Protocol: sodep
Interfaces: ClientInterface
}

main
{
	if ( args[0] == "open" ) {
		openRoom@Chat( { .username = args[1], .roomName = args[2] } )()
	} else if ( args[0] == "close" ) {
		closeRoom@Chat( { .username = args[1], .roomName = args[2] } )()
	} else if ( args[0] == "pub" ) {
		publish@Chat( { .username = args[1], .roomName = args[2], .message = args[3] } )()
	} else if ( args[0] == "hist" ) {
		getHistory@Chat( { .roomName = args[1] } )( history );
		println@Console( history )()
	} else if ( args[0] == "enter" ) {
		enterRoom@Chat( {
			.roomName = args[1],
			.location = global.inputPorts.MyInput.location
		} )();
		provide
			[ update( upd ) ] {
				m = "[" + upd.roomName + "] "
					+ upd.username + ": "
					+ upd.message;
				println@Console( m )()
			}
		until
			[ roomClosed() ]
	}
}
