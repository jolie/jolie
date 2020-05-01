/*
 *   Copyright (C) 2020 by Claudio Guidi <cguidi@italianasoftware.com>    
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

 include "types/definition_types.iol"

type CompareValuesRequest: bool | void {
    v1: undefined
    v2: undefined
}

type InterfaceDefinitionLessThanRequest: void {
	i1: Interface 
	i2: Interface
}
type InterfaceDefinitionLessThanResponse: void {
	result: bool 
	errors*: string
}

type TypeDefinitionLessThanRequest: void {
	t1: string {
		types*: TypeDefinition
	}
	t2: string {
		types*: TypeDefinition
	}
}

type PortDefinitionLessThanRequest: void {
	p1: Port 
	p2: Port
}
type PortDefinitionLessThanResponse: void {
	result: bool 
	errors*: string
}

interface MetaJolieServiceInterface {
RequestResponse:
	/**!
	it checks if two values are exactly the same
	vectors are strictly compared by index
	returns void if the comparison had success, raises ComparisonFailed fault otherwise 
	**/
	compareValuesStrict( CompareValuesRequest )( void ) throws ComparisonFailed( string ),

	/**!
	it checks if two values are exactly the same
	vectors are compared by element presence without testing the index
	returns void if the comparison had success, raises ComparisonFailed fault otherwise 
	**/
	compareValuesVectorLight( CompareValuesRequest )( void ) throws ComparisonFailed( string ),

	/**!
	it checks if a type is less than another type. A type is less than another if it is contained in it
	*/
	typeDefinitionLessThan( TypeDefinitionLessThanRequest )( bool ) throws TypeMissing( string ),

	/**!
	it checks if an interface is less than another interface. An interface is less than another if it has at least all the 
	operations declared by the second one, and all the used types are less than the target ones. 
	*/
	interfaceDefinitionLessThan( InterfaceDefinitionLessThanRequest )( InterfaceDefinitionLessThanResponse ) throws TypeMissing( string ),

	/**!
	it checks if a port is less than another port. It checks of there is the same protocol declared and then it checks if all the aggregated interfaces
	with the interfaceDefinitionLessThan
	*/
	portDefinitionLessThan( PortDefinitionLessThanRequest )( PortDefinitionLessThanResponse ) throws TypeMissing( string )
}

