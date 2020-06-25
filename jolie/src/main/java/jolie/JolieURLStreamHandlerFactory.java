/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi                                *
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

package jolie;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jolie.jap.JapURLStreamHandler;

/**
 *
 * @author Fabrizio Montesi
 */
public class JolieURLStreamHandlerFactory implements URLStreamHandlerFactory {
	private final static AtomicBoolean REGISTERED = new AtomicBoolean( false );

	private final Map< String, URLStreamHandler > handlers = new HashMap<>();

	public JolieURLStreamHandlerFactory() {
		handlers.put( "jap", new JapURLStreamHandler() );
	}

	@Override
	public URLStreamHandler createURLStreamHandler( String protocol ) {
		return handlers.get( protocol );
	}

	public void putURLStreamHandler( String protocol, URLStreamHandler handler ) {
		handlers.put( protocol, handler );
	}

	public static void registerInVM() {
		if( REGISTERED.compareAndSet( false, true ) ) {
			URL.setURLStreamHandlerFactory( new JolieURLStreamHandlerFactory() );
		}
	}
}
