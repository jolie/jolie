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
include "string_utils.iol"
include "console.iol"

constants {
	ARRAY_DIM = 10
}
init {
	for (i=0, i<ARRAY_DIM, i++) {
		global.array[i] = i
	}

	undef(i)
}

define doTest
{
	i = 0
	for (el in global.array) {
		if (el != i) {
			tmp.array << global.array
			valueToPrettyString@StringUtils(tmp)(s)
			println@Console("Valore di global.array: "+s)()
			throw( TestFailed, "global.array[" + i + "] = " + el + "instead of " + i )
		}
		i++
	}
}

