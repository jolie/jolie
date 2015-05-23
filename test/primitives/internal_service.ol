/***************************************************************************
 *   Copyright (C) 2015 by Martin Wolf                                     *
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
include "math.iol"

interface SrvIface {
RequestResponse:
	add(int)(int),
	set(int)(void)
}

interface CellIface {
RequestResponse:
	read(void)(int),
	writeAbs(int)(void)
}

interface RecursionInterface {
  RequestResponse: myOperation(int)(int) 
}

service Cell {
	Interfaces: CellIface
	init {
		global.value = 0
	}
	main {
		[ read()( global.value ) ]
		[ writeAbs( val )() {
			abs@Math( val )( global.value )
		} ]
	}
}

service Srv {
	Interfaces: SrvIface
	main {
		[ set( x )() {
			writeAbs@Cell( x )()
		} ]
		[ add( x )( y ) {
			read@Cell()( z );
			y = z + x;
			writeAbs@Cell( y )()
		} ]
	}
}

service RecursionService {
  Interfaces: RecursionInterface
  main 
  {
    [ myOperation ( req )( resp ){
      resp = req + 1;
      if ( req < 5 ) {
        myOperation@RecursionService ( resp )( resp )
      }
    } ]
  }
}

define doTest
{
	set@Srv( -42 )();
	add@Srv( 0 )( x );
	if ( x != 42 ) {
		throw( TestFailed, "Unexpected result" )
	};
	set@Srv( -10 )();
	add@Srv( 10 )( x );
	if ( x != 20 ) {
		throw( TestFailed, "Unexpected result" )
	};

	//test if recursion works
	myOperation@RecursionService( 0 )( x );
	if ( x != 6 ) {
		throw( TestFailed, "Unexpected result" )
	}
	
}

