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

type NativeType: void {
  .string_type?: int
  .int_type?: int
  .double_type?: int
  .any_type?: int
  .void_type?: int
  .undefined_type?: int
  .link?: string
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
  .type_link?: string 
}

type Type: void {
  .name: string
  .root_type: NativeType
  .sub_type*: SubType
}

type Operation: void {
  .name: string
  .input: string
  .output?: string
}

type Interface: void {
  .name: string
  .types*: Type
  .operations*: Operation
}

type Participant: void {
  .name: string
  .protocol: string
  .location: any
  .interfaces: Interface
}

type Conversation: void {
  .operation: string
  .participant_type: void {
    .is_input?: int
    .is_output?: int
  }
}

type Role: void {
  .name: string
  .input: Participant
  .output?: Participant
  .conversation*: Conversation 	
}