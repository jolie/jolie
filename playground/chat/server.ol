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

execution { concurrent }

inputPort ChatInput {
Location: "socket://localhost:8000/"
Protocol: sodep
Interfaces: ChatInterface
}

outputPort Client {
Protocol: sodep
Interfaces: ClientInterface
}

cset {
roomName:
	OpenRoomRequest.roomName
	CloseRoomRequest.roomName
	PublishRequest.roomName
	GetHistory.roomName
	EnterRoomRequest.roomName
}

define updateClients
{
	for( i = 0, i < #clients, i++ ) {
		Client.location = clients[i].location;
		update@Client( pubReq )
	}
}

define closeClients
{
	for( i = 0, i < #clients, i++ ) {
		Client.location = clients[i].location;
		roomClosed@Client()
	}
}

main
{
	openRoom( openReq )( openResp ) {
		csets.roomName = openReq.roomName;
		println@Console(
			"Created room "
			+ openReq.roomName
			+ " by " + openReq.username
		)()
	};
	history = "";
	provide
		[ publish( pubReq )() {
			m = "[" + pubReq.roomName + "] "
				+ pubReq.username + ": "
				+ pubReq.message;
			println@Console( m )();
			history += m + "\n";
			updateClients
		} ]

		[ getHistory()( history ) {
			nullProcess
		} ]

		[ enterRoom( enterRoomReq )() {
			clients[#clients].location = enterRoomReq.location
		} ]
	until
		[ closeRoom( closeReq )() {
			println@Console(
				"Closed room "
				+ closeReq.roomName
				+ " by " + closeReq.username
			)();
			closeClients
		} ]
}
