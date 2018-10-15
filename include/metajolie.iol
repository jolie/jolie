/***************************************************************************
 *   Copyright (C) 2011 by Claudio Guidi <cguidi@italianasoftware.com>     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

include "types/role_types.iol"

// FAULTS
type InputPortMetaDataFault: void {
  .message: string
}

// MESSAGE TYPES

type CheckNativeTypeRequest: void {
  .type_name: string
}

type CheckNativeTypeResponse: void {
  .result: bool
}

type GetMetaDataRequest: void {
  .filename: string
  .name: Name
}

type GetMetaDataResponse: void {
  .service: Service
  .input*: Participant
  .output*: Participant
  .interfaces*: Interface
  .types*: Type
  .embeddedServices*: void {
	.type: string
	.servicepath: string
	.portId: string
  }
}

type GetInputPortMetaDataRequest: void {
  .filename: string
  .name: Name
}

type GetInputPortMetaDataResponse: void {
  .input*: Participant
}

type MessageTypeCastRequest: void {
  .message: undefined
  .types: void {
	.messageTypeName: Name
	.types*: Type
  }
}

type MessageTypeCastResponse: void {
  .message: undefined
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

type ParseRoleRequest: void {
  .rolename: Name
  .filename: string
}


/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface MetaJolieInterface {
RequestResponse:
	checkNativeType( CheckNativeTypeRequest )( CheckNativeTypeResponse ),
	getMetaData( GetMetaDataRequest )( GetMetaDataResponse )
	    throws ParserException( ParserExceptionType )
		   SemanticException( SemanticExceptionType ),
	getInputPortMetaData( GetInputPortMetaDataRequest )( GetInputPortMetaDataResponse )
	    throws InputPortMetaDataFault
		   ParserException( ParserExceptionType )
		   SemanticException( SemanticExceptionType ),
	messageTypeCast( MessageTypeCastRequest )( MessageTypeCastResponse )
	    throws TypeMismatch,
	parseRoles( ParseRoleRequest)( Role )
	
}

outputPort MetaJolie {
Interfaces: MetaJolieInterface
}

embedded {
Java:
	"joliex.meta.MetaJolie" in MetaJolie
}
