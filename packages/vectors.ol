/*
 *   Copyright (C) 2022 by Fabrizio Montesi <famontesi@gmail.com>     
 *                                                                         
 *   This program is free software; you can redistribute it and/or modify  
 *   it under the terms of the GNU Library General Public License as       
 *   published by the Free Software Foundation; either version 2 of the    
 *   License, or (at your option) any later version.                       
 *                                                                         
 *   This program is distributed in the hope that it will be useful,       
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 *   GNU General Public License for more details.                          
 *                                                                         
 *   You should have received a copy of the GNU Library General Public     
 *   License along with this program; if not, write to the                 
 *   Free Software Foundation, Inc.,                                       
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
 *                                                                         
 *   For details about the authors of this software, see the AUTHORS file. 
 */

type Vector {
	items*:undefined
}

type VectorPair {
	fst:Vector
	snd:Vector
}

type InsertRequest {
	index:int
	item:undefined
	vector:Vector
}

type AddRequest {
	vector:Vector
	item:undefined
}

type SliceRequest:void {
	from:int
	to:int
	vector:Vector
} | void {
	to:int
	vector:Vector
} | void {
	from:int
	vector:Vector
}

interface VectorsInterface {
RequestResponse:
	/// Checks if two vectors are deeply equal (all elements of the two vectors must be respectively deeply equal).
	equals( VectorPair )( bool ),
	/// Inserts an element at the specified index. The rest of the vector is shifted to the right.
	insert( InsertRequest )( Vector ),
	/// Slices the vector from an index to another.
	slice( SliceRequest )( Vector ),
	/// Adds an element to the end of a vector.
	add( AddRequest )( Vector ),
	/// Concatenates two vectors.
	concat( VectorPair )( Vector )
}

service Vectors {
	inputPort Input {
		location: "local"
		interfaces: VectorsInterface
	}

	foreign java {
		class: "joliex.lang.Vectors"
	}
}