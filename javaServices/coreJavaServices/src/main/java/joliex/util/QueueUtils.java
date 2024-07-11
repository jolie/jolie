/***************************************************************************
 *   Copyright (C) by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
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

package joliex.util;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class QueueUtils extends JavaService {
	private final Map< String, LinkedList< Value > > queueMap = new ConcurrentHashMap<>();

	private boolean has_queue( String queue_name ) {
		return queueMap.containsKey( queue_name );
	}

	/**
	 * Creates a new queue in the HashMap with the given queue_name as key
	 *
	 * @param queue_name the key corresponding to the queue
	 * @return Boolean - false if the queue_name is already in use
	 */
	public Boolean new_queue( String queue_name ) {
		if( has_queue( queue_name ) ) {
			return false;
		} else {
			LinkedList< Value > new_queue = new LinkedList<>();
			queueMap.put( queue_name, new_queue );
			return true;
		}
	}

	/**
	 * Removes an existing queue from the HashMap
	 *
	 * @param queue_name the key corresponding to the queue
	 * @return Boolean - false if the queue_name does not exist
	 */
	public Boolean delete_queue( String queue_name ) {
		if( has_queue( queue_name ) ) {
			queueMap.remove( queue_name );
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Pushes an element at the end of an existing queue
	 *
	 * @param request
	 * @return Boolean - false if the queue does not exist
	 */
	public Boolean push( Value request ) {
		String queue_key = request.getFirstChild( "queue_name" ).strValue();
		if( has_queue( queue_key ) ) {
			Value element = request.getFirstChild( "element" );
			LinkedList< Value > queue = queueMap.get( queue_key );
			queue.offerLast( element );
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retrieves, but does not remove, the head of the queue
	 *
	 * @param queue_name
	 * @return The element, null otherwise
	 */
	public Value peek( String queue_name ) {
		Value element = null;
		if( has_queue( queue_name ) ) {
			element = queueMap.get( queue_name ).peekFirst();
		}
		return element;
	}

	/**
	 * Removes and returns the head of the queue
	 *
	 * @param queue_name
	 * @return The element, null otherwise
	 */
	public Value poll( String queue_name ) {
		Value element = null;
		if( has_queue( queue_name ) ) {
			element = queueMap.get( queue_name ).pollFirst();
		}
		return element;
	}

	/**
	 * Returns the size of an existing queue, null otherwise
	 *
	 * @param queue_name
	 * @return The size of the queue, null otherwise
	 */
	public Integer size( String queue_name ) {
		Integer queue_size = null;
		if( has_queue( queue_name ) ) {
			LinkedList< Value > queue = queueMap.get( queue_name );
			queue_size = queue.size();
		}
		return queue_size;
	}

}
