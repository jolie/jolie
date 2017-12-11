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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import jolie.net.coap.communication.codec.CoapMessageDecoder;
import jolie.net.coap.communication.codec.CoapMessageEncoder;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
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
  public void setupPipeline(ChannelPipeline p) {
//    p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
    p.addLast("DECODER", new CoapMessageDecoder());
    p.addLast("ENCODER", new CoapMessageEncoder());
    p.addLast("CODEC", new CoapToCommMessageCodec(this));
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
  public ValueVector getOperationSpecificParameterVector(String operationName,
      String parameterName) {
    return super.getOperationSpecificParameterVector(operationName,
        parameterName);
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

  private String valueToPrettyString(Value request) {
    Writer writer = new StringWriter();
    ValuePrettyPrinter printer = new ValuePrettyPrinter(request, writer, "");
    try {
      printer.run();
    } catch (IOException e) {
    } // Should never happen
    return writer.toString();

  }

  /**
   * Given the <code>alias</code> for an operation, it searches iteratively in
   * the <code>configurationPath</code> of the {@link AsyncCommProtocol} to find
   * the corresponsding <code>operationName</code>.
   *
   * @param alias the alias for the wanted operation
   * @return The operation name String
   */
  public String getOperationFromOperationSpecificStringParameter(String parameter,
      String parameterStringValue) {

    for (Map.Entry<String, ValueVector> first : configurationPath().getValue().children().entrySet()) {
      String first_level_key = first.getKey();
      ValueVector first_level_valueVector = first.getValue();
      if (first_level_key.equals("osc")) {
        for (Iterator<Value> first_iterator = first_level_valueVector.iterator(); first_iterator.hasNext();) {
          Value fisrt_value = first_iterator.next();
          for (Map.Entry<String, ValueVector> second : fisrt_value.children().entrySet()) {
            String second_level_key = second.getKey();
            ValueVector second_level_valueVector = second.getValue();
            for (Iterator<Value> second_iterator = second_level_valueVector.iterator(); second_iterator.hasNext();) {
              Value second_value = second_iterator.next();
              for (Map.Entry<String, ValueVector> third : second_value.children().entrySet()) {
                String third_level_key = third.getKey();
                ValueVector third_level_valueVector = third.getValue();
                if (third_level_key.equals(parameter)) {
                  StringBuilder sb = new StringBuilder("");
                  for (Iterator<Value> third_iterator = third_level_valueVector.iterator(); third_iterator.hasNext();) {
                    Value third_value = third_iterator.next();
                    sb.append(third_value.strValue());
                  }
                  if (sb.toString().equals(parameterStringValue)) {
                    return second_level_key;
                  }
                }
              }
            }
          }
        }
      }
    }
    return parameterStringValue;
  }
}
