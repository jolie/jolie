/***************************************************************************
 *   Copyright (C) 2008-2009 by Fabrizio Montesi <famontesi@gmail.com>     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.net;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;
import javax.bluetooth.L2CAPConnection;
import jolie.net.protocols.CommProtocol;
import joliex.net.BluetoothSocketWrapper;

public class BTL2CapCommChannel extends StreamingCommChannel
{
	protected final JolieCommChannelHandler jolieCommChannelHandler;
	private final BluetoothSocketWrapper connection;
	
	public BTL2CapCommChannel( L2CAPConnection connection, URI location, CommProtocol protocol )
		throws IOException
	{
		super( location, protocol );
		this.connection = new BluetoothSocketWrapper( connection, location );
		setToBeClosed( false ); // Bluetooth connections are kept open by default.
		jolieCommChannelHandler = new JolieCommChannelHandler( this );
	}
	
	protected void sendImpl( StatefulMessage message, final Function<Void, Void> completionHandler ) 
		throws IOException
	{
		ChannelFuture future = jolieCommChannelHandler.write( message );
		future.addListener( new GenericFutureListener<Future<Void>>()
		{
			public void operationComplete( Future<Void> future1 ) throws Exception
			{
				if ( future1.isSuccess() ) {
					if (completionHandler != null)
						completionHandler.apply( null );
				} else {
					throw new IOException( future1.cause() );
				}
			}
		});
	}

	protected CommMessage recvImpl()
		throws IOException
	{
		return null;
	}
	
	protected void closeImpl()
		throws IOException
	{
		connection.close();
	}
}
