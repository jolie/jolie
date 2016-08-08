package jolie.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jolie.Interpreter;
import jolie.net.ports.OutputPort;

/**
 *
 * @author martin
 */
@ChannelHandler.Sharable
public class NioSocketCommChannelHandler extends SimpleChannelInboundHandler<CommMessage>
{

	private Channel channel;
	private final NioSocketCommChannel commChannel;
	private final Interpreter interpreter;

	NioSocketCommChannelHandler( NioSocketCommChannel channel )
	{
		this.commChannel = channel;
		this.interpreter = Interpreter.getInstance();
	}

	@Override
	public void channelRegistered( ChannelHandlerContext ctx ) throws Exception
	{
		super.channelRegistered( ctx );
		this.channel = ctx.pipeline().channel();
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, CommMessage msg ) throws Exception
	{
		if(commChannel.parentPort() instanceof OutputPort) {
			this.commChannel.recievedResponse( msg );
		} else {
			this.commChannel.messageRecv( msg );
		}
	}

	protected ChannelFuture write( CommMessage msg )
	{
		return this.channel.writeAndFlush( msg );
	}

	protected ChannelFuture close()
	{
		return channel.close();
	}

};
