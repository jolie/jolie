/*
 *   Copyright (C) 2011 by Claudio Guidi <cguidi@italianasoftware.com>    
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

// FAULTS
type InputPortMetaDataFault: void {
  .message: string
}

// MESSAGE TYPES

type CheckNativeTypeRequest: void {
  .type_name: string          //< the type name to check it is native
}

type CheckNativeTypeResponse: void {
  .result: bool
}

type GetMetaDataRequest: void {
  .filename: string             //< the filename where the service definition is
}

type GetMetaDataResponse: void {
  .service*: Service             //< the definition of the service
  .input*: Port                 //< the definitions of all the input ports
  .output*: Port                //< the definitions of all the output ports
  .interfaces*: Interface       //< the definitions of all the interfaces
  .types*: TypeDefinition       //< the definitions of all the types
  /// the definitions of all the embedded services
  .embeddedServices*: void {    
	    .type: string             //< type of the embedded service
	    .servicepath: string      //< path where the service can be found
	    .portId?: string           //< target output port where the embedded service is bound
  }
  .communication_dependencies*: CommunicationDependency
}

type CommunicationDependency: void {
  .input_operation: void {
      .name: string             //<name of the operation
      .type: string             //<RequestResponse or OneWay
  }
  .dependencies*: void {
      .name: string             //< name of the operation
      .port?: string            //< defined only if type is Notification or SolicitResponse
      .type: string             //<RequestResponse, OneWay, SolicitResponse or Notification
  }

}

type GetInputPortMetaDataResponse: void {
  .input*: Port                 //< the full description of each input port of the service definition
}

type GetOutputPortMetaDataResponse: void {
  .output*: Port                 //< the full description of each output port of the service definition
}



type MessageTypeCastRequest: void {
  .message: undefined           //< the message to be cast
  /// the types to use for casting the message
  .types: void {                
	     .messageTypeName: string   //< starting type to user for casting
	     .types*: Type            //< list of all the required types
  }
}

type MessageTypeCastResponse: void {
  .message: undefined            //< casted message
}

type ParserExceptionType: void {
  .message: string
  .line: int
  .sourceName: string
}

type SemanticExceptionType: void {
  .error*: void {
      .message: string
      .line: int
      .sourceName: string
  }
}

type GetNativeTypeFromStringRequest: void {
  .type_name: string
}

type GetNativeTypeStringListResponse: void {
  .native_type*: string
}


interface MetaJolieJavaServiceInterface {
RequestResponse:
	checkNativeType( CheckNativeTypeRequest )( CheckNativeTypeResponse ),
  getNativeTypeFromString( GetNativeTypeFromStringRequest )( NativeType ) throws NativeTypeDoesNotExist,
  getNativeTypeStringList( void )( GetNativeTypeStringListResponse ),
	getMetaData( GetMetaDataRequest )( GetMetaDataResponse )
	    throws  ParserException( ParserExceptionType )
		          SemanticException( SemanticExceptionType ),
	getInputPortMetaData( GetMetaDataRequest )( GetInputPortMetaDataResponse )
	    throws  InputPortMetaDataFault
		          ParserException( ParserExceptionType )
		          SemanticException( SemanticExceptionType ),
  getOutputPortMetaData( GetMetaDataRequest )( GetOutputPortMetaDataResponse )
	    throws  OutputPortMetaDataFault
		          ParserException( ParserExceptionType )
		          SemanticException( SemanticExceptionType ),
	messageTypeCast( MessageTypeCastRequest )( MessageTypeCastResponse )
	    throws  TypeMismatch
}

outputPort MetaJolieJavaService {
Interfaces: MetaJolieJavaServiceInterface
}

embedded {
Java:
	"joliex.meta.MetaJolie" in MetaJolieJavaService
}
