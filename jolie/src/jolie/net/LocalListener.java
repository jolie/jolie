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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AggregatedOperation;

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
		super( interpreter, new HashSet< String >(), new HashMap< String, AggregatedOperation >(), new HashMap< String, OutputPort >() );
	}
	
	public void addOperationNames( Collection< String > operationNames )
	{
		this.operationNames.addAll( operationNames );
	}
	
	public void addRedirections( Map< String, OutputPort > redirectionMap )
	{
		this.redirectionMap.putAll( redirectionMap );
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
