package jolie.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jolie.net.ports.OutputPort;

/**
 *
 * @author martin
 */
@ChannelHandler.Sharable
public class JolieCommChannelHandler extends SimpleChannelInboundHandler<StatefulMessage>
{

	private Channel channel;
	private final CommChannel commChannel;

	JolieCommChannelHandler( AbstractCommChannel channel )
	{
		this.commChannel = channel;
	}

	@Override
	public void channelRegistered( ChannelHandlerContext ctx ) throws Exception
	{
		super.channelRegistered( ctx );
		this.channel = ctx.pipeline().channel();
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, StatefulMessage msg ) throws Exception
	{
		if(commChannel.parentPort() instanceof OutputPort) {
			this.commChannel.recievedResponse( msg.message() );
		} else {
			this.commChannel.messageRecv( msg.context(), msg.message() );
		}
	}

	protected ChannelFuture write( StatefulMessage msg )
	{
		return this.channel.writeAndFlush( msg );
	}
	
	protected ChannelFuture close() {
		return this.channel.close();
	}

};
