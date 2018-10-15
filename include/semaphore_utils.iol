/********************************************************************************
 *   Copyright (C) 2013 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>	*
 *                                                                         	*
 *   This program is free software; you can redistribute it and/or modify  	*
 *   it under the terms of the GNU Library General Public License as       	*
 *   published by the Free Software Foundation; either version 2 of the    	*
 *   License, or (at your option) any later version.                       	*
 *                                                                         	*
 *   This program is distributed in the hope that it will be useful,       	*
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        	*
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         	*
 *   GNU General Public License for more details.                          	*
 *                                                                         	*
 *   You should have received a copy of the GNU Library General Public     	*
 *   License along with this program; if not, write to the                 	*
 *   Free Software Foundation, Inc.,                                       	*
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             	*
 *                                                                         	*
 *   For details about the authors of this software, see the AUTHORS file. 	*
 ********************************************************************************/

type SemaphoreRequest: void {
	.name: string
	// the optional number of permits to release/acquire
	.permits?: int
}

interface SemaphoreUtilsInterface {
	RequestResponse:
	
	/**!
	* Releases permits to a semaphore.
	* If there exists no semaphore with the given ".name", "release" creates a
	* new semaphore with that name and as many permits as indicated in ".permits".
	* The default behaviour when value ".permits" is absent is to release one permit.
	*/
	release( SemaphoreRequest )( bool ),
	
	/**!
	 * Acquires permits from a semaphore.
	 * If there exists no semaphore with the given ".name", "acquire" creates a 
	 * new semaphore with 0 permits with that name.
	 * The operation returns a response when a new permit is released (see operation "release").
	 * The default behaviour when value ".permits" is absent is to acquire one permit.
	 */
	acquire( SemaphoreRequest )( bool )
}

outputPort SemaphoreUtils {
	Interfaces: SemaphoreUtilsInterface
}

embedded {
Java:
	"joliex.util.SemaphoreUtils" in SemaphoreUtils
}
