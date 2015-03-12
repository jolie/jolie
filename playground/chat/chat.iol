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

type OpenRoomRequest:void {
	.username:string
	.roomName:string
}

type PublishRequest:void {
	.message:string
	.username:string
	.roomName:string
}

type GetHistory:void {
	.roomName:string
}

type EnterRoomRequest:void {
	.location:string
	.roomName:string
}

type CloseRoomRequest:void {
	.username:string
	.roomName:string
}

interface ChatInterface {
RequestResponse:
	openRoom(OpenRoomRequest)(void),
	closeRoom(CloseRoomRequest)(void),
	publish(PublishRequest)(void),
	getHistory(GetHistory)(string),
	enterRoom(EnterRoomRequest)(void)
}

interface ClientInterface {
OneWay:
	update(PublishRequest),
	roomClosed(void)
}
