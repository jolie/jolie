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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import java.nio.ByteBuffer;

import java.nio.charset.Charset;
import java.util.*;

import jolie.Interpreter;
import jolie.net.coap.options.EmptyOptionValue;
import jolie.net.coap.options.OpaqueOptionValue;
import jolie.net.coap.options.Option;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.options.UintOptionValue;

/**
 * This class is the base class for inheriting subtypes, e.g. requests and
 * responses. This abstract class provides the cut-set in terms of functionality
 * of {@link CoapRequest} and {@link CoapResponse}.
 *
 * @author Oliver Kleine
 */
public class CoapMessage {

  public static final int PROTOCOL_VERSION = 1;
  public static final Charset CHARSET = CharsetUtil.UTF_8;
  public static final int UNDEFINED_MESSAGE_ID = -1;
  public static final int MAX_TOKEN_LENGTH = 8;
  private static final String WRONG_OPTION_TYPE = "Option no. %d is no "
      + "option of type %s";
  private static final String DOES_NOT_ALLOW_CONTENT = "CoAP messages "
      + "with code %s do not allow payload.";

  private int messageType;
  private int messageCode;
  private int messageID;
  private ByteBuf content;
  private Token token;

  protected Map<Integer, OptionValue> options;

  /**
   * Creates a new instance of {@link CoapMessage}.
   *
   * @param messageType the number representing the {@link MessageType} for this
   * {@link CoapMessage}
   * @param messageCode the number representing the {@link MessageCode} for this
   * {@link CoapMessage}
   * @param messageID the message ID for this {@link CoapMessage}
   * @param token the {@link Token} for this {@link CoapMessage}
   *
   * @throws IllegalArgumentException if one of the given arguments is invalid
   */
  public CoapMessage(int messageType, int messageCode, int messageID,
      Token token) throws IllegalArgumentException {

    if (!MessageType.isMessageType(messageType)) {
      throw new IllegalArgumentException("No. " + messageType
          + " is not corresponding to any message type.");
    }

    if (!MessageCode.isMessageCode(messageCode)) {
      throw new IllegalArgumentException("No. " + messageCode
          + " is not corresponding to any message code.");
    }

    this.setMessageType(messageType);
    this.messageCode(messageCode);
    this.setMessageID(messageID);
    this.setToken(token);
    this.options = new TreeMap<>();
    this.content = Unpooled.EMPTY_BUFFER;
  }

  /**
   * Method to create an empty reset message which is strictly speaking neither
   * a request nor a response
   *
   * @param messageID the message ID of the reset message.
   *
   * @return an instance of {@link CoapMessage} with {@link MessageType#RST}
   *
   * @throws IllegalArgumentException if the given message ID is out of the
   * allowed range
   */
  public static CoapMessage createEmptyReset(int messageID)
      throws IllegalArgumentException {
    return new CoapMessage(MessageType.RST, MessageCode.EMPTY, messageID,
        new Token(new byte[0])) {
    };
  }

  /**
   * Method to create an empty acknowledgement message which is strictly
   * speaking neither a request nor a response
   *
   * @param messageID the message ID of the acknowledgement message.
   *
   * @return an instance of {@link CoapMessage} with {@link MessageType#ACK}
   *
   * @throws IllegalArgumentException if the given message ID is out of the
   * allowed range
   */
  public static CoapMessage createEmptyAcknowledgement(int messageID)
      throws IllegalArgumentException {
    return new CoapMessage(MessageType.ACK, MessageCode.EMPTY, messageID,
        new Token(new byte[0])) {
    };
  }

  /**
   * Method to create an empty confirmable message which is considered a PIMG
   * message on application layer, i.e. a message to check if a CoAP endpoints
   * is alive (not only the host but also the CoAP application!).
   *
   * @param messageID the message ID of the acknowledgement message.
   *
   * @return an instance of {@link CoapMessage} with {@link MessageType#CON}
   *
   * @throws IllegalArgumentException if the given message ID is out of the
   * allowed range
   */
  public static CoapMessage createPing(int messageID)
      throws IllegalArgumentException {
    return new CoapMessage(MessageType.CON, MessageCode.EMPTY, messageID,
        new Token(new byte[0])) {
    };
  }

