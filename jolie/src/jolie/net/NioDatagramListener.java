package jolie.net;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.net.URI;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

public class NioDatagramListener extends CommListener {

    private final Bootstrap bootstrap;
    private Channel serverChannel;
    private final EventLoopGroup workerGroup;
    private InetSocketAddress remoteAddress;

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

	    bootstrap.group(workerGroup);
	    bootstrap.channel(NioDatagramChannel.class);
	    bootstrap.handler(new SimpleChannelInboundHandler<DatagramPacket>() {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx,
			DatagramPacket packet) throws Exception {

		    remoteAddress = packet.sender();
		    URI location = new URI(
			    "datagram://"
			    + remoteAddress.getHostName()
			    + ":"
			    + remoteAddress.getPort());

		    ctx.pipeline().addLast(new ChannelInitializer<NioDatagramChannel>() {

			@Override
			protected void initChannel(NioDatagramChannel ch)
				throws Exception {

			    CommProtocol protocol = createProtocol();
			    assert (protocol instanceof AsyncCommProtocol);

			    NioDatagramCommChannel datagramChannel
				    = new NioDatagramCommChannel(location,
					    (AsyncCommProtocol) protocol);

			    protocol.setChannel(datagramChannel);
			    datagramChannel.setParentInputPort(inputPort());

			    ChannelPipeline p = ch.pipeline();
			    ((AsyncCommProtocol) protocol).setupPipeline(p);

			    p.addFirst(new ChannelOutboundHandlerAdapter() {
				@Override
				public void flush(ChannelHandlerContext ctx)
					throws Exception {

				    ctx.flush();
				}
			    });
			    p.addLast(datagramChannel.commChannelHandler);
			    p.addLast(new ChannelInboundHandlerAdapter() {

				@Override
				public void exceptionCaught(
					ChannelHandlerContext ctx,
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
		}
	    });
	    ChannelFuture f = bootstrap.bind(new InetSocketAddress(
		    inputPort().location().getHost(),
		    inputPort().location().getPort())).sync();
	    serverChannel = f.channel();
	    serverChannel.closeFuture().sync();

	} catch (InterruptedException ex) {
	    interpreter().logWarning(ex);
	} finally {
	    workerGroup.shutdownGracefully();
	}
    }
}
