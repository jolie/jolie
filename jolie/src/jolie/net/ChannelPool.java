/*******************************************************************************
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
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
package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jolie.Interpreter;
import jolie.net.ports.OutputPort;

public class ChannelPool
	{

		private final Map<String, Map<String, Set<CommChannel>>> threadSafeChannelPool = new HashMap<>();
		private final Map<String, Map<String, Set<CommChannel>>> nonThreadSafeChannelPool = new HashMap<>();

		public ChannelPool()
		{
		}

		private Map<String, Map<String, Set<CommChannel>>> getPool( boolean threadSafe )
		{
			if ( threadSafe ) {
				return threadSafeChannelPool;
			} else {
				return nonThreadSafeChannelPool;
			}
		}

		public CommChannel getChannel( boolean threadSafe, URI loc, OutputPort out ) throws IOException, URISyntaxException
		{
			Map< String, Map< String, Set< CommChannel>>> pool = getPool( threadSafe );
			synchronized( pool ) {
				CommChannel ret = null;
				String location = loc.toString();
				String protocol = "none";
				try {
					protocol = out.getProtocol().name();
				} catch( IOException e ) {
				}
				if ( !pool.containsKey( location ) ) {
					pool.put( location, new HashMap<>() );
				}
				if ( !pool.get( location ).containsKey( protocol ) ) {
					pool.get( location ).put( protocol, new HashSet<>() );
				}
				if ( !pool.get( location ).get( protocol ).isEmpty() ) {
					ret = pool.get( location ).get( protocol ).stream().findFirst().get();
					pool.get( location ).get( protocol ).remove( ret );
				}
				if ( ret == null || !ret.isOpen() ) {
					// We create a fresh channel
					ret = Interpreter.getInstance().commCore().createCommChannel( loc, out );
					//Interpreter.getInstance().logInfo( "created a new channel " + ret.toString() );
				}
				// else {
				//	Interpreter.getInstance().logInfo( "reusing the existing channel " + ret.toString() );
				// }
				return ret;
			}

		}

		public void releaseChannel( boolean threadSafe, URI location, String protocol, CommChannel c )
		{
			Map< String, Map< String, Set< CommChannel>>> pool = getPool( threadSafe );
			synchronized( pool ) {
				pool.get( location.toString() ).get( protocol ).add( c );
			}
		}

	}
