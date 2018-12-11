/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck                     *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
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

package jolie.net.coap.message.options;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import jolie.net.coap.message.MessageCode;
import static jolie.net.coap.message.options.Option.Occurence.MULTIPLE;
import static jolie.net.coap.message.options.Option.Occurence.NONE;
import static jolie.net.coap.message.options.Option.Occurence.ONCE;

public class Option
{

	public static final int UNKNOWN = -1;
	public static final int IF_MATCH = 1;
	public static final int URI_HOST = 3;
	public static final int ETAG = 4;
	public static final int IF_NONE_MATCH = 5;
	public static final int OBSERVE = 6;
	public static final int URI_PORT = 7;
	public static final int LOCATION_PATH = 8;
	public static final int URI_PATH = 11;
	public static final int CONTENT_FORMAT = 12;
	public static final int MAX_AGE = 14;
	public static final int URI_QUERY = 15;
	public static final int ACCEPT = 17;
	public static final int LOCATION_QUERY = 20;
	public static final int BLOCK_2 = 23;
	public static final int BLOCK_1 = 27;
	public static final int SIZE_2 = 28;
	public static final int PROXY_URI = 35;
	public static final int PROXY_SCHEME = 39;
	public static final int SIZE_1 = 60;
	public static final int ENDPOINT_ID_1 = 124;
	public static final int ENDPOINT_ID_2 = 189;

	private static final HashMap<Integer, String> OPTIONS = new HashMap<>();

	static {

		OPTIONS.put( IF_MATCH, "IF MATCH" );
		OPTIONS.put( IF_NONE_MATCH, "IF NONE MATCH" );
		OPTIONS.put( OBSERVE, "OBSERVE" );
		OPTIONS.put( CONTENT_FORMAT, "CONTENT FORMAT" );
		OPTIONS.put( URI_HOST, "URI HOST" );
		OPTIONS.put( URI_PORT, "URI PORT" );
		OPTIONS.put( URI_PATH, "URI PATH" );
		OPTIONS.put( URI_QUERY, "URI QUERY" );
		OPTIONS.put( LOCATION_PATH, "LOCATION PATH" );
		OPTIONS.put( LOCATION_QUERY, "LOCATION QUERY" );
		OPTIONS.put( ETAG, "ETAG" );
		OPTIONS.put( MAX_AGE, "MAX AGE" );
		OPTIONS.put( ACCEPT, "ACCEPT" );
		OPTIONS.put( BLOCK_1, "BLOCK 1" );
		OPTIONS.put( BLOCK_2, "BLOCK 2" );
		OPTIONS.put( SIZE_1, "SIZE 1" );
		OPTIONS.put( SIZE_2, "SIZE 2" );
		OPTIONS.put( PROXY_URI, "PROXY URI" );
		OPTIONS.put( PROXY_SCHEME, "PROXY SCHEME" );
		OPTIONS.put( ENDPOINT_ID_1, "ENDPOINT ID 1" );
		OPTIONS.put( ENDPOINT_ID_2, "ENDPOINT ID 2" );
	}

	public static String asString( int optionNumber )
	{
		String result = OPTIONS.get( optionNumber );
		return result == null ? "UNKOWN (" + optionNumber + ")" : result;
	}

	/**
	 * Returns <code>true</code> if the option is critical and <code>false</code>
	 * if the option is elective
	 *
	 * @param optionNumber
	 * @return <code>true</code> if the option is critical and <code>false</code>
	 * if the option is elective
	 */
	public static boolean isCritical( int optionNumber )
	{
		return (optionNumber & 1) == 1;
	}

	/**
	 * Returns <code>true</code> if the option is safe-to-forward and
	 * <code>false</code> if the option is unsafe-to-forward by a proxy
	 *
	 * @param optionNumber the option number to be checked for safeness.
	 * @return <code>true</code> if the option is safe-to-forward and
	 * <code>false</code> if the option is unsafe-to-forward by a proxy
	 */
	public static boolean isSafe( int optionNumber )
	{
		return !((optionNumber & 2) == 2);
	}

	/**
	 * Returns <code>true</code> if the option is part of the cache key and
	 * <code>false</code> if the option is not part of the cache key for proxies.
	 *
	 * @param optionNumber the option number to be checked if it is part of the
	 * cache key.
	 * @return <code>true</code> if the option is part of the cache key and
	 * <code>false</code> if the option is not part of the cache key for proxies.
	 */
	public static boolean isCacheKey( int optionNumber )
	{
		return !((optionNumber & 0x1e) == 0x1c);
	}

	private static final Hashtable<Integer, Map<Integer, Occurence>> OCCURENCE_CONSTRAINTS
		= new Hashtable<>();

