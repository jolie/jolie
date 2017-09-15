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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import java.util.concurrent.Semaphore;
import jolie.Interpreter;

public class NioDatagramCommChannel extends StreamingCommChannel {

    public static AttributeKey<ExecutionThread> EXECUTION_CONTEXT
	    = AttributeKey.valueOf("ExecutionContext");
    public static AttributeKey<CommChannel> COMMCHANNEL
	    = AttributeKey.valueOf("CommChannel");
    public static AttributeKey<Semaphore> SEND_RELEASE
	    = AttributeKey.valueOf("SendRelease");

    private Bootstrap bootstrap;
    protected CompletableFuture<CommMessage> waitingForMsg = null;
    protected final NioDatagramCommChannelHandler nioDatagramCommChannelHandler;

    public NioDatagramCommChannel(URI location, AsyncCommProtocol protocol) {
	super(location, protocol);
	nioDatagramCommChannelHandler = new NioDatagramCommChannelHandler(this);
    }

//    public NioDatagramCommChannelHandler getChannelHandler() {
//      return nioDatagramCommChannelHandler;
//    }

    public static NioDatagramCommChannel CreateChannel(URI location,
	    AsyncCommProtocol protocol, EventLoopGroup workerGroup) {

	NioDatagramCommChannel channel
		= new NioDatagramCommChannel(location, protocol);

	ExecutionThread ethread = ExecutionThread.currentThread();

	channel.bootstrap = new Bootstrap();
	channel.bootstrap.group(workerGroup)
		.channel(NioDatagramChannel.class)
		.handler(new LoggingHandler(LogLevel.INFO))
		.handler(new ChannelInitializer() {
		    @Override
		    protected void initChannel(Channel ch) throws Exception {
			ChannelPipeline p = ch.pipeline();
			protocol.setupPipeline(p);
			p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
			p.addLast(channel.nioDatagramCommChannelHandler);
			ch.attr(EXECUTION_CONTEXT).set(ethread);
			ch.attr(SEND_RELEASE).set(new Semaphore(0));
		    }
		});

	return channel;
    }

    protected ChannelFuture connect(URI location) throws InterruptedException {
	return bootstrap
		.connect(new InetSocketAddress(location.getHost(),
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
	    nioDatagramCommChannelHandler.write(message).sync();
	    Interpreter.getInstance().logInfo("MESSAGE DELIVERED");
	} catch (InterruptedException ex) {
	    throw new IOException(ex);
	}
    }

    @Override
    protected void closeImpl() throws IOException {
	try {
	    nioDatagramCommChannelHandler.close().sync();
	} catch (InterruptedException ex) {
	    throw new IOException(ex);
	}
    }

}
