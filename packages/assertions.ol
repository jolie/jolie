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

from values import Values
from string-utils import StringUtils

type AssertionRequest {
	actual:undefined
	expected:undefined
}

interface AssertionsInterface {
RequestResponse:
	equals(AssertionRequest)(void) throws AssertionError(string) //(ValueDiff)
}

service Assertions {
	execution: concurrent

	embed Values as valuesService
	embed StringUtils as stringUtils
	
	inputPort Input {
		location: "local"
		interfaces: AssertionsInterface
	}

	init {
		install( default => nullProcess )
	}

	main {
		equals( request )() {
			if( !equals@valuesService( { fst -> request.expected, snd -> request.actual } ) ) {
				throw( AssertionError, valueToPrettyString@stringUtils(request) ) //, diff@valuesService( request ) )
			}
		}
	}
}