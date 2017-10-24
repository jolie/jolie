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

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DatagramPacketDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.IOException;

import jolie.net.coap.CoapCodecHandler;
import jolie.net.coap.CoapMessageDecoder;
import jolie.net.coap.CoapMessageEncoder;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;

import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

/**
 * Implementations of Async Comm Protocol COstrained APplications (http over
 * UDP) for IoT
 *
 * @author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol {

  public boolean isInput;

  /**
   *
   * @param configurationPath VariablePath
   * @param isInput boolean
   */
  public CoapProtocol(VariablePath configurationPath, boolean isInput) {
    super(configurationPath);
    this.isInput = isInput;
  }

  @Override
  public void setupPipeline(ChannelPipeline pipeline) {
    pipeline.addLast("UDP DECODER", new DatagramPacketDecoder(
        new CoapMessageDecoder()));
    pipeline.addLast("ENCODER", new CoapMessageEncoder());
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

  @Override
  public boolean checkBooleanParameter(String param) {
    return super.checkBooleanParameter(param);
  }

  @Override
  public boolean hasOperationSpecificParameter(String on, String p) {
    return super.hasOperationSpecificParameter(on, p);
  }

  @Override
  public String getOperationSpecificStringParameter(String on, String p) {
    return super.getOperationSpecificStringParameter(on, p);
  }

  @Override
  public Value getOperationSpecificParameterFirstValue(String on,
      String p) {
    return super.getOperationSpecificParameterFirstValue(on, p);
  }

  @Override
  public boolean checkStringParameter(String id, String value) {
    return super.checkStringParameter(id, value);
  }

  @Override
  public CommChannel channel() {
    return super.channel();
  }

  /**
   * Retrieve Send Type for the CommMessage in input. TODO: move to super class
   *
   * @param operationName String
   * @return Type
   * @throws IOException
   */
  public Type getSendType(String operationName)
      throws IOException {

    Type ret = null;

    if (channel().parentPort() == null) {
      throw new IOException("Could not retrieve communication "
          + "port for " + this.name() + " protocol");
    }

    OperationTypeDescription opDesc = channel().parentPort()
        .getOperationTypeDescription(operationName, "/");

    if (opDesc == null) {
      return null;
    }

    if (opDesc.asOneWayTypeDescription() != null) {
      OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
      ret = ow.requestType();
    } else if (opDesc.asRequestResponseTypeDescription() != null) {
      RequestResponseTypeDescription rr
          = opDesc.asRequestResponseTypeDescription();
      ret = (channel().parentPort() instanceof InputPort)
          ? rr.responseType() : rr.requestType();
    }

    return ret;
  }
}
