/***************************************************************************
 *   Copyright 2011 (C) by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.net.ports;

import java.net.URI;
import java.util.Map;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.VariablePath;

/**
 * Represents an input port definition.
 * @author Fabrizio Montesi
 */
public class InputPort implements Port
{
	private final Interface iface;
	private final VariablePath protocolConfigurationPath;
	private final Map< String, AggregatedOperation > aggregationMap;
	private final Map< String, OutputPort > redirectionMap;
	private final URI location;

	/**
	 * Constructor
	 * @param location the location of the input port
	 * @param protocolConfigurationPath the protocol configuration variable path of this port
	 * @param iface the interface of this input port
	 * @param aggregationMap the aggregation map for this input port
	 * @param redirectionMap the redirection map for this input port
	 */
	public InputPort(
		URI location,
		VariablePath protocolConfigurationPath,
		Interface iface,
		Map< String, AggregatedOperation > aggregationMap,
		Map< String, OutputPort > redirectionMap
	) {
		this.location = location;
		this.iface = iface;
		this.aggregationMap = aggregationMap;
		this.redirectionMap = redirectionMap;
		this.protocolConfigurationPath = protocolConfigurationPath;
	}

	public Interface getInterface()
	{
		return iface;
	}

	public VariablePath protocolConfigurationPath()
	{
		return protocolConfigurationPath;
	}

	public Map< String, AggregatedOperation > aggregationMap()
	{
		return aggregationMap;
	}

	/**
	 * Returns the redirection map of this listener.
	 * @return the redirection map of this listener
	 */
	public Map< String, OutputPort > redirectionMap()
	{
		return redirectionMap;
	}

	/**
	 * Returns the location URI of this input port.
	 * @return the location URI of this input port.
	 */
	public URI location()
	{
		return location;
	}

	/**
	 * Returns <code>true</code> if this input port can handle a message for operation operationName (either directly or through aggregation), false otherwise.
	 * @param operation the <code>InputOperation</code> name to check for
	 * @return <code>true</code> if this CommListener can handle a message for the given operationName, <code>false</code> otherwise
	 */
	public boolean canHandleInputOperation( String operationName )
	{
		if ( canHandleInputOperationDirectly( operationName ) ) {
			return true;
		} else {
			return aggregationMap.containsKey( operationName );
		}
	}

	/**
	 * Returns <code>true</code> if this input port can handle a message for operation operationName directly (i.e. without recurring to aggregated output ports), <code>false</code> otherwise.
	 * @param operation the input operation name to check for
	 * @return <code>true</code> if this listener can handle a message for the given operationName directly, <code>false</code> otherwise.
	 */
	public boolean canHandleInputOperationDirectly( String operationName )
	{
		return iface.containsOperation( operationName );
	}

	public AggregatedOperation getAggregatedOperation( String operationName )
	{
		return aggregationMap.get( operationName );
	}
}
