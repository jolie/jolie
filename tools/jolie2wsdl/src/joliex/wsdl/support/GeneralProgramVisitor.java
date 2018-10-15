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
public abstract class GeneralProgramVisitor  implements GeneralProgramVisitorInterface{

    protected Program program;

    public GeneralProgramVisitor(Program program){
        this.program=program;
       }
   abstract public void run();
   abstract public void clearLists();
   abstract public InterfaceDefinition[] getInterfaceDefinitions();
   abstract public OutputPortInfo[] getOutputPortInfo();
   abstract public InputPortInfo[] getInputPortInfo();
}
