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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;

/**
 * This Class extends { @link ChannelInboundHanlderAdapter }
 * in order to override the 2 basic methods of Inbound Handlers: null {@link channelRead }
 * {@link channelInactive }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
class MqttProtocolHandler extends ChannelInboundHandlerAdapter {

    private MqttProtocolClient mpc;
    private ICallback callback;
    private boolean connectionLost;

    /**
     * The channelRead that provides parameters to read from the channel inbound
     * buffer the MqttMessage arriving, calls messageReceived from the { @link MqttProtocolClient
     * } implementation
     *
     * @param ctx is the Context of the handler
     * @param message the Object (MqttMessage in this case) arriving
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        if (this.callback != null) {
            this.callback.call(msg);
        }
    }

    /**
     * The Channel Inactive capture the inactivness of the connection with the
     * broker TODO implement a way to keep alive the connection
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.setConnectionLost(true);
        ctx.close(/* false */);
    }

    /**
     * Interface to implement the callback for the arriving of { @link MqttMessage
     * }  TODO change it in Future Listener
     */
    public interface ICallback {

        void call(MqttMessage msg);
    }

    public MqttProtocolClient getMpc() {
        return mpc;
    }

    public void setMpc(MqttProtocolClient mpc) {
        this.mpc = mpc;
    }

    public ICallback getCallback() {
        return callback;
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }

    public boolean isConnectionLost() {
        return connectionLost;
    }

    public void setConnectionLost(boolean connectionLost) {
        this.connectionLost = connectionLost;
    }

}
