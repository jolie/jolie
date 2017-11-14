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

import jolie.Interpreter;

import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.ExecutionThread;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.net.InetSocketAddress;
import java.net.URI;
import jolie.net.protocols.CommProtocol;

public class DatagramListener extends CommListener {

  private final EventLoopGroup workerGroup;
  private Channel serverChannel;

  public DatagramListener(Interpreter interpreter,
      CommProtocolFactory protocolFactory, InputPort inputPort,
      EventLoopGroup workerGroup) {
    super(interpreter, protocolFactory, inputPort);
    this.workerGroup = workerGroup;
  }

  @Override
  public void shutdown() {
    serverChannel.close();
  }

  @Override
  public void run() {
    try {

      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(workerGroup).channel(NioDatagramChannel.class);
      bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {

        @Override
        protected void initChannel(NioDatagramChannel ch)
            throws Exception {

          CommProtocol protocol = createProtocol();
          if (!(protocol instanceof AsyncCommProtocol)) {
            throw new UnsupportedCommProtocolException("Use an async protocol");
          }
          ChannelPipeline p = ch.pipeline();

          p.addLast("INBOUND", new SimpleChannelInboundHandler<DatagramPacket>() {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket in)
                throws Exception {

              DatagramPacket msg = in.copy();
              URI location = new URI("datagram://" + msg.sender().getHostName()
                  + ":" + msg.sender().getPort());

              DatagramCommChannel channel = DatagramCommChannel.
                  CreateChannel(location, (AsyncCommProtocol) protocol, workerGroup,
                      inputPort());

              ChannelFuture cf = channel.connect(location);
              cf.addListener(new FutureListener() {
                @Override
                public void operationComplete(Future f) throws Exception {
                  cf.channel().pipeline().fireChannelRead(msg.content().retain());
                }
              });
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
              ctx.close();
              throw new Exception(cause);
            }
          });
        }
      });
      ChannelFuture f = bootstrap.bind(new InetSocketAddress(
          inputPort().location().getHost(),
          inputPort().location().getPort())).sync();
      serverChannel = f.channel();
      serverChannel.closeFuture().sync();
    } catch (InterruptedException ex) {
      Interpreter.getInstance().logWarning(ex);
    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
