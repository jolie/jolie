/********************************************************************************
 *   Copyright (C) 2013 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>	*
 *                                                                         			*
 *   This program is free software; you can redistribute it and/or modify  			*
 *   it under the terms of the GNU Library General Public License as       			*
 *   published by the Free Software Foundation; either version 2 of the    			*
 *   License, or (at your option) any later version.                       			*
 *                                                                         			*
 *   This program is distributed in the hope that it will be useful,       			*
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        			*
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         			*
 *   GNU General Public License for more details.                          			*
 *                                                                         			*
 *   You should have received a copy of the GNU Library General Public     			*
 *   License along with this program; if not, write to the                 			*
 *   Free Software Foundation, Inc.,                                       			*
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             			*
 *                                                                         			*
 *   For details about the authors of this software, see the AUTHORS file. 			*
 ********************************************************************************/


type QueueRequest: void {
	.queue_name: string
	.element: undefined
}

interface QueueUtilsInterface {
RequestResponse:
	//Creates a new queue with queue_name as key
	new_queue( string )( bool ),

	//Removes an existing queue
	delete_queue( string )( bool ),

	//Pushes an element at the end of an existing queue
	push( QueueRequest )( bool ),

	//Retrieves, but does not remove, the head of the queue
	peek( string )( undefined ),
	
	//Removes and returns the head of the queue
	poll( string )( undefined ),

	//Returns the size of an existing queue, null otherwise
	size( string )( int )
}

outputPort QueueUtils {
Interfaces: QueueUtilsInterface
}

embedded {
Java:
	"joliex.util.QueueUtils" in QueueUtils
}
