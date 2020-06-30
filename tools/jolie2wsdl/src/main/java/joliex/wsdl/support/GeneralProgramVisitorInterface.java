package joliex.wsdl.support;


import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OutputPortInfo;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author balint
 */
public interface GeneralProgramVisitorInterface {
	void run();

	void clearLists();

	InterfaceDefinition[] getInterfaceDefinitions();

	OutputPortInfo[] getOutputPortInfo();

	InputPortInfo[] getInputPortInfo();

}
