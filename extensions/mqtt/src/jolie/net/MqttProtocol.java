/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either mqttVersion 3 of the License, or
 * (at your option) any later mqttVersion.
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

import jolie.Interpreter;
import jolie.net.mqtt.PublishHandler;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 *
 * @author stefanopiozingaro
 */
public class MqttProtocol extends AsyncCommProtocol {

  private static final MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
  private final List<MqttPublishMessage> pendingPublishes;
  private final List<MqttSubscribeMessage> pendingSubscriptions;
  private final Map<String, PublishHandler> subscriptions;
  private final boolean inInputPort;

  private static class Parameters {

	private static final String BROKER = "broker";
	private static final String CONCURRENT = "concurrent";
	private static final String ALIAS = "alias";
	private static final String QOS = "qos"; // TODO test
	private static final String KEEP_ALIVE = "keepAlive"; // TODO test
	private static final String WILL_TOPIC = "willTopic";
	private static final String WILL_MESSAGE = "willMessage";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String CLIENT_ID = "clientId";
  }

  /**
   * The only variable that is accessible from the outside is the list of subscriptions for the specific topic, no duplicates!
   *
   * @return HashMap
   */
  public Map<String, PublishHandler> getSubscriptions() {
	return subscriptions;
  }

  /**
   * Constructor of the Mqtt Protocol class
   *
   * @param inInputPort boolean
   * @param configurationPath VariablePath
   */
  public MqttProtocol(boolean inInputPort, VariablePath configurationPath) {

	super(configurationPath);
	this.pendingPublishes = new ArrayList<>();
	this.pendingSubscriptions = new ArrayList<>();
	this.subscriptions = new HashMap<>();
	this.inInputPort = inInputPort;
  }

  @Override
  public void setupPipeline(ChannelPipeline pipeline) {
	pipeline.addLast(new LoggingHandler(LogLevel.INFO));
	pipeline.addLast(MqttEncoder.INSTANCE);
	pipeline.addLast(new MqttDecoder());
	pipeline.addLast("Ping", new MqttPingHandler());
	pipeline.addLast(new MqttCommMessageCodec());
  }

  @Override
  public String name() {
	return "mqtt";
  }

