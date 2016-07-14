/***************************************************************************
 *   Copyright (C) 2016 by Martin Wolf <mawo@martinwolf.eu>                *
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
include "console.iol"
define simpleReThrowTest
{
  scope( s1 ) {
    install ( MyFault => x = 1 );
  	scope( s2 ) {
  		install( MyFault => rethrow );
  		throw( MyFault )
  	};
    x = 5
  };
	if ( x != 1 ) {
		throw( TestFailed, "an installed fault handler was not executed" )
	}
}

define doTest
{
	simpleReThrowTest
}
