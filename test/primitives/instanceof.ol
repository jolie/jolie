/***************************************************************************
 *   Copyright (C) 2012-2021 by Fabrizio Montesi <famontesi@gmail.com>     *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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
include "security_utils.iol"

define doTest {
	x = undefined;
	if ( !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with undefined variables" )
	};
	if ( !(undefined instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with undefined constants" )
	};
	if ( !(x instanceof any) || !(x instanceof void) ) {
		throw( TestFailed, "instanceof does not work with undefined variables" )
	};

	x = void;
	if ( !(x instanceof void) ) {
		throw( TestFailed, "instanceof does not work with void variables" )
	};
	if ( !(void instanceof void) ) {
		throw( TestFailed, "instanceof does not work with void constants" )
	};
	if ( !(x instanceof undefined) || !(x instanceof any) ) {
		throw( TestFailed, "instanceof does not work with void variables" )
	};

	x = any;
	if ( !(x instanceof any) ) {
		throw( TestFailed, "instanceof does not work with any variables" )
	};
	if ( !(any instanceof any) ) {
		throw( TestFailed, "instanceof does not work with any constants" )
	};
	if ( !(x instanceof undefined) || !(x instanceof void) ) {
		throw( TestFailed, "instanceof does not work with any variables" )
	};

	x = false;
	if ( !(x instanceof bool) ) {
		throw( TestFailed, "instanceof does not work with bool variables" )
	};
	if ( !(false instanceof bool) ) {
		throw( TestFailed, "instanceof does not work with bool constants" )
	};
	x = bool( 0 );
	if ( !(x instanceof bool) ) {
		throw( TestFailed, "instanceof does not work with bool variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with bool variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with bool variables" )
	};

	x = 42;
	if ( !(x instanceof int) ) {
		throw( TestFailed, "instanceof does not work with int variables" )
	};
	if ( !(42 instanceof int) ) {
		throw( TestFailed, "instanceof does not work with int constants" )
	};
	x = int( 42.0 );
	if ( !(x instanceof int) ) {
		throw( TestFailed, "instanceof does not work with int variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with int variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with int variables" )
	};

	x = 42L;
	if ( !(x instanceof long) ) {
		throw( TestFailed, "instanceof does not work with long variables" )
	};
	if ( !(42L instanceof long) ) {
		throw( TestFailed, "instanceof does not work with long constants" )
	};
	x = 42l;
	if ( !(x instanceof long) ) {
		throw( TestFailed, "instanceof does not work with long variables" )
	};
	if ( !(42l instanceof long) ) {
		throw( TestFailed, "instanceof does not work with long constants" )
	};
	x = long( true );
	if ( !(x instanceof long) ) {
		throw( TestFailed, "instanceof does not work with long variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with long variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with long variables" )
	};

	x = 42.0;
	if ( !(x instanceof double) ) {
		throw( TestFailed, "instanceof does not work with double variables" )
	};
	if ( !(42.0 instanceof double) ) {
		throw( TestFailed, "instanceof does not work with double constants" )
	};
	x = double( "42.0" );
	if ( !(x instanceof double) ) {
		throw( TestFailed, "instanceof does not work with double variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with double variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with double variables" )
	};

	x = "my string";
	if ( !(x instanceof string) ) {
		throw( TestFailed, "instanceof does not work with string variables" )
	};
	if ( !("my string" instanceof string) ) {
		throw( TestFailed, "instanceof does not work with constant strings" )
	};
	x = string( 42 );
	if ( !(x instanceof string) ) {
		throw( TestFailed, "instanceof does not work with string variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with string variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with string variables" )
	};

	req.size = 0;
	secureRandom@SecurityUtils(req)(x);
	if ( !(x instanceof raw) ) {
		throw( TestFailed, "instanceof does not work with raw variables" )
	};
	if ( !(x instanceof any) || !(x instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with raw variables" )
	};
	if ( x instanceof void ) {
		throw( TestFailed, "instanceof does not work with raw variables" )
	};

	person.name = "John";
	person.age = 21;
	person.address.street = "My street";
	person.address.number = 23;
	if ( !(person instanceof Person) ) {
		throw( TestFailed, "instanceof does not work with structured data types" )
	};
	if ( !(person instanceof undefined) ) {
		throw( TestFailed, "instanceof does not work with structured data types" )
	};
	if ( person instanceof void || person instanceof any ) {
		throw( TestFailed, "instanceof does not work with structured data types" )
	}

	if( !("Hi" { name = "Homer" age = 42 } instanceof string { name: string age: int }) )
		throw( TestFailed, "inline tree does not have the expected inline type" )
}
