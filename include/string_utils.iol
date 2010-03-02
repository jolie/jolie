/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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



type ReplaceAllRequest:string {
	.regex:string
	.replacement:string
}

type JoinRequest:void {
	.piece[0,*]:string
	.delimiter:string
}

type SplitByLengthRequest:string {
	.length:int
}

type SplitResult:void {
	.result[0,*]:string
}

type SplitRequest:string {
	.limit?:int
	.regex:string
}

type PadRequest:string {
	.length:int
	.char:string
}

type MatchRequest:string {
	.regex:string
}

type MatchResult:int { // 1 if at least a match was found, 0 otherwise.
	.group[0,*]:string
}

type StartsWithRequest:string {
	.prefix:string
}

interface StringUtilsInterface {
RequestResponse:
	join(JoinRequest)(string),
	leftPad(PadRequest)(string),
	rightPad(PadRequest)(string),
	length(string)(int),
	match(MatchRequest)(MatchResult),
	replaceAll(ReplaceAllRequest)(string), 
	split(SplitRequest)(SplitResult),
	splitByLength(SplitByLengthRequest)(SplitResult),
	trim(string)(string),
	startsWith(StartsWithRequest)(int),
	valueToPrettyString(undefined)(string)
}

outputPort StringUtils {
Interfaces: StringUtilsInterface
}

embedded {
Java:
	"joliex.util.StringUtils" in StringUtils
}
