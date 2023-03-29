/***************************************************************************
 *   Copyright (C) 2022 by Mathias Christensen <mathi.christensen@gmail.com>*
 *                                                                          *
 *   This program is free software; you can redistribute it and/or modify   *
 *   it under the terms of the GNU Library General Public License as        *
 *   published by the Free Software Foundation; either version 2 of the     *
 *   License, or (at your option) any later version.                        *
 *                                                                          *
 *   This program is distributed in the hope that it will be useful,        *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *   GNU General Public License for more details.                           *
 *                                                                          *
 *   You should have received a copy of the GNU Library General Public      *
 *   License along with this program; if not, write to the                  *
 *   Free Software Foundation, Inc.,                                        *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.              *
 *                                                                          *
 *   For details about the authors of this software, see the AUTHORS file.  *
 ***************************************************************************/

from ..test-unit import TestUnitInterface
from math import Math


service Main {

    embed Math as Math

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	main {
		test()() {
            request << {
                from = 1,
                to = 1,
            }

            sum = summation@Math( request )

            if( sum != 1 ) {
                throw(TestFailed, "return value does not match expected value")
            }

            request.to = 2
            sum = abs@Math( summation@Math( request ) )
            if( sum != 3 ) {
                throw(TestFailed, "doesn't work when nested")
            }

            request.to = 1
            sum = summation@Math( request ) * 1 + ( summation@Math( request ) + 1 ) * 2
            if( sum != 5 ) {
                throw(TestFailed, "doesn't work with multiple terms and factors")
            }
			
		}
	}
}
