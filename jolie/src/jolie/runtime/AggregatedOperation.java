/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.runtime;

import jolie.lang.Constants;
import jolie.net.OutputPort;

/**
 * An AggregatedOperation instance contains information about an operation that is aggregate by an input port.
 * @author Fabrizio Montesi
 */
public class AggregatedOperation
{
	final private Constants.OperationType type;
	final private String name;
	final private OutputPort port;

	public AggregatedOperation( String name, Constants.OperationType type, OutputPort port )
	{
		this.name = name;
		this.type = type;
		this.port = port;
	}

	/**
	 * Returns the name of this operation.
	 * @return the name of this operation.
	 */
	public String name()
	{
		return name;
	}

	/**
	 * Returns true if this operation is a Request-Response.
	 * @return true if this operation is a Request-Response.
	 */
	public boolean isRequestResponse()
	{
		return type == Constants.OperationType.REQUEST_RESPONSE;
	}

	/**
	 * Returns true if this operation is a One-Way.
	 * @return true if this operation is a One-Way.
	 */
	public boolean isOneWay()
	{
		return type == Constants.OperationType.ONE_WAY;
	}

	/**
	 * Returns the OutputPort of this aggregated operation.
	 * @return the OutputPort of this aggregated operation.
	 */
	public OutputPort port()
	{
		return port;
	}
}
