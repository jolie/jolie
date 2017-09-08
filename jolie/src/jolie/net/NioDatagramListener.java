package jolie.net;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

public class NioDatagramListener extends CommListener {

    private Channel serverChannel;
    private final EventLoopGroup workerGroup;

    public NioDatagramListener(
	    Interpreter interpreter,
	    CommProtocolFactory protocolFactory,
	    InputPort inputPort,
	    EventLoopGroup workerGroup
    ) {
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
	    Bootstrap b = new Bootstrap();
	    b.group(workerGroup);
	    b.channel(NioDatagramChannel.class);
	    /*
	    b.handler(new ChannelInitializer<NioDatagramChannel>() {
		@Override
		protected void initChannel(NioDatagramChannel ch)
			throws Exception {
		    CommProtocol protocol = createProtocol();
		    assert (protocol instanceof AsyncCommProtocol);
		    ChannelPipeline p = ch.pipeline();
		    ((AsyncCommProtocol) protocol).setupPipeline(p);
		}
	    });
	     */
	    b.handler(new ChannelInboundHandlerAdapter() {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		    super.channelRead(ctx, msg);
		    Interpreter.getInstance().logInfo("The message: "
			    + msg.toString());
		}

	    });
	    ChannelFuture f = b.bind(new InetSocketAddress(inputPort()
		    .location().getPort())).sync();
	    serverChannel = f.channel();
	    serverChannel.closeFuture().sync();
	} catch (InterruptedException ex) {
	    interpreter().logWarning(ex);
	} finally {
	    workerGroup.shutdownGracefully();
	}
    }
}
