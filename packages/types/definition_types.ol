/*
 *   Copyright (C) 2009 Claudio Guidi <cguidi@italianasoftware.com>
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

type NativeType: void {
  .string_type: bool
} | void {
  .int_type: bool
} | void {
  .double_type: bool
} | void {
  .any_type: bool
} | void {
  .void_type: bool
} | void {
  .raw_type: bool
} | void {
  .bool_type: bool
} | void {
  .long_type: bool
} 


type Cardinality: void {
  .min: int
  .max?: int
  .infinite?: int
}

type SubType: void {
  .name: string
  .cardinality: Cardinality
  .type: Type
  .documentation?: string
}

type TypeInLine: void {
  .root_type: NativeType
  .sub_type*: SubType
}

type TypeLink: void {
  .link_name: string
}

type TypeChoice: void {
  .choice: void {
      .left_type: TypeInLine | TypeLink 
      .right_type: Type
  }
}

type TypeUndefined: void {
  .undefined: bool
}

type Type: TypeInLine | TypeLink | TypeChoice | TypeUndefined 

type TypeDefinition: void {
  .name: string
  .type: Type
  .documentation?: string
}


type Fault: void {
  .name: string
  .type: NativeType | TypeUndefined | TypeLink
}

type Operation: void {
  .operation_name: string
  .documentation?: string
  .input: string
  .output?: string
  .fault*: Fault
}

type Interface: void {
  .name: string
  .types*: TypeDefinition
  .operations*: Operation
  .documentation?: string
}

type Port: void {
  .name: string
  .protocol: string
  .location: any
  .interfaces*: Interface
}

type Service: void {
  .name: string
  .input*: string
  .output*: string
}
