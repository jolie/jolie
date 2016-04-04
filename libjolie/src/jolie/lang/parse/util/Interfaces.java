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

import java.util.LinkedList;
import java.util.Map.Entry;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;

/**
 *
 * @author Claudio Guidi
 */
public class Interfaces
{
	public static InterfaceDefinition extend(
		InterfaceDefinition inputInterface,
		InterfaceExtenderDefinition extender,
		String namePrefix
	) {
		LinkedList<String> operationNameList = new LinkedList<>();
		operationNameList.addAll( inputInterface.operationsMap().keySet() );
		InterfaceDefinition outputInterface = inputInterface;
		if ( extender != null ) {
			outputInterface = new InterfaceDefinition( inputInterface.context(), inputInterface.name() );

			// for each operation of the interface, the extension is performed
			for( Entry<String, OperationDeclaration> op : inputInterface.operationsMap().entrySet() ) {

				if ( op.getValue() instanceof RequestResponseOperationDeclaration ) {
					RequestResponseOperationDeclaration extenderOperation;
					// check if the extension is defined for the current operation or if the default one must be used.
					if ( extender.operationsMap().containsKey( op.getKey() ) ) {
						extenderOperation = (RequestResponseOperationDeclaration) extender.operationsMap().get( op.getKey() );
					} else {
						extenderOperation = extender.defaultRequestResponseOperation();
					}

					RequestResponseOperationDeclaration newOp = Operations.extend( (RequestResponseOperationDeclaration) op.getValue(), extenderOperation, namePrefix );
					outputInterface.addOperation( newOp );

				} else if ( op.getValue() instanceof OneWayOperationDeclaration ) {
					OneWayOperationDeclaration extenderOperation;
					// check if the extension is defined for the current operation or if the default one must be used.
					if ( extender.operationsMap().containsKey( op.getKey() ) ) {
						extenderOperation = (OneWayOperationDeclaration) extender.operationsMap().get( op.getKey() );
					} else {
						extenderOperation = extender.defaultOneWayOperation();
					}

					OneWayOperationDeclaration newOp = Operations.extend( (OneWayOperationDeclaration) op.getValue(), extenderOperation, namePrefix );
					outputInterface.addOperation( newOp );
				}
				operationNameList.remove( op.getKey() );
			}
		}
		return outputInterface;
	}
}
