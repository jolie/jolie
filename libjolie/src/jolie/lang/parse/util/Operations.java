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
package jolie.lang.parse.util;

import java.util.HashMap;
import java.util.Map;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;

/**
 *
 * @author Claudio Guidi
 */
public class Operations
{
	public static RequestResponseOperationDeclaration extend(
		RequestResponseOperationDeclaration operation,
		RequestResponseOperationDeclaration extender,
		String namePrefix
	) {
		TypeDefinition newRequestType = operation.requestType();
		TypeDefinition newResponseType = operation.responseType();
		Map<String, TypeDefinition> extendedFaultMap = new HashMap<>();
		extendedFaultMap.putAll( operation.faults() );
		if ( extender != null ) {
			newRequestType = TypeDefinition.extend( operation.requestType(), extender.requestType(), namePrefix );
			newResponseType = TypeDefinition.extend( operation.responseType(), extender.responseType(), namePrefix );
			extendedFaultMap.putAll( extender.faults() );
		}
		RequestResponseOperationDeclaration newOp =
			new RequestResponseOperationDeclaration( operation.context(), operation.id(), newRequestType, newResponseType, extendedFaultMap );

		return newOp;
	}

	public static OneWayOperationDeclaration extend(
		OneWayOperationDeclaration operation,
		OneWayOperationDeclaration extender,
		String namePrefix
	) {

		TypeDefinition newRequestType = operation.requestType();
		if ( extender != null ) {
			newRequestType = TypeDefinition.extend( operation.requestType(), extender.requestType(), namePrefix );
		}
		OneWayOperationDeclaration newOp =
			new OneWayOperationDeclaration( operation.context(), operation.id() );
		newOp.setRequestType( newRequestType );

		return newOp;
	}
}
