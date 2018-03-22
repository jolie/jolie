/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import jolie.net.serial.JSCDeviceAddress;
import jolie.net.serial.JSCC;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Sends one message to a serial device
 */
public final class JSerialCommClient {

	static final String location = System.getProperty( "port", "/dev/tty.SLAB_USBtoUART" );

	public static void main( String[] args ) throws Exception {

		EventLoopGroup workerGroup = new OioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group( workerGroup )
				.channel( JSCC.class )
				.handler( new ChannelInitializer<JSCC>() {
					@Override
					public void initChannel( JSCC ch ) throws Exception {
						ch.pipeline().addLast(
							new LoggingHandler( LogLevel.INFO ),
							new StringEncoder(),
							new StringDecoder(),
							new JSerialCommClientHandler()
						);
					}
				} );

			ChannelFuture f = b.connect(new JSCDeviceAddress( location ) ).sync();
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
		}
	}
}

class JSerialCommClientHandler extends SimpleChannelInboundHandler<String> {

	@Override
	public void channelActive( ChannelHandlerContext ctx ) {
		ctx.writeAndFlush( "J" );
	}

	@Override
	public void channelRead0( ChannelHandlerContext ctx, String msg ) throws Exception {
		if ( "K".equals( msg ) ) {
			System.out.println( "Serial port responded to J" );
			ctx.close();
		}
		if ( "J".equals( msg ) ) {
			System.out.println( "Serial port send J" );
			ctx.writeAndFlush( "K" );
			ctx.close();
		}
	}
}
