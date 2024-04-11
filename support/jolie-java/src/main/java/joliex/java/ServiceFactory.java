/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.java;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jolie.runtime.Value;
import joliex.java.impl.SocketSodepService;

/**
 * A factory for creating {@link Service} instances.
 *
 * <strong>NOTE:</strong> if your program does not exit correctly after execution you probably need
 * to call the shutdown method of the service factories you created.
 *
 * @author Fabrizio Montesi
 */
public class ServiceFactory implements Executor {
	private final ExecutorService executor;

	public ServiceFactory() {
		this( Executors.newCachedThreadPool() );
	}

	public ServiceFactory( ExecutorService executor ) {
		this.executor = executor;
	}

	@Override
	public void execute( Runnable runnable ) {
		executor.execute( runnable );
	}

	/**
	 * Shutdown this factory.
	 */
	public void shutdown() {
		executor.shutdown();
	}

	/**
	 * Creates a new {@link Service} instance.
	 *
	 * @param location the location (e.g. {@code "socket://www.jolie-lang.org:80/"}) to connect to.
	 * @param protocolName the name of the protocol to be used, e.g. {@code "sodep"}.
	 * @param protocolConfiguration the configuration {@link jolie.runtime.Value} for the protocol.
	 * @return
	 * @throws IOException if the service creation failed.
	 */
	public Service create( URI location, String protocolName, Value protocolConfiguration )
		throws IOException {
		if( executor.isShutdown() ) {
			throw new IOException( "Service factory has been shut down" );
		}

		String mediumName = location.getScheme();
		if( "socket".equals( mediumName ) ) {
			if( "sodep".equals( protocolName ) ) {
				return new SocketSodepService( this, location, protocolConfiguration );
			} else {
				throw new IOException( "Unsupported communication protocol: " + protocolName );
			}
		} else {
			throw new IOException( "Unsupported communication medium: " + mediumName );
		}
	}
}
