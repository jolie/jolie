/***************************************************************************
 *   Copyright (C) 2020 by Claudio Guidi <cguidi@italianasoftware.com>     *
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
include "runtime.iol"
include "time.iol"

interface AggregationInterfaceTest {
    RequestResponse: a, b
}

outputPort AggregationA {
    Interfaces: AggregationInterfaceTest
}

define doTest
{   
    emb.type = "Jolie"
    emb.filepath = "private/aggregation_a.ol"
    loadEmbeddedService@Runtime( emb )( AggregationA.location )

    sleep@Time( 2000 )()


    a@AggregationA()( result )
    b@AggregationA()()
    if ( !result ) {
        throw( TestFailed, "Something went wrong with aggregation")
    }
}