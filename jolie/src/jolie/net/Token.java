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
package jolie.net;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * A {@link Token} is the identifier to relate Requests with
 * Responses. It consists of a byte array with a size between 0 and
 * 8 (both inclusive). So, {@link Token} basically is a wrapper class for a byte
 * array.
 *
 * The byte array content has no semantic meaning and thus, e.g. a {@link Token}
 * instance backed by a byte array containing a single zero byte (all bits set
 * to 0) is different from a byte array backed by a byte array containing two
 * zero bytes.
 *
 * @author Oliver Kleine
 */
public class Token implements Comparable<Token> {

  public static int MAX_LENGTH = 8;
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
  private byte[] token;

  /**
   * Creates a new {@link Token} instance.
   *
   * @param token the byte array this {@link Token} is supposed to consist of
   *
   * @throws java.lang.IllegalArgumentException if the length of the given byte
   * array is larger than 8
   */
  public Token(byte[] token) {
    if (token.length > 8) {
      throw new IllegalArgumentException("Maximum token length is 8 "
          + "(but given length was " + token.length + ")");
    }
    this.token = token;
  }

  /**
   * Returns the byte array this {@link Token} instance wraps
   *
   * @return the byte array this {@link Token} instance wraps
   */
  public byte[] getBytes() {
    return this.token;
  }

  /**
   * Returns a random generated token of maximum length.
   *
   * @param tokenLength
   * @return
   */
  public static Token getRandomToken() {
    byte[] token = new byte[MAX_LENGTH];
    new Random().nextBytes(token);
    return new Token(token);
  }

  /**
   * Returns a representation of the token in form of a HEX string or "<EMPTY>"
   * for tokens of length 0
   *
   * @return a representation of the token in form of a HEX string or "<EMPTY>"
   * for tokens of length 0
   */
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
