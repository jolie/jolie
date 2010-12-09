package joliex.java.support;


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
  abstract public void run( );
  abstract public void clearLists();
  abstract public   InterfaceDefinition[] getInterfaceDefinitions();
  abstract public OutputPortInfo[] getOutputPortInfo();
  abstract public InputPortInfo[] getInputPortInfo();
  
}
