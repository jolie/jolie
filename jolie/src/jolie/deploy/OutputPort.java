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

package jolie.deploy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Vector;

import jolie.Constants;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommProtocol;
import jolie.net.HTTPProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.process.AssignmentProcess;
import jolie.process.NullProcess;
import jolie.process.Process;
import jolie.process.SequentialProcess;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.Expression;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.Pair;

public class OutputPort extends AbstractIdentifiableObject
{
	private Collection< String > operations;
	private Constants.ProtocolId protocolId;
	private Process configurationProcess;
	private VariablePath locationVariablePath, protocolConfigurationVariablePath;

	public OutputPort(
			String id,
			Collection< String > operations,
			Constants.ProtocolId protocolId,
			Process protocolConfigurationProcess,
			URI locationURI
			)
	{
		super( id );
		this.operations = operations;
		this.protocolId = protocolId;
		
		// Create the location VariablePath
		Vector< Pair< String, Expression > > path =
					new Vector< Pair< String, Expression > >();
		path.add( new Pair< String, Expression >( id, null ) );
		path.add( new Pair< String, Expression >( "location", null ) );
		this.locationVariablePath = new VariablePath( path, null, false );
		
		// Create the configuration Process
		Process a = ( locationURI == null ) ? NullProcess.getInstance() : 
			new AssignmentProcess( this.locationVariablePath, Value.create( locationURI.toString() ) );
		SequentialProcess s = new SequentialProcess();
		s.addChild( a );
		s.addChild( protocolConfigurationProcess );
		this.configurationProcess = s;
		
		path = new Vector< Pair< String, Expression > >();
		path.add( new Pair< String, Expression >( id, null ) );
		path.add( new Pair< String, Expression >( "protocol", null ) );
		this.protocolConfigurationVariablePath = new VariablePath( path, null, false );
	}
	
	private CommProtocol getProtocol( URI uri )
		throws URISyntaxException, IOException
	{
		if ( protocolId == null )
			throw new IOException( "Unknown protocol for output port " + id() );
		CommProtocol ret = null;
		if ( protocolId.equals( Constants.ProtocolId.SODEP ) ) {
			ret = new SODEPProtocol( protocolConfigurationVariablePath );
		} else if ( protocolId.equals( Constants.ProtocolId.SOAP ) ) {
			ret = new SOAPProtocol(
						protocolConfigurationVariablePath,
						uri,
						Interpreter.getInstance()
					);
		} else if ( protocolId.equals( Constants.ProtocolId.HTTP ) ) {
			ret = new HTTPProtocol(
						protocolConfigurationVariablePath,
						uri
					);
		}
		
		assert( ret != null );
		return ret;
	}
	
	public CommChannel createCommChannel()
		throws URISyntaxException, IOException
	{
		CommChannel channel;
		Value loc = locationVariablePath.getValue();
		if ( loc.isChannel() )
			channel = loc.channelValue();
		else {
			URI uri = new URI( loc.strValue() );
			CommProtocol protocol = getProtocol( uri );
			channel =
				CommChannel.createCommChannel(
					uri,
					protocol
					);
		}
		
		return channel;
	}
	
	public VariablePath locationVariablePath()
	{
		return locationVariablePath;
	}
	
	public Collection< String > operations()
	{
		return operations;
	}
	
	public Constants.ProtocolId protocolId()
	{
		return protocolId;
	}
	
	public Process configurationProcess()
	{
		return configurationProcess;
	}
	
}
