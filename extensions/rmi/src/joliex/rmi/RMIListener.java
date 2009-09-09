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
import java.net.URI;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Map;
import jolie.net.CommListener;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.runtime.VariablePath;
import jolie.net.OutputPort;
import jolie.runtime.AggregatedOperation;

public class RMIListener extends CommListener
{
	private Registry registry;
	final private String entryName;
	final private JolieRemote jolieRemoteStub;

	public RMIListener(
				Interpreter interpreter,
				URI location,
				CommProtocolFactory protocolFactory,
				VariablePath protocolConfigurationPath,
				Collection< String > operationNames,
				Map< String, AggregatedOperation > aggregationMap,
				Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		super( interpreter, location, protocolFactory, protocolConfigurationPath, operationNames, aggregationMap, redirectionMap );

		JolieRemote jolieRemote = new JolieRemoteImpl( interpreter, this );
		jolieRemoteStub = (JolieRemote) UnicastRemoteObject.exportObject( jolieRemote );
		registry = LocateRegistry.getRegistry( location.getHost(), location.getPort() );
		entryName = location.getPath();
		try {
			registry.bind( entryName, jolieRemoteStub );
		} catch( AlreadyBoundException e ) {
			throw new IOException( e );
		} catch( RemoteException e ) {
			if ( e instanceof java.rmi.ConnectException ) {
				registry = LocateRegistry.createRegistry( location.getPort() );
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
