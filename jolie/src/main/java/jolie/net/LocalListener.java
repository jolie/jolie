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

import java.util.HashMap;
import java.util.Map;

import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.ports.InputPort;
import jolie.net.ports.Interface;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;

/**
 * <code>LocalListener</code> is used internally by the interpreter for receiving local messages.
 *
 * @author Fabrizio Montesi
 */
public class LocalListener extends CommListener {
	public static LocalListener create( Interpreter interpreter, InputPort inputPort ) {
		VariablePath locationPath =
			new ClosedVariablePath(
				new VariablePathBuilder( true )
					.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
					.add( Constants.LOCAL_INPUT_PORT_NAME, 0 )
					.add( Constants.LOCATION_NODE_NAME, 0 )
					.toVariablePath(),
				interpreter.globalValue() );
		return new LocalListener( interpreter, locationPath, inputPort );
	}

	public static LocalListener create( Interpreter interpreter ) {
		return create( interpreter, null );
	}

	private LocalListener( Interpreter interpreter, VariablePath locationPath, InputPort inputPort ) {
		super( interpreter, inputPort == null ? new InputPort(
			Constants.LOCAL_INPUT_PORT_NAME,
			locationPath,
			new VariablePathBuilder( true ).toVariablePath(),
			new Interface(
				new HashMap<>(),
				new HashMap<>() ),
			new HashMap<>(),
			new HashMap<>() ) : inputPort );
		locationPath.getValue().setValue( Constants.LOCAL_LOCATION_KEYWORD );
	}

	public void mergeInterface( Interface iface ) {
		inputPort().getInterface().merge( iface );
	}

	public void addRedirections( Map< String, OutputPort > redirectionMap ) {
		inputPort().redirectionMap().putAll( redirectionMap );
	}

	public void addAggregations( Map< String, AggregatedOperation > aggregationMap ) {
		inputPort().aggregationMap().putAll( aggregationMap );
	}

	@Override
	public void onShutdown() {}

	@Override
	public void run() {}

	@Override
	public CommProtocol createProtocol() {
		return null;
	}

	@Override
	final public synchronized void start() {}
}
