package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.SessionContext;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

/**
 *
 * @author martin
 */
public class NioSocketCommChannelFactory extends CommChannelFactory
{

	EventLoopGroup workerGroup;

	public NioSocketCommChannelFactory( CommCore commCore, EventLoopGroup workerGroup )
	{
		super( commCore );
		this.workerGroup = workerGroup;
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port, SessionContext ctx )
		throws IOException
	{
		CommProtocol protocol;
		try {
			protocol = port.getProtocol( ctx );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}

		if ( !(protocol instanceof AsyncCommProtocol) ) {
			throw new UnsupportedCommProtocolException( "Use an async protocol" );
		}

		NioSocketCommChannel channel = NioSocketCommChannel.CreateChannel( location, (AsyncCommProtocol) protocol, workerGroup, ctx );

		try {
			ChannelFuture f = channel.connect( location );
			f.sync();
			if ( !f.isSuccess() ) {
				throw (IOException) f.cause();
			}
		} catch( InterruptedException e ) {
			throw new IOException( e );
		}
		return channel;
	}

}
