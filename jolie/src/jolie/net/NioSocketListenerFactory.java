package jolie.net;

import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import jolie.Interpreter;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

/**
 *
 * @author martin
 */
public class NioSocketListenerFactory extends CommListenerFactory
{

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public NioSocketListenerFactory( CommCore commCore, EventLoopGroup bossGroup, EventLoopGroup workerGroup )
	{
		super( commCore );
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
	}

	@Override
	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort
	)
		throws IOException
	{ 
		return new NioSocketListener( interpreter, protocolFactory, inputPort, bossGroup, workerGroup );
	}
}
