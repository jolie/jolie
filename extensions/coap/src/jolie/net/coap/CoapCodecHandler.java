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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

import java.util.List;
import jolie.net.CommMessage;

import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.runtime.Value;

public class CoapCodecHandler
	extends MessageToMessageCodec<CoapMessage, CommMessage> {

    private boolean input;

    public CoapCodecHandler(boolean input) {
	this.input = input;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx,
	    CommMessage in, List<Object> out) throws Exception {

	if (input) {

	} else {
	    //output port - OW and RR

	    // CREATE COMM MESSAGE FROM COAP MESSAGE
	    String payload = in.value().strValue();
	    CoapMessage msg = new CoapMessage(MessageType.NON, MessageCode.POST) {
	    };
	    msg.setContent(Unpooled.wrappedBuffer(payload.getBytes(CharsetUtil.UTF_8)));
	    //CoapMessage empty = CoapMessage.createEmptyAcknowledgement((int) in.id());
	    out.add(msg);
	    // SEND THE ACK back to CommCore
	    ctx.pipeline().fireChannelRead(new CommMessage(
		    in.id(), in.operationName(),
		    "/", Value.create(), null));
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
	    CoapMessage in, List<Object> out) throws Exception {
	System.out.println("Message received!" + in.toString());
    }
}
