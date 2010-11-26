package support;


import java.io.IOException;
import java.io.Writer;
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
import jolie.lang.parse.ast.types.TypeDefinitionLink;

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
            OneWayOperationDeclaration operation = (OneWayOperationDeclaration) i.next();
            ScanTypes(operation.requestType());
  }
  for ( Iterator i=requestResponseOperationsSet.iterator();i.hasNext();)
       {
                  RequestResponseOperationDeclaration operation = (RequestResponseOperationDeclaration) i.next();
                  ScanTypes(operation.requestType());
                  ScanTypes(operation.responseType());
      
       }

 
}
private void ScanTypes( TypeDefinition typeDefinition)
 {
        boolean addFlag;
        addFlag=true;
    if (typeDefinition instanceof TypeDefinitionLink){

       for (Map<String,TypeDefinition> supportMap:typeMap)
               {
                     if ((supportMap.containsKey(((TypeDefinitionLink)typeDefinition).linkedType().context().sourceName())&&(supportMap.containsValue(((TypeDefinitionLink)typeDefinition).linkedType()))))
                     {
                       addFlag=false;
                       break;
                     }
                }
          if (addFlag){
             String nameFile= ((TypeDefinitionLink)typeDefinition).linkedType().context().sourceName();
             TypeDefinition supportType=((TypeDefinitionLink)typeDefinition).linkedType();

             Map<String,TypeDefinition> addingMap= new HashMap<String, TypeDefinition>();
             addingMap.put(nameFile, supportType);
             typeMap.add(addingMap);
             if (supportType.hasSubTypes())
             {
              ScanTypes(supportType);
                  Set <Map.Entry<String,TypeDefinition>> supportSet= supportType.subTypes();

                for (Iterator i = supportSet.iterator();i.hasNext();)
                {
                   Map.Entry me=(Map.Entry)i.next();

                   System.out.print("element of the list "+me.getKey()+"\n");
                   ScanTypes((TypeDefinition)me.getValue());

                }
             }

          }

    }else{

          for (Map<String,TypeDefinition> supportMap:typeMap)
               {
                     if ((supportMap.containsKey(typeDefinition.context().sourceName()))&&(supportMap.containsValue(typeDefinition)))
                     {
                       addFlag=false;
                       break;
                     }
                }
          if (addFlag){
             String nameFile= typeDefinition.context().sourceName();
             TypeDefinition supportType=typeDefinition;

             Map<String,TypeDefinition> addingMap= new HashMap<String, TypeDefinition>();
             addingMap.put(nameFile, supportType);
             typeMap.add(addingMap);
             if (supportType.hasSubTypes())
             {
              ScanTypes(supportType);
                  Set <Map.Entry<String,TypeDefinition>> supportSet= supportType.subTypes();

                for (Iterator i = supportSet.iterator();i.hasNext();)
                {
                   Map.Entry me=(Map.Entry)i.next();

                   if (((TypeDefinition)me.getValue()).hasSubTypes()){
                            ScanTypes((TypeDefinition)me.getValue());
                   }

                }
             }
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

private void PopulateFilesList(){

    for (InterfaceDefinition idef: interfacesArray)
    {
     if (!(filesNameList.contains( idef.context().sourceName()))){
        // filesNameList.add(idef.);
     }
    }
     for (OutputPortInfo outInfo: outputPortArray)
    {
     if (!(filesNameList.contains( outInfo.context().sourceName()))){
        // filesNameList.add(idef.);
     }
    }
     for (InputPortInfo inInfo: inputPortArray)
    {
     if (!(filesNameList.contains( inInfo.context().sourceName()))){
          filesNameList.add(inInfo.context().sourceName());
     }
    }
        Set<Entry<String, TypeDefinition>> suppotSet;
    for (Map<String, TypeDefinition> typeM: typeMap)
    {
         suppotSet=typeM.entrySet();

     if (!(filesNameList.contains( suppotSet.iterator().next().getKey()))){
         filesNameList.add(suppotSet.iterator().next().getKey());
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
abstract public void ConvertInterface(InterfaceDefinition interfaceDefinition,Writer writer)throws IOException ;;
abstract public void ConvertOutputPorts(OutputPortInfo outputPortInfo,Writer writer)throws IOException ;;
abstract public void ConvertInputPorts(InputPortInfo inputPortInfo ,Writer writer)throws IOException ;;
abstract public void ConvertOperations(OperationDeclaration operationDeclaration,Writer writer)throws IOException;
abstract public void ConvertTypes(TypeDefinition typesDefinition ,Writer writer) throws IOException ;


}
