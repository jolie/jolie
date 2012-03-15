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

type ParseRoleRequest: void {
  .rolename: Name
  .filename: string
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
}

type GetInputPortMetaDataRequest: void {
  .filename: string
  .name: Name
}

type GetInputPortMetaDataResponse: void {
  .input*: Participant
}

interface MetaJolieInterface {
RequestResponse:
	parseRoles( ParseRoleRequest)( Role ),
	getMetaData( GetMetaDataRequest )( GetMetaDataResponse ),
	getInputPortMetaData( GetInputPortMetaDataRequest )( GetInputPortMetaDataResponse )
}

outputPort MetaJolie {
Interfaces: MetaJolieInterface
}

embedded {
Java:
	"joliex.meta.MetaJolie" in MetaJolie
}
