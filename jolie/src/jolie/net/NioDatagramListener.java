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
import jolie.net.protocols.CommProtocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

public class NioDatagramListener extends CommListener {

  private final Bootstrap bootstrap;
  private final EventLoopGroup workerGroup;

  private Channel serverChannel;

  public NioDatagramListener(Interpreter interpreter,
      CommProtocolFactory protocolFactory, InputPort inputPort,
      EventLoopGroup workerGroup) {

    super(interpreter, protocolFactory, inputPort);
    this.bootstrap = new Bootstrap();
    this.workerGroup = workerGroup;
  }

  @Override
  public void shutdown() {
    serverChannel.close();
  }

  @Override
  public void run() {

    try {

      bootstrap
          .group(workerGroup)
          .channel(NioDatagramChannel.class)
          .handler(new UdpServerHandler());

      // bind the listener
      ChannelFuture f = bootstrap.bind(new InetSocketAddress(
          inputPort().location().getHost(),
          inputPort().location().getPort())).sync();
      serverChannel = f.channel();
      serverChannel.closeFuture().sync();

    } catch (InterruptedException | IOException ex) {
      interpreter().logWarning(ex);
    } finally {
      workerGroup.shutdownGracefully();
    }
  }

  private class UdpServerHandler
      extends SimpleChannelInboundHandler<DatagramPacket> {

    private CommProtocol protocol;
    private NioDatagramCommChannel responseChannel;
    private URI location;
    private ChannelPipeline p;

    /**
     *
     * @throws IOException
     */
    public UdpServerHandler() throws IOException {
      protocol = createProtocol();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg)
        throws Exception {

      // create the response channel and attach it to the protocol
      this.location = new URI("datagram://" + msg.sender().getHostName() + ":"
          + msg.sender().getPort());
      responseChannel = new NioDatagramCommChannel(this.location,
          (AsyncCommProtocol) protocol);
      protocol.setChannel(responseChannel);
      responseChannel.setParentInputPort(inputPort());

      // setup the pipeline 
      p = ctx.pipeline();
      assert (protocol instanceof AsyncCommProtocol);
      ((AsyncCommProtocol) protocol).setupPipeline(p);

      // add response handlers
      p.addFirst(new ChannelOutboundHandlerAdapter() {

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
          ctx.flush();
        }
      });
      p.addLast(responseChannel.commChannelHandler);
      p.addLast(new ChannelInboundHandlerAdapter() {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
          cause.printStackTrace();
          ctx.close();
        }
      });

      // set the execution context
      ctx.channel().attr(NioDatagramCommChannel.EXECUTION_CONTEXT)
          .set(interpreter().initThread());

      // pass it to the next hanlder in the pipeline
      ctx.fireChannelRead(msg.retain());
    }
  }
}
