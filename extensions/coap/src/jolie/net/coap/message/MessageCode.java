/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
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
package jolie.net.coap.message;

import java.util.HashMap;

public class MessageCode {

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
  public static final int CONTINUE_231 = 95;
  public static final int BAD_REQUEST_400 = 128;
  public static final int UNAUTHORIZED_401 = 129;
  public static final int BAD_OPTION_402 = 130;
  public static final int FORBIDDEN_403 = 131;
  public static final int NOT_FOUND_404 = 132;
  public static final int METHOD_NOT_ALLOWED_405 = 133;
  public static final int NOT_ACCEPTABLE_406 = 134;
  public static final int REQUEST_ENTITY_INCOMPLETE_408 = 136;
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
      = new HashMap<>();

  static {

    MESSAGE_CODES.put(EMPTY, "EMPTY");
    MESSAGE_CODES.put(GET, "GET");
    MESSAGE_CODES.put(POST, "POST");
    MESSAGE_CODES.put(PUT, "PUT");
    MESSAGE_CODES.put(DELETE, "DELETE");
    MESSAGE_CODES.put(CREATED_201, "201");
    MESSAGE_CODES.put(DELETED_202, "202");
    MESSAGE_CODES.put(VALID_203, "203");
    MESSAGE_CODES.put(CHANGED_204, "204");
    MESSAGE_CODES.put(CONTENT_205, "205");
    MESSAGE_CODES.put(CONTINUE_231, "231");
    MESSAGE_CODES.put(BAD_REQUEST_400, "400");
    MESSAGE_CODES.put(UNAUTHORIZED_401, "401");
    MESSAGE_CODES.put(BAD_OPTION_402, "402");
    MESSAGE_CODES.put(FORBIDDEN_403, "403");
    MESSAGE_CODES.put(NOT_FOUND_404, "404");
    MESSAGE_CODES.put(METHOD_NOT_ALLOWED_405, "405");
    MESSAGE_CODES.put(NOT_ACCEPTABLE_406, "406");
    MESSAGE_CODES.put(REQUEST_ENTITY_INCOMPLETE_408, "408");
    MESSAGE_CODES.put(PRECONDITION_FAILED_412, "412");
    MESSAGE_CODES.put(REQUEST_ENTITY_TOO_LARGE_413, "413");
    MESSAGE_CODES.put(UNSUPPORTED_CONTENT_FORMAT_415, "415");
    MESSAGE_CODES.put(INTERNAL_SERVER_ERROR_500, "500");
    MESSAGE_CODES.put(NOT_IMPLEMENTED_501, "501");
    MESSAGE_CODES.put(BAD_GATEWAY_502, "502");
    MESSAGE_CODES.put(SERVICE_UNAVAILABLE_503, "503");
    MESSAGE_CODES.put(GATEWAY_TIMEOUT_504, "504");
    MESSAGE_CODES.put(PROXYING_NOT_SUPPORTED_505, "505");
  }

  /**
   *
   * @param messageCode
   * @return String
   */
  public static String asString(int messageCode) {
    String result = MESSAGE_CODES.get(messageCode);
    return result == null ? "UNKOWN" : result;
  }

  /**
   *
   * @param number
   * @return boolean
   */
  public static boolean isMessageCode(int number) {
    return MESSAGE_CODES.containsKey(number);
  }

  /**
   *
   * @param messageCode
   * @return boolean
   */
  public static boolean isRequest(int messageCode) {
    return (messageCode > 0 && messageCode < 5);
  }

  /**
   *
   * @param messageCode
   * @return boolean
   */
  public static boolean isResponse(int messageCode) {
    return messageCode >= 5;
  }

  /**
   *
   * @param codeNumber
   * @return boolean
   */
  public static boolean isErrorMessage(int codeNumber) {
    return (codeNumber >= 128);
  }

  /**
   *
   * @param codeNumber
   * @return boolean
   */
  public static boolean allowsContent(int codeNumber) {
    return !(codeNumber == GET || codeNumber == DELETE);
  }

}
