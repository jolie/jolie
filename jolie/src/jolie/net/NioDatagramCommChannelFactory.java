package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

public class NioDatagramCommChannelFactory extends CommChannelFactory {

    EventLoopGroup workerGroup;

    public NioDatagramCommChannelFactory(CommCore commCore,
	    EventLoopGroup workerGroup) {
	super(commCore);
	this.workerGroup = workerGroup;
    }

    @Override
    public CommChannel createChannel(URI location, OutputPort port)
	    throws IOException {
	CommProtocol protocol;
	try {
	    protocol = port.getProtocol();
	} catch (URISyntaxException e) {
	    throw new IOException(e);
	}

	if (!(protocol instanceof AsyncCommProtocol)) {
	    throw new UnsupportedCommProtocolException("Use an async protocol");
	}

	NioDatagramCommChannel channel = NioDatagramCommChannel.
		CreateChannel(location, (AsyncCommProtocol) protocol,
			workerGroup);

	try {
	    ChannelFuture f = channel.connect(location);
	    f.sync();
	    if (!f.isSuccess()) {
		throw (IOException) f.cause();
	    }
	} catch (InterruptedException e) {
	    throw new IOException(e);
	}

	return channel;
    }

}
