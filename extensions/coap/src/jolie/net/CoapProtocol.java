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

import jolie.net.coap.CoapCodecHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import jolie.net.coap.codec.CoapMessageDecoder;
import jolie.net.coap.codec.CoapMessageEncoder;
import jolie.net.protocols.AsyncCommProtocol;
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
	return false;
    }
}
