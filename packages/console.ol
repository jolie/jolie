/*
 *   Copyright (C) 2008-2020 by Fabrizio Montesi <famontesi@gmail.com>     
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

type EnableTimestampRequest: bool {
	format?: string
}

type RegisterForInputRequest {
	enableSessionListener?: bool
}


type ReadLineRequest: void {
	secret?: bool
}

type SubscribeSessionListener {
	token: string
}

type UnsubscribeSessionListener {
	token: string
}

type InRequest:string {
	token?: string
} | void {
	token?: string
}

interface ConsoleIface {
RequestResponse:

	/**!
	*   If `escapeControlChars` subfield is present, special characters in the value will be escaped before being sent to the terminal.
	*/
	print( undefined )( void ), 

	/**!
	*   If `escapeControlChars` subfield is present, special characters in the value will be escaped before being sent to the terminal.
	*/
	println( undefined )( void ), 
	
	/**!
	*	It enables timestamp inline printing for each console output operation call: print, println
	*	Parameter format allows to specifiy the timestamp output format. Bad Format will be printed out if format value is not allowed.
	*/
	enableTimestamp( EnableTimestampRequest )( void ),

	/**!
	* Read a line from the console using a synchronous call
	*/
	readLine( ReadLineRequest )( string ),

	/**!
	*  it enables the console for input listening
	*  parameter enableSessionListener enables console input listening for more than one service session (default=false)
	*/
	registerForInput( RegisterForInputRequest )( void ),

	/**!
	* it receives a token string which identifies a service session.
	* it enables the session to receive inputs from the console
	*/
	subscribeSessionListener( SubscribeSessionListener )( void ),
	
	/**!
	* it disables a session to receive inputs from the console, previously registered with subscribeSessionListener operation
	*/
	unsubscribeSessionListener( UnsubscribeSessionListener )( void )
}

interface ConsoleInputInterface {
OneWay:
	in( InRequest )
}

service Console {
	// outputPort ConsoleInputPort {
	// 	location: "local"
	// 	interfaces: ConsoleInputInterface
	// }

	inputPort ConsoleInput {
		location: "local"
		interfaces: ConsoleIface
	}

	foreign java {
		class: "joliex.io.ConsoleService"
	}
}