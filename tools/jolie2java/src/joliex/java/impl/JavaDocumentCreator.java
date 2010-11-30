/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.java.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.types.TypeDefinition;
import support.GeneralDocumentCreator;
import support.GeneralProgramVisitor;
import support.treeOLObject;



/**
 *
 * @author balint
 */
public class JavaDocumentCreator extends GeneralDocumentCreator{
   public JavaDocumentCreator(GeneralProgramVisitor visitor){
   
    super(visitor);
   
   }
    @Override
    public void ConvertDocument() {
        List<treeOLObject> olTree= GetOlTree();

    }

    @Override
    public void ConvertInterface(InterfaceDefinition interfaceDefinition, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertOutputPorts(OutputPortInfo outputPortInfo, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertInputPorts(InputPortInfo inputPortInfo, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertOperations(OperationDeclaration operationDeclaration, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertTypes(TypeDefinition typesDefinition, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
