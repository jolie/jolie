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
package jolie.net.coap.options;

import java.util.HashMap;

public abstract class Option {

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

  private static HashMap<Integer, String> OPTIONS = new HashMap<>();

  static {

    OPTIONS.put(IF_MATCH, "IF MATCH");
    OPTIONS.put(URI_HOST, "URI HOST");
    OPTIONS.put(ETAG, "ETAG");
    OPTIONS.put(IF_NONE_MATCH, "IF NONE MATCH");
    OPTIONS.put(OBSERVE, "OBSERVE");
    OPTIONS.put(URI_PORT, "URI PORT");
    OPTIONS.put(LOCATION_PATH, "LOCATION PATH");
    OPTIONS.put(URI_PATH, "URI PATH");
    OPTIONS.put(CONTENT_FORMAT, "CONTENT FORMAT");
    OPTIONS.put(MAX_AGE, "MAX AGE");
    OPTIONS.put(URI_QUERY, "URI QUERY");
    OPTIONS.put(ACCEPT, "ACCEPT");
    OPTIONS.put(LOCATION_QUERY, "LOCATION QUERY");
    OPTIONS.put(BLOCK_2, "BLOCK 2");
    OPTIONS.put(BLOCK_1, "BLOCK 1");
    OPTIONS.put(SIZE_2, "SIZE 2");
    OPTIONS.put(PROXY_URI, "PROXY URI");
    OPTIONS.put(PROXY_SCHEME, "PROXY SCHEME");
    OPTIONS.put(SIZE_1, "SIZE 1");
    OPTIONS.put(ENDPOINT_ID_1, "ENDPOINT ID 1");
    OPTIONS.put(ENDPOINT_ID_2, "ENDPOINT ID 2");
  }

  public static String asString(int optionNumber) {
    String result = OPTIONS.get(optionNumber);
    return result == null ? "UNKOWN (" + optionNumber + ")" : result;
  }

  public static boolean isCritical(int optionNumber) {
    return (optionNumber & 1) == 1;
  }

  public static boolean isSafe(int optionNumber) {
    return !((optionNumber & 2) == 2);
  }

  public static boolean isCacheKey(int optionNumber) {
    return !((optionNumber & 0x1e) == 0x1c);
  }
}
