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
package jolie.net.coap.message.options;

import io.netty.util.CharsetUtil;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

/**
 * {@link OptionValue} is the abstract base class for CoAP options. It provides
 * a number of useful static constants and methods as well as other methods to
 * be inherited by extending classes.
 *
 * @author Oliver Kleine
 */
public abstract class OptionValue<T> {

  private static final String UNKNOWN_OPTION = "Unknown option no. %d";
  private static final String VALUE_IS_DEFAULT_VALUE = "Given value is "
      + "default value for option no. %d.";
  private static final String OUT_OF_ALLOWED_RANGE = "Given value length "
      + "(%d) is out of allowed range "
      + "for option no. %d (min: %d, max; %d).";
  private static final Charset charset = CharsetUtil.UTF_8;

  /**
   * Provides names of available option types (basically for internal use)
   */
  public static enum Type {
    EMPTY, STRING, UINT, OPAQUE
  }

  /**
   * Corresponds to 60, i.e. 60 seconds
   */
  public static final long MAX_AGE_DEFAULT = 60;

  /**
   * Corresponds to the maximum value of the max-age option (app. 136 years)
   */
  public static final long MAX_AGE_MAX = 0xFFFFFFFFL;

  /**
   * Corresponds to the encoded value of {@link #MAX_AGE_DEFAULT}
   */
  public static final byte[] ENCODED_MAX_AGE_DEFAULT
      = new BigInteger(1, ByteBuffer.allocate(Long.BYTES)
          .putLong(MAX_AGE_DEFAULT).array()).toByteArray();

  /**
   * Corresponds to 5683
   */
  public static final long URI_PORT_DEFAULT = 5683;

  /**
   * Corresponds to the encoded value of {@link #URI_PORT_DEFAULT}
   */
  public static final byte[] ENCODED_URI_PORT_DEFAULT
      = new BigInteger(1, ByteBuffer.allocate(Long.BYTES)
          .putLong(URI_PORT_DEFAULT).array()).toByteArray();

  private static class Characteristics {

    private Type type;
    private int minLength;
    private int maxLength;

    private Characteristics(Type type, int minLength, int maxLength) {
      this.type = type;
      this.minLength = minLength;
      this.maxLength = maxLength;
    }

    public Type getType() {
      return type;
    }

    public int getMinLength() {
      return minLength;
    }

    public int getMaxLength() {
      return maxLength;
    }
  }

  private static HashMap<Integer, Characteristics> CHARACTERISTICS
      = new HashMap<>();

  static {
    CHARACTERISTICS.put(Option.IF_MATCH,
        new Characteristics(OptionValue.Type.OPAQUE, 0, 8));
    CHARACTERISTICS.put(Option.URI_HOST,
        new Characteristics(OptionValue.Type.STRING, 1, 255));
    CHARACTERISTICS.put(Option.ETAG,
        new Characteristics(OptionValue.Type.OPAQUE, 1, 8));
    CHARACTERISTICS.put(Option.IF_NONE_MATCH,
        new Characteristics(OptionValue.Type.EMPTY, 0, 0));
    CHARACTERISTICS.put(Option.URI_PORT,
        new Characteristics(OptionValue.Type.UINT, 0, 2));
    CHARACTERISTICS.put(Option.LOCATION_PATH,
        new Characteristics(OptionValue.Type.STRING, 0, 255));
    CHARACTERISTICS.put(Option.OBSERVE,
        new Characteristics(OptionValue.Type.UINT, 0, 3));
    CHARACTERISTICS.put(Option.URI_PATH,
        new Characteristics(OptionValue.Type.STRING, 0, 255));
    CHARACTERISTICS.put(Option.CONTENT_FORMAT,
        new Characteristics(OptionValue.Type.UINT, 0, 2));
    CHARACTERISTICS.put(Option.MAX_AGE,
        new Characteristics(OptionValue.Type.UINT, 0, 4));
    CHARACTERISTICS.put(Option.URI_QUERY,
        new Characteristics(OptionValue.Type.STRING, 0, 255));
    CHARACTERISTICS.put(Option.ACCEPT,
        new Characteristics(OptionValue.Type.UINT, 0, 2));
    CHARACTERISTICS.put(Option.LOCATION_QUERY,
        new Characteristics(OptionValue.Type.STRING, 0, 255));
    CHARACTERISTICS.put(Option.BLOCK_2,
        new Characteristics(OptionValue.Type.UINT, 0, 3));
    CHARACTERISTICS.put(Option.BLOCK_1,
        new Characteristics(OptionValue.Type.UINT, 0, 3));
    CHARACTERISTICS.put(Option.SIZE_2,
        new Characteristics(OptionValue.Type.UINT, 0, 4));
    CHARACTERISTICS.put(Option.PROXY_URI,
        new Characteristics(OptionValue.Type.STRING, 1, 1034));
    CHARACTERISTICS.put(Option.PROXY_SCHEME,
        new Characteristics(OptionValue.Type.STRING, 1, 255));
    CHARACTERISTICS.put(Option.SIZE_1,
        new Characteristics(OptionValue.Type.UINT, 0, 4));
    CHARACTERISTICS.put(Option.ENDPOINT_ID_1,
        new Characteristics(OptionValue.Type.OPAQUE, 0, 8));
    CHARACTERISTICS.put(Option.ENDPOINT_ID_2,
        new Characteristics(OptionValue.Type.OPAQUE, 0, 8));
  }

