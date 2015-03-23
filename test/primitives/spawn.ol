/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

define doTest
{
	for( i = 0, i < 10, i++ ) {
		vec[i] = 1 - i
	};

	spawn( i over #vec ) in result {
		abs@Math( vec[ i ] )( result )
	};

	if ( #result != 10 ) {
		throw( TestFailed, "result vector (size: " + #result + ") does not have expected size (" + #vec + ")" )
	};

	for( i = 0, i < #result, i++ ) {
		abs@Math( vec[ i ] )( a );
		if ( result[i] != a ) {
			throw( TestFailed, "result vector does not have expected result at position " + i )
		}
	}
}

