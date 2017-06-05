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
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
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

    // da github di waylau
    private static final int READ_IDEL_TIME_OUT = 4;
    private static final int WRITE_IDEL_TIME_OUT = 5;
    private static final int ALL_IDEL_TIME_OUT = 7;

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
     * pipeline for Mqtt use Encoder and Decoder, we added { @link MqttProtocolInboundHandler
     * }
     *
     * @param pipeline the pipeline to fill with specific protocol handlers
     */
    @Override
    public void setupPipeline(ChannelPipeline pipeline) {

        pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
        pipeline.addLast("MqttDecoder", new MqttDecoder());
        pipeline.addLast("MqttEncoder", MqttEncoder.INSTANCE);
        pipeline.addLast("IdleState", new IdleStateHandler(
                READ_IDEL_TIME_OUT,
                WRITE_IDEL_TIME_OUT,
                ALL_IDEL_TIME_OUT,
                TimeUnit.SECONDS
        ));
        pipeline.addLast("MqttOnActive", new MqttOnActiveHandler());

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
