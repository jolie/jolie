/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net.coap.message;

import java.util.HashMap;

/**
Class utility for CoAP Message Codes handling.
@author stefanopiozingaro
 */
public class MessageCode
{

	public static final int EMPTY = 0;
	public static final int GET = 1;
	public static final int POST = 2;
	public static final int PUT = 3;
	public static final int DELETE = 4;
	public static final int CREATED_201 = 65;
	public static final int DELETED_202 = 66;
	public static final int VALID_203 = 67;
	public static final int CHANGED_204 = 68;
	public static final int CONTENT_205 = 69;
	public static final int BAD_REQUEST_400 = 128;
	public static final int UNAUTHORIZED_401 = 129;
	public static final int BAD_OPTION_402 = 130;
	public static final int FORBIDDEN_403 = 131;
	public static final int NOT_FOUND_404 = 132;
	public static final int METHOD_NOT_ALLOWED_405 = 133;
	public static final int NOT_ACCEPTABLE_406 = 134;
	public static final int PRECONDITION_FAILED_412 = 140;
	public static final int REQUEST_ENTITY_TOO_LARGE_413 = 141;
	public static final int UNSUPPORTED_CONTENT_FORMAT_415 = 143;
	public static final int INTERNAL_SERVER_ERROR_500 = 160;
	public static final int NOT_IMPLEMENTED_501 = 161;
	public static final int BAD_GATEWAY_502 = 162;
	public static final int SERVICE_UNAVAILABLE_503 = 163;
	public static final int GATEWAY_TIMEOUT_504 = 164;
	public static final int PROXYING_NOT_SUPPORTED_505 = 165;

	public static final HashMap<Integer, String> MESSAGE_CODES
		= new HashMap<>( 26 );

	static {
		MESSAGE_CODES.put( EMPTY, "EMPTY" );
		MESSAGE_CODES.put( GET, "GET" );
		MESSAGE_CODES.put( POST, "POST" );
		MESSAGE_CODES.put( PUT, "PUT" );
		MESSAGE_CODES.put( DELETE, "DELETE" );
		MESSAGE_CODES.put( CREATED_201, "2.01 Created" );
		MESSAGE_CODES.put( DELETED_202, "2.02 Deleted" );
		MESSAGE_CODES.put( VALID_203, "2.03 Valid" );
		MESSAGE_CODES.put( CHANGED_204, "2.04 Changed" );
		MESSAGE_CODES.put( CONTENT_205, "2.05 Content" );
		MESSAGE_CODES.put( BAD_REQUEST_400, "4.00 Bad Request" );
		MESSAGE_CODES.put( UNAUTHORIZED_401, "4.01 Unauthorized" );
		MESSAGE_CODES.put( BAD_OPTION_402, "4.02 Bad Option" );
		MESSAGE_CODES.put( FORBIDDEN_403, "4.03 Forbidden" );
		MESSAGE_CODES.put( NOT_FOUND_404, "4.04 Not Found" );
		MESSAGE_CODES.put( METHOD_NOT_ALLOWED_405, "4.05 Method Not Allowed" );
		MESSAGE_CODES.put( NOT_ACCEPTABLE_406, "4.06 Not Acceptable" );
		MESSAGE_CODES.put( PRECONDITION_FAILED_412, "4.12 Precondition Failed" );
		MESSAGE_CODES.put( REQUEST_ENTITY_TOO_LARGE_413, "4.13 Request Entity Too Large" );
		MESSAGE_CODES.put( UNSUPPORTED_CONTENT_FORMAT_415, "4.15 Unsupported Content-Format" );
		MESSAGE_CODES.put( INTERNAL_SERVER_ERROR_500, "5.00 Internal Server Error" );
		MESSAGE_CODES.put( NOT_IMPLEMENTED_501, "5.01 Not Implemented" );
		MESSAGE_CODES.put( BAD_GATEWAY_502, "5.02 Bad Gateway" );
		MESSAGE_CODES.put( SERVICE_UNAVAILABLE_503, "5.03 Service Unavailable" );
		MESSAGE_CODES.put( GATEWAY_TIMEOUT_504, "5.04 Gateway Timeout" );
		MESSAGE_CODES.put( PROXYING_NOT_SUPPORTED_505, "5.05 Proxying Not Supported" );
	}

	public static final HashMap<String, Integer> JOLIE_ALLOWED_MESSAGE_CODE
		= new HashMap<>();

