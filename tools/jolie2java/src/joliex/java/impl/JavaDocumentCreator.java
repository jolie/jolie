/***************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/
package joliex.java.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import joliex.java.support.GeneralDocumentCreator;
import joliex.java.support.GeneralProgramVisitor;
import joliex.java.support.treeOLObject;
import jolie.runtime.Value;

/**
 *
 * @author balint
 */
public class JavaDocumentCreator {

    private Vector<TypeDefinition> subclass;
    private boolean subtypePresent = false;
    private String namespace;
    private LinkedHashMap<String, TypeDefinition> typeMap;
    private LinkedHashMap<String, TypeDefinition> subTypeMap;
    ProgramInspector inspector;

    public JavaDocumentCreator(ProgramInspector inspector, String namespace) {

        this.inspector = inspector;
        this.namespace = namespace;
    }

    public void ConvertDocument() {


        typeMap = new LinkedHashMap<String, TypeDefinition>();
        subTypeMap = new LinkedHashMap<String, TypeDefinition>();
        subclass = new Vector<TypeDefinition>();
        int counterSubClass;
        TypeDefinition[] support = inspector.getTypes();
        InputPortInfo[] inputPorts = inspector.getInputPorts();
        OperationDeclaration operation;
        RequestResponseOperationDeclaration requestResponseOperation;

        for (InputPortInfo inputPort : inputPorts) {
                 
            Collection<OperationDeclaration> operations = inputPort.operations();
            
            String sourceString=inputPort.context().source().toString();
            System.out.println("Questo e il path del file:"+sourceString+ " per Porta :"+inputPort.id());
            if ((sourceString.contains("/"))||sourceString.contains("\\")){
            System.out.println("name input port: " + inputPort.id() + " " + new Integer(inputPorts.length).toString());
            Iterator<OperationDeclaration> operatorIterator = operations.iterator();
            while (operatorIterator.hasNext()) {
                operation = operatorIterator.next();
                System.out.println(operation.id());
                if (operation instanceof RequestResponseOperationDeclaration) {
                    requestResponseOperation = (RequestResponseOperationDeclaration) operation;
                    if (!typeMap.containsKey(requestResponseOperation.requestType().id())) {
                        typeMap.put(requestResponseOperation.requestType().id(), requestResponseOperation.requestType());
                    }
                    if (!typeMap.containsKey(requestResponseOperation.responseType().id())) {
                        typeMap.put(requestResponseOperation.responseType().id(), requestResponseOperation.responseType());
                    }
                } else {
                    OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
                    if (!typeMap.containsKey(oneWayOperationDeclaration.requestType().id())) {
                        typeMap.put(oneWayOperationDeclaration.requestType().id(), oneWayOperationDeclaration.requestType());
                    }
                }
            }
           }
          }

        Iterator<Entry<String, TypeDefinition>> typeMapIterator = typeMap.entrySet().iterator();
        while (typeMapIterator.hasNext()) {
        Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
            if (!(typeEntry.getKey().equals("undefined"))) {
               parseSubType(typeEntry.getValue());
            }
        }
        Iterator<Entry<String, TypeDefinition>> subTypeMapIterator = subTypeMap.entrySet().iterator();
        while (subTypeMapIterator.hasNext()) {
            Entry<String, TypeDefinition> subTypeEntry = subTypeMapIterator.next();
            typeMap.put(subTypeEntry.getKey(), subTypeEntry.getValue());
            
        }
        typeMapIterator = typeMap.entrySet().iterator();
        while (typeMapIterator.hasNext()) {
            Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
            if (!(typeEntry.getKey().equals("undefined"))) {
                subclass = new Vector<TypeDefinition>();
                subtypePresent = false;
                counterSubClass = 0;
                String nameFile = typeEntry.getKey() + ".java";
                Writer writer;
                try {
                    writer = new BufferedWriter(new FileWriter(nameFile));

                    ConvertTypes(typeEntry.getValue(), writer);
                    
                    writer.flush();
                    writer.close();

                } catch (IOException ex) {
                    Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }



        }

    }

    public void ConvertInterface(InterfaceDefinition interfaceDefinition, Writer writer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ConvertOutputPorts(OutputPortInfo outputPortInfo, Writer writer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ConvertInputPorts(InputPortInfo inputPortInfo, Writer writer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ConvertOperations(OperationDeclaration operationDeclaration, Writer writer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ConvertTypes(TypeDefinition typeDefinition, Writer writer)
            throws IOException {


        StringBuilder builderHeaderclass = new StringBuilder();
        builderHeaderclass.append("package " + namespace + ";\n");
        importsCreate(builderHeaderclass, typeDefinition);
        builderHeaderclass.append("public class " + typeDefinition.id() + " {" + "\n");
        if (typeDefinition.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> supportSet = typeDefinition.subTypes();
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink){
                        typeMap.put(((TypeDefinitionLink) me.getValue()).linkedTypeName(),((TypeDefinitionLink)me.getValue()).linkedType());



                    }else if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) me.getValue()).hasSubTypes())) {
                    builderHeaderclass.append("public class " + ((TypeDefinition) me.getValue()).id() + " {" + "\n");
                    variableCreate(builderHeaderclass, ((TypeDefinition) me.getValue()));
                    constructorCreate(builderHeaderclass, ((TypeDefinition) me.getValue()), false);
                    methodsCreate(builderHeaderclass, ((TypeDefinition) me.getValue()), false);
                    builderHeaderclass.append("}\n");
                }

            }


/////////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


            variableCreate(builderHeaderclass, typeDefinition);
            constructorCreate(builderHeaderclass, typeDefinition, true);
            methodsCreate(builderHeaderclass, typeDefinition, true);
            builderHeaderclass.append("}\n");
            writer.append(builderHeaderclass.toString());


        }
    }

    private void closeClass(Writer writer) {
        StringBuilder builderHeaderclass = new StringBuilder();
        builderHeaderclass.append(" }\n");
        try {
            writer.append(builderHeaderclass.toString());
        } catch (IOException ex) {
            Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void importsCreate(StringBuilder stringBuilder, TypeDefinition type) {





        String nameFile = type.context().sourceName();
        TypeDefinition supportType = type;
        //System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
        List<String> a = new LinkedList<String>();
        boolean addListImport = false;



        if (supportType.hasSubTypes()) {
            subtypePresent = true;
            stringBuilder.append("import java.util.List;\n");
            stringBuilder.append("import java.util.LinkedList;\n");
            stringBuilder.append("import jolie.runtime.Value;;\n");
            stringBuilder.append("\n");
        }
    }

    private void variableCreate(StringBuilder stringBuilder, TypeDefinition type) {
        TypeDefinition supportType = type;
        if (supportType.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {
                        stringBuilder.append("private List< " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + "> " + "_" + ((TypeDefinitionLink) me.getValue()).id() + ";\n");
                    } else {
                        stringBuilder.append("private " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + " " + "_" + ((TypeDefinitionLink) me.getValue()).id() + ";\n");
                    }
                } else if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) me.getValue()).hasSubTypes())) {
                    StringBuilder supBuffer = new StringBuilder();
                    this.subclass.add(((TypeInlineDefinition) me.getValue()));
                    if (((TypeInlineDefinition) me.getValue()).cardinality().max() > 1) {
                        stringBuilder.append("private List< ").append(((TypeDefinition) me.getValue()).id()).append("> " + "_").append(((TypeDefinition) me.getValue()).id()).append(";\n");
                    } else {
                        stringBuilder.append("private ").append(((TypeDefinition) me.getValue()).id()).append(" " + "_").append(((TypeDefinition) me.getValue()).id()).append(";\n");
                    }
                    subtypePresent = true;
                } else {
                    if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {
                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("private List<Integer> " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");
                        } else if (typeName.equals("double")) {
                            stringBuilder.append("private List<Double> " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");
                        } else if (typeName.equals("string")) {
                            stringBuilder.append("private List<String> " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");

                        }
                    } else {
                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("private Integer " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");
                        } else if (typeName.equals("double")) {
                            stringBuilder.append("private Double " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");
                        } else if (typeName.equals("string")) {
                            stringBuilder.append("private String " + " " + "_" + ((TypeDefinition) me.getValue()).id() + ";\n");
                        }
                    }
                }

            }
            stringBuilder.append("private Value v ;\n");
            stringBuilder.append("private Value vReturn=  Value.create() ;\n");
            stringBuilder.append("\n");
        }


    }

    private void constructorCreate(StringBuilder stringBuilder, TypeDefinition type, boolean naturalType) {



        TypeDefinition supportType = type;
        stringBuilder.append("public " + supportType.id() + "(Value v){\n");
        stringBuilder.append("\n");
        stringBuilder.append("this.v=v;\n");
        String nameVariable;

        if (supportType.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> subTypesSet = supportType.subTypes();
            Iterator supportTypeIterator = subTypesSet.iterator();
            while (supportTypeIterator.hasNext()) {
                Map.Entry me = (Map.Entry) supportTypeIterator.next();
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) me.getValue()).id();
                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {
                        stringBuilder.append("_" + nameVariable + "= new LinkedList<" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();" + "\n");
                        stringBuilder.append("}\n");
                    } else {
                        stringBuilder.append("_" + nameVariable + "=new " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "( v.getFirstChild(\"" + nameVariable + "\"));" + "\n");
                        stringBuilder.append("}\n");
                    }
                } else if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) me.getValue()).hasSubTypes()) {
                    nameVariable = ((TypeInlineDefinition) me.getValue()).id();
                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeInlineDefinition) me.getValue()).cardinality().max() > 1) {
                        stringBuilder.append("_" + nameVariable + "= new LinkedList<" + ((TypeInlineDefinition) me.getValue()).id() + ">();" + "\n");
                        stringBuilder.append("}\n");
                    } else {
                        stringBuilder.append("_" + nameVariable + "=new " + ((TypeInlineDefinition) me.getValue()).id() + "( v.getFirstChild(\"" + nameVariable + "\"));" + "\n");
                        stringBuilder.append("}\n");
                    }
                } else {
                    nameVariable = ((TypeDefinition) me.getValue()).id();
                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {
                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<Integer>();" + "\n");
                        } else if (typeName.equals("double")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<Double>();" + "\n");
                        } else if (typeName.equals("string")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<String>();" + "\n");
                        }
                        stringBuilder.append("}\n");
                    } else {

                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {

                            stringBuilder.append("_" + nameVariable + "=new Integer(v.getFirstChild(\"" + nameVariable + "\").intValue());" + "\n");
                            stringBuilder.append("}\n");
                        } else if (typeName.equals("double")) {

                            stringBuilder.append("_" + nameVariable + "=new Double(v.getFirstChild(\"" + nameVariable + "\").doubleValue());" + "\n");
                            stringBuilder.append("}\n");

                        } else if (typeName.equals("string")) {
                            stringBuilder.append("_" + nameVariable + "=v.getFirstChild(\"" + nameVariable + "\").strValue();" + "\n");
                            stringBuilder.append("}\n");


                        }

                    }
                }

            }
            supportTypeIterator = subTypesSet.iterator();

            while (supportTypeIterator.hasNext()) {
                Map.Entry me = (Map.Entry) supportTypeIterator.next();
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) me.getValue()).id();

                    if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {


                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\t" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + " support").append(nameVariable).append("=new " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + "(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append("));\n");
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                        stringBuilder.append("\t}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    }

                } else if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) me.getValue()).hasSubTypes()) {
                    if (((TypeInlineDefinition) me.getValue()).cardinality().max() > 1) {

                        nameVariable = ((TypeInlineDefinition) me.getValue()).id();

                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\t" + ((TypeInlineDefinition) me.getValue()).id() + " support").append(nameVariable).append("=new " + ((TypeInlineDefinition) me.getValue()).id() + "(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append("));\n");
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                        stringBuilder.append("\t}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    }

                } else {
                    nameVariable = ((TypeDefinition) me.getValue()).id();

                    if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {

                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            //Value dsadda;
                            //dsadda.getChildren( typeName ).
                            stringBuilder.append("\n");
                            stringBuilder.append("\t" + "for(int counter").append(nameVariable).append("=0;" + "counter").append(nameVariable).append("<v.getChildren(\"").append(nameVariable).append("\").size();counter").append(nameVariable).append("++){\n");
                            stringBuilder.append("\t\t" + "Integer support").append(nameVariable).append("=new Integer(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").intValue());\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                            stringBuilder.append("\t" + "}\n");
                        } else if (typeName.equals("double")) {
                            stringBuilder.append(nameVariable + "= new LinkedList<Double>();" + "\n");
                            stringBuilder.append("\t" + "for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                            stringBuilder.append("\t\t" + "Double support").append(nameVariable).append("=new Double(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").doubleValue());\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                            stringBuilder.append("\t}\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                            stringBuilder.append("\t\t" + "String support").append(nameVariable).append("=new String(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").strValue());\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                            stringBuilder.append("}\n");


                        }
                        stringBuilder.append("}\n");
                    }



                }

            }

            stringBuilder.append("}\n");
        }

