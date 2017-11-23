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
package jolie.net.coap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.options.EmptyOptionValue;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.options.OpaqueOptionValue;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.message.Token;
import jolie.net.coap.options.UintOptionValue;

import jolie.Interpreter;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;

public class CoapMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in,
      List<Object> out) throws Exception {

    CoapMessage msg = decode_internal(in);
    if (msg != null) {
      out.add(msg);
    } else {
      throw new Exception("Marlformed Encoded CoAP messages! In order"
          + "to accept it try setting protocol parameter \"strict\" to false ");
    }
  }

  private CoapMessage decode_internal(ByteBuf in) {

    if (in.readableBytes() < 4) {
      Interpreter.getInstance().logSevere("Encoded CoAP messages MUST have "
          + "min. 4 bytes. This has " + in.readableBytes() + "!");
      return null;
    }

    //Decode the header values
    int encodedHeader = in.readInt();
    int version = (encodedHeader >>> 30) & 0x03;
    int messageType = (encodedHeader >>> 28) & 0x03;
    int tokenLength = (encodedHeader >>> 24) & 0x0F;
    int messageCode = (encodedHeader >>> 16) & 0xFF;
    int messageId = (encodedHeader) & 0xFFFF;

    if (version != CoapMessage.PROTOCOL_VERSION) {
      Interpreter.getInstance().logSevere("Coap Message Decoder >>> "
          + "CoAP Message with \"version\": " + version
          + " other than \"1\", is invalid!");
      return null;
    }

    if (tokenLength > CoapMessage.MAX_TOKEN_LENGTH) {
      Interpreter.getInstance().logSevere("Coap Message Decoder >>> "
          + "Coap Message with Token Length: " + tokenLength
          + " larger than 8, is invalid!");
      return null;
    }

    if (in.readableBytes() < tokenLength) {
      Interpreter.getInstance().logSevere("Coap Message Decoder >>> "
          + "Coap Message with Token Length: " + tokenLength
          + " and readable " + in.readableBytes() + " bytes left, is invalid!");
      return null;
    }

    if (messageCode == MessageCode.EMPTY) {

      if (messageType == MessageType.ACK) {
        return CoapMessage.createEmptyAcknowledgement(messageId);
      } else if (messageType == MessageType.RST) {
        return CoapMessage.createEmptyReset(messageId);
      } else if (messageType == MessageType.CON) {
        return CoapMessage.createPing(messageId);
      } else {
        Interpreter.getInstance().logSevere("Coap Message Decoder >>> "
            + "Coap Message with Message Code: \"" + messageCode + "\" and "
            + "Message Type: \"" + messageCode + "\" is invalid!");
      }
    }

    byte[] token = new byte[tokenLength];
    in.readBytes(token);

    CoapMessage coapMessage;
    if (MessageCode.isRequest(messageCode)) {
      coapMessage = new CoapRequest(messageType, messageCode);
    } else {
      coapMessage = new CoapResponse(messageType, messageCode);
      coapMessage.setMessageType(messageType);
    }

    coapMessage.setMessageId(messageId);
    coapMessage.setToken(new Token(token));

    if (in.readableBytes() > 0) {
      try {
        setOptions(coapMessage, in);
      } catch (OptionCodecException ex) {
        Interpreter.getInstance().logSevere(ex);
        return null;
      }
    }

    in.discardReadBytes();
    try {
      coapMessage.setContent(in.retain());
    } catch (IllegalArgumentException ex) {
      Interpreter.getInstance().logSevere(ex);
      return null;
    }

    return coapMessage;
  }

  private void setOptions(CoapMessage coapMessage, ByteBuf bb)
      throws OptionCodecException {

    int previousOptionNumber = 0;
    int firstByte = bb.readByte() & 0xFF;

    while (firstByte != 0xFF && bb.readableBytes() >= 0) {

      int optionDelta = (firstByte & 0xF0) >>> 4;
      int optionLength = firstByte & 0x0F;

      if (optionDelta == 13) {
        optionDelta += bb.readByte() & 0xFF;
      } else if (optionDelta == 14) {
        optionDelta = 269 + ((bb.readByte() & 0xFF) << 8)
            + (bb.readByte() & 0xFF);
      }

      if (optionLength == 13) {
        optionLength += bb.readByte() & 0xFF;
      } else if (optionLength == 14) {
        optionLength = 269 + ((bb.readByte() & 0xFF) << 8)
            + (bb.readByte() & 0xFF);
      }

      int actualOptionNumber = previousOptionNumber + optionDelta;

      byte[] optionValue = new byte[optionLength];
      bb.readBytes(optionValue);

      switch (OptionValue.getType(actualOptionNumber)) {
        case EMPTY: {
          EmptyOptionValue value
              = new EmptyOptionValue(actualOptionNumber);
          coapMessage.addOption(actualOptionNumber, value);
          break;
        }
        case OPAQUE: {
          OpaqueOptionValue value
              = new OpaqueOptionValue(actualOptionNumber,
                  optionValue);
          coapMessage.addOption(actualOptionNumber, value);
          break;
        }
        case STRING: {
          StringOptionValue value
              = new StringOptionValue(actualOptionNumber,
                  optionValue, true);
          coapMessage.addOption(actualOptionNumber, value);
          break;
        }
        case UINT: {
          UintOptionValue value
              = new UintOptionValue(actualOptionNumber,
                  optionValue, true);
          coapMessage.addOption(actualOptionNumber, value);
          break;
        }
        default: {
          throw new OptionCodecException(actualOptionNumber);
        }
      }

      previousOptionNumber = actualOptionNumber;

      if (bb.readableBytes() > 0) {
        firstByte = bb.readByte() & 0xFF;
      } else {
        firstByte = 0xFF;
      }
    }
  }

  private static String toBinaryString(int byteValue) {
    StringBuilder buffer = new StringBuilder(8);

    for (int i = 7; i >= 0; i--) {
      if ((byteValue & (int) Math.pow(2, i)) > 0) {
        buffer.append("1");
      } else {
        buffer.append("0");
      }
    }

    return buffer.toString();
  }
}
