/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import java.io.IOException;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

/**
 *
 * @author martin
 */
public class WebSocketListenerFactory extends NioSocketListenerFactory
{
	public WebSocketListenerFactory( CommCore commCore )
	{
		super( commCore, commCore.getBossGroup(), commCore.getWorkerGroup() );
	}

	@Override
	public CommListener createListener(
							Interpreter interpreter,
							CommProtocolFactory protocolFactory,
							InputPort inputPort
						)
		throws IOException
	{
		return new WebSocketListener(interpreter, protocolFactory, inputPort, bossGroup, workerGroup );
	}
}