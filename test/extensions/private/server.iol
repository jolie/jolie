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

constants {
	Location_SODEPServer = "socket://localhost:10101",
	Location_SODEPSServer = "socket://localhost:10102",
	Location_SOAPServer = "socket://localhost:10103",
	Location_JSONRPCServer = "socket://localhost:10104",
	Location_HTTPServer = "socket://localhost:10105",
	Location_HTTPSServer = "socket://localhost:10106",

	KeystorePassword = "superjolie"
}

type Person:void {
	.id:long
	.firstName:string
	.lastName:string
	.age:int
	.size:double
	.male:bool
	.unknown:any
	.unknown2:undefined
	.array*:any
	.object:void {
		.data:any
	}
}

interface ServerInterface {
OneWay:
	shutdown(void),
	consume2(any)
RequestResponse:
	echoPerson(Person)(Person),
	identity(any)(any),
	consume(any)(void)
}
