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
import java.io.IOException;

import jolie.net.coap.codec.CoapMessageDecoder;
import jolie.net.coap.codec.CoapMessageEncoder;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

public class CoapProtocol extends AsyncCommProtocol {

    public boolean isInput;

    public CoapProtocol(VariablePath configurationPath, boolean isInput) {
	super(configurationPath);
	this.isInput = isInput;
    }

    @Override
    public void setupPipeline(ChannelPipeline pipeline) {
	pipeline.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
	pipeline.addLast("ENCODER", new CoapMessageEncoder());
	pipeline.addLast("DECODER", new CoapMessageDecoder());
	pipeline.addLast("CODEC", new CoapCodecHandler(this));
    }

    @Override
    public String name() {
	return "coap";
    }

    @Override
    public boolean isThreadSafe() {
	return false;
    }

    public Type getSendType(CommMessage message)
	    throws IOException {
	Type ret = null;

	if (channel().parentPort() == null) {
	    throw new IOException("Could not retrieve communication "
		    + "port for Coap protocol");
	}

	OperationTypeDescription opDesc = channel().parentPort()
		.getOperationTypeDescription(message.operationName(), "/");

	if (opDesc == null) {
	    return null;
	}

	if (opDesc.asOneWayTypeDescription() != null) {
	    if (message.isFault()) {
		ret = Type.UNDEFINED;
	    } else {
		OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
		ret = ow.requestType();
	    }
	} else if (opDesc.asRequestResponseTypeDescription() != null) {
	    RequestResponseTypeDescription rr
		    = opDesc.asRequestResponseTypeDescription();
	    if (message.isFault()) {
		ret = rr.getFaultType(message.fault().faultName());
		if (ret == null) {
		    ret = Type.UNDEFINED;
		}
	    } else {
		ret = (channel().parentPort() instanceof InputPort)
			? rr.responseType() : rr.requestType();
	    }
	}

	return ret;
    }

    public boolean _checkBooleanParameter(String param) {
	return this.checkBooleanParameter(param);
    }

    public boolean _hasOperationSpecificParameter(String operationName,
	    String param) {
	return this.hasOperationSpecificParameter(operationName, param);
    }

    public String _getOperationSpecificStringParameter(String operationName,
	    String param) {
	return this.getOperationSpecificStringParameter(operationName, param);
    }
}
