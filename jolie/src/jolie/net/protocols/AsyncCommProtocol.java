package jolie.net.protocols;

import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jolie.StatefulContext;
import jolie.net.CommMessage;
import jolie.runtime.VariablePath;

/**
 *
 * @author martin
 */
public abstract class AsyncCommProtocol extends CommProtocol
{

	public AsyncCommProtocol( VariablePath configurationPath )
	{
		super( configurationPath );
	}

	abstract public void setupPipeline( ChannelPipeline pipeline );
	
	public void setupWrapablePipeline( ChannelPipeline pipeline ) {
		setupPipeline( pipeline );
	};

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}
	
	public void initialize(StatefulContext ctx) {
		// Do nothing.
	}

}