  /**
   * Sets the message type of this {@link CoapMessage}. Usually there is no need
   * to use this method as the value is either set via constructor parameter
   * (for requests) or automatically by the nCoAP framework (for responses).
   *
   * @param messageType the number representing the message type of this method
   *
   * @throws java.lang.IllegalArgumentException if the given message type is not
   * supported.
   */
  public void setMessageType(int messageType)
      throws IllegalArgumentException {
    if (!MessageType.isMessageType(messageType)) {
      throw new IllegalArgumentException("Invalid message type ("
          + messageType
          + "). Only numbers 0-3 are allowed.");
    }

    this.messageType = messageType;
  }

  public boolean isPing() {
    return this.messageCode == MessageCode.EMPTY
        && this.messageType == MessageType.CON;
  }

  public boolean isRequest() {
    return MessageCode.isRequest(this.getMessageCode());
  }

  public boolean isResponse() {
    return MessageCode.isResponse(this.getMessageCode());
  }

  public boolean isAck() {
    return MessageType.isMessageType(MessageType.ACK);
  }

  /**
   * Adds an option to this {@link CoapMessage}. However, it is recommended to
   * use the options specific methods from {@link CoapRequest} and
   * {@link CoapResponse} to add options. This method is intended for framework
   * internal use.
   *
   * @param optionNumber the number representing the option type
   * @param optionValue the {@link OptionValue} of this option
   *
   * @throws java.lang.IllegalArgumentException if the given option number is
   * unknwon, or if the given value is either the default value or exceeds the
   * defined length limits for options with the given option number
   */
  public void addOption(int optionNumber, OptionValue optionValue)
      throws IllegalArgumentException {
    this.options.put(optionNumber, optionValue);
  }

