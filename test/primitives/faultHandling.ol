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

include "../AbstractTestUnit.iol"

include "private/fault_handling_main.iol"

outputPort FaultHandlingMain {
Interfaces: FaultHandlingMainIface
}

embedded {
Jolie: "private/fault_handling_main.ol" in FaultHandlingMain
}

define terminationTest
{
	i = y = 0
	while( i++ < 5 ) {
		scope( m ) {
			install( MyFault => comp( s2 ) )
			{
				scope( s1 ) {
					throw( MyFault )
				}
				|
				scope( s2 ) {
					install( this => y++ )
					nullProcess
				}
			}
		}
	}
	if ( y != i - 1 ) {
		throw( TestFailed, "termination/compensation handling in parallel scopes did not work" )
	}
}

define simpleFaultTest
{
	scope( s ) {
		install(
			MyFault => x = 1
		)
		throw( MyFault )
		x = 5
	}
	if ( x != 1 ) {
		throw( TestFailed, "an installed fault handler was not executed" )
	}
}

define runtimeExceptionTest {
	scope( ae ) {
		install( ArithmeticException => z = 1 )
		z = 1 / 0
	}
	if ( z != 1 ){
		throw( TestFailed, "ArithmeticException not caught correctly" )
	}
	scope( aae1 ){
		install( AliasAccessException => z = 2 )
		t -> a
		t -> a.b[ 0 ]
		c = t
	}
	if ( z != 2 ){
		throw( TestFailed, "AliasAccessException not caught correctly" )
	}
	scope( aae2 ){
		install( AliasAccessException => z = 3 )
		t -> t
		d = t
	}
	if ( z != 3 ){
		throw( TestFailed, "AliasAccessException not caught correctly" )
	}
}

define doTest
{
	simpleFaultTest
	terminationTest
	runtimeExceptionTest
	reply@FaultHandlingMain(42)(x)
	if ( x != 42 ) {
		throw( TestFailed, "Fault handling main error" )
	}
}

