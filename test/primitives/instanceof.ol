/***************************************************************************
 *   Copyright (C) 2012 by Fabrizio Montesi <famontesi@gmail.com>          *
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

type Person:void {
	.name:string
	.age?:int
	.address:void {
		.street:string
		.number:int
	}
}

include "../AbstractTestUnit.iol"

define doTest
{
	if ( !("my string" instanceof string) ) {
		throw( TestFailed, "instanceof does not work with constant strings" )
	};
	
	x = 42;
	if ( !(x instanceof int) ) {
		throw( TestFailed, "instanceof does not work with int variables" )
	};

	person.name = "John";
	person.age = 21;
	person.address.street = "My street";
	person.address.number = 23;

	if ( !(person instanceof Person) ) {
		throw( TestFailed, "instanceof does not work with structured data types" )
	}	
}

