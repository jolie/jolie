/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.lang.parse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;

/**
 *
 * @author claudio
 */
public class Interfaces {

	public static InterfaceDefinition extend( InterfaceDefinition inputInterface, InterfaceExtenderDefinition extender, String prefix_name ) {

		LinkedList<String> operationNameList = new LinkedList<String>();
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
						extenderOperation = ( RequestResponseOperationDeclaration ) extender.operationsMap().get( op.getKey() );
					} else {
						extenderOperation = extender.defaultRequestResponseOperation();
					}

					RequestResponseOperationDeclaration newOp = Operations.extend( (RequestResponseOperationDeclaration) op.getValue(), extenderOperation, prefix_name  );
					outputInterface.addOperation( newOp );

				} else if ( op.getValue() instanceof OneWayOperationDeclaration ) {
					OneWayOperationDeclaration extenderOperation;
					// check if the extension is defined for the current operation or if the default one must be used.
					
					if ( extender.operationsMap().containsKey( op.getKey() ) ) {
						extenderOperation = ( OneWayOperationDeclaration ) extender.operationsMap().get( op.getKey() );
					} else {
						extenderOperation = extender.defaultOneWayOperation();
					}

					OneWayOperationDeclaration newOp = Operations.extend( (OneWayOperationDeclaration) op.getValue(), extenderOperation, prefix_name );
					outputInterface.addOperation( newOp );


				}
				operationNameList.remove( op.getKey() );
			}
		}
		return outputInterface;
	}


}
