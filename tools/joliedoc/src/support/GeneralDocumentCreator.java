package support;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
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
private List<Map<String,TypeDefinition>>typeMap;
private List<String> filesNameList;
private GeneralProgramVisitor program;
public GeneralDocumentCreator(GeneralProgramVisitor program)
    {
      oneWayOperationsSet= new HashSet<OneWayOperationDeclaration>();
      requestResponseOperationsSet= new HashSet<RequestResponseOperationDeclaration>();
      typeMap=new Vector<Map<String,TypeDefinition>>();
      filesNameList=new Vector<String>();
      this.program=program;
      this.program.run();
      PopulateInterfaceLists();
      PopulatePortsLists();
      PopulateOperationsSet();
      PopulateTypesSet();


    }
private void PopulateTypesSet(){
        List<TypeDefinition> supportTypeDefList;
        String nameFile;
        TypeDefinition supportType;

  for ( Iterator i=oneWayOperationsSet.iterator();i.hasNext();)
  {
       OneWayOperationDeclaration oneWayOperation= (OneWayOperationDeclaration) i.next();
            boolean addFlag = true;

               for (Map<String,TypeDefinition> supportMap:typeMap)
               {
                     if ((supportMap.containsKey(oneWayOperation.requestType().context().sourceName())&&(supportMap.containsValue(oneWayOperation.requestType()))))
                     {
                       addFlag=false;

                     }
                }
         if (addFlag)
         {
             nameFile= oneWayOperation.requestType().context().sourceName();
             supportType=oneWayOperation.requestType();

             Map<String,TypeDefinition> addingMap= new HashMap<String, TypeDefinition>();
             addingMap.put(nameFile, supportType);
             typeMap.add(addingMap);
          }
      }

        /// request response
  /*  for ( Iterator i=requestResponseOperationsSet.iterator();i.hasNext();)
  {
       OneWayOperationDeclaration requestResponseOperation= (OneWayOperationDeclaration) i.next();
            boolean addFlag = true;

               for (Map<String,TypeDefinition> supportMap:typeMap)
               {
                     if ((supportMap.containsKey(oneWayOperation.requestType().context().sourceName())&&(supportMap.containsValue(oneWayOperation.requestType()))))
                     {
                       addFlag=false;

                     }
                }
         if (addFlag)
         {
             nameFile= oneWayOperation.requestType().context().sourceName();
             supportType=oneWayOperation.requestType();
             Map<String,TypeDefinition> addingMap= new HashMap<String, TypeDefinition>();
             addingMap.put(nameFile, supportType);
             typeMap.add(addingMap);
          }
      }


  */
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

private void PopulateFilesList(){
    for (InterfaceDefinition idef: interfacesArray)
    {
     if (!(filesNameList.contains( idef.context().sourceName()))){
        // filesNameList.add(idef.);
     }

    }


}
protected List<Map<String,TypeDefinition>> GetTypesSet(){
    return typeMap;
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
abstract public void ConvertDocument();
abstract public void ConvertInterface();
abstract public void ConvertOutputPorts();
abstract public void ConvertInputPorts();
abstract public void ConvertOperations();
abstract public void ConvertTypes();

}