	static {
		JOLIE_ALLOWED_MESSAGE_CODE.put( "EMPTY", EMPTY );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "GET", GET );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "POST", POST );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "PUT", PUT );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "DELETE", DELETE );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "2.01 CREATED", CREATED_201 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "201", CREATED_201 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "CREATED", CREATED_201 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "2.02 DELETED", DELETED_202 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "202", DELETED_202 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "DELETED", DELETED_202 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "2.03 VALID", VALID_203 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "203", VALID_203 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "VALID", VALID_203 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "2.04 CHANGED", CHANGED_204 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "204", CHANGED_204 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "CHANGED", CHANGED_204 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "2.05 CONTENT", CONTENT_205 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "205", CONTENT_205 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "CONTENT", CONTENT_205 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.00 BAD REQUEST", BAD_REQUEST_400 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "400", BAD_REQUEST_400 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "BAD REQUEST", BAD_REQUEST_400 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.01 UNAUTHORIZED", UNAUTHORIZED_401 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "401", UNAUTHORIZED_401 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "UNAUTHORIZED", UNAUTHORIZED_401 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.02 BAD OPTION", BAD_OPTION_402 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "402", BAD_OPTION_402 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "BAD OPTION", BAD_OPTION_402 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.03 FORBIDDEN", FORBIDDEN_403 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "403", FORBIDDEN_403 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "FORBIDDEN", FORBIDDEN_403 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.04 NOT FOUND", NOT_FOUND_404 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "404", NOT_FOUND_404 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "NOT FOUND", NOT_FOUND_404 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.05 METHOD NOT ALLOWED", METHOD_NOT_ALLOWED_405 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "405", METHOD_NOT_ALLOWED_405 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "METHOD NOT ALLOWED", METHOD_NOT_ALLOWED_405 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.06 NOT ACCEPTABLE", NOT_ACCEPTABLE_406 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "406", NOT_ACCEPTABLE_406 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "NOT ACCEPTABLE", NOT_ACCEPTABLE_406 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.12 PRECONDITION FAILED", PRECONDITION_FAILED_412 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "412", PRECONDITION_FAILED_412 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "PRECONDITION FAILED", PRECONDITION_FAILED_412 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.13 REQUEST ENTITY TOO LARGE", REQUEST_ENTITY_TOO_LARGE_413 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "413", REQUEST_ENTITY_TOO_LARGE_413 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "REQUEST ENTITY TOO LARGE", REQUEST_ENTITY_TOO_LARGE_413 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "4.15 UNSUPPORTED CONTENT-FORMAT", UNSUPPORTED_CONTENT_FORMAT_415 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "415", UNSUPPORTED_CONTENT_FORMAT_415 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "UNSUPPORTED CONTENT-FORMAT", UNSUPPORTED_CONTENT_FORMAT_415 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.00 INTERNAL SERVER ERROR", INTERNAL_SERVER_ERROR_500 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "500", INTERNAL_SERVER_ERROR_500 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "INTERNAL SERVER ERROR", INTERNAL_SERVER_ERROR_500 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.01 NOT IMPLEMENTED", NOT_IMPLEMENTED_501 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "501", NOT_IMPLEMENTED_501 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "NOT IMPLEMENTED", NOT_IMPLEMENTED_501 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.02 BAD GATEWAY", BAD_GATEWAY_502 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "502", BAD_GATEWAY_502 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "BAD GATEWAY", BAD_GATEWAY_502 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.03 SERVICE UNAVAILABLE", SERVICE_UNAVAILABLE_503 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "503", SERVICE_UNAVAILABLE_503 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "SERVICE UNAVAILABLE", SERVICE_UNAVAILABLE_503 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.04 GATEWAY TIMEOUT", GATEWAY_TIMEOUT_504 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "504", GATEWAY_TIMEOUT_504 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "GATEWAY TIMEOUT", GATEWAY_TIMEOUT_504 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "5.05 PROXYING NOT SUPPORTED", PROXYING_NOT_SUPPORTED_505 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "505", PROXYING_NOT_SUPPORTED_505 );
		JOLIE_ALLOWED_MESSAGE_CODE.put( "PROXYING NOT SUPPORTED", PROXYING_NOT_SUPPORTED_505 );
	}

	/**
	 *
	 * @param messageCode
	 * @return String
	 */
	public static String asString( int messageCode )
	{
		String result = MESSAGE_CODES.get( messageCode );
		return result == null ? "UNKOWN" : result;
	}

	/**
	 *
	 * @param number
	 * @return boolean
	 */
	public static boolean isValidMessageCode( int number )
	{
		return MESSAGE_CODES.containsKey( number );
	}

	/**
	 *
	 * @param messageCode
	 * @return boolean
	 */
	public static boolean isRequest( int messageCode )
	{
		return (messageCode > 0 && messageCode < 5);
	}

	/**
	 *
	 * @param messageCode
	 * @return boolean
	 */
	public static boolean isResponse( int messageCode )
	{
		return messageCode >= 5;
	}

	/**
	 *
	 * @param codeNumber
	 * @return boolean
	 */
	public static boolean isErrorMessage( int codeNumber )
	{
		return (codeNumber >= 128);
	}

	/**
	 *
	 * @param codeNumber
	 * @return boolean
	 */
	public static boolean allowsContent( int codeNumber )
	{
		return !(codeNumber == GET || codeNumber == DELETE);
	}

}