	static {

		Map<Integer, Occurence> GET = new HashMap<>();
		GET.put( OBSERVE, ONCE );
		GET.put( URI_HOST, ONCE );
		GET.put( URI_PORT, ONCE );
		GET.put( URI_PATH, MULTIPLE );
		GET.put( URI_QUERY, MULTIPLE );
		GET.put( ETAG, MULTIPLE );
		GET.put( ACCEPT, MULTIPLE );
		GET.put( BLOCK_2, ONCE );
		GET.put( SIZE_2, ONCE );
		GET.put( PROXY_URI, ONCE );
		GET.put( PROXY_SCHEME, ONCE );
		GET.put( ENDPOINT_ID_1, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.GET, GET );

		Map<Integer, Occurence> POST = new HashMap<>();
		POST.put( URI_HOST, ONCE );
		POST.put( URI_PORT, ONCE );
		POST.put( URI_PATH, MULTIPLE );
		POST.put( URI_QUERY, MULTIPLE );
		POST.put( ACCEPT, MULTIPLE );
		POST.put( PROXY_URI, ONCE );
		POST.put( PROXY_SCHEME, ONCE );
		POST.put( CONTENT_FORMAT, ONCE );
		POST.put( BLOCK_2, ONCE );
		POST.put( BLOCK_1, ONCE );
		POST.put( SIZE_2, ONCE );
		POST.put( SIZE_1, ONCE );
		POST.put( ENDPOINT_ID_1, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.POST, POST );

		Map<Integer, Occurence> PUT = new HashMap<>();
		PUT.put( URI_HOST, ONCE );
		PUT.put( URI_PORT, ONCE );
		PUT.put( URI_PATH, MULTIPLE );
		PUT.put( URI_QUERY, MULTIPLE );
		PUT.put( ACCEPT, MULTIPLE );
		PUT.put( PROXY_URI, ONCE );
		PUT.put( PROXY_SCHEME, ONCE );
		PUT.put( CONTENT_FORMAT, ONCE );
		PUT.put( IF_MATCH, ONCE );
		PUT.put( IF_NONE_MATCH, ONCE );
		PUT.put( BLOCK_2, ONCE );
		PUT.put( BLOCK_1, ONCE );
		PUT.put( SIZE_2, ONCE );
		PUT.put( SIZE_1, ONCE );
		PUT.put( ENDPOINT_ID_1, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.PUT, PUT );

		Map<Integer, Occurence> DELETE = new HashMap<>();
		DELETE.put( URI_HOST, ONCE );
		DELETE.put( URI_PORT, ONCE );
		DELETE.put( URI_PATH, MULTIPLE );
		DELETE.put( URI_QUERY, MULTIPLE );
		DELETE.put( PROXY_URI, ONCE );
		DELETE.put( PROXY_SCHEME, ONCE );
		DELETE.put( ENDPOINT_ID_1, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.DELETE, DELETE );

		Map<Integer, Occurence> CREATED_201 = new HashMap<>();
		CREATED_201.put( ETAG, ONCE );
		CREATED_201.put( OBSERVE, ONCE );
		CREATED_201.put( LOCATION_PATH, MULTIPLE );
		CREATED_201.put( LOCATION_QUERY, MULTIPLE );
		CREATED_201.put( CONTENT_FORMAT, ONCE );
		CREATED_201.put( BLOCK_2, ONCE );
		CREATED_201.put( BLOCK_1, ONCE );
		CREATED_201.put( SIZE_2, ONCE );
		CREATED_201.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.CREATED_201, CREATED_201 );

