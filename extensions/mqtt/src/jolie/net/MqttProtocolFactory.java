/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jolie.net;

import java.io.IOException;
import java.net.URI;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.VariablePath;

/**
 * For future development of extensions: Create MqttProtocolFactory called by Jolie Class Loader,
 * update file manifest.mf with value X-JOLIE-ProtocolExtension: mqtt:jolie.net.MqttProtocolFactory
 *
 * @author stefanopiozingaro
 */
public class MqttProtocolFactory extends CommProtocolFactory {

  /**
   *
   * @param commCore CommCore
   */
  public MqttProtocolFactory(CommCore commCore) {
    super(commCore);
  }

  /**
   * This is a subscriber, it could be a One Way or a Request Response
   *
   * @param configurationPath VariablePath
   * @param location URI
   * @return CommProtocol
   * @throws IOException
   */
  @Override
  public CommProtocol createInputProtocol(VariablePath configurationPath, URI location)
          throws IOException {
    System.out.println(location.toString());
    return new MqttProtocol(Boolean.TRUE, configurationPath);
  }

  /**
   * This is a Publisher, just a One Way Publisher
   *
   * @param configurationPath VariablePath
   * @param location URI
   * @return CommProtocol
   * @throws IOException
   */
  @Override
  public CommProtocol createOutputProtocol(VariablePath configurationPath, URI location)
          throws IOException {
    return new MqttProtocol(Boolean.FALSE, configurationPath);
  }
}
