package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import jolie.net.jssc.JsscChannel;
import jolie.net.jssc.JsscDeviceAddress;

public class SimpleLineBasedSerialChannel {

	ChannelFuture f;
	EventLoopGroup group;

	public SimpleLineBasedSerialChannel( String port ) {
		group = new OioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group( group )
			.channel( JsscChannel.class )
			.handler( new ChannelInitializer<JsscChannel>() {
				@Override
				public void initChannel( JsscChannel ch ) throws Exception {
					ch.pipeline().addLast(
						new LineBasedFrameDecoder( Integer.MAX_VALUE ),
						new StringDecoder(),
						new SimpleChannelInboundHandler<String>() {
						@Override
						protected void channelRead0( io.netty.channel.ChannelHandlerContext ctx, String msg ) throws Exception {
							System.out.println( "msg" );
						}
					}
					);
				}
			} );

		f = b.connect( new JsscDeviceAddress( port ) ).syncUninterruptibly();
	}

	public void write( Object msg ) {
		f.channel().writeAndFlush( msg );
	}

	public void close() {
		f.channel().closeFuture().syncUninterruptibly();
		group.shutdownGracefully();
	}
}
