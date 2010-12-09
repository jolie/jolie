/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.java.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import joliex.java.support.GeneralDocumentCreator;
import joliex.java.support.GeneralProgramVisitor;
import joliex.java.support.treeOLObject;





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
        for(treeOLObject treeObject : olTree){
              //for (int countrerInterftreeObject.GetLinkedObjetSize();
        
        
        }
        List<Map<String, TypeDefinition>> typesList = this.GetTypesSet();
        for (Map<String, TypeDefinition> typeDefinitionMap : typesList) {
            String nameFile = typeDefinitionMap.entrySet().iterator().next().getValue().id()+".java";
                            Writer writer;
            try {
                writer = new BufferedWriter(new FileWriter(nameFile));
                ConvertTypes(typeDefinitionMap.entrySet().iterator().next().getValue(), writer);
               writer.flush();
               writer.close();
            } catch (IOException ex) {
                Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
            
          
          
                        
         }
                
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
         StringBuilder builderHeaderclass = new StringBuilder();
         ImportCreate(builderHeaderclass, typesDefinition);
         builderHeaderclass.append("public "+ typesDefinition.id()+" {"+"\n");
         VariableCreate(builderHeaderclass, typesDefinition);
         ConstructorCreate(builderHeaderclass, typesDefinition);
         builderHeaderclass.append(" }\n");
         writer.append(builderHeaderclass.toString());
    }
    private void ImportCreate(StringBuilder stringBuilder,TypeDefinition type){





			String nameFile = type.context().sourceName();
			TypeDefinition supportType = type;
			//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
                        List<String> a= new LinkedList<String>();


			if ( supportType.hasSubTypes() ) {


				Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
				Iterator i = supportSet.iterator();
				while( i.hasNext() ) {
					Map.Entry me = (Map.Entry) i.next();

					if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {

					    stringBuilder.append("import types."+((TypeDefinitionLink)me.getValue()).linkedType().id()+"\n");

					}

				}
                             stringBuilder.append("import jolie.runtime.Value;\n");
                             stringBuilder.append("\n");
			}
		

    }
    private void VariableCreate(StringBuilder stringBuilder,TypeDefinition type){





			String nameFile = type.context().sourceName();
			TypeDefinition supportType = type;
			//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );


			if ( supportType.hasSubTypes() ) {


				Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
				Iterator i = supportSet.iterator();
				while( i.hasNext() ) {
					Map.Entry me = (Map.Entry) i.next();

					if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {

					    stringBuilder.append("private "+((TypeDefinitionLink)me.getValue()).linkedType().id()+" "+((TypeDefinitionLink)me.getValue()).id() +"\n");

					}else
                                        {
                                           // System.out.print("tipo "+((TypeDefinition)me.getValue()).nativeType().id()+"\n");
                                           stringBuilder.append("private " + ((TypeDefinition)me.getValue()).nativeType().id() + " "+ ((TypeDefinition)me.getValue()).id()+ "\n");

                                        }

				}
                             stringBuilder.append("private Value v \n");
                             stringBuilder.append("\n");
			}


    }
    private void ConstructorCreate(StringBuilder stringBuilder,TypeDefinition type){



			TypeDefinition supportType = type;
			//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
                       stringBuilder.append("public  class "+supportType.id()+"(Value v){\n");
   

			if ( supportType.hasSubTypes() ) {


				Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
				Iterator i = supportSet.iterator();
				while( i.hasNext() ) {
					Map.Entry me = (Map.Entry) i.next();

					if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {

					    //stringBuilder.append("private "+((TypeDefinitionLink)me.getValue()).linkedType().id()+" "+((TypeDefinitionLink)me.getValue()).id() +"\n");

					}else
                                        {
                                            //System.out.print("tipo "+((TypeDefinition)me.getValue()).nativeType().id()+"\n");
                                           //stringBuilder.append("private " + ((TypeDefinition)me.getValue()).nativeType().id() + " "+ ((TypeDefinition)me.getValue()).id()+ "\n");

                                        }

				}

                             stringBuilder.append("}\n");
			}


    }
}
