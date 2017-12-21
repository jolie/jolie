/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2017                                                    *
 *      by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>               *
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
	Location_SODEPServer = "socket://localhost:10111",
	Location_SODEPSServer = "socket://localhost:10110",
	Location_SOAPServer = "socket://localhost:10109",
	Location_JSONRPCServer = "socket://localhost:10108",
	Location_HTTPServer = "socket://localhost:10107",
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
	shutdown(void)
RequestResponse:
	echoPerson(Person)(Person),
	identity(any)(any)
}
