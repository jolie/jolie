/*
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import jolie.Interpreter;
import jolie.util.Helpers;

public class ChannelCache {
	// Location URI -> Protocol name -> Persistent CommChannel object
	private final Map< URI, Map< String, CommChannel > > persistentChannels = new HashMap<>();

	private void removePersistentChannel( URI location, String protocol, Map< String, CommChannel > protocolChannels ) {
		protocolChannels.remove( protocol );
		if( protocolChannels.isEmpty() ) {
			persistentChannels.remove( location );
		}
	}

	private void removePersistentChannel( URI location, String protocol, CommChannel channel ) {
		if( persistentChannels.containsKey( location ) ) {
			if( persistentChannels.get( location ).get( protocol ) == channel ) {
				removePersistentChannel( location, protocol, persistentChannels.get( location ) );
			}
		}
	}

	public CommChannel getPersistentChannel( URI location, String protocol ) {
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if( protocolChannels != null ) {
				final var ret = protocolChannels.get( protocol );
				if( ret != null ) {
					removePersistentChannel( location, protocol, protocolChannels );
					if( Helpers.tryLockOrElse( ret.rwLock, () -> ret.isOpen() && ret.cancelTimeoutHandler(),
						() -> false ) ) {
						return ret;
					}
				}
			}
		}

		return null;
	}

	public void putPersistentChannel( URI location, String protocol, final CommChannel channel,
		Interpreter interpreter ) {
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels =
				persistentChannels.computeIfAbsent( location, k -> new HashMap<>() );
			// Set the timeout
			setTimeoutHandler( channel, location, protocol, interpreter );
			// Put the protocol in the cache (may overwrite another one)
			protocolChannels.put( protocol, channel );
			/*
			 * if ( protocolChannels.size() <= connectionCacheSize && protocolChannels.containsKey( protocol )
			 * == false ) { // Set the timeout setTimeoutHandler( channel ); // Put the protocol in the cache
			 * protocolChannels.put( protocol, channel ); } else { try { if ( protocolChannels.get( protocol )
			 * != channel ) { channel.close(); } else { setTimeoutHandler( channel ); } } catch( IOException e )
			 * { interpreter.logWarning( e ); } }
			 */
		}
	}

	private void setTimeoutHandler( final CommChannel channel, final URI location, final String protocol,
		Interpreter interpreter ) {
		channel.setTimeoutHandler( () -> {
			try {
				synchronized( persistentChannels ) {
					removePersistentChannel( location, protocol, channel );
					channel.close();
				}
			} catch( IOException e ) {
				interpreter.logSevere( e );
			}
		}, interpreter, interpreter.persistentConnectionTimeout() );
	}
}
