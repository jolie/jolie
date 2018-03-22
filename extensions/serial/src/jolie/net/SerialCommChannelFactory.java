/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AndJarDeps;

@AndJarDeps({"jSerialComm.jar"})
public class SerialCommChannelFactory extends CommChannelFactory {

	protected final EventLoopGroup workerGroup;

	public SerialCommChannelFactory( CommCore commCore ) {

		super( commCore );
		this.workerGroup = new OioEventLoopGroup( 4, commCore.getNewExecutionContextThreadFactory() );
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort outputPort ) throws IOException {

		CommProtocol protocol;
		try {
			protocol = outputPort.getProtocol();
		} catch ( URISyntaxException e ) {
			throw new IOException( e );
		}

		if ( !( protocol instanceof AsyncCommProtocol ) ) {
			throw new UnsupportedCommProtocolException( "Use an async protocol" );
		}

		SerialCommChannel channel = SerialCommChannel.createChannel( location, ( AsyncCommProtocol ) protocol, workerGroup, outputPort );

		try {
			ChannelFuture f = channel.connect( location );
			f.sync();
			if ( !f.isSuccess() ) {
				throw ( IOException ) f.cause();
			}
		} catch ( InterruptedException e ) {
			throw new IOException( e );
		}

		return channel;
	}

	@Override
	public CommChannel createInputChannel( URI location, InputPort inputPort, CommProtocol protocol ) throws IOException {

		if ( !( protocol instanceof AsyncCommProtocol ) ) {
			throw new UnsupportedCommProtocolException( "Use an async protocol" );
		}

		SerialCommChannel channel = SerialCommChannel.createChannel( location, ( AsyncCommProtocol ) protocol, workerGroup, inputPort );

		try {
			ChannelFuture f = channel.connect( location );
			f.sync();
			if ( !f.isSuccess() ) {
				throw ( IOException ) f.cause();
			}
		} catch ( InterruptedException e ) {
			throw new IOException( e );
		}
		return channel;
	}

}
