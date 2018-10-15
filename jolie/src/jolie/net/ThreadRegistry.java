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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jolie.ExecutionThread;


class ThreadRegistry
{
	private final ConcurrentMap<String, ExecutionThread> registry = new ConcurrentHashMap<>();
	
	private void addThread( String k, ExecutionThread t  ){
		registry.put( k, t );
	}
	
	public void addThread( CommChannel c, ExecutionThread t ){
		addThread( c.toString(), t );
	}
	
	public void addThread( CommMessage c, ExecutionThread t ){
		if( c.hasGenericId() ){
			throw new UnsupportedOperationException( "Requested registry of thread " + t.toString() + " under a generic message ID." );
		}
		addThread( Long.toString( c.id() ), t );
	}
	
	// GET REMOVES THE THREAD
	private ExecutionThread getThread( String k ){
		return registry.remove( k );
	}
	
	public ExecutionThread getThread( Long id ){
		if( CommMessage.GENERIC_ID == id ){
			throw new UnsupportedOperationException( "Requested retrieval of execution thread under a generic message ID." );
		}
		return getThread( Long.toString( id ) );
	}
	
	public ExecutionThread getThread( CommChannel c ){
		return getThread( c.toString() );
	}
	
	// PICK FETCHS BUT DOES NOT REMOVE THE THREAD
	private ExecutionThread pickThread( String k ){
		return registry.get( k );
	}
	
	public ExecutionThread pickThread( Long id ){
		if( CommMessage.GENERIC_ID == id ){
			throw new UnsupportedOperationException( "Requested retrieval of execution thread under a generic message ID." );
		}
		return pickThread( Long.toString( id ) );
	}
	
	public ExecutionThread pickThread( CommChannel c ){
		return pickThread( c.toString() );
	}
}
