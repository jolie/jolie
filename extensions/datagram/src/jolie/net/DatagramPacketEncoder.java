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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.InetSocketAddress;
import java.util.List;

class DatagramPacketEncoder extends MessageToMessageEncoder<ByteBuf> {

  private InetSocketAddress recipient;

  DatagramPacketEncoder(InetSocketAddress recipient) {
    this.recipient = recipient;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg,
      List<Object> out) throws Exception {

//    DatagramPacket dp;
//    if (msg.isDirect() && msg.nioBufferCount() == 1) {
//      dp = new DatagramPacket(msg, recipient);
//    } else {
//      dp = new DatagramPacket(newDirectBuffer(msg), recipient);
//    }
//    out.add(dp);
      out.add(new DatagramPacket(msg.retain(), recipient));
  }

//  public final ByteBuf newDirectBuffer(ByteBuf buf) {
//    final int readableBytes = buf.readableBytes();
//    if (readableBytes == 0) {
//      return Unpooled.EMPTY_BUFFER;
//    }
//
//    final ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;
//    if (alloc.isDirectBufferPooled()) {
//      ByteBuf directBuf = alloc.directBuffer(readableBytes);
//      directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
//      return directBuf;
//    }
//
//    final ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
//    if (directBuf != null) {
//      directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
//      return directBuf;
//    }
//
//    return buf;
//  }
}
