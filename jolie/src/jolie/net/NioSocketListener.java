/** ******************************************************************************
 *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk> *
 * Copyright (C) 2017 by Fabrizio Montesi <famontesi@gmail.com> * Copyright (C)
 * 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> * * This program
 * is free software; you can redistribute it and/or modify * it under the terms
 * of the GNU Library General Public License as * published by the Free Software
 * Foundation; either version 2 of the * License, or (at your option) any later
 * version. * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General
 * Public License for more details. * * You should have received a copy of the
 * GNU Library General Public * License along with this program; if not, write
 * to the * Free Software Foundation, Inc., * 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *****************************************************************************
 */
package jolie.net;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

import io.netty.bootstrap.Bootstrap;
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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.IOException;
import java.net.Inet4Address;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import jolie.runtime.Value;

public class NioSocketListener extends CommListener {

    private final ServerBootstrap bootstrap;
    private Channel serverChannel;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final CommProtocolFactory protocolFactory;
    private final ReentrantReadWriteLock responseChannels = new ReentrantReadWriteLock();

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
	    if (protocolFactory.getClass().getName().toLowerCase().contains("mqtt")) {
		Thread.sleep(50);
		URI broker = URI.create(inputPort().protocolConfigurationPath().getValue().getFirstChild("broker").strValue());
		AsyncCommProtocol mp = (AsyncCommProtocol) createProtocol();
		NioSocketCommChannel channel = new NioSocketCommChannel(broker, mp);
		channel.setParentInputPort(inputPort());
		Bootstrap b = new Bootstrap()
			.group(workerGroup)
			.channel(NioSocketChannel.class)
			.localAddress(new InetSocketAddress(inputPort().location().getPort()))
			.remoteAddress(new InetSocketAddress(broker.getHost(), broker.getPort()))
			.handler(new ChannelInitializer() {
			    @Override
			    protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				//p.addLast(new LoggingHandler(LogLevel.INFO));
				mp.setupPipeline(ch.pipeline());
				p.addFirst(new ChannelOutboundHandlerAdapter() {

				    @Override
				    public void flush(ChannelHandlerContext ctx) throws Exception {
					ctx.flush();
				    }
				});
				p.addLast(channel.nioSocketCommChannelHandler);
				p.addLast(new ChannelInboundHandlerAdapter() {

				    @Override
				    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
					cause.printStackTrace();
					ctx.close();
					serverChannel.close();
				    }

				});
				ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT).set(interpreter().initThread());
			    }
			});
		ChannelFuture f = b.connect().sync();
		serverChannel = f.channel();
		List<String> tmp = new ArrayList<>();
		visitValue(inputPort().protocolConfigurationPath().getValue(), tmp);
		channel.sendImpl(new CommMessage(CommMessage.getNewMessageId(), tmp.get(tmp.indexOf("osc") + 1), "/", inputPort().protocolConfigurationPath().getValue(), null));
		serverChannel.closeFuture().sync();
	    } else {
		bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 100)
			//.handler( new LoggingHandler( LogLevel.INFO ) )
			.childHandler(new ChannelInitializer<SocketChannel>() {

			    @Override
			    protected void initChannel(SocketChannel ch) throws Exception {
				addResponseChannel();
				CommProtocol protocol = createProtocol();
				assert (protocol instanceof AsyncCommProtocol);

				NioSocketCommChannel channel = new NioSocketCommChannel(null, (AsyncCommProtocol) protocol);
				channel.setParentInputPort(inputPort());

				//interpreter().commCore().scheduleReceive(channel, inputPort());
				ChannelPipeline p = ch.pipeline();
				((AsyncCommProtocol) protocol).setupPipeline(p);

				// the pipeline is an inbound one, hence outbound traffic goes 
				// from bottom-up into the pipeline. We add the outbound adapter 
				// as first to observe the ultimate send as the response from the 
				// nioSocketCommChannelHandler.
				p.addFirst(new ChannelOutboundHandlerAdapter() {

				    @Override
				    public void flush(ChannelHandlerContext ctx) throws Exception {
					ctx.flush();
					removeResponseChannel();
				    }
				});
				p.addLast(channel.nioSocketCommChannelHandler);
				p.addLast(new ChannelInboundHandlerAdapter() {

				    @Override
				    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
					cause.printStackTrace();
					ctx.close();
					serverChannel.close();
				    }

				});
				ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT).set(interpreter().initThread());
			    }
			});
		ChannelFuture f = bootstrap.bind(new InetSocketAddress(inputPort().location().getPort())).sync();
		serverChannel = f.channel();
		serverChannel.closeFuture().sync();
	    }
	} catch (InterruptedException | IOException ioe) {
	    interpreter().logWarning(ioe);
	} finally {
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	}
    }

    private void visitValue(Value value, List<String> tmp) {
	value.children().forEach((s, v) -> {
	    tmp.add(s);
	    if (v.first().hasChildren()) {
		visitValue(v.first(), tmp);
	    } else {
		tmp.add(v.first().strValue());
	    }
	});
    }
}
