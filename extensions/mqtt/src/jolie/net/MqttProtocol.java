package jolie.net;

/**
 * MQTT protocol implementation
 * Released Under Creative Common License
 * @author Stefano Pio Zingaro on 24th May 2017
 */
public class MqttProtocol {
    /**
     *
     * TODO
     * Write a runnable example using netty library iplementing
     * an MQTT sender using channel and pipelines:
     * 1. Create a socket channel
     * 2. Get the pipeline from the created channel
     * 3. Add the following steps to the pipeline using addLast method,
     *  for each method simply print the current status:
     *  3.1 Handle the socket creation event (e.g. new SocketHandler)
     *  3.2 Handle the Netty message to Mqtt message codec event
     *      (extends MessageToMessageCodec<NettyMsg,MqttMsg>) and
     *      encode or decode the event, finally out.add(message)
     *  3.3 Handle the outbound of a message event (override write())
     *  3.4 Handle the event that close the channel
     *
     */


}