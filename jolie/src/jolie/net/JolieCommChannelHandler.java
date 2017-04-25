package jolie.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *
 * @author Martin M. Andersen
 */
@ChannelHandler.Sharable
public class JolieCommChannelHandler extends SimpleChannelInboundHandler<StatefulMessage>
{

	private Channel channel;
	private final CommChannel commChannel;

	public JolieCommChannelHandler( AbstractCommChannel channel )
	{
		this.commChannel = channel;
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void channelRegistered( ChannelHandlerContext ctx ) throws Exception
	{
		super.channelRegistered( ctx );
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, StatefulMessage msg ) throws Exception
	{
		if( !msg.message().isRequest() ) {
			this.commChannel.receivedResponse( msg.message() );
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

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
	

};
