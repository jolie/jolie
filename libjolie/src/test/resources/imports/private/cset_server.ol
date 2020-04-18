/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

include "cset_server.iol"

execution { concurrent }

cset {
	firstName: StartMessage.person.firstName Person.firstName,
	lastName: StartMessage.person.lastName Person.lastName
}

cset {
	otherSid: PreEnd.otherSid
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
	startSession( request )( response ) {
		synchronized( Lock ) {
			response.sid = global.sid++;
			csets.otherSid = new;
			response.otherSid = csets.otherSid;
			response.person << request.person
		};
		Client.location = request.clientLocation
	};
	endSession( person );
	preEndSession( r );
	event.person -> person;
	event.sid -> response.sid;
	onSessionEnd@Client( event )
}