///// constructor with out value
        stringBuilder.append("public " + supportType.id() + "(){\n");
        stringBuilder.append("\n");



        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();

                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) me.getValue()).id();
                    if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {

                        stringBuilder.append("_" + nameVariable + "= new LinkedList<" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();" + "\n");


                    } else {
                        //stringBuilder.append( nameVariable + "=new " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "( v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );
                    }


                } else {

                    nameVariable = ((TypeDefinition) me.getValue()).id();
                    if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {

                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<Integer>();" + "\n");

                        } else if (typeName.equals("double")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<Double>();" + "\n");



                        } else if (typeName.equals("string")) {

                            stringBuilder.append("_" + nameVariable + "= new LinkedList<String>();" + "\n");



                        }
                    } else {
                        //stringBuilder.append( "private " + ((TypeDefinition) me.getValue()).nativeType().id() + " " + ((TypeDefinition) me.getValue()).id() + "\n" );
                        //stringBuilder.append( nameVariable + "=v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );
                    }
                }

            }
        }
        stringBuilder.append("}\n");
    }

    private void methodsCreate(StringBuilder stringBuilder, TypeDefinition type, boolean naturalType) {
        TypeDefinition supportType = type;
        String nameVariable, nameVariableOp;
        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();

                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) me.getValue()).id();
                    String startingChar = nameVariable.substring(0, 1);
                    String remaningStr = nameVariable.substring(1, nameVariable.length());
                    nameVariableOp = startingChar.toUpperCase() + remaningStr;
                    Integer maxIndex = new Integer(((TypeDefinitionLink) me.getValue()).cardinality().max());
                    Integer minIndex = new Integer(((TypeDefinitionLink) me.getValue()).cardinality().min());
                    if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {

                        stringBuilder.append("public " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " get" + nameVariableOp + "Value(int index){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ".get(index);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                        stringBuilder.append("}\n");


                        stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " value ){\n");
                        //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                        stringBuilder.append("\n\t\t" + "_" + nameVariable + ".add(value);\n");
                        //stringBuilder.append( "\t}\n" );
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                        //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                        //stringBuilder.append( "\t}\n" );
                        stringBuilder.append("}\n");

                    } else {

                        stringBuilder.append("public " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " get" + nameVariableOp + "(){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void set" + nameVariableOp + "(" + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " value ){\n");
                        stringBuilder.append("\n\t" + "_" + nameVariable + "=value;\n");
                        stringBuilder.append("}\n");

                    }


                } else if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) me.getValue()).hasSubTypes()) {

                    nameVariable = ((TypeInlineDefinition) me.getValue()).id();
                    String startingChar = nameVariable.substring(0, 1);
                    String remaningStr = nameVariable.substring(1, nameVariable.length());
                    nameVariableOp = startingChar.toUpperCase() + remaningStr;
                    Integer maxIndex = new Integer(((TypeInlineDefinition) me.getValue()).cardinality().max());
                    Integer minIndex = new Integer(((TypeInlineDefinition) me.getValue()).cardinality().min());
                    if (((TypeInlineDefinition) me.getValue()).cardinality().max() > 1) {

                        stringBuilder.append("public " + ((TypeInlineDefinition) me.getValue()).id() + " get" + nameVariableOp + "Value(int index){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ".get(index);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                        stringBuilder.append("}\n");


                        stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + ((TypeInlineDefinition) me.getValue()).id() + " value ){\n");
                        //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                        stringBuilder.append("\n\t\t" + "_" + nameVariable + ".add(value);\n");
                        //stringBuilder.append( "\t}\n" );
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                        //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                        //stringBuilder.append( "\t}\n" );
                        stringBuilder.append("}\n");

                    } else {

                        stringBuilder.append("public " + ((TypeInlineDefinition) me.getValue()).id() + " get" + nameVariableOp + "(){\n");
                        stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void set" + nameVariableOp + "(" + ((TypeInlineDefinition) me.getValue()).id() + " value ){\n");
                        stringBuilder.append("\n\t" + "_" + nameVariable + "=value;\n");
                        stringBuilder.append("}\n");

                    }


                } else {

                    nameVariable = ((TypeDefinition) me.getValue()).id();
                    if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {
                        Integer maxIndex = new Integer(((TypeDefinition) me.getValue()).cardinality().max());
                        Integer minIndex = new Integer(((TypeDefinition) me.getValue()).cardinality().min());
                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        String startingChar = nameVariable.substring(0, 1);
                        String remaningStr = nameVariable.substring(1, nameVariable.length());
                        nameVariableOp = startingChar.toUpperCase() + remaningStr;
                        if (typeName.equals("int")) {



                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");



                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value(int value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "Integer support").append(nameVariable).append("=new Integer(value);\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(" + "support" + nameVariable + " );\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                        } else if (typeName.equals("double")) {
                            //stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "double" + " get" + nameVariableOp + "Value(int index){\n");
                            stringBuilder.append("\treturn " + "_" + nameVariable + ".get(index).doubleValue();\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value( double value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "Double support").append(nameVariable).append("=new Double(value);\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(" + "support" + nameVariable + " );\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "String" + " get" + nameVariableOp + "Value(int index){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value( String value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(value);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");



                        }
                    } else {


                        String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                        String startingChar = nameVariable.substring(0, 1);
                        String remaningStr = nameVariable.substring(1, nameVariable.length());
                        nameVariableOp = startingChar.toUpperCase() + remaningStr;
                        if (typeName.equals("int")) {
                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".intValue();\n");
                            stringBuilder.append("}\n");
                            Integer a = new Integer(2);


                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value(int value ){\n");
                            stringBuilder.append("\n\t" + "_" + nameVariable + "=new Integer(value);\n");
                            stringBuilder.append("}\n");

                        } else if (typeName.equals("double")) {
                            //stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
                            stringBuilder.append("public " + "double" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".doubleValue();\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value( double value ){\n");
                            stringBuilder.append("\n\t\t" + "_" + nameVariable + "=new Double(value);\n");
                            stringBuilder.append("}\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("public " + "String" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value( String value ){\n");
                            stringBuilder.append("\n\t\t" + "_" + nameVariable + "=value;\n");
                            stringBuilder.append("}\n");



                        }


                    }
                }

            }




        }
        //// getVALUE
        stringBuilder.append("public " + "Value get" + "Value(){\n");
        Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
        Iterator i = supportSet.iterator();

        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            //Value v
            //v.getNewChild( nameVariable ).deepCopy( v );
            //v.hasChildren( nameVariable );
            if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                nameVariable = ((TypeDefinitionLink) me.getValue()).id();

                if (((TypeDefinitionLink) me.getValue()).cardinality().max() > 1) {
//
                    stringBuilder.append("if(!(_").append(nameVariable).append(".isEmpty()) && (_").append(nameVariable).append("!=null)){\n");
                    stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                    stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\").deepCopy(" + "_" + nameVariable + ".get(counter" + nameVariable + ").getValue());\n");
                    stringBuilder.append("\t}\n");
                    stringBuilder.append("}\n");





                } else {
//
                    stringBuilder.append("if((_").append(nameVariable).append("!=null)){\n");
                    stringBuilder.append("vReturn.getNewChild(\"" + nameVariable + "\")" + ".deepCopy(" + "_" + nameVariable + ".getValue());\n");
                    stringBuilder.append("}\n");


                }


            } else {

                nameVariable = ((TypeDefinition) me.getValue()).id();
                if (((TypeDefinition) me.getValue()).cardinality().max() > 1) {
                    stringBuilder.append("if(!(_").append(nameVariable).append(".isEmpty()) && (_").append(nameVariable).append("!=null)){\n");
                    String typeName = ((TypeDefinition) me.getValue()).nativeType().id();

                    if (typeName.equals("int")) {
//						stringBuilder.append( "if (v.hasChildren(\"" + nameVariable + "\"))" + "{\n" );
//						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
//						stringBuilder.append( "\t\tv.getChild(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + ";new Value(" + nameVariable + ".get(counter" + nameVariable + ").intValue()));\n" );
//						stringBuilder.append( "\t}\n}else{\n" );
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\").setValue(" + "_" + nameVariable + ".get(counter" + nameVariable + "));\n");
                        stringBuilder.append("\t}");
//						stringBuilder.append( "\n}\n" );




                    } else if (typeName.equals("double")) {
                        //stringBuilder.append( "if (v.hasChildren(\"" + nameVariable + "\"))" + "{\n" );
                        //stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
                        //stringBuilder.append( "\t\tvreturn.getChild(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + ",new Value(" + nameVariable + ".get(counter" + nameVariable + ").doubleValue()));\n" );
                        //stringBuilder.append( "\t}\n}else{\n" );
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\").setValue(" + "_" + nameVariable + ".get(counter" + nameVariable + "));\n");
                        stringBuilder.append("\t}");
                        //stringBuilder.append( "\n}\n" );
                    } else if (typeName.equals("string")) {
//						stringBuilder.append( "if (v.hasChildren(\"" + nameVariable + "\"))" + "{\n" );
//						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
//						stringBuilder.append( "\t\tv.getChild(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + "," + nameVariable + ".get(counter" + nameVariable + ").strValue());\n" );
//						stringBuilder.append( "\t}\n}else{\n" );
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\")" + ".setValue(" + "_" + nameVariable + ".get(counter" + nameVariable + "));\n");
                        stringBuilder.append("\t}");
                        //stringBuilder.append( "\n}\n" );

                    }
                    stringBuilder.append("}\n");
                } else {


                    String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
                    String startingChar = nameVariable.substring(0, 1);
                    String remaningStr = nameVariable.substring(1, nameVariable.length());
                    nameVariableOp = startingChar.toUpperCase() + remaningStr;
                    stringBuilder.append("if((_").append(nameVariable).append("!=null)){\n");
                    if (typeName.equals("int")) {
//						
                        stringBuilder.append("vReturn.getNewChild(\"" + nameVariable + "\")" + ".setValue(" + "_" + nameVariable + ");\n");
                        //stringBuilder.append("\t}");
                        //stringBuilder.append( "}\n" );
                    } else if (typeName.equals("double")) {

                        stringBuilder.append("vReturn.getNewChild(\"" + nameVariable + "\")" + ".setValue(" + "_" + nameVariable + ");\n");
//						//stringBuilder.append("\t}");
//						stringBuilder.append( "}\n" );


                    } else if (typeName.equals("string")) {

//
                        stringBuilder.append("vReturn.getNewChild(\"" + nameVariable + "\")" + ".setValue(" + "_" + nameVariable + ");\n");




                    }
                    stringBuilder.append("}\n");

                }
            }

        }




        stringBuilder.append("return vReturn ;\n");
        stringBuilder.append("}\n");

    }
    private void parseSubType(TypeDefinition typeDefinition){
        if (typeDefinition.hasSubTypes()) {
                     Set<Map.Entry<String, TypeDefinition>> supportSet = typeDefinition.subTypes();
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink){
                       if (!subTypeMap.containsKey(((TypeDefinitionLink) me.getValue()).linkedTypeName())){
                        subTypeMap.put(((TypeDefinitionLink) me.getValue()).linkedTypeName(),((TypeDefinitionLink)me.getValue()).linkedType());
                        parseSubType(((TypeDefinitionLink)me.getValue()).linkedType());
                       }
                }
                   }
                }
        }
}
