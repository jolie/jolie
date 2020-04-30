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
include "string_utils.iol"
include "console.iol"

define doTest
{
	a.left << "Left" { .x = 1, .y << 2 { left = "y_l" right = "y_r" } };
	a.right << "Right" { .x = "Right_x" };
	b << a;

	if ( a.left != "Left" ) {
		throw( TestFailed, "root expression of inline tree does not match expected content" )
	};

	if ( a.left.y.left != "y_l" ) {
		throw( TestFailed, "inline tree node does not match expected content" )
	};

	if ( b.left.x != 1 ) {
		throw( TestFailed, "composition of inline trees does not match original" )
	};

	valueToPrettyString@StringUtils( b )( s_b );
	valueToPrettyString@StringUtils(
		{
			.left = "Left",
			.left.x = 1,
			.left.y = 2,
			.left.y.left = "y_l",
			.left.y.right = "y_r",

			.right = "Right",
			.right.x = "Right_x"
		}
	)( s_inline );

	if ( s_b != s_inline ) {
		throw( TestFailed, "Inline tree does not match composition of sub-trees" )
	}

	for( i = 0, i < 10, i++ ) {
		z.u[ i ] = i
	}
	w << { u << z.u }

	if ( #w.u != #z.u ) {
		throw( TestFailed, "vector deep copy cardinality of subnodes does not match original" )
	}
	for( i = 0, i < #w.u, i++ ) {
		if ( w.u[ i ] != z.u[i ]) {
			throw( TestFailed, "Element " + i + " of path w.u is different from the corresponding element of z.u" )
		}
	}	
}
