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

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

/**
 * Implementation of the { @link AsyncCommProtocol } for MQTT protocol relying
 * on TCP/IP socket, uses netty and Non blocking Sockets
 *
 * TODO MOdificare { @link CommCore} 1. in caso di una InputPort: 1.1 in caso di
 * protocollo PublishSubscribeProtocol (e.g. MqttProtocol extends
 * PublishSubscribeProtocol) si dovrà creare un CommChannel (che rimarrà aperto)
 * 2.1 altrimenti creò SocketListener e faccio la solita roba
 *
 * @author stefanopiozingaro
 */
public class MqttProtocol extends AsyncCommProtocol {

    /*
    Instance of the mqtt handler that will taje care of everything
     */
    final MqttProtocolHandler handler = new MqttProtocolHandler();

    /**
     * Default Constructor for MqttProtocol going super Look at the { @link
     * HttpProtocol.java} one
     *
     * @param configurationPath
     */
    public MqttProtocol(VariablePath configurationPath) {
        super(configurationPath);
    }

    /*
     * Inner class Parameters inherit from { @link HttpProtocol }
     * Since now (01/06/2017) we use only the concurrent param
     */
    private static class Parameters {

        private static final String CONCURRENT = "concurrent";

    }

    /**
     * Method overrinding setupPipeline of { @link AsyncCommProtocol } Default
     * pipeline for Mqtt use Encoder and Decoder, we added { @link MqttProtocolHandler
     * }
     *
     * @param pipeline the pipeline to fill with specific protocol handlers
     */
    @Override
    public void setupPipeline(ChannelPipeline pipeline) {

        pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
        pipeline.addLast("decoder", new MqttDecoder());
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);
        pipeline.addLast("handler", handler);
    }

    /**
     *
     * @return the name of the protocol, in which case is mqtt
     */
    @Override
    public String name() {
        return "mqtt";
    }

    /**
     *
     * @return if the behaviour is concurrent or not (i guess @author
     * stefanopiozingaro)
     */
    @Override
    public boolean isThreadSafe() {
        return checkBooleanParameter(Parameters.CONCURRENT);
    }
}
