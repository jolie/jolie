/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.bluetooth.L2CAPConnection;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;

public class BTL2CapCommChannel extends StreamingCommChannel implements PollableCommChannel
{
	final private L2CAPConnection connection;
	final private int sendMTU;
	final private int recvMTU;
	
	public BTL2CapCommChannel( L2CAPConnection connection, URI location, CommProtocol protocol )
		throws IOException
	{
		super( location, protocol );
		this.connection = connection;
		sendMTU = connection.getTransmitMTU();
		recvMTU = connection.getReceiveMTU();
		setToBeClosed( false ); // Bluetooth connections are kept open by default.
	}
	
	protected void sendImpl( CommMessage message )
		throws IOException
	{
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		protocol().send( ostream, message, null ); // TODO Fix this null pointer.
		byte[] result = ostream.toByteArray();
		if ( result.length >= sendMTU ) {
			int times = (result.length / sendMTU) + 1;
			byte[] chunk;
			for( int i = 0; i < times; i++ ) {
				chunk = Arrays.copyOfRange( result, i*sendMTU, (i*sendMTU)+1 );
				connection.send( chunk );
			}
		} else {
			connection.send( result );
		}
	}
	
	protected CommMessage recvImpl()
		throws IOException
	{
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		byte[] chunk;
		int len = recvMTU + 1;
		while( len >= recvMTU ) {
			chunk = new byte[ recvMTU ];
			len = connection.receive( chunk );
			ostream.write( chunk );
		}
		ByteArrayInputStream istream = new ByteArrayInputStream( ostream.toByteArray() );
		return protocol().recv( istream, null ); // TODO fix this null pointer
	}
	
	protected void closeImpl()
		throws IOException
	{
		connection.close();
	}
	
	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForPolling( this );
	}
	
	public boolean isReady()
	{
		try {
			return connection.ready();
		} catch( IOException e ) {
			return false;
		}
	}
}