  /**
   * Adds an string option to this {@link CoapMessage}. However, it is
   * recommended to use the options specific methods from {@link CoapRequest}
   * and {@link CoapResponse} to add options. This method is intended for
   * framework internal use.
   *
   * @param optionNumber the number representing the option type
   * @param value the value of this string option
   *
   * @throws java.lang.IllegalArgumentException if the given option number
   * refers to an unknown option or if the given {@link OptionValue} is not
   * valid, e.g. to long
   */
  public void addStringOption(int optionNumber, String value)
      throws IllegalArgumentException {

    if (!(OptionValue.getType(optionNumber) == OptionValue.Type.STRING)) {
      throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE,
          optionNumber, OptionValue.Type.STRING));
    }

    addOption(optionNumber, new StringOptionValue(optionNumber, value));
  }

  /**
   * Adds an uint option to this {@link CoapMessage}. However, it is recommended
   * to use the options specific methods from {@link CoapRequest} and
   * {@link CoapResponse} to add options. This method is intended for framework
   * internal use.
   *
   * @param optionNumber the number representing the option type
   * @param value the value of this uint option
   *
   * @throws java.lang.IllegalArgumentException
   */
  public void addUintOption(int optionNumber, long value)
      throws IllegalArgumentException {

    if (!(OptionValue.getType(optionNumber) == OptionValue.Type.UINT)) {
      throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE,
          optionNumber, OptionValue.Type.STRING));
    }

    byte[] byteValue = ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    int index = 0;
    while (index < byteValue.length && byteValue[index] == 0) {
      index++;
    }
    addOption(optionNumber, new UintOptionValue(optionNumber,
        Arrays.copyOfRange(byteValue, index, byteValue.length)));
  }

  /**
   * Adds an opaque option to this {@link CoapMessage}. However, it is
   * recommended to use the options specific methods from {@link CoapRequest}
   * and {@link CoapResponse} to add options. This method is intended for
   * framework internal use.
   *
   * @param optionNumber the number representing the option type
   * @param value the value of this opaque option
   *
   * @throws java.lang.IllegalArgumentException
   */
  public void addOpaqueOption(int optionNumber, byte[] value)
      throws IllegalArgumentException {

    if (!(OptionValue.getType(optionNumber) == OptionValue.Type.OPAQUE)) {
      throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE,
          optionNumber, OptionValue.Type.OPAQUE));
    }

    addOption(optionNumber, new OpaqueOptionValue(optionNumber, value));
  }

  /**
   * Adds an empty option to this {@link CoapMessage}. However, it is
   * recommended to use the options specific methods from {@link CoapRequest}
   * and {@link CoapResponse} to add options. This method is intended for
   * framework internal use.
   *
   * @param optionNumber the number representing the option type
   *
   * @throws java.lang.IllegalArgumentException if the given option number
   * refers to an unknown option or to a not-empty option.
   */
  public void addEmptyOption(int optionNumber)
      throws IllegalArgumentException {

    if (!(OptionValue.getType(optionNumber) == OptionValue.Type.EMPTY)) {
      throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE,
          optionNumber, OptionValue.Type.EMPTY));
    }
    addOption(optionNumber, new EmptyOptionValue(optionNumber));
  }

  /**
   * Removes all options with the given option number from this
   * {@link CoapMessage} instance.
   *
   * @param optionNumber the option number to remove from this message
   *
   * @return the number of options that were removed, i.e. the count.
   */
  public int removeOptions(int optionNumber) {
    this.options.remove(optionNumber);
    int result = options.size();
    return result;
  }

  private static long extractBits(final long value, final int bits,
      final int offset) {
    final long shifted = value >>> offset;
    final long masked = (1L << bits) - 1L;
    return shifted & masked;
  }

  /**
   * Returns the CoAP protocol version used for this {@link CoapMessage}
   *
   * @return the CoAP protocol version used for this {@link CoapMessage}
   */
  public int getProtocolVersion() {
    return PROTOCOL_VERSION;
  }

  /**
   * Sets a Random geenerated message ID for this message. However, there is no
   * need to set the message ID manually. It is set (or overwritten)
   * automatically by the nCoAP framework.
   *
   */
  public void setRandomMessageID() {
    this.setMessageID(new Random().nextInt(65535));
  }

  /**
   * Sets the message ID for this message. However, there is no need to set the
   * message ID manually. It is set (or overwritten) automatically by the nCoAP
   * framework.
   *
   * @param messageID the message ID for the message
   */
  public void setMessageID(int messageID) throws IllegalArgumentException {

    if (messageID < -1 || messageID > 65535) {
      throw new IllegalArgumentException("Message ID "
          + messageID + " is either negative or greater than 65535");
    }

    this.messageID = messageID;
  }

  /**
   * Returns the message ID (or {@link CoapMessage#UNDEFINED_MESSAGE_ID} if not
   * set)
   *
   * @return the message ID (or {@link CoapMessage#UNDEFINED_MESSAGE_ID} if not
   * set)
   */
  public int getMessageID() {
    return this.messageID;
  }

  /**
   * Returns the number representing the {@link MessageType} of this
   * {@link CoapMessage}
   *
   * @return the number representing the {@link MessageType} of this
   * {@link CoapMessage}
   */
  public int getMessageType() {
    return this.messageType;
  }

  /**
   * Returns the {@link java.lang.String} representation of this
   * {@link CoapMessage}s type
   *
   * @return the {@link java.lang.String} representation of this
   * {@link CoapMessage}s type
   */
  public String getMessageTypeName() {
    return MessageType.asString(this.messageType);
  }

  /**
   * Returns the number representing the {@link MessageCode} of this
   * {@link CoapMessage}
   *
   * @return the number representing the {@link MessageCode} of this
   * {@link CoapMessage}
   */
  public int getMessageCode() {
    return this.messageCode;
  }

  /**
   * Returns the {@link java.lang.String} representation of this
   * {@link CoapMessage}s code
   *
   * @return the {@link java.lang.String} representation of this
   * {@link CoapMessage}s code
   */
  public String getMessageCodeName() {
    return MessageCode.asString(this.messageCode);
  }

  /**
   * Sets a {@link Token} to this {@link CoapMessage}. However, there is no need
   * to set the {@link Token} manually, as it is set (or overwritten)
   * automatically by the framework.
   *
   * @param token the {@link Token} for this {@link CoapMessage}
   */
  public void setToken(Token token) {
    this.token = token;
  }

  /**
   * Returns the {@link Token} of this {@link CoapMessage}
   *
   * @return the {@link Token} of this {@link CoapMessage}
   */
  public Token getToken() {
    return this.token;
  }

  /**
   * Sets the observing option in this {@link CoapRequest} and returns
   * <code>true</code> if the option is set after method returns (may already
   * have been set beforehand in a prior method invocation) or
   * <code>false</code> if the option is not set, e.g. because that option has
   * no meaning with the message code of this {@link CoapRequest}.
   *
   * @param value <code>true</code> if this {@link CoapRequest} is supposed to
   * register as an observer and <code>false</code> to deregister as observer,
   * i.e. cancel an ongoing observation
   */
  public void setObserve(long value) {
    try {
      this.removeOptions(Option.OBSERVE);
      value = value & 0xFFFFFF;
      this.addUintOption(Option.OBSERVE, value);
    } catch (IllegalArgumentException e) {
      this.removeOptions(Option.OBSERVE);
      Interpreter.getInstance().logInfo("This should never happen." + e);
    }
  }

  /**
   * Returns the value of the observing option (no.6) or
   * {@link UintOptionValue#UNDEFINED} if there is no such option present in
   * this {@link CoapRequest}.
   *
   * @return he value of the observing option (no.6) or
   * {@link UintOptionValue#UNDEFINED} if there is no such option present in
   * this {@link CoapRequest}.
   */
  public long getObserve() {
    if (!options.containsKey(Option.OBSERVE)) {
      return UintOptionValue.UNDEFINED;
    } else {
      return (long) options.get(Option.OBSERVE).getDecodedValue();
    }
  }

  /**
   * Sets the getContent (payload) of this {@link CoapMessage}.
   *
   * @param content {@link ChannelBuffer} containing the message getContent
   * @param contentFormat a long value representing the format of the getContent
   * (see {@link ContentFormat} for some predefined numbers (according to the
   * CoAP specification)
   *
   * @throws java.lang.IllegalArgumentException if the messages code does not
   * allow getContent and for the given {@link ChannelBuffer#readableBytes()} is
   * greater then zero.
   */
  public void setContent(ByteBuf content, long contentFormat)
      throws IllegalArgumentException {

    try {
      this.addUintOption(Option.CONTENT_FORMAT, contentFormat);
      if (!(MessageCode.allowsContent(this.messageCode))
          && content.readableBytes() > 0) {
        throw new IllegalArgumentException(String.format(
            DOES_NOT_ALLOW_CONTENT, this.getMessageCodeName()));
      }

      this.content = content;
    } catch (IllegalArgumentException e) {
      this.content = Unpooled.EMPTY_BUFFER;
      this.removeOptions(Option.CONTENT_FORMAT);
      throw e;
    }
  }

  /**
   * Returns the messages getContent. If the message does not contain any
   * getContent, this method returns an empty
   * {@link ChannelBuffer} ({@link ChannelBuffers#EMPTY_BUFFER}).
   *
   * @return Returns the messages getContent.
   */
  public ByteBuf getContent() {
    return this.content;
  }

  public byte[] contentAsByteArray() {
    byte[] result = new byte[this.contentLength()];
    this.getContent().readBytes(result, 0, this.contentLength());
    return result;
  }

  public int contentLength() {
    return this.content.readableBytes();
  }

  /**
   *
   * @param messageCode
   * @throws IllegalArgumentException
   */
  public void messageCode(int messageCode)
      throws IllegalArgumentException {
    if (!MessageCode.isMessageCode(messageCode)) {
      throw new IllegalArgumentException("Invalid message code no. "
          + messageCode);
    }

    this.messageCode = messageCode;
  }

  /**
   * Returns a {@link Map} with the option numbers as keys and
   * {@link OptionValue}s as values. The returned multimap does not contain
   * options with default values.
   *
   * @return a {@link Map} with the option numbers as keys and
   * {@link OptionValue}s as values.
   */
  public Map<Integer, OptionValue> getAllOptions() {
    return this.options;
  }

  /**
   * Returns the {@link OptionValue}s that are explicitly set in this
   * {@link CoapMessage}. The returned set does not contain options with default
   * values. If this {@link CoapMessage} does not contain any options of the
   * given option number, then the returned set is empty.
   *
   * @param optionNumber the option number
   *
   * @return a {@link Set} containing the {@link OptionValue}s that are
   * explicitly set in this {@link CoapMessage}.
   */
  public OptionValue getOptions(int optionNumber) {
    return this.options.get(optionNumber);
  }

  /**
   * Returns <code>true</code> if an option with the given number is contained
   * in this {@link CoapMessage} and <code>false</code> otherwise.
   *
   * @param optionNumber the option number
   *
   * @return <code>true</code> if an option with the given number is contained
   * in this {@link CoapMessage} and <code>false</code> otherwise.
   */
  public boolean containsOption(int optionNumber) {
    return getOptions(optionNumber) != null;
  }

  @Override
  public int hashCode() {
    return toString().hashCode() + content.hashCode();
  }

  @Override
  public boolean equals(Object object) {

    if (!(object instanceof CoapMessage)) {
      return false;
    }

    CoapMessage other = (CoapMessage) object;

    //Check header fields
    if (this.getProtocolVersion() != other.getProtocolVersion()) {
      return false;
    }

    if (this.getMessageType() != other.getMessageType()) {
      return false;
    }

    if (this.getMessageCode() != other.getMessageCode()) {
      return false;
    }

    if (this.getMessageID() != other.getMessageID()) {
      return false;
    }

    if (!this.getToken().equals(other.getToken())) {
      return false;
    }

    //Iterators iterate over the contained options
    Iterator<Map.Entry<Integer, OptionValue>> iterator1
        = this.getAllOptions().entrySet().iterator();
    Iterator<Map.Entry<Integer, OptionValue>> iterator2
        = other.getAllOptions().entrySet().iterator();

    //Check if both CoAP Messages contain the same options in the same order
    while (iterator1.hasNext()) {

      //Check if iterator2 has no more options while iterator1 has at least one more
      if (!iterator2.hasNext()) {
        return false;
      }

      Map.Entry<Integer, OptionValue> entry1
          = iterator1.next();
      Map.Entry<Integer, OptionValue> entry2
          = iterator2.next();

      if (!entry1.getKey().equals(entry2.getKey())) {
        return false;
      }

      if (!entry1.getValue().equals(entry2.getValue())) {
        return false;
      }
    }

    //Check if iterator2 has at least one more option while iterator1 has no more
    if (iterator2.hasNext()) {
      return false;
    }

    //Check setContent
    return this.getContent().equals(other.getContent());
  }

  @Override
  public String toString() {

    StringBuilder result = new StringBuilder();

    //Header + Token
    result.append("[Header: (V) ").append(getProtocolVersion())
        .append(", (T) ").append(getMessageTypeName())
        .append(", (TKL) ").append(token.getBytes().length)
        .append(", (C) ").append(getMessageCodeName())
        .append(", (ID) ").append(getMessageID())
        .append(" | (Token) ").append(token).append(" | ");

    //Options
    result.append("Options:");
    for (int optionNumber : getAllOptions().keySet()) {
      result.append(" (No. ").append(optionNumber).append(") ");
      OptionValue optionValue = this.getOptions(optionNumber);
      result.append(optionValue.toString());
    }
    result.append(" | ");

    //Content
    result.append("Content: ");
    long payloadLength = getContent().readableBytes();
    if (payloadLength == 0) {
      result.append("<no content>]");
    } else {
      result.append(getContent().toString(0,
          Math.min(getContent().readableBytes(), 20),
          CoapMessage.CHARSET)).append("... ( ")
          .append(payloadLength).append(" bytes)]");
    }

    return result.toString();

  }
}
