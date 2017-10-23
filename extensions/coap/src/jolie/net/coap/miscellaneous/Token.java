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
package jolie.net.coap.miscellaneous;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Token implements Comparable<Token> {

  public static int MAX_LENGTH = 8;
  private static final char[] hexArray
      = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").toCharArray();
  private byte[] token;

  public Token(byte[] token) {
    if (token.length > 8) {
      throw new IllegalArgumentException("Maximum token length is 8 "
          + "(but given length was " + token.length + ")");
    }
    this.token = token;
  }

  public byte[] getBytes() {
    return this.token;
  }

  public static Token getRandomToken(int tokenLength) {
    byte[] token = new byte[tokenLength];
    new Random().nextBytes(token);

    return new Token(token);
  }

  @Override
  public String toString() {
    String tmp = bytesToHex(getBytes());

    if (tmp.length() == 0) {
      return "<EMPTY>";
    } else {
      return "0x" + tmp;
    }
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || (!(object instanceof Token))) {
      return false;
    }

    Token other = (Token) object;
    return Arrays.equals(this.getBytes(), other.getBytes());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(token);
  }

  @Override
  public int compareTo(Token other) {

    if (other.equals(this)) {
      return 0;
    }

    if (this.getBytes().length < other.getBytes().length) {
      return -1;
    }

    if (this.getBytes().length > other.getBytes().length) {
      return 1;
    }

    ByteBuffer.allocate(Long.BYTES);

    Long a = ByteBuffer.wrap(this.getBytes()).getLong();
    Long b = ByteBuffer.wrap(other.getBytes()).getLong();

    return Long.compareUnsigned(a, b);
  }
}
