/**
package jolie.net.mqtt;

import io.netty.bootstrap.Bootstrap;
import java.net.InetSocketAddress;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NioSocketListener extends CommListener {

  private Channel listenerChannel;
  private final CommProtocolFactory protocolFactory;
  private final InputPort inputPort;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final ReentrantReadWriteLock responseChannels = new ReentrantReadWriteLock();

  public NioSocketListener(Interpreter interpreter, CommProtocolFactory protocolFactory, InputPort inputPort, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
    super(interpreter, protocolFactory, inputPort);
    this.bossGroup = bossGroup;
    this.workerGroup = workerGroup;
    this.protocolFactory = protocolFactory;
    this.inputPort = inputPort;
  }

  @Override
  public void shutdown() {
    if (listenerChannel != null) {
      responseChannels.writeLock().lock();
      try {
        listenerChannel.close();
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

    ChannelFuture f;

    try {

      if (protocolFactory.getClass().toString().contains("MqttProtocolFactory")) {

        AsyncCommProtocol mp = (AsyncCommProtocol) createProtocol();

        URI broker = URI.create("socket://iot.eclipse.org:1883");
        NioSocketCommChannel channel = new NioSocketCommChannel(broker, mp);
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.localAddress(new InetSocketAddress(inputPort.location().getPort()));
        b.remoteAddress(new InetSocketAddress(broker.getHost(), broker.getPort()));
        b.handler(new ChannelInitializer() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            mp.setupPipeline(ch.pipeline());
            ch.pipeline().addLast(channel.nioSocketCommChannelHandler);
          }
        });
        f = b.connect().sync();

      } else {

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 100);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {

            addResponseChannel();
            AsyncCommProtocol protocol = (AsyncCommProtocol) createProtocol();

            NioSocketCommChannel channel = new NioSocketCommChannel(null, protocol);

            channel.setParentInputPort(inputPort());

            protocol.setupPipeline(ch.pipeline());
            ch.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {

              @Override
              public void flush(ChannelHandlerContext ctx) throws Exception {
                ctx.flush();
                removeResponseChannel();
              }
            });
            ch.pipeline().addLast(channel.nioSocketCommChannelHandler);
            ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT).set(interpreter().initThread());
          }
        });

        f = bootstrap.bind(new InetSocketAddress(inputPort.location().getPort())).sync();
      }
      listenerChannel = f.channel();
      listenerChannel.closeFuture().sync();

    } catch (InterruptedException | IOException ex) {
      interpreter().logWarning(ex);
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
**/
