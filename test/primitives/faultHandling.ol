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

define terminationTest
{
	i = y = 0;
	while( i++ < 5 ) {
		scope( m ) {
			install( MyFault => comp( s2 ) );
			{
				scope( s1 ) {
					throw( MyFault )
				}
				|
				scope( s2 ) {
					install( this => y++ );
					nullProcess
				}
			}
		}
	};
	if ( y != i - 1 ) {
		throw( TestFailed, "termination/compensation handling in parallel scopes did not work" )
	}
}

define simpleFaultTest
{
	scope( s ) {
		install(
			MyFault => x = 1
		);
		throw( MyFault );
		x = 5
	};
	if ( x != 1 ) {
		throw( TestFailed, "an installed fault handler was not executed" )
	}
}

define doTest
{
	simpleFaultTest;
	terminationTest
}

