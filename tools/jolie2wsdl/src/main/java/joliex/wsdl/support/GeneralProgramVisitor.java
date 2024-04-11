package joliex.wsdl.support;


import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.Program;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author balint
 */
public abstract class GeneralProgramVisitor implements GeneralProgramVisitorInterface {

	protected final Program program;

	public GeneralProgramVisitor( Program program ) {
		this.program = program;
	}

	@Override
	abstract public void run();

	@Override
	abstract public void clearLists();

	@Override
	abstract public InterfaceDefinition[] getInterfaceDefinitions();

	@Override
	abstract public OutputPortInfo[] getOutputPortInfo();

	@Override
	abstract public InputPortInfo[] getInputPortInfo();
}
