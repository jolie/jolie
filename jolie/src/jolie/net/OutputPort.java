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

import java.util.WeakHashMap;
import jolie.lang.Constants;
import jolie.process.AssignmentProcess;
import jolie.process.NullProcess;
import jolie.process.Process;
import jolie.process.SequentialProcess;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.VariablePathBuilder;

/**
 * This class represents a JOLIE output port, offering methods for getting
 * proper communication channels for it.
 *
 * @author Fabrizio Montesi
 */
public class OutputPort extends AbstractIdentifiableObject
{
	final private Interpreter interpreter;
	final private Process configurationProcess;
	final private VariablePath
				locationVariablePath,
				protocolVariablePath;

	/* To be called at runtime, after main is run.
	 * Requires the caller to set the variables by itself.
	 */
	public OutputPort( Interpreter interpreter, String id )
	{
		super( id );
		this.interpreter = interpreter;

		this.protocolVariablePath =
					new VariablePathBuilder( false )
					.add( id(), 0 )
					.add( Constants.PROTOCOL_NODE_NAME, 0 )
					.toVariablePath();
		
		this.locationVariablePath =
					new VariablePathBuilder( false )
					.add( id(), 0 )
					.add( Constants.LOCATION_NODE_NAME, 0 )
					.toVariablePath();

		this.configurationProcess = null;
	}
	
	// To be called by OOITBuilder
	public OutputPort(
			Interpreter interpreter,
			String id,
			String protocolId,
			Process protocolConfigurationProcess,
			URI locationURI
	) {
		super( id );
		this.interpreter = interpreter;

		this.protocolVariablePath =
					new VariablePathBuilder( false )
					.add( id(), 0 )
					.add( Constants.PROTOCOL_NODE_NAME, 0 )
					.toVariablePath();
		
		this.locationVariablePath =
					new VariablePathBuilder( false )
					.add( id(), 0 )
					.add( Constants.LOCATION_NODE_NAME, 0 )
					.toVariablePath();
		
		// Create the configuration Process
		Process a = ( locationURI == null ) ? NullProcess.getInstance() : 
			new AssignmentProcess( this.locationVariablePath, Value.create( locationURI.toString() ) );
		SequentialProcess s = new SequentialProcess();
		s.addChild( a );
		if ( protocolId != null ) {
			s.addChild( new AssignmentProcess( this.protocolVariablePath, Value.create( protocolId ) ) );
		}
		s.addChild( protocolConfigurationProcess );
		this.configurationProcess = s;
	}

	/**
	 * Gets the protocol to be used for communicating with this output port.
	 * @return the protocol to be used for communicating with this output port.
	 * @throws java.io.IOException
	 * @throws java.net.URISyntaxException
	 */
	public CommProtocol getProtocol()
		throws IOException, URISyntaxException
	{
		return getProtocol( new URI( locationVariablePath.getValue().strValue() ) );
	}

	/**
	 * Gets the protocol to be used for communicating with this output port,
	 * configuring it using the passed location URI.
	 * @param location the location to use for configuring the returned protocol
	 * @return the protocol to be used for communicating with this output port
	 * @throws java.io.IOException
	 */
	public CommProtocol getProtocol( URI location )
		throws IOException
	{
		String protocolId = protocolVariablePath.getValue().strValue();
		if ( protocolId.isEmpty() ) {
			throw new IOException( "Unspecified protocol for output port " + id() );
		}
		return interpreter.commCore().createCommProtocol(
			protocolId,
			protocolVariablePath,
			location
		);
	}

	private synchronized CommChannel getCommChannel( boolean forceNew )
		throws URISyntaxException, IOException
	{
		CommChannel ret = null;
		Value loc = locationVariablePath.getValue();
		if ( loc.isChannel() ) {
			// It's a local channel
			ret = loc.channelValue();
		} else {
			URI uri = getLocation();
			if ( forceNew ) {
				// A fresh channel was requested
				ret = interpreter.commCore().createCommChannel( uri, this );
			} else {
				// Try reusing an existing channel first
				String protocol = protocolVariablePath.getValue().strValue();
				ret = interpreter.commCore().getPersistentChannel( uri, protocol );
				if ( ret == null ) {
					ret = interpreter.commCore().createCommChannel( uri, this );
				}
			}
		}

		return ret;
	}

	private static class LazyLocalUriHolder {
		static final URI uri = URI.create( "local" );
	}

	private static final WeakHashMap< String, URI > uriCache = new WeakHashMap< String, URI > ();

	/**
	 * Returns the location URI of this output port.
	 * @return the location URI of this output port
	 * @throws java.net.URISyntaxException
	 */
	public URI getLocation()
		throws URISyntaxException
	{
		Value loc = locationVariablePath.getValue();
		if ( loc.isChannel() ) {
			return LazyLocalUriHolder.uri;
		}
		String s = loc.strValue();
		URI ret;
		if ( (ret=uriCache.get( s )) == null ) {
			ret = new URI( s );
			uriCache.put( s, ret );
		}
		return ret;
	}

	/**
	 * Returns a new and unused CommChannel for this OutputPort
	 * @return a CommChannel for this OutputPort
	 * @throws java.net.URISyntaxException
	 * @throws java.io.IOException
	 */
	public CommChannel getNewCommChannel()
		throws URISyntaxException, IOException
	{
		return getCommChannel( true );
	}

	/**
	 * Returns a CommChannel for this OutputPort, possibly reusing an
	 * open persistent channel.
	 * @return a CommChannel for this OutputPort
	 * @throws java.net.URISyntaxException
	 * @throws java.io.IOException
	 */
	public CommChannel getCommChannel()
		throws URISyntaxException, IOException
	{
		return getCommChannel( false );
	}

	/**
	 * Returns the location variable path of this output port.
	 * @return the location variable path of this output port
	 */
	public VariablePath locationVariablePath()
	{
		return locationVariablePath;
	}

	/**
	 * Returns the protocol configuration process of this output port.
	 * @return the protocol configuration process of this output port
	 */
	public Process configurationProcess()
	{
		return configurationProcess;
	}
}
