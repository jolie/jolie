package support;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author balint
 */
public  abstract class GeneralDocumentCreator {
private OutputPortInfo[] outputPortArray;
private InputPortInfo[] inputPortArray;
private InterfaceDefinition[] interfacesArray;
private Set<RequestResponseOperationDeclaration> requestResponseOperationsSet;
private Set<OneWayOperationDeclaration> oneWayOperationsSet;
private Set<TypeDefinition>typesSet;
private List<String> filesNameList;
private GeneralProgramVisitor program;
public GeneralDocumentCreator(GeneralProgramVisitor program)
    {
      oneWayOperationsSet= new HashSet<OneWayOperationDeclaration>();
      requestResponseOperationsSet= new HashSet<RequestResponseOperationDeclaration>();
      typesSet=new HashSet<TypeDefinition>();
      this.program=program;
      this.program.run();
      PopulateInterfaceLists();
      PopulatePortsLists();
      PopulateOperationsSet();
      PopulateTypesSet();


    }
private void PopulateTypesSet(){
  for ( Iterator i=oneWayOperationsSet.iterator();i.hasNext();)
  {
       OneWayOperationDeclaration oneWayOperation= (OneWayOperationDeclaration) i.next();
       if (!(typesSet.contains(oneWayOperation.requestType()))){
        typesSet.add(oneWayOperation.requestType());

       }

  }
  for ( Iterator i=requestResponseOperationsSet.iterator();i.hasNext();)
  {
       RequestResponseOperationDeclaration requestResponseOperation= (RequestResponseOperationDeclaration) i.next();
       if (!(typesSet.contains(requestResponseOperation.requestType()))){
        typesSet.add(requestResponseOperation.requestType());

       }
     if (!(typesSet.contains(requestResponseOperation.responseType()))){
        typesSet.add(requestResponseOperation.responseType());

       }
  }
}
private void PopulateOperationsSet(){
        Entry<String, OperationDeclaration> operation;



 for (InterfaceDefinition idef:interfacesArray)
 {
            Map<String, OperationDeclaration> v = idef.operationsMap();
            //v.entrySet().
     for( Iterator i=v.entrySet().iterator();i.hasNext();)
     {
      operation=(Entry<String, OperationDeclaration>) i.next();
      if (operation.getValue() instanceof RequestResponseOperationDeclaration){
        requestResponseOperationsSet.add((RequestResponseOperationDeclaration) operation.getValue());


      }else
      {

       oneWayOperationsSet.add((OneWayOperationDeclaration) operation.getValue());
      }

     }


 }

}
private void PopulatePortsLists(){
outputPortArray= program.getOutputPortInfo();
inputPortArray=program.getInputPortInfo();

}
private void PopulateInterfaceLists(){

interfacesArray=program.getInterfaceDefinitions();

}

protected Set<TypeDefinition> GetTypesSet(){
    return typesSet;
}

protected Set<RequestResponseOperationDeclaration > GetRequestResponseOperations(){
    return requestResponseOperationsSet;
}
protected Set<OneWayOperationDeclaration > GetOneWayOperations(){
    return oneWayOperationsSet;
}
protected InterfaceDefinition[] GetInterfaceArray(){
    return interfacesArray;
}
protected OutputPortInfo[] GetOutputPortArray(){
    return outputPortArray;
}
protected InputPortInfo[] GetInputPortArray(){
    return inputPortArray;
}
protected List<String> GetFilesNameList(){
    return filesNameList;
}
abstract void ConvertDocument();
abstract void ConvertInterface();
abstract void ConvertOutputPorts();
abstract void ConvertInputPorts();
abstract void ConvertOperations();
abstract void ConvertTypes();

}
