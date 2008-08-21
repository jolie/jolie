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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Vector;

import jolie.Constants;
import jolie.process.AssignmentProcess;
import jolie.process.NullProcess;
import jolie.process.Process;
import jolie.process.SequentialProcess;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.Expression;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.Pair;
import jolie.Interpreter;

public class OutputPort extends AbstractIdentifiableObject
{
	final private Interpreter interpreter;
	final private String protocolId;
	final private Process configurationProcess;
	final private VariablePath locationVariablePath, protocolConfigurationVariablePath;

	public OutputPort(
			Interpreter interpreter,
			String id,
			String protocolId,
			Process protocolConfigurationProcess,
			URI locationURI
			)
	{
		super( id );
		this.interpreter = interpreter;
		this.protocolId = protocolId;
		
		// Create the location VariablePath
		Vector< Pair< Expression, Expression > > path =
					new Vector< Pair< Expression, Expression > >();
		path.add( new Pair< Expression, Expression >( Value.create( id ), null ) );
		path.add( new Pair< Expression, Expression >( Value.create( Constants.LOCATION_NODE_NAME ), null ) );
		this.locationVariablePath = new VariablePath( path, false );
		
		// Create the configuration Process
		Process a = ( locationURI == null ) ? NullProcess.getInstance() : 
			new AssignmentProcess( this.locationVariablePath, Value.create( locationURI.toString() ) );
		SequentialProcess s = new SequentialProcess();
		s.addChild( a );
		s.addChild( protocolConfigurationProcess );
		this.configurationProcess = s;
		
		path = new Vector< Pair< Expression, Expression > >();
		path.add( new Pair< Expression, Expression >( Value.create( id ), null ) );
		path.add( new Pair< Expression, Expression >( Value.create( Constants.PROTOCOL_NODE_NAME ), null ) );
		this.protocolConfigurationVariablePath = new VariablePath( path, false );
	}
	
	private CommProtocol protocol = null;
	
	public CommProtocol getProtocol()
		throws IOException, URISyntaxException
	{
		CommProtocol retVal = protocol;
		if ( retVal == null ) {
			if ( protocolId == null ) {
				throw new IOException( "Unspecified protocol for output port " + id() );
			}
			retVal = interpreter.commCore().createCommProtocol(
				protocolId,
				protocolConfigurationVariablePath,
				new URI( locationVariablePath.getValue().strValue() )
			);
		} else {
			retVal = retVal.clone();
		}
		
		
		return retVal;
	}
	
	private CommChannel channel = null;
	private URI channelURI = null;

	public CommChannel getCommChannel()
		throws URISyntaxException, IOException
	{
		Value loc = locationVariablePath.getValue();
		if ( loc.isChannel() ) {
			channel = loc.channelValue();
		} else {
			URI uri = new URI( loc.strValue() );
			if ( !uri.equals( channelURI ) || !channel.canBeReused() ) {
				channel = interpreter.commCore().createCommChannel( uri, this );
				channelURI = uri;
			} else {
				channel.refreshProtocol();
			}
		}
		
		return channel;
	}
	
	public VariablePath locationVariablePath()
	{
		return locationVariablePath;
	}	
	
	public Process configurationProcess()
	{
		return configurationProcess;
	}
	
}
