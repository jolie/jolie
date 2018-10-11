package jolie.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import java.net.URI;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;

public class DatagramListener extends CommListener {

	private final int inboundPort;
	private final String inboundAddress;
	private final EventLoopGroup group;

	private Channel channel;

	public DatagramListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort,
		EventLoopGroup workerGroup
	) {

		super( interpreter, protocolFactory, inputPort );
		this.inboundPort = inputPort.location().getPort();
		this.inboundAddress = inputPort.location().getHost();
		this.group = workerGroup;
	}

	public void shutdown() {
		this.channel.close();
	}

	@Override
	public void run() {
		ChannelHandler childHandler = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel( Channel channel ) throws Exception {
				channel.pipeline()
					.addLast( new SimpleChannelInboundHandler<ByteBuf>() {

						@Override
						public void exceptionCaught( ChannelHandlerContext ctx, Throwable e ) {
							Interpreter.getInstance().logSevere( e.getMessage() );
							ctx.close();
						}

						@Override
						protected void channelRead0( ChannelHandlerContext ctx, ByteBuf msg ) throws Exception {
							AsyncCommProtocol protocol = ( AsyncCommProtocol ) createProtocol();
							ChannelPipeline pipeline = ctx.pipeline();
							protocol.setupPipeline( pipeline );
							InetSocketAddress isa = ( InetSocketAddress ) ctx.channel().remoteAddress();
							URI location = new URI( "datagram", null, isa.getHostName(), isa.getPort(), null, null, null );
							DatagramCommChannel outboundChannel = DatagramCommChannel.createChannel( location, protocol, group, inputPort() );
							outboundChannel.bind( new InetSocketAddress( 0 ) ).sync();
							pipeline.addLast( "COMM MESSAGE INBOUND", outboundChannel.commChannelHandler );
//								protocol.setInitExecutionThread( interpreter().initThread() );
							ctx.fireChannelRead( Unpooled.copiedBuffer( msg ) );
						}
					} );
			}
		};
		ServerBootstrap bootstrap = new ServerBootstrap()
			.group( group )
			.channel( UdpServerChannel.class )
			.childHandler( childHandler );

		ChannelFuture future = bootstrap.bind( inboundAddress, inboundPort ).syncUninterruptibly();
		this.channel = future.channel();
	}
}
