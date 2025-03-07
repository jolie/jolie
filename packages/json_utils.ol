/*
 *   Copyright (C) 2013 by Claudio Guidi                                  
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                      
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

type GetJsonStringRequest: undefined
type GetJsonStringResponse: string

type GetJsonValueRequest: any {
	.strictEncoding?: bool
	.charset?:string // set the encoding. Default: system (eg. for Unix-like OS UTF-8) or header specification
}
type GetJsonValueResponse: undefined

type ValidateJsonRequest {
	json: string 
	schema: string 
}

type ValidateJsonResponse {
	validationMessage* {
		message: string 
		code: string 
		type: string 
		error: string
	}
}

interface JsonUtilsInterface {
RequestResponse:
	/**!
	 * Returns the value converted into a JSON string
	 *
	 * Each child value corresponds to an attribute, the base values are saved as the default values (attribute "$" or singular value), the "_" helper childs disappear (e.g. a._[i]._[j] -> a[i][j]), the rest gets converted recursively
	 */
	getJsonString( GetJsonStringRequest )( GetJsonStringResponse )
	      throws JSONCreationError,

	/**!
	 * Returns the JSON string converted into a value
	 *
	 * Each attribute corresponds to a child value, the default values (attribute "$" or singular value) are saved as the base values, nested arrays get mapped with the "_" helper childs (e.g. a[i][j] -> a._[i]._[j]), the rest gets converted recursively
	 */
	getJsonValue( GetJsonValueRequest )( GetJsonValueResponse )
	      throws JSONCreationError,


	/** 
	* Validates a json against a jsonSchema 
	**/
	validateJson( ValidateJsonRequest )( ValidateJsonResponse )
}

service JsonUtils {
    inputPort ip {
        location:"local"
        interfaces: JsonUtilsInterface
    }

    foreign java {
        class: "joliex.util.JsonUtilsService"
    }
}