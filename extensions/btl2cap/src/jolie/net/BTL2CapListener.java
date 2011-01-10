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

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.microedition.io.Connector;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class BTL2CapListener extends CommListener
{
	private final L2CAPConnectionNotifier connectionNotifier;
	public BTL2CapListener(
				Interpreter interpreter,
				CommProtocolFactory protocolFactory,
				InputPort inputPort
			)
		throws IOException
	{
		super(
			interpreter,
			protocolFactory,
			inputPort
		);
		connectionNotifier =
				(L2CAPConnectionNotifier)Connector.open( inputPort().location().toString() );
	}
	
	@Override
	public void run()
	{
		try {
			L2CAPConnection clientConnection;
			CommChannel channel;
			while ( (clientConnection = connectionNotifier.acceptAndOpen()) != null ) {
				channel = new BTL2CapCommChannel(
							clientConnection,
							inputPort().location(),
							createProtocol() );
				channel.setParentInputPort( inputPort() );
				interpreter().commCore().scheduleReceive( channel, inputPort() );
				channel = null; // Dispose for garbage collection
			}
		} catch( ClosedByInterruptException ce ) {
			try {
				connectionNotifier.close();
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown()
	{
		try {
			connectionNotifier.close();
		} catch( IOException e ) {}
	}
}
