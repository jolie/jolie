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

package joliex.rmi;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import jolie.net.CommListener;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class RMIListener extends CommListener
{
	private Registry registry;
	final private String entryName;
	final private JolieRemote jolieRemoteStub;

	public RMIListener(
				Interpreter interpreter,
				CommProtocolFactory protocolFactory,
				InputPort inputPort
			)
		throws IOException
	{
		super( interpreter, protocolFactory, inputPort );

		JolieRemote jolieRemote = new JolieRemoteImpl( interpreter, this );
		jolieRemoteStub = (JolieRemote) UnicastRemoteObject.exportObject( jolieRemote );
		registry = LocateRegistry.getRegistry( inputPort.location().getHost(), inputPort.location().getPort() );
		entryName = inputPort.location().getPath();
		try {
			registry.bind( entryName, jolieRemoteStub );
		} catch( AlreadyBoundException e ) {
			throw new IOException( e );
		} catch( RemoteException e ) {
			if ( e instanceof java.rmi.ConnectException ) {
				registry = LocateRegistry.createRegistry( inputPort.location().getPort() );
				try {
					registry.bind( entryName, jolieRemoteStub );
				} catch( AlreadyBoundException ae ) {
					throw new IOException( ae );
				}
			} else {
				throw new IOException( e );
			}
		}
	}

	@Override
	public void shutdown()
	{
		try {
			registry.unbind( entryName );
		}
		catch( RemoteException e ) {}
		catch( NotBoundException e ) {}
	}

	@Override
	public void run()
	{}

	@Override
	public void start()
	{}
}
