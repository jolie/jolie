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

type RoundRequestType:double {
	.decimals?:int
}

type PowRequest:void {
	.base:double
	.exponent:double
}

type SummationRequest:void {
	.from:int
	.to:int
}

interface MathInterface {
RequestResponse:
	/**! Returns the absolute value of the input integer. */
	abs(int)(int),

	/**! Returns a random number d such that 0.0 <= d < 1.0. */
	random(void)(double),

	
	round(RoundRequestType)(double),

	/**! Returns the result of .base to the power of .exponent (see request data type). */
	pow(PowRequest)(double),

	/**! Returns the summation of values from .from to .to (see request data type). For example, .from=2 and .to=5 would produce a return value of 2+3+4+5=14. */
	summation(SummationRequest)(int)
}

outputPort Math {
Interfaces: MathInterface
}

embedded {
Java:
	"joliex.util.MathService" in Math
}