  @Override
  public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
  }

  /**
   *
   * @return MqttConnectMessage
   */
  public MqttConnectMessage buildConnect() {

	String clientId = getStringParameter(Parameters.CLIENT_ID, "jolie/" + (int) (Math.random() * 65536));
	String willTopic = getStringParameter(Parameters.WILL_TOPIC);
	String willMessage = getStringParameter(Parameters.WILL_MESSAGE);
	String mqttUserName = getStringParameter(Parameters.USERNAME);
	String mqttPassword = getStringParameter(Parameters.PASSWORD);
	boolean isDup = Boolean.FALSE; // TODO check
	boolean isConnectRetain = Boolean.FALSE; // TODO check

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, isDup, MqttQoS.AT_MOST_ONCE, isConnectRetain, 0);
	MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
			mqttVersion.protocolName(),
			mqttVersion.protocolLevel(),
			checkBooleanParameter(mqttUserName, false),
			checkBooleanParameter(mqttPassword, false),
			Boolean.FALSE, // TODO check
			MqttQoS.AT_MOST_ONCE.value(),
			checkBooleanParameter(willTopic, false),
			Boolean.FALSE, // TODO check
			2
	);
	MqttConnectPayload payload = new MqttConnectPayload(clientId, willTopic, willMessage, mqttUserName, mqttPassword);
	return new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
  }

  /**
   *
   * @param messageId
   * @param topic String
   * @param message String
   * @return
   */
  public MqttPublishMessage buildPublication(long messageId, String topic, Object message) {

	boolean isDup = Boolean.FALSE;
	MqttQoS publishQoS = MqttQoS.AT_LEAST_ONCE;
	boolean isConnectRetain = Boolean.FALSE;

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, publishQoS, isConnectRetain, 0);
	MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, (int) messageId);
	ByteBuf payload = parseObject(message);
	MqttPublishMessage mpm = new MqttPublishMessage(mqttFixedHeader, variableHeader, payload);

	return mpm;
  }

  public MqttSubscribeMessage buildSubscription(long messageId, List<MqttTopicSubscription> topics, PublishHandler handler) {

	boolean isDup = Boolean.FALSE;
	MqttQoS subscribeQoS = MqttQoS.AT_LEAST_ONCE;
	boolean isSubscribeRetain = Boolean.FALSE;

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, isDup, subscribeQoS, isSubscribeRetain, 0);
	MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload payload = new MqttSubscribePayload(topics);
	MqttSubscribeMessage msm = new MqttSubscribeMessage(mqttFixedHeader, variableHeader, payload);

	return msm;
  }

  private ByteBuf parseObject(Object message) {
	ByteBuf bb = Unpooled.buffer();
	if (message instanceof String) {
	  String msg = (String) message;
	  bb = Unpooled.copiedBuffer(msg.getBytes(CharsetUtil.UTF_8));
	}
	return bb;
  }

  private static class MqttPingHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {

	  if (evt instanceof IdleStateEvent) {
		IdleStateEvent event = (IdleStateEvent) evt;
		switch (event.state()) {
		  case READER_IDLE:
		  case WRITER_IDLE:
		  case ALL_IDLE:
			MqttFixedHeader fixedHeader = new MqttFixedHeader(
					MqttMessageType.PINGREQ,
					false,
					MqttQoS.AT_MOST_ONCE,
					false,
					0
			);
			ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
			break;
		  default:
			Interpreter.getInstance().logSevere("State: " + event.state());
		}
	  }
	}
  }

  private class MqttCommMessageCodec extends MessageToMessageCodec<MqttMessage, CommMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, CommMessage message, List<Object> out)
			throws Exception {

	  ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
			  ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	  if (inInputPort) {

		List<MqttTopicSubscription> topics = Collections.singletonList(new MqttTopicSubscription("jolie/temperature/request", MqttQoS.AT_LEAST_ONCE));
		PublishHandler handler = new PublishHandler() {
		  @Override
		  public void handleMessage(String topic, ByteBuf payload) {
			// distinguish the two case of one way subscriber or request response
			Interpreter.getInstance().logInfo(Unpooled.copiedBuffer(payload).toString(CharsetUtil.UTF_8));
		  }
		};

		pendingSubscriptions.add(buildSubscription(message.id(), topics, handler));
		for (ListIterator<MqttTopicSubscription> i = topics.listIterator(); i.hasNext();) {
		  subscriptions.put(i.next().topicName(), handler);
		}

	  } else {
		String pubTopic = message.operationName();
		if (hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS)) {
		  pubTopic = getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS);
		}
		pendingPublishes.add(buildPublication(message.id(), pubTopic, message.value().strValue()));

	  }
	  out.add(buildConnect());
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out)
			throws Exception {

	  Channel channel = ctx.channel();

	  ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
			  channel.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	  MqttMessageType mmt = msg.fixedHeader().messageType();
	  Interpreter.getInstance().logInfo(mmt.name() + " received");

	  switch (mmt) {
		case CONNACK:
		  if (((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
			if (inInputPort) {
			  channel.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(1, 1, 1));
			  for (ListIterator<MqttSubscribeMessage> i = pendingSubscriptions.listIterator(); i.hasNext();) {
				channel.writeAndFlush(i.next());
				i.remove();
			  }
			} else {
			  for (ListIterator<MqttPublishMessage> j = pendingPublishes.listIterator(); j.hasNext();) {
				channel.writeAndFlush(j.next());
				j.remove();
			  }
			}
		  }
		  break;
		case PUBLISH:
		  if (inInputPort) {
			MqttPublishMessage mpm = (MqttPublishMessage) msg;
			getSubscriptions().get(mpm.variableHeader().topicName()).handleMessage(mpm.variableHeader().topicName(), Unpooled.copiedBuffer(mpm.payload()));
			switch (mpm.fixedHeader().qosLevel()) {
			  case AT_MOST_ONCE:
				break;
			  case AT_LEAST_ONCE:
				if (mpm.variableHeader().messageId() != -1) {
				  MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
				  channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId())));
				}
				break;
			  case EXACTLY_ONCE:
				// TODO
				break;
			  default:
				throw new AssertionError(mpm.fixedHeader().qosLevel().name());
			}
			// TODO

		  }
		  break;
		case PUBACK:
		  break;
		case PUBREC:
		  break;
		case PUBREL:
		  break;
		case PUBCOMP:
		  break;
		case SUBSCRIBE:
		  break;
		case SUBACK:
		  break;
		case UNSUBSCRIBE:
		  break;
		case UNSUBACK:
		  break;
		default:
		  Interpreter.getInstance().logSevere("Unhandled " + mmt.name() + " message!");
	  }
	  CommMessage message = CommMessage.UNDEFINED_MESSAGE;
	  out.add(message);
	}
  }
}
