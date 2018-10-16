/*******************************************************************************
 *   Copyright (C) 2016 by Claudio Guidi <guidiclaudio@gmail.com>              *
 *   Copyright (C) 2018 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package jolie.net.auto;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AndJarDeps;

/**
 *
 * @author Claudio Guidi
 */
@AndJarDeps( {
	"ini4j.jar"
} )
public class AutoChannelFactory extends CommChannelFactory {

	private final CommCore commCore;
	private final HashMap<URI, String> locationMap = new HashMap<>();

	public AutoChannelFactory( CommCore commCore ) {
		super( commCore );
		this.commCore = commCore;
	}

	@Override
	public CommChannel createChannel( URI locationURI, OutputPort port ) throws IOException {

		String location = null;
		if ( !this.locationMap.containsKey( locationURI ) ) {
			String[] ss = locationURI.getSchemeSpecificPart().split( ":", 2 );
			if ( "ini".equals( ss[ 0 ] ) ) {
				location = AutoHelper.getLocationFromIni( ss[ 1 ] );
				locationMap.put( locationURI, location );
			} else {
				AutoHelper.throwIOException( "unsupported scheme: " + locationURI.getScheme() );
			}
		} else {
			location = locationMap.get( locationURI );
		}

		AutoHelper.assertIOException( location == null, "internal error: location is null" );
		AutoHelper.assertIOException( location.equals( Constants.LOCAL_LOCATION_KEYWORD ), "autoconf does not support local locations" );

		try {
			URI uri = new URI( location );
			return commCore.createCommChannel( uri, port );
		} catch ( URISyntaxException e ) {
			throw new IOException( e );
		}
	}

	@Override
	public CommChannel createInputChannel( URI location, InputPort port, CommProtocol protocol ) throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}

}
