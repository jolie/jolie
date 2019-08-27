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

type Name: void {
  .name: string
  .domain?: string
  .registry?: string       // if omitted = local
}

type NativeType: void {
  .string_type: bool
} 
|
void {
  .int_type: bool
}
|
void {
  .double_type: bool
}
|
void {
  .any_type: bool
}
| 
void {
  .void_type: bool
}
| 
void {
  .raw_type: bool
}
|
void {
  .bool_type: bool
}
|
void {
  .long_type: bool
}
| 
void {
  .link: void {
     .name: string
     .domain?: string
  }
}

type Cardinality: void {
  .min: int
  .max?: int
  .infinite?: int
}

type SubType: void {
  .name: string
  .cardinality: Cardinality
  .type_inline: Type
}
|
void {
  .name: string
  .cardinality: Cardinality
  .type_link: Name
}

type ChoiceBranch: void {
  .type_inline: Type
}
|
void {
  .type_link: Name
}

type Type: void {
  .name: Name
  .root_type: NativeType
  .sub_type*: SubType
} | void {
  .name?: Name
  .choice?: void {
      .left_type: ChoiceBranch 
      .right_type: ChoiceBranch 
  }
}


type Fault: void {
  .name: Name
  .type_name?: Name
}

type Operation: void {
  .operation_name: string
  .documentation?: any
  .input: Name
  .output?: Name
  .fault*: Fault
}

type Interface: void {
  .name: Name
  .types*: Type
  .operations*: Operation
}

type Port: void {
  .name: Name
  .protocol: string
  .location: any
  .interfaces*: Interface
}

type Service: void {
  .name: Name
  .input*: void {
    .name: string
    .domain: string
  }
  .output*: Name
}
