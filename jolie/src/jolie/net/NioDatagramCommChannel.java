package jolie.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jolie.ExecutionThread;
import jolie.net.protocols.AsyncCommProtocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.ports.Port;

public class NioDatagramCommChannel extends StreamingCommChannel {

    public final static String CHANNEL_HANDLER_NAME
	    = "STREAMING-CHANNEL-HANDLER";
    public static AttributeKey<ExecutionThread> EXECUTION_CONTEXT
	    = AttributeKey.valueOf("ExecutionContext");

    private Bootstrap bootstrap;
    protected CompletableFuture<CommMessage> waitingForMsg = null;
    protected StreamingCommChannelHandler commChannelHandler;
    private ChannelPipeline channelPipeline;

    public NioDatagramCommChannel(URI location, AsyncCommProtocol protocol) {
	super(location, protocol);
	this.commChannelHandler = new StreamingCommChannelHandler(this);
    }

    @Override
    public StreamingCommChannelHandler getChannelHandler() {
	return commChannelHandler;
    }

    private void setChannelPipeline(ChannelPipeline channelPipeline) {
	this.channelPipeline = channelPipeline;
    }

    public ChannelPipeline getChannelPipeline() {
	return channelPipeline;
    }

    public static NioDatagramCommChannel CreateChannel(URI location,
	    AsyncCommProtocol protocol, EventLoopGroup workerGroup, Port port) {

	ExecutionThread ethread = ExecutionThread.currentThread();
	NioDatagramCommChannel channel
		= new NioDatagramCommChannel(location, protocol);

	channel.bootstrap = new Bootstrap();
	channel.bootstrap.group(workerGroup)
		.channel(NioDatagramChannel.class)
		.handler(new LoggingHandler(LogLevel.INFO))
		.handler(new ChannelInitializer() {
		    @Override
		    protected void initChannel(Channel ch) throws Exception {
			ChannelPipeline p = ch.pipeline();
			if (port instanceof InputPort) {
			    channel.setParentInputPort((InputPort) port);
			}
			if (port instanceof OutputPort) {
			    channel.setParentOutputPort((OutputPort) port);
			}
			protocol.setChannel(channel);
			channel.setChannelPipeline(p);
			System.out.println(protocol.toString());
			protocol.setupPipeline(p);
			p.addLast(CHANNEL_HANDLER_NAME,
				channel.commChannelHandler);
			ch.attr(EXECUTION_CONTEXT).set(ethread);
		    }
		});

	return channel;
    }

    public ChannelFuture connect(URI location) throws InterruptedException {
	return bootstrap.connect(new InetSocketAddress(location.getHost(),
		location.getPort()));
    }

    /**
     * This is blocking to integrate with existing CommCore and
     * ExecutionThreads.
     *
     * @return
     * @throws IOException
     */
    @Override
    protected CommMessage recvImpl() throws IOException {
	try {
	    if (waitingForMsg != null) {
		throw new UnsupportedOperationException("Waiting for multiple "
			+ "messages is currently not supported!");
	    }
	    waitingForMsg = new CompletableFuture<>();
	    CommMessage msg = waitingForMsg.get();
	    waitingForMsg = null;
	    return msg;
	} catch (InterruptedException | ExecutionException ex) {
	}
	return null;
    }

    protected void completeRead(CommMessage message) {
	while (waitingForMsg == null) {
	    // spinlock
	}
	if (waitingForMsg == null) {
	    throw new IllegalStateException("No pending read to complete!");
	} else {
	    waitingForMsg.complete(message);
	}
    }

    @Override
    protected void sendImpl(CommMessage message) throws IOException {
	try {
	    commChannelHandler.write(message).sync();
	} catch (InterruptedException ex) {
	    throw new IOException(ex);
	}
    }

    @Override
    protected void closeImpl() throws IOException {
	try {
	    commChannelHandler.close().sync();
	} catch (InterruptedException ex) {
	    throw new IOException(ex);
	}
    }
}
