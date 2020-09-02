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

import jolie.Interpreter;
import jolie.runtime.TimeoutHandler;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
		CommChannel ret = null;
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if( protocolChannels != null ) {
				ret = protocolChannels.get( protocol );
				if( ret != null ) {
					if( ret.lock.tryLock() ) {
						if( ret.isOpen() ) {
							/*
							 * We are going to return this channel, but first check if it supports concurrent use. If
							 * not, then others should not access this until the caller is finished using it.
							 */
							// if ( ret.isThreadSafe() == false ) {
							removePersistentChannel( location, protocol, protocolChannels );
							// } else {
							// If we return a channel, make sure it will not timeout!
							ret.setTimeoutHandler( null );
							// if ( ret.timeoutHandler() != null ) {
							// interpreter.removeTimeoutHandler( ret.timeoutHandler() );
							// ret.setTimeoutHandler( null );
							// }
							// }
							ret.lock.unlock();
						} else { // Channel is closed
							removePersistentChannel( location, protocol, protocolChannels );
							ret.lock.unlock();
							ret = null;
						}
					} else { // Channel is busy
						removePersistentChannel( location, protocol, protocolChannels );
						ret = null;
					}
				}
			}
		}

		return ret;
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
		/*
		 * if ( channel.timeoutHandler() != null ) { interpreter.removeTimeoutHandler(
		 * channel.timeoutHandler() ); }
		 */

		final TimeoutHandler handler = new TimeoutHandler( interpreter.persistentConnectionTimeout() ) {
			@Override
			public void onTimeout() {
				try {
					synchronized( persistentChannels ) {
						if( channel.timeoutHandler() == this ) {
							removePersistentChannel( location, protocol, channel );
							channel.close();
							channel.setTimeoutHandler( null );
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		};
		channel.setTimeoutHandler( handler );
		interpreter.addTimeoutHandler( handler );
	}
}
