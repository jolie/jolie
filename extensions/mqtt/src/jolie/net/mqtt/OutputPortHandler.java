/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *   Copyright (C) 2017 by Ivan Lanese <ivan.lanese@unibo.com>                 
 *   Copyright (C) 2017 by Maurizio Gabbrielli <maurizio.gabbrielli@unibo.com> 
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
package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class OutputPortHandler
    extends MessageToMessageCodec<MqttMessage, CommMessage> {

  private final MqttProtocol mp;
  private Channel cc;
  private MqttPublishMessage pendingMpm;
  private CommMessage cmReq;
  private MqttPublishMessage qos2pendingPublish;

  /**
   *
   * @param mp MqttProtocol
   */
  public OutputPortHandler(MqttProtocol mp) {
    this.mp = mp;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, CommMessage in,
      List<Object> out) throws Exception {

    init(ctx);
    // we start by connecting to the broker
    out.add(mp.connectMsg());
    // we store the message and wait to receive a CONNACK
    cmReq = in;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, MqttMessage in,
      List<Object> out) throws Exception {

    switch (in.fixedHeader().messageType()) {
      case CONNACK:
        MqttConnectReturnCode crc = ((MqttConnAckMessage) in)
            .variableHeader().connectReturnCode();
        if (crc.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
          mp.startPing(cc.pipeline());
          handleMessageSend();
        }
        break;
      case PUBLISH:
        // TODO support wildcards and variables
        MqttPublishMessage mpmIn = ((MqttPublishMessage) in);
        // WE SEND BACK THE APPROPRIATE RESPONSE, IF NECESSARY
        mp.recv_pub(cc, mpmIn);
        if (MqttProtocol.getQoS(mpmIn).equals(MqttQoS.EXACTLY_ONCE)) {
          // IF QoS = 2 we wait for PUBCOMP to actually "receive" the message
          qos2pendingPublish = mpmIn.retain(); // we retain the message as it will be used by another "channel"
        } else {
          // we received a QoS 0 or QoS 1 response, we send it up in the pipeline and close the channel
          CommMessage cmResp = mp.recv_pubReqResp(mpmIn, cmReq);
          out.add(cmResp);
          mp.stopPing(cc.pipeline());
        }
        break;
      case SUBACK:
        // SINCE WE SUBSCRIBED, WE HAVE A REQUEST-RESPONSE TO SEND
        cc.write(pendingMpm);
        if (MqttProtocol.getQoS(pendingMpm).equals(MqttQoS.AT_MOST_ONCE)) {
          mp.releaseMessage(MqttProtocol.getMessageID(pendingMpm));
        }
        break;
      case PUBACK:
      case PUBCOMP:
        // the message had either a QoS 1 or QoS 2
        // if the request was a OneWay, we send up the ACK
        if (mp.isOneWay(cmReq.operationName())) {
          out.add(CommMessage.createEmptyResponse(cmReq));
        }
        mp.markAsSentAndStopPing(cc, (int) cmReq.id());
        break;
      case PUBREC:
        // we sent a request on QoS 2, we received a PUBREC, we respond with a PUBREL
        mp.handlePubrec(cc, in);
        break;
      case PUBREL:
        // we received a QoS 2 response and its related PUBREC, we can foward it to CommCore and close
        mp.handlePubrel(cc, in);
        if (qos2pendingPublish != null) {
          CommMessage cmResp = mp.recv_pubReqResp(qos2pendingPublish, cmReq);
          out.add(cmResp);
          mp.stopPing(cc.pipeline());
        }
        break;
    }
  }

  private void handleMessageSend() throws Exception {
    if (mp.isOneWay(cmReq.operationName())) {
      // SENDING THE ONE-WAY REQUEST
      cc.writeAndFlush(mp.pubOneWayRequest(cmReq));
      // IF QoS = 0
      if (mp.checkQoS(cmReq, MqttQoS.AT_MOST_ONCE)) {
        // SEND THE ACK back to CommCore
        cc.pipeline().fireChannelRead(new CommMessage(
            cmReq.id(),
            cmReq.operationName(), "/",
            Value.create(), null));
        // AND WE CLOSE THE CHANNEL
        mp.markAsSentAndStopPing(cc, (int) cmReq.id());
      }
    } else {
      // WE ARE SENDING A Req-Res, we first subscribe to the response topic
      cc.writeAndFlush(mp.subRequestResponseRequest(cmReq));
      // and we save the message for later submission (at SUBACK)
      pendingMpm = mp.pubRequestResponseRequest(cmReq);
    }
  }

  private void init(ChannelHandlerContext ctx) {
    cc = ctx.channel();
    ((CommCore.ExecutionContextThread) Thread.currentThread())
        .executionThread(cc
            .attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    mp.checkDebug(ctx.pipeline());

  }
}
