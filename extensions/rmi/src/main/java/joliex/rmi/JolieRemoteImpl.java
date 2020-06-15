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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import jolie.Interpreter;
import jolie.net.CommListener;

/**
 *
 * @author Fabrizio Montesi
 */
public class JolieRemoteImpl implements JolieRemote {
	final private Interpreter interpreter;
	final private CommListener listener;

	public JolieRemoteImpl( Interpreter interpreter, CommListener listener )
		throws RemoteException {
		this.interpreter = interpreter;
		this.listener = listener;
	}

	@Override
	public RemoteBasicChannel createRemoteBasicChannel()
		throws RemoteException {
		RemoteBasicChannelImpl channel =
			new RemoteBasicChannelImpl( interpreter.commCore().getLocalCommChannel( listener ), this );
		return (RemoteBasicChannel) UnicastRemoteObject.exportObject( channel );
	}

	protected void disposeOf( RemoteBasicChannel channel ) {}
}
