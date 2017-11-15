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

import java.io.IOException;
import java.util.Map;

import jolie.net.coap.CoapToCommMessageCodec;
import jolie.net.coap.CoapMessageDecoder;
import jolie.net.coap.CoapMessageEncoder;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

/**
 * Implementations of {@link AsyncCommProtocol} CoAP for Jolie.
 *
 * @author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol {

  public boolean isInput;

  /**
   *
   * @param configurationPath
   * @param isInput
   */
  public CoapProtocol(VariablePath configurationPath, boolean isInput) {
    super(configurationPath);
    this.isInput = isInput;
  }

  @Override
  public void setupPipeline(ChannelPipeline pipeline) {
    pipeline.addLast("DECODER", new CoapMessageDecoder());
    pipeline.addLast("ENCODER", new CoapMessageEncoder());
    pipeline.addLast("CODEC", new CoapToCommMessageCodec(this));
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
   * Retrieves the send {@link Type}, {@link OneWayTypeDescription} or
   * {@link RequestResponseTypeDescription}, for the {@link InputPort}, it
   * searches iteratively in the parent port interface, looking for
   * {@link OperationTypeDescription}.
   *
   * @param operationName the operation name {@link String}
   * @return Type The type for the specified operation.
   * @throws IOException If the communicatino port is not reachable.
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

  /**
   * Given the <code>alias</code> for an operation, it searches iteratively in
   * the <code>configurationPath</code> of the {@link AsyncCommProtocol} to find
   * the corresponsding <code>operationName.</code>.
   *
   * @param alias the alias for the wanted operation
   * @return The operation name String
   */
  public String getOperationFromAlias(String alias) {

    if (configurationPath().getValue().hasChildren("osc")) {
      for (Map.Entry<String, ValueVector> i : configurationPath().getValue()
          .getFirstChild("osc").children().entrySet()) {
        for (Map.Entry<String, ValueVector> j : i.getValue().first().children()
            .entrySet()) {
          if (j.getKey().equals("alias") && j.getValue().first().strValue()
              .equals(alias)) {
            return i.getKey();
          }
        }
      }
    }
    // else we return directly the topic
    return alias;
  }
}
