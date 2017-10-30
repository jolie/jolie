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
import java.net.InetSocketAddress;

import java.util.List;
import jolie.Interpreter;

import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.options.EmptyOptionValue;
import jolie.net.coap.message.MessageCode;
import static jolie.net.coap.message.MessageCode.BAD_OPTION_402;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.options.OpaqueOptionValue;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.message.Token;
import jolie.net.coap.options.Option;
import jolie.net.coap.options.UintOptionValue;
import jolie.runtime.FaultException;

public class CoapMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in,
      List<Object> out) throws Exception {

    CoapMessage coapMessage;

    //Decode the Message Header which must have a length of exactly 4 bytes
    if (in.readableBytes() < 4) {
      throw new FaultException("Encoded CoAP messages MUST have min. 4 bytes."
          + " This has " + in.readableBytes() + "!");
    }

    //Decode the header values
    int encodedHeader = in.readInt();
    int version = (encodedHeader >>> 30) & 0x03;
    int messageType = (encodedHeader >>> 28) & 0x03;
    int tokenLength = (encodedHeader >>> 24) & 0x0F;
    int messageCode = (encodedHeader >>> 16) & 0xFF;
    int messageID = (encodedHeader) & 0xFFFF;

    //Check whether the protocol version is supported (=1)
    if (version != CoapMessage.PROTOCOL_VERSION) {
      throw new FaultException("CoAP version (" + version + ") is other "
          + "than \"1\"!");
    }

    //Check whether TKL indicates a not allowed token length
    if (tokenLength > CoapMessage.MAX_TOKEN_LENGTH) {
      throw new FaultException("TKL value (" + tokenLength + ") is larger "
          + "than 8!");
    }

    //Check whether there are enough unread bytes left to read the token
    if (in.readableBytes() < tokenLength) {
      throw new FaultException("TKL value is " + tokenLength + " but only "
          + in.readableBytes() + " bytes left!");
    }

    //Handle empty message (ignore everything but the first 4 bytes)
    if (messageCode == MessageCode.EMPTY) {

      if (messageType == MessageType.ACK) {
        coapMessage = CoapMessage.createEmptyAcknowledgement(messageID);
      } else if (messageType == MessageType.RST) {
        coapMessage = CoapMessage.createEmptyReset(messageID);
      } else if (messageType == MessageType.CON) {
        coapMessage = CoapMessage.createPing(messageID);
      } else {
        throw new FaultException("Empty NON messages are invalid!");
      }
    } else {

      //Read the token
      byte[] token = new byte[tokenLength];
      in.readBytes(token);

      //Handle non-empty messages (CON, NON or ACK)
      if (MessageCode.isRequest(messageCode)) {
        coapMessage = new CoapRequest(messageType, messageCode);
      } else {
        coapMessage = new CoapResponse(messageType, messageCode);
      }

      coapMessage.setMessageID(messageID);
      Token t = new Token(token);
      coapMessage.setToken(t);

      //Decode and set the options
      if (in.readableBytes() > 0) {
        setOptions(coapMessage, in);
      }

      //The remaining bytes (if any) are the messages payload. 
      //If there is no payload, reader and writer index are
      //at the same position (buf.readableBytes() == 0).
      in.discardReadBytes();
      try {
        coapMessage.setContent(in);
      } catch (IllegalArgumentException ex) {
        throw new FaultException(ex);
      }
    }

    Interpreter.getInstance().logWarning("Decoded Message: " + coapMessage);
    out.add(coapMessage);
  }

  private void setOptions(CoapMessage coapMessage, ByteBuf bb)
      throws OptionCodecException {

    //Decode the options
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

      try {
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
          default:
            throw new RuntimeException("This should never happen!");
        }
      } catch (IllegalArgumentException e) {
        //failed option creation leads to an illegal argument exception
        Interpreter.getInstance().logWarning("Exception while decoding "
            + "option!");

        if (MessageCode.isResponse(coapMessage.getMessageCode())) {
          //Malformed options in responses are silently ignored...
          Interpreter.getInstance().logWarning("Silently ignore malformed "
              + "option no. " + actualOptionNumber + " in inbound response.");
        } else if (Option.isCritical(actualOptionNumber)) {
          //Critical malformed options in requests cause an exception
          throw new OptionCodecException(actualOptionNumber);
        } else {
          //Not critical malformed options in requests are silently ignored...
          Interpreter.getInstance().logWarning("Silently ignore elective option "
              + " no. " + actualOptionNumber + " in inbound request.");
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

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {

    //Invalid Header Exceptions cause a RST
    if (cause instanceof HeaderDecodingException) {
      HeaderDecodingException ex = (HeaderDecodingException) cause;

      if (ex.getMessageID() != CoapMessage.UNDEFINED_MESSAGE_ID) {
        writeReset(ctx, ex.getMessageID());
      } else {
        Interpreter.getInstance().logWarning("Ignore inbound message with "
            + "malformed header...");
      }
    } else if (cause instanceof OptionCodecException) {
      OptionCodecException ex = (OptionCodecException) cause;
      int messageType = ex.getMessageType() == MessageType.CON
          ? MessageType.ACK : MessageType.NON;

      writeBadOptionResponse(ctx, messageType, ex.getMessage());
    } else {
      ctx.fireExceptionCaught(cause);
    }
  }

  private void writeReset(ChannelHandlerContext ctx, int messageID) {
    CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
    ctx.write(resetMessage);
  }

  private void writeBadOptionResponse(ChannelHandlerContext ctx,
      int messageType, String message) {
    CoapResponse errorResponse
        = CoapResponse.createErrorResponse(messageType, BAD_OPTION_402,
            message);
    ctx.write(errorResponse);
  }
}
