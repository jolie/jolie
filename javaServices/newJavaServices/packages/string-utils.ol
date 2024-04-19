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

///@GenerateBuilder(false)
type EndsWithRequest: string {
	suffix: string
}

///@GenerateBuilder(false)
type ReplaceRequest:string {
	regex:string
	replacement:string
}

///@GenerateBuilder(false)
type JoinRequest:void {
	piece[0,*]:string
	delimiter:string
}

///@GenerateBuilder(false)
type SplitByLengthRequest:string {
	length:int
}

///@GenerateBuilder(false)
type SplitResult:void {
	result[0,*]:string
}

///@GenerateBuilder(false)
type SplitRequest:string {
	limit?:int
	regex:string
}

///@GenerateBuilder(false)
type PadRequest:string {
	length:int
	char:string //<@JavaName("chars")
}

///@GenerateBuilder(false)
type MatchRequest:string {
	regex:string
}

type MatchResult:int { // 1 if at least a match was found, 0 otherwise.
	group[0,*]:string
}

///@GenerateBuilder(false)
type StartsWithRequest:string {
	prefix:string
}

///@GenerateBuilder(false)
type SubStringRequest:string {
	begin:int
	end?:int
}

///@GenerateBuilder(false)
type StringItemList:void {
	item*:string
}

///@GenerateBuilder(false)
type IndexOfRequest: string {
	word: string
}

type IndexOfResponse: int

///@GenerateBuilder(false)
type ContainsRequest:string {
	substring:string
}

///@GenerateBuilder(false)
type UrlEncodeRequest: string {
	charset?: string 
}

///@GenerateBuilder(false)
type UrlDecodeRequest: UrlEncodeRequest

///@GenerateBuilder(false)
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