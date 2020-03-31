/*
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

include "../AbstractTestUnit.iol"
include "jap:file:private/twice.jap!/twice/twice_api.iol"

outputPort Twice1 { interfaces: TwiceAPI }
outputPort Twice2 { interfaces: TwiceAPI }

embedded {
Jolie:
	// Test jap as library
	// To create this jap, run scripts/mktestjaps from the root directory
	"-l private/twice.jap twice/main.ol" in Twice1,

	// Test jap as package
	"private/twice.jap" in Twice2
}

define doTest
{
	twice@Twice1( 2 )( x )
	if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
	twice@Twice2( 2 )( x )
	if ( x != 4 ) throw( TestFailed, "expected 4, received " + x )
}
