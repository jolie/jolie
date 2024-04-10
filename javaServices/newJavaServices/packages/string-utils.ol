/*
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>         
 *                                                                        
 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
 */

type EndsWithRequest: string {
	suffix: string
}

type ReplaceRequest:string {
	regex:string
	replacement:string
}

type JoinRequest:void {
	piece[0,*]:string
	delimiter:string
}

type SplitByLengthRequest:string {
	length:int
}

type SplitResult:void {
	result[0,*]:string
}

type SplitRequest:string {
	limit?:int
	regex:string
}

type PadRequest:string {
	length:int
	chars:string
}

type MatchRequest:string {
	regex:string
}

type MatchResult:int { // 1 if at least a match was found, 0 otherwise.
	group[0,*]:string
}

type StartsWithRequest:string {
	prefix:string
}

type SubStringRequest:string {
	begin:int
	end?:int
}

type StringItemList:void {
	item*:string
}

type IndexOfRequest: string {
	word: string
}

type IndexOfResponse: int

type ContainsRequest:string {
	substring:string
}

type UrlEncodeRequest: string {
	charset?: string 
}

type UrlDecodeRequest: UrlEncodeRequest

type FormatRequest: string { ? }
	| void {
		format: string
		locale: string
		data: void { ? }
	}

/**!
 * An interface for supporting string manipulation operations.
 */
interface StringUtilsInterface {
RequestResponse:
	/**!
	  checks if a string ends with a given suffix
	*/
	endsWith( EndsWithRequest )( bool ),

	/**!
	* it returns a random UUID
	*/
	getRandomUUID( void )( string ),

	/**!
	 * Returns true if the string contains .substring
	 */
	contains( ContainsRequest )( bool ),
	indexOf(IndexOfRequest)(IndexOfResponse),
	substring(SubStringRequest)(string),
	join(JoinRequest)(string),
	leftPad(PadRequest)(string),
	rightPad(PadRequest)(string),
	length(string)(int),
	match(MatchRequest)(MatchResult),
	find(MatchRequest)(MatchResult),
	replaceAll(ReplaceRequest)(string),
	replaceFirst(ReplaceRequest)(string),
	sort(StringItemList)(StringItemList),
	split(SplitRequest)(SplitResult),
	splitByLength(SplitByLengthRequest)(SplitResult),
	trim(string)(string),
	toLowerCase(string)(string),
	toUpperCase(string)(string),
	urlEncode( UrlEncodeRequest )( string ),
	urlDecode( UrlDecodeRequest )( string ),

	/**!
	* checks if the passed string starts with a given prefix
	*/
	startsWith(StartsWithRequest)( bool ),
	valueToPrettyString(undefined)(string),

	/**! Formats a string.
	* For example, a request value "Hello {name}" { name = "Homer" } is transformed into "Hello Homer"
	* You can use formatting rules as in Java's MessageFormat, for example, "Up to {pct,number,percent}" { pct = 0.6 } becomes "Up to 60%"
	*/
	fmt( FormatRequest )( string )
}


service StringUtils {
    inputPort ip {
        location:"local"
        interfaces: StringUtilsInterface
    }

    foreign java {
        class: "joliex.util.NewStringUtils"
    }
}