  /**
   * Returns the Type the given option number refers to
   *
   * @param optionNumber the option number to return the type of
   *
   * @return the {@link de.uzl.itm.ncoap.message.options.OptionValue.Type} the
   * given option number refers to
   *
   * @throws java.lang.IllegalArgumentException if the given option number
   * refers to an unknown option
   */
  public static Type getType(int optionNumber)
      throws IllegalArgumentException {

    Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
    if (characteristics == null) {
      throw new IllegalArgumentException(String.format(UNKNOWN_OPTION,
          optionNumber));
    } else {
      return characteristics.getType();
    }
  }

  /**
   * Returns the minimum length for the given option number in bytes.
   *
   * @param optionNumber the option number to check the minimum length of
   *
   * @return the minimum length for the given option number in bytes
   *
   * @throws java.lang.IllegalArgumentException if the given option number
   * refers to an unknown option
   */
  public static int getMinLength(int optionNumber)
      throws IllegalArgumentException {

    Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
    if (characteristics == null) {
      throw new IllegalArgumentException(String.format(UNKNOWN_OPTION,
          optionNumber));
    } else {
      return characteristics.getMinLength();
    }
  }

  /**
   * Returns the maximum length for the given option number in bytes.
   *
   * @param optionNumber the option number to check the maximum length of
   *
   * @return the maximum length for the given option number in bytes
   *
   * @throws java.lang.IllegalArgumentException if the given option number
   * refers to an unknown option
   */
  public static int getMaxLength(int optionNumber)
      throws IllegalArgumentException {

    Characteristics characteristics = CHARACTERISTICS.get(optionNumber);
    if (characteristics == null) {
      throw new IllegalArgumentException(String.format(UNKNOWN_OPTION,
          optionNumber));
    } else {
      return characteristics.getMaxLength();
    }
  }

  /**
   * Returns <code>true</code> if the given value is the default value for the
   * given option number and <code>false</code> if it is not the default value.
   * Options with default value cannot be created.
   *
   * @param optionNumber the option number
   * @param value the value to check if it is the default value for the option
   * number
   *
   * @return <code>true</code> if the given value is the default value for the
   * given option number
   */
  public static boolean isDefaultValue(int optionNumber, byte[] value) {

    if (optionNumber == Option.URI_PORT && Arrays.equals(value,
        ENCODED_URI_PORT_DEFAULT)) {
      return true;
    } else if (optionNumber == Option.MAX_AGE && Arrays.equals(value,
        ENCODED_MAX_AGE_DEFAULT)) {
      return true;
    } else if (optionNumber == Option.URI_HOST) {
      String hostName = new String(value, charset);
      if (hostName.startsWith("[") && hostName.endsWith("]")) {
        hostName = hostName.substring(1, hostName.length() - 1);
      }

      try { // check correctness
        InetAddress.getAllByName(hostName);
        return true;
      } catch (UnknownHostException e) {
        return false;
      }
    }

    return false;
  }

  protected byte[] value;

  /**
   * @param optionNumber the number of the {@link OptionValue} to be created.
   * @param value the encoded value of the option to be created.
   *
   * @throws java.lang.IllegalArgumentException if the {@link OptionValue}
   * instance could not be created because either the given value is the default
   * value or the length of the given value exceeds the defined limits.
   */
  protected OptionValue(int optionNumber, byte[] value, boolean allowDefault)
      throws IllegalArgumentException {

    if (!allowDefault && OptionValue.isDefaultValue(optionNumber, value)) {
      throw new IllegalArgumentException(String.format(
          VALUE_IS_DEFAULT_VALUE, optionNumber));
    }

    if (getMinLength(optionNumber) > value.length
        || getMaxLength(optionNumber) < value.length) {
      throw new IllegalArgumentException(String.format(
          OUT_OF_ALLOWED_RANGE, value.length, optionNumber,
          getMinLength(optionNumber), getMaxLength(optionNumber)));
    }

    this.value = value;
  }

  /**
   * Returns the encoded value of this {@link OptionValue} as byte array. The
   * way how to interpret the returned value depends on the {@link Type}.
   * Usually it is more convenient to use {@link #getDecodedValue()} instead.
   *
   * @return the encoded value of this option as byte array
   */
  public byte[] getValue() {
    return this.value;
  }

  /**
   * Returns the decoded value of this {@link OptionValue} as an instance of
   * <code>T</code>.
   *
   * @return the decoded value of this {@link OptionValue} as an instance of
   * <code>T</code>
   */
  public abstract T getDecodedValue();

  @Override
  public abstract int hashCode();

  /**
   * Returns <code>true</code> if the given object is an instance of
   * {@link OptionValue} and both byte arrays returned by respective
   * {@link #getValue()} method contain the same bytes, i.e. the same array
   * length and the same byte values in the same order.
   *
   * @param object the object to check for equality with this instance of
   * {@link OptionValue}
   *
   * @return <code>true</code> if the given object is an instance of
   * {@link OptionValue} and both byte arrays returned by respective
   * {@link #getValue()} method contain the same bytes, i.e. the same array
   * length and the same byte values in the same order.
   */
  @Override
  public abstract boolean equals(Object object);

  /**
   * Returns a {@link String} representation of this option.
   *
   * @return a {@link String} representation of this option.
   */
  @Override
  public String toString() {
    return "" + this.getDecodedValue();
  }

}
