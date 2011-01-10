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

import java.net.URI;
import java.util.HashMap;
import jolie.net.ports.OutputPort;
import java.util.Map;

import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.ports.InputPort;
import jolie.net.ports.Interface;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

/**
 * <code>LocalListener</code> is used internally by the interpreter for receiving
 * local messages.
 * 
 * @author Fabrizio Montesi
 */
public class LocalListener extends CommListener
{
	public LocalListener( Interpreter interpreter )
	{
		super( interpreter, new InputPort(
				URI.create( Constants.LOCAL_LOCATION_KEYWORD ),
				new VariablePathBuilder( true ).toVariablePath(),
				new Interface(
					new HashMap< String, OneWayTypeDescription >(),
					new HashMap< String, RequestResponseTypeDescription >()
				),
				new HashMap< String, AggregatedOperation >(),
				new HashMap< String, OutputPort >()
			)
		);
	}
	
	public void mergeInterface( Interface iface )
	{
		inputPort().getInterface().merge( iface );
	}
	
	public void addRedirections( Map< String, OutputPort > redirectionMap )
	{
		inputPort().redirectionMap().putAll( redirectionMap );
	}

	public void addAggregations( Map< String, AggregatedOperation > aggregationMap )
	{
		inputPort().aggregationMap().putAll( aggregationMap );
	}

	@Override
	public void shutdown()
	{}
	
	@Override
	public void run()
	{}
	
	@Override
	public CommProtocol createProtocol()
	{
		return null;
	}
	
	@Override
	final public void start()
	{}
}
