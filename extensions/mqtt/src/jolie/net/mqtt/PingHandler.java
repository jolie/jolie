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
package jolie.net.mqtt;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;

/**
 *
 * @author stefanopiozingaro
 */
public class PingHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

	if (evt instanceof IdleStateEvent) {
	    IdleStateEvent event = (IdleStateEvent) evt;
	    switch (event.state()) {
		case READER_IDLE:
		    break;
		case WRITER_IDLE:
		    MqttFixedHeader fixedHeader = new MqttFixedHeader(
			    MqttMessageType.PINGREQ,
			    false,
			    MqttQoS.AT_MOST_ONCE,
			    false,
			    0);
		    ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
	    }
	}
    }
}
