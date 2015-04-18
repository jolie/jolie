 /****************************************************************************
   Copyright 2010 by Claudio Guidi <cguidi@italianasoftware.com>      
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
********************************************************************************/

type Name: void {
  .name: string
  .domain?: string
  .registry?: string       // if omitted = local
}

type NativeType: void {
  .string_type?: bool
  .int_type?: bool
  .double_type?: bool
  .any_type?: bool
  .void_type?: bool
  .raw_type?: bool
  //.undefined_type?: bool
  .bool_type?: bool
  .long_type?: bool
  .link?: void {
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
  .type_inline?: Type
  .type_link?: Name
}

type Type: void {
  .name: Name
  .root_type: NativeType
  .sub_type*: SubType
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

type Participant: void {
  .name: Name
  .protocol: string
  .location: any
  .interfaces*: Interface
}

type Conversation: void {
  .operation: string
  .participant_type: void {
    .is_input?: int
    .is_output?: int
  }
}

type Role: void {
  .name: Name
  .input: Participant
  .output?: Participant
  .conversation*: Conversation 	
}

type Service: void {
  .name: Name
  .input*: void {
    .name: string
    .domain: string
  }
  .output*: Name
}
