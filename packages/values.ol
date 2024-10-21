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

type ComparisonRequest {
	fst:undefined //< The first value
	snd:undefined //< The second value
}

interface ValuesInterface {
	requestResponse:
		/**
		 * Returns true if two values are deeply equal to each other and false otherwise.
		 */
		equals(ComparisonRequest)(bool),
		/**
		 * Returns a hash code value for the argument.
		 */
		hashCode(undefined)(int)
}

service Values {
	inputPort input {
		location: "local"
		interfaces: ValuesInterface
	}
	
	foreign java {
		class: "joliex.lang.Values"
	}
}