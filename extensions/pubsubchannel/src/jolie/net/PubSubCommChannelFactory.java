/*****************************************************************************
 * Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                           *
 *                                                                           *
 *   This program is free software; you can redistribute it and/or modify    *
 *   it under the terms of the GNU Library General Public License as         *  
 *   published by the Free Software Foundation; either version 2 of the      *
 *   License, or (at your option) any later version.                         *
 *                                                                           *
 *   This program is distributed in the hope that it will be useful,         *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 *   GNU General Public License for more details.                            *
 *                                                                           *
 *   You should have received a copy of the GNU Library General Public       *
 *   License along with this program; if not, write to the                   *
 *   Free Software Foundation, Inc.,                                         *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.               *
 *                                                                           *
 *   For details about the authors of this software, see the AUTHORS file.   *
 *****************************************************************************/
package jolie.net;

import jolie.net.ports.OutputPort;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import jolie.Interpreter;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.CommProtocol;

public class PubSubCommChannelFactory extends CommChannelFactory {

	public PubSubCommChannelFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port )
		throws IOException {

		StreamingCommChannel channel = ( ( StreamingCommChannel ) Interpreter.getInstance().commCore().createCommChannel( location, port ) );
		channel.setParentOutputPort( port );

		Map< Long, CompletableFuture<Void>> sendRelease = new ConcurrentHashMap<>();

		PubSubCommChannel ret = null;

		try {
			ret = new PubSubCommChannel( location, port.getProtocol(), channel, sendRelease );
			channel.getChannelHandler().setInChannel( ret );
		} catch ( URISyntaxException e ) {
			throw new IOException( e );
		}
		return ret;
	}

	@Override
	public CommChannel createInputChannel( URI location, InputPort port, CommProtocol protocol ) throws IOException {
		throw new UnsupportedOperationException( "createInputPortChannel for PubSubChannel not supported yet." );
	}

}
