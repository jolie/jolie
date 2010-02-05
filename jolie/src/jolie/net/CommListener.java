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

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import jolie.Interpreter;
import jolie.JolieThread;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.VariablePath;

/**
 * Base class for a communication input listener. A <code>CommListener</code>
 * exposes an input port, receiving message for it.
 * This abstract class is meant to be extended by classes which implement
 * an input receiving loop. See {@link SocketListener <code>SocketListener</code>} as an example.
 * @author Fabrizio Montesi
 */
abstract public class CommListener extends JolieThread
{
	private static int index = 0;

	private final CommProtocolFactory protocolFactory;
	protected final Collection< String > operationNames;
	protected final Map< String, AggregatedOperation > aggregationMap;
	protected final Map< String, OutputPort > redirectionMap;

	private final VariablePath protocolConfigurationPath;
	private final URI location;

	/**
	 * Constructor
	 * @param interpreter the interpreter this listener will refer to
	 * @param location the location of the input port related to this listener
	 * @param protocolFactory the protocol factory for this listener
	 * @param protocolConfigurationPath the protocol configuration variable path for configuring the generated protocol instances
	 * @param operationNames the operation names this listener will have to handle
	 * @param aggregationMap the aggregation map for this listener
	 * @param redirectionMap the redirection map for this listener
	 */
	public CommListener(
				Interpreter interpreter,
				URI location,
				CommProtocolFactory protocolFactory,
				VariablePath protocolConfigurationPath,
				Collection< String > operationNames,
				Map< String, AggregatedOperation > aggregationMap,
				Map< String, OutputPort > redirectionMap
			)
	{
		super( interpreter, interpreter.commCore().threadGroup(), "CommListener-" + index++ );
		this.protocolFactory = protocolFactory;
		this.operationNames = operationNames;
		this.aggregationMap = aggregationMap;
		this.redirectionMap = redirectionMap;
		this.protocolConfigurationPath = protocolConfigurationPath;
		this.location = location;
	}
	
	protected CommListener(
				Interpreter interpreter,
				Collection< String > operationNames,
				Map< String, AggregatedOperation > aggregationMap,
				Map< String, OutputPort > redirectionMap
			)
	{
		super( interpreter );
		this.protocolFactory = null;
		this.operationNames = operationNames;
		this.aggregationMap = aggregationMap;
		this.redirectionMap = redirectionMap;
		this.protocolConfigurationPath = null;
		this.location = null;
	}

	protected CommProtocol createProtocol()
		throws IOException
	{
		return protocolFactory.createProtocol( protocolConfigurationPath, location );
	}
	
	/**
	 * Returns the redirection map of this listener.
	 * @return the redirection map of this listener
	 */
	public Map< String, OutputPort > redirectionMap()
	{
		return redirectionMap;
	}

	protected URI location()
	{
		return location;
	}
	
	/**
	 * Returns <code>true</code> if this listener can handle a message for operation operationName (either directly or through aggregation), false otherwise.
	 * @param operation the <code>InputOperation</code> name to check for
	 * @return <code>true</code> if this CommListener can handle a message for the given operationName, <code>false</code> otherwise
	 */
	public boolean canHandleInputOperation( String operationName )
	{
		if ( operationNames.contains( operationName ) ) {
			return true;
		} else {
			return aggregationMap.containsKey( operationName );
		}
	}

	/**
	 * Returns <code>true</code> if this listener can handle a message for operation operationName directly (i.e. without recurring to aggregated output ports), <code>false</code> otherwise.
	 * @param operation the input operation name to check for
	 * @return <code>true</code> if this listener can handle a message for the given operationName directly, <code>false</code> otherwise.
	 */
	public boolean canHandleInputOperationDirectly( String operationName )
	{
		return operationNames.contains( operationName );
	}

	public AggregatedOperation getAggregatedOperation( String operationName )
	{
		return aggregationMap.get( operationName );
	}

	/**
	 * Requests the shutdown of this listener, so that it receives no more messages.
	 *
	 * The behaviour of this method depends on the implementation: there is no
	 * guarantee that the shutdown has been completed on return of this method,
	 * only that it has been requested.
	 */
	abstract public void shutdown();
}
