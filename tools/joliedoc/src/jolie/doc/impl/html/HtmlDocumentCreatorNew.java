/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.doc.impl.html;

import java.io.Writer;
import java.util.List;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.types.TypeDefinition;
import support.GeneralDocumentCreator;
import support.GeneralProgramVisitor;

/**
 *
 * @author balint
 */
public class HtmlDocumentCreatorNew extends GeneralDocumentCreator {
private Writer writer;
private List<String> filesList;

 public HtmlDocumentCreatorNew(GeneralProgramVisitor program){
        super (program);
}

    @Override
    public void ConvertDocument() {
        filesList=GetFilesNameList();
        
        for (String nameFile: filesList)
        {
                 
        
        }
    }

    @Override
    public void ConvertInterface(InterfaceDefinition interfaceDefinition, Writer writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertOutputPorts(OutputPortInfo outputPortInfo, Writer writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertInputPorts(InputPortInfo inputPortInfo, Writer writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertOperations(OperationDeclaration operationDeclaration, Writer writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertTypes(TypeDefinition typesDefinition, Writer writer) {

    }

  

}
