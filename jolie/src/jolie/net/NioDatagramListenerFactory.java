package jolie.net;

import io.netty.channel.EventLoopGroup;
import java.io.IOException;

import jolie.Interpreter;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class NioDatagramListenerFactory extends CommListenerFactory {

    protected final EventLoopGroup workerGroup;

    public NioDatagramListenerFactory(CommCore commCore,
	    EventLoopGroup workerGroup) {
	super(commCore);
	this.workerGroup = workerGroup;

    }

    @Override
    public CommListener createListener(
	    Interpreter interpreter,
	    CommProtocolFactory protocolFactory,
	    InputPort inputPort
    )
	    throws IOException {
	return new NioDatagramListener(interpreter, protocolFactory,
		inputPort, workerGroup);
    }
}
