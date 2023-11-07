/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package joliex.metaservice;

import java.io.IOException;
import jolie.net.CommChannel;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

/**
 * The MetaService class is a bridge to a MetaService JOLIE service.
 *
 * @TODO: support nested paths
 * @author Fabrizio Montesi
 */
abstract public class MetaService {
	private final static String SHUTDOWN = "shutdown";
	private final static String ADD_REDIRECTION = "addRedirection";
	private final static String REMOVE_REDIRECTION = "removeRedirection";
	private final static String LOAD_EMBEDDED_JOLIE_SERVICE = "loadEmbeddedJolieService";
	private final static String UNLOAD_EMBEDDED_JOLIE_SERVICE = "unloadEmbeddedService";

	abstract public MetaServiceChannel getChannel()
		throws IOException;

	abstract protected CommChannel createCommChannel()
		throws IOException;

	/**
	 * Shuts down this MetaService instance.
	 *
	 * @throws java.io.IOException
	 */
	public void shutdown()
		throws IOException {
		getChannel().send( SHUTDOWN, Value.create() );
	}

	/**
	 * Adds a redirection.
	 *
	 * @param resourcePrefix the first part of the resource name the redirection will be published
	 *        under, e.g. if resourceName="MediaPlayer" then the redirection will be published in
	 *        /MediaPlayer or in /MediaPlayer-s, where s is a string.
	 * @param location the location (in JOLIE format) the redirection has to point to.
	 * @param protocol the protocol (in JOLIE format) the redirection has to use.
	 * @param metadata additional descriptive metadata to be added to the redirection.
	 * @return a MetaServiceChannel pointing to the added redirection.
	 * @throws java.io.IOException in case of communication error.
	 * @throws jolie.runtime.FaultException in case of a fault sent by the MetaService service.
	 */
	public MetaServiceChannel addRedirection( String resourcePrefix, String location, Value protocol, Value metadata )
		throws IOException, FaultException {
		final MetaServiceChannel channel = getChannel();
		Value request = Value.create();
		request.getFirstChild( "resourcePrefix" ).setValue( resourcePrefix );
		request.getFirstChild( "location" ).setValue( location );
		request.getFirstChild( "protocol" ).deepCopy( protocol );
		request.getFirstChild( "metadata" ).deepCopy( metadata );
		channel.send( ADD_REDIRECTION, request );
		Value ret = channel.recv();
		return new MetaServiceChannel( this, '/' + ret.strValue() );
	}

	/**
	 * Removes a redirection.
	 *
	 * @param resourceName the resource name identifying the redirection to remove.
	 */
	public void removeRedirection( String resourceName )
		throws IOException {
		final MetaServiceChannel channel = getChannel();
		channel.send( REMOVE_REDIRECTION, Value.create( resourceName ) );
		try {
			channel.recv(); // This is a synchronous request.
		} catch( FaultException f ) { // This should never happen.
			throw new IOException( f );
		}
	}

	/**
	 * Starts an embedded jolie service reading its source code file, publishes it and returns the
	 * created resource name.
	 *
	 * @param resourcePrefix the first part of the resource name the redirection will be published
	 *        under, e.g. if resourceName="MediaPlayer" then the redirection will be published in
	 *        /MediaPlayer or in /MediaPlayer-s, where s is a string.
	 * @param filepath the source file path of the jolie service to embed.
	 * @param metadata additional descriptive metadata to be added to the embedded service.
	 * @return a MetaServiceChannel pointing to the embedded service
	 * @throws java.io.IOException in case of communication error.
	 * @throws jolie.runtime.FaultException in case of a fault sent by the MetaService service.
	 */
	public MetaServiceChannel loadEmbeddedJolieService( String resourcePrefix, String filepath, Value metadata )
		throws IOException, FaultException {
		final MetaServiceChannel channel = getChannel();
		Value request = Value.create();
		request.getFirstChild( "resourcePrefix" ).setValue( resourcePrefix );
		request.getFirstChild( "filepath" ).setValue( filepath );
		request.getFirstChild( "metadata" ).deepCopy( metadata );
		channel.send( LOAD_EMBEDDED_JOLIE_SERVICE, request );
		Value ret = channel.recv();
		return new MetaServiceChannel( this, '/' + ret.strValue() );
	}

	/**
	 * Unloads an embedded JOLIE service.
	 *
	 * @param resourceName the resource name identifying the embedded service to remove.
	 */
	public void unloadEmbeddedJolieService( String resourceName )
		throws IOException {
		final MetaServiceChannel channel = getChannel();
		channel.send( UNLOAD_EMBEDDED_JOLIE_SERVICE, Value.create( resourceName ) );
		try {
			channel.recv(); // This is a synchronous request.
		} catch( FaultException f ) { // This should never happen.
			throw new IOException( f );
		}
	}
}
