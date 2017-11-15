/*
 *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk>
 *   Copyright (C) 2017 by Fabrizio Montesi <famontesi@gmail.com>
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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NioSocketListener extends CommListener {

  private final ServerBootstrap bootstrap;
  private Channel serverChannel;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final CommProtocolFactory protocolFactory;
  private final ReentrantReadWriteLock responseChannels
      = new ReentrantReadWriteLock();

  public NioSocketListener(
      Interpreter interpreter,
      CommProtocolFactory protocolFactory,
      InputPort inputPort,
      EventLoopGroup bossGroup,
      EventLoopGroup workerGroup
  ) {
    super(interpreter, protocolFactory, inputPort);
    bootstrap = new ServerBootstrap();
    this.bossGroup = bossGroup;
    this.workerGroup = workerGroup;
    this.protocolFactory = protocolFactory;
  }

  @Override
  public void shutdown() {
    if (serverChannel != null) {
      responseChannels.writeLock().lock();
      try {
        serverChannel.close();
      } finally {
        responseChannels.writeLock().unlock();
      }
    }
  }

  public void addResponseChannel() {
    responseChannels.readLock().lock();
  }

  public void removeResponseChannel() {
    responseChannels.readLock().unlock();
  }

  @Override
  public void run() {

    try {

      bootstrap.group(bossGroup, workerGroup);
      bootstrap.channel(NioServerSocketChannel.class);
      bootstrap.option(ChannelOption.SO_BACKLOG, 100);
      bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

        @Override
        protected void initChannel(SocketChannel ch)
            throws Exception {

          addResponseChannel();

          CommProtocol protocol = createProtocol();
          assert (protocol instanceof AsyncCommProtocol);

          NioSocketCommChannel channel
              = new NioSocketCommChannel(
                  null, (AsyncCommProtocol) protocol);
          protocol.setChannel(channel);
          channel.setParentInputPort(inputPort());

          ChannelPipeline p = ch.pipeline();
          ((AsyncCommProtocol) protocol).setupPipeline(p);

          p.addFirst(new ChannelOutboundHandlerAdapter() {

            @Override
            public void flush(ChannelHandlerContext ctx)
                throws Exception {
              ctx.flush();
              removeResponseChannel();
            }
          });
          p.addLast(channel.commChannelHandler);
          p.addLast(new ChannelInboundHandlerAdapter() {

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx,
                Throwable cause) throws Exception {
              cause.printStackTrace();
              ctx.close();
              serverChannel.close();
            }
          });
          ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT)
              .set(interpreter().initThread());
        }
      });
      ChannelFuture f = bootstrap.bind(new InetSocketAddress(inputPort()
          .location().getPort())).sync();
      serverChannel = f.channel();
      serverChannel.closeFuture().sync();
    } catch (InterruptedException ioe) {
      interpreter().logWarning(ioe);
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
