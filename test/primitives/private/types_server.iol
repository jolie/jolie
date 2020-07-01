/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

type CoListNode:ListNode

// A list, stupidly and purposefully implemented as a co-recursive type
type ListNode:int {
	next?:CoListNode
}

// Test for the type equality check
type ListNode:int {
	next?:CoListNode
}

type ChoiceRequest: ChoiceLeft | ChoiceRight | ChoiceThird

type ChoiceLeft { left:int } // defaults to void native type
type ChoiceRight:void { right:string }
type ChoiceThird: void {
	.third:string
}

type SomeTrickyType {
	x {
		x1
		x2:int {
			x21? {
				x211
			}
		}
	}
}

type ChoiceResponse: int | string

type ConstrainedStringType: string( length( [0,4] ) ) {
	f1: string( length( [0,10] ) )
	f2: string( enum(["hello","homer","simpsons"]))
	f3: string( length( [0,20] ) )
	f4?: int( ranges( [1,4], [10,20], [100,200], [300, *]) )
	f5?: long( ranges( [3L,4L], [10L,20L], [100L,200L], [300L, *]) )
	f6?: double( ranges( [4.0,5.0], [10.0,20.0], [100.0,200.0], [300.0, *]) )
	f7?: string( regex(".*@.*\\..*") )
}

interface ServerInterface {
RequestResponse:
	call(ListNode)(int),
	choice(ChoiceRequest)(ChoiceResponse),
	constrainedString(ConstrainedStringType)( void )
OneWay:
	shutdown(void)
}
