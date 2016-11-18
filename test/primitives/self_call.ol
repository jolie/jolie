/***************************************************************************
 *   Copyright (C) 2016 by Fabrizio Montesi <famontesi@gmail.com>          *
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

constants {
	MyLocation = "socket://localhost:8005"
}

interface TestIface {
RequestResponse:
	id(int)(int)
}

outputPort Self {
Location: MyLocation
Protocol: sodep
Interfaces: TestIface
}

inputPort MyInput {
Location: MyLocation
Protocol: sodep
Interfaces: TestIface
}

define doTest
{
	i = k = 0;
	{
		while( i++ < 20 ) {
			id@Self( i )( x );
			if ( x != i ) {
				throw( TestFailed, "Unexpected result" )
			}
		}
		|
		while( k++ < 20 ) {
			id( u )( y ) { y = u }
		}
	}
}
