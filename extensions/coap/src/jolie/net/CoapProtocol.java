/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   
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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.util.List;

import jolie.net.coap.CoapMessage;
import jolie.net.coap.CoapMessageDecoder;
import jolie.net.coap.CoapMessageEncoder;
import jolie.net.coap.MessageCode;
import jolie.net.coap.MessageType;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class CoapProtocol extends AsyncCommProtocol {

    private final boolean isInput;

    public CoapProtocol(VariablePath configurationPath, boolean isInput) {
	super(configurationPath);
	this.isInput = isInput;
    }

    @Override
    public void setupPipeline(ChannelPipeline pipeline) {
	pipeline.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
	pipeline.addLast("ENCODER", new CoapMessageEncoder());
	pipeline.addLast("DECODER", new CoapMessageDecoder());
	pipeline.addLast("CODEC", new CoapCodecHandler(isInput));
    }

    @Override
    public String name() {
	return "coap";
    }

    @Override
    public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
    }

    static class Parameters {

	private static String CONCURRENT = "concurrent";

    }

    private static class CoapCodecHandler
	    extends MessageToMessageCodec<CoapMessage, CommMessage> {

	private boolean input;

	private CoapCodecHandler(boolean input) {
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

}
