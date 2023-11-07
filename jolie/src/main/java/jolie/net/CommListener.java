/***************************************************************************
 *   Copyright 2006-2011 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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

import jolie.Interpreter;
import jolie.NativeJolieThread;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.CommProtocol;

/**
 * Base class for a communication input listener. A <code>CommListener</code> exposes an input port,
 * receiving message for it. This abstract class is meant to be extended by classes which implement
 * an input receiving loop. See {@link SocketListener <code>SocketListener</code>} as an example.
 *
 * @author Fabrizio Montesi
 */
public abstract class CommListener extends NativeJolieThread {
	private static int index = 0;

	private final CommProtocolFactory protocolFactory;
	private final InputPort inputPort;

	/**
	 * Constructor
	 *
	 * @param interpreter the interpreter this listener will refer to
	 * @param protocolFactory the protocol factory for this listener
	 * @param inputPort the {@link InputPort} for this listener
	 */
	public CommListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort ) {
		super( interpreter, interpreter.commCore().threadGroup(), "CommListener-" + index++ );
		this.protocolFactory = protocolFactory;
		this.inputPort = inputPort;
	}

	protected CommListener( Interpreter interpreter, InputPort inputPort ) {
		super( interpreter );
		this.protocolFactory = null;
		this.inputPort = inputPort;
	}

	protected CommProtocol createProtocol()
		throws IOException {
		return protocolFactory.createInputProtocol( inputPort.protocolConfigurationPath(), inputPort.location() );
	}

	/**
	 * Returns the {@link InputPort} associated to this listener.
	 *
	 * @return the input port associated to this listener.
	 */
	public InputPort inputPort() {
		return inputPort;
	}

	/**
	 * Specific shutdown behavior for each implementation.
	 *
	 * The behaviour of this method depends on the implementation: there is no guarantee that the
	 * shutdown has been completed on return of this method, only that it has been requested.
	 */
	abstract public void onShutdown();

	/**
	 * Requests the shutdown of this listener, so that it receives no more messages.
	 */
	public void shutdown() {
		this.onShutdown();
		this.inputPort().clearLocationValue();
		super.clearInterpreter();
	}
}