		Map<Integer, Occurence> DELETED_202 = new HashMap<>();
		DELETED_202.put( CONTENT_FORMAT, ONCE );
		DELETED_202.put( BLOCK_2, ONCE );
		DELETED_202.put( BLOCK_1, ONCE );
		DELETED_202.put( SIZE_2, ONCE );
		DELETED_202.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.DELETED_202, DELETED_202 );

		Map<Integer, Occurence> VALID_203 = new HashMap<>();
		VALID_203.put( OBSERVE, ONCE );
		VALID_203.put( ETAG, ONCE );
		VALID_203.put( MAX_AGE, ONCE );
		VALID_203.put( CONTENT_FORMAT, ONCE );
		VALID_203.put( ENDPOINT_ID_1, ONCE );
		VALID_203.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.VALID_203, VALID_203 );

		Map<Integer, Occurence> CHANGED_204 = new HashMap<>();
		CHANGED_204.put( ETAG, ONCE );
		CHANGED_204.put( CONTENT_FORMAT, ONCE );
		CHANGED_204.put( BLOCK_2, ONCE );
		CHANGED_204.put( BLOCK_1, ONCE );
		CHANGED_204.put( SIZE_2, ONCE );
		CHANGED_204.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.CHANGED_204, CHANGED_204 );

		Map<Integer, Occurence> CONTENT_205 = new HashMap<>();
		CONTENT_205.put( OBSERVE, ONCE );
		CONTENT_205.put( CONTENT_FORMAT, ONCE );
		CONTENT_205.put( MAX_AGE, ONCE );
		CONTENT_205.put( ETAG, ONCE );
		CONTENT_205.put( BLOCK_2, ONCE );
		CONTENT_205.put( BLOCK_1, ONCE );
		CONTENT_205.put( SIZE_2, ONCE );
		CONTENT_205.put( ENDPOINT_ID_1, ONCE );
		CONTENT_205.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.CONTENT_205, CONTENT_205 );

		Map<Integer, Occurence> BAD_REQUEST_400 = new HashMap<>();
		BAD_REQUEST_400.put( MAX_AGE, ONCE );
		BAD_REQUEST_400.put( CONTENT_FORMAT, ONCE );
		BAD_REQUEST_400.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.BAD_REQUEST_400, BAD_REQUEST_400 );

		Map<Integer, Occurence> UNAUTHORIZED_401 = new HashMap<>();
		UNAUTHORIZED_401.put( MAX_AGE, ONCE );
		UNAUTHORIZED_401.put( CONTENT_FORMAT, ONCE );
		UNAUTHORIZED_401.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.UNAUTHORIZED_401, UNAUTHORIZED_401 );

		Map<Integer, Occurence> BAD_OPTION_402 = new HashMap<>();
		BAD_OPTION_402.put( MAX_AGE, ONCE );
		BAD_OPTION_402.put( CONTENT_FORMAT, ONCE );
		BAD_OPTION_402.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.BAD_OPTION_402, BAD_OPTION_402 );

		Map<Integer, Occurence> FORBIDDEN_403 = new HashMap<>();
		FORBIDDEN_403.put( MAX_AGE, ONCE );
		FORBIDDEN_403.put( CONTENT_FORMAT, ONCE );
		FORBIDDEN_403.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.FORBIDDEN_403, FORBIDDEN_403 );

		Map<Integer, Occurence> NOT_FOUND_404 = new HashMap<>();
		NOT_FOUND_404.put( MAX_AGE, ONCE );
		NOT_FOUND_404.put( CONTENT_FORMAT, ONCE );
		NOT_FOUND_404.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.NOT_FOUND_404, NOT_FOUND_404 );

		Map<Integer, Occurence> METHOD_NOT_ALLOWED_405 = new HashMap<>();
		METHOD_NOT_ALLOWED_405.put( MAX_AGE, ONCE );
		METHOD_NOT_ALLOWED_405.put( CONTENT_FORMAT, ONCE );
		METHOD_NOT_ALLOWED_405.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.METHOD_NOT_ALLOWED_405, METHOD_NOT_ALLOWED_405 );

		Map<Integer, Occurence> NOT_ACCEPTABLE_406 = new HashMap<>();
		NOT_ACCEPTABLE_406.put( MAX_AGE, ONCE );
		NOT_ACCEPTABLE_406.put( CONTENT_FORMAT, ONCE );
		NOT_ACCEPTABLE_406.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.NOT_ACCEPTABLE_406, NOT_ACCEPTABLE_406 );

		Map<Integer, Occurence> PRECONDITION_FAILED_412 = new HashMap<>();
		PRECONDITION_FAILED_412.put( MAX_AGE, ONCE );
		PRECONDITION_FAILED_412.put( CONTENT_FORMAT, ONCE );
		PRECONDITION_FAILED_412.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.PRECONDITION_FAILED_412, PRECONDITION_FAILED_412 );

		Map<Integer, Occurence> REQUEST_ENTITY_TOO_LARGE_413 = new HashMap<>();
		REQUEST_ENTITY_TOO_LARGE_413.put( MAX_AGE, ONCE );
		REQUEST_ENTITY_TOO_LARGE_413.put( CONTENT_FORMAT, ONCE );
		REQUEST_ENTITY_TOO_LARGE_413.put( BLOCK_1, ONCE );
		REQUEST_ENTITY_TOO_LARGE_413.put( SIZE_1, ONCE );
		REQUEST_ENTITY_TOO_LARGE_413.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.REQUEST_ENTITY_TOO_LARGE_413, REQUEST_ENTITY_TOO_LARGE_413 );

		Map<Integer, Occurence> UNSUPPORTED_CONTENT_FORMAT_415 = new HashMap<>();
		UNSUPPORTED_CONTENT_FORMAT_415.put( MAX_AGE, ONCE );
		UNSUPPORTED_CONTENT_FORMAT_415.put( CONTENT_FORMAT, ONCE );
		UNSUPPORTED_CONTENT_FORMAT_415.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.UNSUPPORTED_CONTENT_FORMAT_415, UNSUPPORTED_CONTENT_FORMAT_415 );

		Map<Integer, Occurence> INTERNAL_SERVER_ERROR_500 = new HashMap<>();
		INTERNAL_SERVER_ERROR_500.put( MAX_AGE, ONCE );
		INTERNAL_SERVER_ERROR_500.put( CONTENT_FORMAT, ONCE );
		INTERNAL_SERVER_ERROR_500.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.INTERNAL_SERVER_ERROR_500, INTERNAL_SERVER_ERROR_500 );

		Map<Integer, Occurence> NOT_IMPLEMENTED_501 = new HashMap<>();
		NOT_IMPLEMENTED_501.put( MAX_AGE, ONCE );
		NOT_IMPLEMENTED_501.put( CONTENT_FORMAT, ONCE );
		NOT_IMPLEMENTED_501.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.NOT_IMPLEMENTED_501, NOT_IMPLEMENTED_501 );

		Map<Integer, Occurence> BAD_GATEWAY_502 = new HashMap<>();
		BAD_GATEWAY_502.put( MAX_AGE, ONCE );
		BAD_GATEWAY_502.put( CONTENT_FORMAT, ONCE );
		BAD_GATEWAY_502.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.BAD_GATEWAY_502, BAD_GATEWAY_502 );

		Map<Integer, Occurence> GATEWAY_TIMEOUT_504 = new HashMap<>();
		GATEWAY_TIMEOUT_504.put( MAX_AGE, ONCE );
		GATEWAY_TIMEOUT_504.put( CONTENT_FORMAT, ONCE );
		GATEWAY_TIMEOUT_504.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.GATEWAY_TIMEOUT_504, GATEWAY_TIMEOUT_504 );

		Map<Integer, Occurence> PROXYING_NOT_SUPPORTED_505 = new HashMap<>();
		PROXYING_NOT_SUPPORTED_505.put( MAX_AGE, ONCE );
		PROXYING_NOT_SUPPORTED_505.put( CONTENT_FORMAT, ONCE );
		PROXYING_NOT_SUPPORTED_505.put( ENDPOINT_ID_2, ONCE );
		OCCURENCE_CONSTRAINTS.put( MessageCode.PROXYING_NOT_SUPPORTED_505, PROXYING_NOT_SUPPORTED_505 );
	}

	/**
	 * Returns the permitted option Occurence within a message with the given code
	 *
	 * @param optionNumber the options number
	 * @param messageCode the number corresponding to a message code
	 * @return the permitted option Occurence within a message with the given code
	 */
	public static Occurence getPermittedOccurence( int optionNumber, int messageCode )
	{

		Map<Integer, Occurence> m = OCCURENCE_CONSTRAINTS.get( messageCode );
		Occurence result = m.get( optionNumber );
		return result == null ? NONE : result;
	}

	private static final HashMap<Integer, Integer> MUTUAL_EXCLUSIONS = new HashMap<>();

	static {
		MUTUAL_EXCLUSIONS.put( URI_HOST, PROXY_URI );
		MUTUAL_EXCLUSIONS.put( URI_PORT, PROXY_URI );
		MUTUAL_EXCLUSIONS.put( URI_PATH, PROXY_URI );
		MUTUAL_EXCLUSIONS.put( URI_QUERY, PROXY_URI );
		MUTUAL_EXCLUSIONS.put( PROXY_SCHEME, PROXY_URI );
	}

	/**
	 * Returns <code>true</code> if and only if the co-existence of both options
	 * is not allowed in a single message. As this method checks for mutual
	 * exclusion, the order of the given arguments has no impact on the result.
	 *
	 * @param firstOptionNumber the first option number
	 * @param secondOptionNumber the second option number
	 * @return <code>true</code> if the co-existence of the given option numbers
	 * is not allowed in a single message
	 */
	public static boolean mutuallyExcludes( int firstOptionNumber, int secondOptionNumber )
	{
		if ( firstOptionNumber == Option.PROXY_URI ) {
			return MUTUAL_EXCLUSIONS.get( secondOptionNumber ) == firstOptionNumber;
		} else if ( secondOptionNumber == Option.PROXY_URI ) {
			return MUTUAL_EXCLUSIONS.get( firstOptionNumber ) == secondOptionNumber;
		}
		return false;
	}

	public enum Occurence
	{
		NONE, ONCE, MULTIPLE
	}
}
