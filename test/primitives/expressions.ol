/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

include "../AbstractTestUnit.iol"

define testBooleans
{
	x = true;
	y = false;
	z = x * y; // z == false
	if ( z ) {
		throw( TestFailed )
	};
	
	z = false + true; // z == true
	w = !z * true; // w == false
	if ( (z && !w) == false ) {
		throw( TestFailed )
	}
}

define testCasts
{
	if ( 1 != int(true) ) {
		throw( TestFailed, "casting error" )
	};
	if ( 1L != long(true) ) {
		throw( TestFailed, "casting error" )
	};
	if ( 1.0 != double(true) ) {
		throw( TestFailed, "casting error" )
	};
	if ( "true" != string(true) ) {
		throw( TestFailed, "casting error" )
	};
	if ( !bool(1) ) {
		throw( TestFailed, "casting error" )
	};
	if ( !bool(1L) ) {
		throw( TestFailed, "casting error" )
	};
	if ( !bool(1.0) ) {
		throw( TestFailed, "casting error" )
	};
	if ( !bool("true") ) {
		throw( TestFailed, "casting error" )
	};
	if ( true != bool(string(bool(int(long(double(long(int(bool(string(bool(true))))))))))) ) {
		throw( TestFailed, "casting error" )
	}
}

define testAIsDouble{
	if (!is_double(a) ) {
		throw( TestFailed, "hierarchy error, " + a + " is not double " )
	}
}

define testAIsLong{
	if (!is_long(a) ) {
		throw( TestFailed, "hierarchy error, " + a + " is not long " )
	}
}

define testAIsInt{
	if (!is_int(a) ) {
		throw( TestFailed, "hierarchy error, " + a + " is not int " )
	}
}

define testAIsString{
	if (!is_string(a) ) {
		throw( TestFailed, "hierarchy error, " + a + " is not string " )
	}
}

define testTypeImplicitConversion{
	// bool < int < long < double < string

	// string + double
	a = "A" + 1000.0
	testAIsString
	// double + string
	a = 1000.0 + "A"
	testAIsString
	// string + long
	a = "A" + 1000L
	testAIsString
	// long + string
	a = 1000L + "A"
	testAIsString
	// string + int
	a = "A" + 1000
	testAIsString
	// int + string
	a = 1000 + "A"
	testAIsString
	// string + bool
	a = "A" + true
	testAIsString
	// bool + string
	a = true + "A"
	testAIsString

	// double _ double
	a = 0.1 + 1000.0
	testAIsDouble

	a = 0.1 - 1000.0
	testAIsDouble

	a = 0.1 * 1000.0
	testAIsDouble

	a = 0.1 / 1000.0
	testAIsDouble

	// double _ long
	a = 0.1 + 1000L
	testAIsDouble

	a = 0.1 - 1000L
	testAIsDouble

	a = 0.1 * 1000L
	testAIsDouble

	a = 0.1 / 1000L
	testAIsDouble

	// long _ double
	a = 1000L + 0.1 
	testAIsDouble

	a = 1000L - 0.1 
	testAIsDouble

	a = 1000L * 0.1 
	testAIsDouble

	a = 1000L / 0.1 
	testAIsDouble
	
	// double _ int
	a = 0.1 + 1000
	testAIsDouble

	a = 0.1 - 1000
	testAIsDouble

	a = 0.1 * 1000
	testAIsDouble

	a = 0.1 / 1000
	testAIsDouble

	// int _ double 
	a = 1000 + 0.1 
	testAIsDouble

	a = 1000 - 0.1 
	testAIsDouble

	a = 1000 * 0.1 
	testAIsDouble

	a = 1000 / 0.1 
	testAIsDouble


	// long _ long
	a = 1000L + 1L
	testAIsLong

	a = 1000L - 1L
	testAIsLong

	a = 1000L * 1L
	testAIsLong

	a = 1000L / 1L
	testAIsLong

	
	// int _ long 
	a = 1 + 1000L
	testAIsLong

	a = 1 - 1000L
	testAIsLong

	a = 1 * 1000L
	testAIsLong

	a = 1 / 1000L
	testAIsLong

	// long _ int
	a = 1000L + 2
	testAIsLong

	a = 1000L - 2
	testAIsLong

	a = 1000L * 2
	testAIsLong

	a = 1000L / 2
	testAIsLong


	// int _ int
	a = 1000 + 2
	testAIsInt

	a = 1000 - 2
	testAIsInt

	a = 1000 * 2
	testAIsInt

	a = 1000 / 2
	testAIsInt
}

define doTest
{
	if ( "Hello, " + "World!" != "Hello, World!" ) {
		throw( TestFailed, "string concatenation does not match correct result" )
	} else if ( 17 != 2 + 3 * 5 ) {
		throw( TestFailed, "arithmetic expression does not match correct result" )
	};

	x = 1;
	x += 8; // x = 9
	x -= 7; // x = 2
	x *= 4; // x = 8
	x /= 2; // x = 4
	if ( x != 4 ) {
		throw( TestFailed, "compact inline arithmetic operators do not work correctly" )
	};

	testBooleans;
	testCasts;
	testTypeImplicitConversion
}

