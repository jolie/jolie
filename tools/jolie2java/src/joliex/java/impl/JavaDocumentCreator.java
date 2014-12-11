/**
 * *************************************************************************
 * Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *************************************************************************
 */
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
import jolie.lang.NativeType;
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
 * @author balint maschio & michele morgagni
 */
public class JavaDocumentCreator {

    private Vector<TypeDefinition> subclass;
    private boolean subtypePresent = false;
    private String namespace;
    private String targetPort;
    private LinkedHashMap<String, TypeDefinition> typeMap;
    private LinkedHashMap<String, TypeDefinition> subTypeMap;
    ProgramInspector inspector;
    private static HashMap<NativeType, String> javaNativeEquivalent = new HashMap<NativeType, String>();
    private static HashMap<NativeType, String> javaNativeMethod = new HashMap<NativeType, String>();

    public JavaDocumentCreator(ProgramInspector inspector, String namespace, String targetPort) {

        this.inspector = inspector;
        this.namespace = namespace;
        this.targetPort = targetPort;


        javaNativeEquivalent.put(NativeType.INT, "Integer");
        javaNativeEquivalent.put(NativeType.BOOL, "Boolean");
        javaNativeEquivalent.put(NativeType.DOUBLE, "Double");
        javaNativeEquivalent.put(NativeType.LONG, "Long");
        javaNativeEquivalent.put(NativeType.STRING, "String");
        javaNativeEquivalent.put(NativeType.ANY, "Object");
        javaNativeEquivalent.put(NativeType.RAW, "ByteArray");

        javaNativeMethod.put(NativeType.INT, "intValue()");
        javaNativeMethod.put(NativeType.BOOL, "boolValue()");
        javaNativeMethod.put(NativeType.DOUBLE, "doubleValue()");
        javaNativeMethod.put(NativeType.LONG, "longValue()");
        javaNativeMethod.put(NativeType.STRING, "strValue()");
        javaNativeMethod.put(NativeType.RAW, "byteArrayValue()");
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

            if (targetPort == null || inputPort.id().equals(targetPort)) {

                Collection<OperationDeclaration> operations = inputPort.operations();

                Iterator<OperationDeclaration> operatorIterator = operations.iterator();
                while (operatorIterator.hasNext()) {
                    operation = operatorIterator.next();
                    if (operation instanceof RequestResponseOperationDeclaration) {
                        requestResponseOperation = (RequestResponseOperationDeclaration) operation;
                        if (!typeMap.containsKey(requestResponseOperation.requestType().id())) {
                            typeMap.put(requestResponseOperation.requestType().id(), requestResponseOperation.requestType());
                        }
                        if (!typeMap.containsKey(requestResponseOperation.responseType().id())) {
                            typeMap.put(requestResponseOperation.responseType().id(), requestResponseOperation.responseType());
                        }
                        for (Entry<String, TypeDefinition> fault : requestResponseOperation.faults().entrySet()) {
                            if (!typeMap.containsKey(fault.getValue().id())) {
                                typeMap.put(fault.getValue().id(), fault.getValue());
                            }

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

            ConvertSubTypes(typeDefinition, builderHeaderclass);

        } else {
            builderHeaderclass.append("}\n");
        }
        writer.append(builderHeaderclass.toString());
    }

    private void ConvertSubTypes(TypeDefinition typeDefinition, StringBuilder builderHeaderclass) {

        Set<Map.Entry<String, TypeDefinition>> supportSet = typeDefinition.subTypes();
        Iterator i = supportSet.iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            //System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
            /*if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink){
             typeMap.put(((TypeDefinitionLink) me.getValue()).linkedTypeName(),((TypeDefinitionLink)me.getValue()).linkedType());



             }else*/ if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) me.getValue()).hasSubTypes())) {
                builderHeaderclass.append("public class " + ((TypeDefinition) me.getValue()).id() + " {" + "\n");
                ConvertSubTypes((TypeDefinition) me.getValue(), builderHeaderclass);
            }

        }

        variableCreate(builderHeaderclass, typeDefinition);
        constructorCreate(builderHeaderclass, typeDefinition);
        methodsCreate(builderHeaderclass, typeDefinition);
        builderHeaderclass.append("}\n");
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
            stringBuilder.append("import jolie.runtime.Value;\n");
            stringBuilder.append("import jolie.runtime.ByteArray;\n");
            stringBuilder.append("\n");
        }
    }

    private void variableCreate(StringBuilder stringBuilder, TypeDefinition type) {

        if (type.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = type.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                if (subType instanceof TypeDefinitionLink) {

                    //link
                    if (((TypeDefinitionLink) subType).cardinality().max() > 1) {
                        stringBuilder.append("private List<" + ((TypeDefinitionLink) subType).linkedType().id() + "> " + "_" + ((TypeDefinitionLink) subType).id() + ";\n");
                    } else {
                        stringBuilder.append("private " + ((TypeDefinitionLink) subType).linkedType().id() + " " + "_" + ((TypeDefinitionLink) subType).id() + ";\n");
                    }

                } else if (subType instanceof TypeInlineDefinition) {

                    if (subType.hasSubTypes()) {

                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/
                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("private List<" + subType.id() + "> " + "_" + subType.id() + ";\n");
                        } else {
                            stringBuilder.append("private " + subType.id() + " _" + subType.id() + ";\n");
                        }

                    } else {
                        //native type
                        String javaCode = javaNativeEquivalent.get(subType.nativeType());
                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("private List<" + javaCode + "> " + "_" + subType.id() + ";\n");
                        } else {
                            stringBuilder.append("private " + javaCode + " _" + subType.id() + ";\n");
                        }
                    }


                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        if (type.hasSubTypes() && type.nativeType() != NativeType.VOID) {
            stringBuilder.append("private " + javaNativeEquivalent.get(type.nativeType()) + " rootValue;\n");
        }

        stringBuilder.append("\n");





    }

    private void constructorCreate(StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/) {

        //constructor with parameters

        stringBuilder.append("public " + type.id() + "(Value v){\n");
        stringBuilder.append("\n");
        //stringBuilder.append("this.v=v;\n");

        if (type.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = type.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                if (subType instanceof TypeDefinitionLink) {
                    //link
                    if (((TypeDefinitionLink) subType).cardinality().max() > 1) {
                        stringBuilder.append("_" + subType.id() + "= new LinkedList<" + ((TypeDefinitionLink) subType).linkedType().id() + ">();" + "\n");
                        //stringBuilder.append("}\n");

                        //to check:
                        stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                        stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<v.getChildren(\"" + subType.id() + "\").size();counter" + subType.id() + "++){\n");
                        stringBuilder.append("" + ((TypeDefinitionLink) subType).linkedTypeName() + " support").append(subType.id()).append(" = new " + ((TypeDefinitionLink) subType).linkedTypeName() + "(v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append("));\n");
                        stringBuilder.append("" + "_" + subType.id() + ".add(support" + subType.id() + ");\n");
                        stringBuilder.append("}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    } else {
                        stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                        stringBuilder.append("_" + subType.id() + " = new " + ((TypeDefinitionLink) subType).linkedTypeName() + "( v.getFirstChild(\"" + subType.id() + "\"));" + "\n");
                        stringBuilder.append("}\n");
                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (subType.hasSubTypes()) {

                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                        if (((TypeInlineDefinition) subType).cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + subType.id() + ">();" + "\n");

                            //to check:
                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<v.getChildren(\"" + subType.id() + "\").size();counter" + subType.id() + "++){\n");
                            stringBuilder.append("" + subType.id() + " support").append(subType.id()).append("=new " + subType.id() + "(v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append("));\n");
                            stringBuilder.append("" + "_" + subType.id() + ".add(support" + subType.id() + ");\n");
                            stringBuilder.append("}\n");
                            //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                            stringBuilder.append("}\n");
                        } else {
                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                            stringBuilder.append("_" + subType.id() + " = new " + subType.id() + "( v.getFirstChild(\"" + subType.id() + "\"));" + "\n");
                            stringBuilder.append("}\n");
                        }


                    } else {
                        //native type
                        String javaCode = javaNativeEquivalent.get(subType.nativeType());
                        String javaMethod = javaNativeMethod.get(subType.nativeType());

                        if (((TypeDefinition) subType).cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + javaCode + ">();" + "\n");

                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0; " + "counter" + subType.id() + "<v.getChildren(\"" + subType.id() + "\").size(); counter" + subType.id() + "++){\n");
                            if (subType.nativeType() != NativeType.ANY) {
                                stringBuilder.append("" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append(")." + javaMethod + ";\n");
                                stringBuilder.append("" + "_" + subType.id() + ".add(support" + subType.id() + ");\n");
                            } else {
                                stringBuilder.append("if(v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").isDouble()){\n"
                                        + "" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").doubleValue();\n"
                                        + "" + "_" + subType.id() + ".add(support" + subType.id() + ");\n"
                                        + "}else if(v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").isString()){\n"
                                        + "" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").strValue();\n"
                                        + "" + "_" + subType.id() + ".add(support" + subType.id() + ");\n"
                                        + "}else if(v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").isInt()){\n"
                                        + "" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").intValue();\n"
                                        + "" + "_" + subType.id() + ".add(support" + subType.id() + ");\n"
                                        + "}else if(v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").isBool()){\n"
                                        + "" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ").boolValue();\n"
                                        + "" + "_" + subType.id() + ".add(support" + subType.id() + ");\n"
                                        + "}\n");
                            }
                            stringBuilder.append("}\n");
                            //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                            stringBuilder.append("}\n");
                        } else {
                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");


                            if (subType.nativeType() != NativeType.ANY) {
                                stringBuilder.append("_" + subType.id() + "= v.getFirstChild(\"" + subType.id() + "\")." + javaMethod + ";" + "\n");
                            } else {
                                stringBuilder.append("if(v.getFirstChild(\"" + subType.id() + "\").isDouble()){\n"
                                        + "_" + subType.id() + " = v.getFirstChild(\"" + subType.id() + "\").doubleValue();\n"
                                        + "}else if(v.getFirstChild(\"" + subType.id() + "\").isString()){\n"
                                        + "_" + subType.id() + " = v.getFirstChild(\"" + subType.id() + "\").strValue();\n"
                                        + "}else if(v.getFirstChild(\"" + subType.id() + "\").isInt()){\n"
                                        + "_" + subType.id() + " = v.getFirstChild(\"" + subType.id() + "\").intValue();\n"
                                        + "}else if(v.getFirstChild(\"" + subType.id() + "\").isBool()){\n"
                                        + "_" + subType.id() + " = v.getFirstChild(\"" + subType.id() + "\").boolValue();\n"
                                        + "}\n");
                            }
                            stringBuilder.append("}\n");
                        }

                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        if (type.hasSubTypes() && type.nativeType() != NativeType.VOID) {

            String javaCode = javaNativeEquivalent.get(type.nativeType());
            String javaMethod = javaNativeMethod.get(type.nativeType());

            if (type.nativeType() != NativeType.ANY) {
                stringBuilder.append("rootValue = v." + javaMethod + ";" + "\n");
            } else {
                stringBuilder.append("if(v.isDouble()){\n"
                        + "rootValue = v.doubleValue();\n"
                        + "}else if(v.isString()){\n"
                        + "rootValue = v.strValue();\n"
                        + "}else if(v.isInt()){\n"
                        + "rootValue = v.intValue();\n"
                        + "}else if(v.isBool()){\n"
                        + "rootValue = v.boolValue();\n"
                        + "}\n");
            }
        }
        stringBuilder.append("}\n");



        //constructor without parameters

        stringBuilder.append("public " + type.id() + "(){\n");
        stringBuilder.append("\n");

        if (type.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = type.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                if (subType instanceof TypeDefinitionLink) {
                    //link
                    if (((TypeDefinitionLink) subType).cardinality().max() > 1) {
                        stringBuilder.append("_" + subType.id() + "= new LinkedList<" + ((TypeDefinitionLink) subType).linkedType().id() + ">();" + "\n");
                        //stringBuilder.append("}\n");
                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (subType.hasSubTypes()) {

                        if (((TypeInlineDefinition) subType).cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + subType.id() + ">();" + "\n");
                        }
                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                    } else {
                        //native type
                        String javaCode = javaNativeEquivalent.get(subType.nativeType());
                        //String javaMethod = javaNativeMethod.get(subType.nativeType());

                        if (((TypeDefinition) subType).cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + javaCode + ">();" + "\n");
                        }
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        stringBuilder.append("}\n");
    }

    private void methodsCreate(StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/) {

        if (type.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = type.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                String nameVariable = subType.id();
                String startingChar = nameVariable.substring(0, 1);
                String remaningStr = nameVariable.substring(1, nameVariable.length());
                String nameVariableOp = startingChar.toUpperCase() + remaningStr;
                if ( nameVariableOp.equals("Value") ) {
                    nameVariableOp = "__Value";
                }

                if (subType instanceof TypeDefinitionLink) {
                    //link

                    if (subType.cardinality().max() > 1) {

                        stringBuilder.append("public " + ((TypeDefinitionLink) subType).linkedTypeName() + " get" + nameVariableOp + "Value(int index){\n");
                        stringBuilder.append("\nreturn " + "_" + nameVariable + ".get(index);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                        stringBuilder.append("\nreturn " + "_" + nameVariable + ".size();\n");
                        stringBuilder.append("}\n");


                        stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + ((TypeDefinitionLink) subType).linkedTypeName() + " value ){\n");
                        stringBuilder.append("\n" + "_" + nameVariable + ".add(value);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                        stringBuilder.append("" + "_" + nameVariable + ".remove(index);\n");
                        stringBuilder.append("}\n");

                    } else {

                        stringBuilder.append("public " + ((TypeDefinitionLink) subType).linkedTypeName() + " get" + nameVariableOp + "(){\n");
                        stringBuilder.append("\nreturn " + "_" + nameVariable + ";\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void set" + nameVariableOp + "(" + ((TypeDefinitionLink) subType).linkedTypeName() + " value ){\n");
                        stringBuilder.append("\n" + "_" + nameVariable + " = value;\n");
                        stringBuilder.append("}\n");

                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (subType.hasSubTypes()) {

                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                        if (subType.cardinality().max() > 1) {

                            stringBuilder.append("public " + subType.id() + " get" + nameVariableOp + "Value(int index){\n");
                            stringBuilder.append("\nreturn " + "_" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\nreturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + subType.id() + " value ){\n");
                            stringBuilder.append("\n" + "_" + nameVariable + ".add(value);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            stringBuilder.append("" + "_" + nameVariable + ".remove(index);\n");
                            stringBuilder.append("}\n");

                        } else {

                            stringBuilder.append("public " + subType.id() + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\nreturn " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "(" + subType.id() + " value ){\n");
                            stringBuilder.append("\n" + "_" + nameVariable + " = value;\n");
                            stringBuilder.append("}\n");

                        }

                    } else {
                        //native type

                        String javaCode = javaNativeEquivalent.get(subType.nativeType());
                        String javaMethod = javaNativeMethod.get(subType.nativeType());

                        if (subType.nativeType() != NativeType.VOID) {

                            if (subType.cardinality().max() > 1) {

                                stringBuilder.append("public int get" + nameVariableOp + "Size(){\n");
                                stringBuilder.append("\nreturn " + "_" + nameVariable + ".size();\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + javaCode + " get" + nameVariableOp + "Value(int index){\n");
                                stringBuilder.append("return " + "_" + nameVariable + ".get(index);\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + javaCode + " value ){\n");
                                stringBuilder.append("" + javaCode + " support").append(nameVariable).append(" = value;\n");
                                stringBuilder.append("" + "_" + nameVariable + ".add(" + "support" + nameVariable + " );\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                                stringBuilder.append("" + "_" + nameVariable + ".remove(index);\n");
                                stringBuilder.append("}\n");

                            } else {
                                stringBuilder.append("public " + javaCode + " get" + nameVariableOp + "(){\n");
                                stringBuilder.append("\nreturn " + "_" + nameVariable + ";\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void set" + nameVariableOp + "(" + javaCode + " value ){\n");
                                stringBuilder.append("\n" + "_" + nameVariable + " = value;\n");
                                stringBuilder.append("}\n");
                            }



                        }
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
            if (type.hasSubTypes() && type.nativeType() != NativeType.VOID) {

                String javaCode = javaNativeEquivalent.get(type.nativeType());
                String javaMethod = javaNativeMethod.get(type.nativeType());

                stringBuilder.append("public " + javaCode + " getRootValue(){\n");
                stringBuilder.append("\nreturn " + "rootValue;\n");
                stringBuilder.append("}\n");

                stringBuilder.append("public void setRootValue(" + javaCode + " value){\n");
                stringBuilder.append("\nrootValue = value;\n");
                stringBuilder.append("}\n");

            }

        }


        //getValue

        stringBuilder.append("public " + "Value getValue(){\n");
        stringBuilder.append("Value vReturn = Value.create();\n");

        if (type.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = type.subTypes();
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                if (subType instanceof TypeDefinitionLink) {
                    //link
                    if (subType.cardinality().max() > 1) {

                        stringBuilder.append("if(_").append(subType.id()).append("!=null){\n");
                        stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<" + "_" + subType.id() + ".size();counter" + subType.id() + "++){\n");
                        stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\").deepCopy(" + "_" + subType.id() + ".get(counter" + subType.id() + ").getValue());\n");
                        stringBuilder.append("}\n");
                        stringBuilder.append("}\n");

                    } else {
                        stringBuilder.append("if((_").append(subType.id()).append("!=null)){\n");
                        stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".deepCopy(" + "_" + subType.id() + ".getValue());\n");
                        stringBuilder.append("}\n");
                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (subType.hasSubTypes()) {

                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/
                        if (subType.cardinality().max() > 1) {

                            stringBuilder.append("if(_").append(subType.id()).append("!=null){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<" + "_" + subType.id() + ".size();counter" + subType.id() + "++){\n");
                            stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\").deepCopy(" + "_" + subType.id() + ".get(counter" + subType.id() + ").getValue());\n");
                            stringBuilder.append("}\n");
                            stringBuilder.append("}\n");

                        } else {
                            stringBuilder.append("if((_").append(subType.id()).append("!=null)){\n");
                            stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".deepCopy(" + "_" + subType.id() + ".getValue());\n");
                            stringBuilder.append("}\n");
                        }

                    } else {
                        //native type

                        String javaCode = javaNativeEquivalent.get(type.nativeType());
                        String javaMethod = javaNativeMethod.get(type.nativeType());

                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("if(_").append(subType.id()).append("!=null){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<" + "_" + subType.id() + ".size();counter" + subType.id() + "++){\n");
                            if (subType.nativeType() != NativeType.ANY) {
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\").setValue(" + "_" + subType.id() + ".get(counter" + subType.id() + "));\n");
                            } else {
                                stringBuilder.append("if(_" + subType.id() + ".get(counter" + subType.id() + ") instanceof Integer){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "((Integer)(_" + subType.id() + ".get(counter" + subType.id() + "))).intValue());\n");
                                stringBuilder.append("}else if(_" + subType.id() + ".get(counter" + subType.id() + ") instanceof Double){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "((Double)(_" + subType.id() + ".get(counter" + subType.id() + "))).doubleValue());\n");
                                stringBuilder.append("}else if(_" + subType.id() + ".get(counter" + subType.id() + ") instanceof String){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "(String)(_" + subType.id() + ".get(counter" + subType.id() + ")));\n");
                                stringBuilder.append("}else if(_" + subType.id() + ".get(counter" + subType.id() + ") instanceof Boolean){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "(Boolean)(_" + subType.id() + ".get(counter" + subType.id() + ")));\n");
                                stringBuilder.append("}");
                            }
                            stringBuilder.append("}");
                            stringBuilder.append("}\n");

                        } else {
                            stringBuilder.append("if((_").append(subType.id()).append("!=null)){\n");
                            if (subType.nativeType() != NativeType.ANY) {
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "_" + subType.id() + ");\n");
                            } else {
                                stringBuilder.append("if(_" + subType.id() + " instanceof Integer){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "((Integer)(_" + subType.id() + ")).intValue());\n");
                                stringBuilder.append("}else if(_" + subType.id() + " instanceof Double){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "((Double)(_" + subType.id() + ")).doubleValue());\n");
                                stringBuilder.append("}else if(_" + subType.id() + " instanceof String){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "(String)(_" + subType.id() + "));\n");
                                
                                stringBuilder.append("}else if(_" + subType.id() + " instanceof Boolean){\n");
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(" + "(Boolean)(_" + subType.id() + "));\n");
                                stringBuilder.append("}");
                            }
                            stringBuilder.append("}\n");
                        }
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        if (type.hasSubTypes() && type.nativeType() != NativeType.VOID) {

            stringBuilder.append("if((rootValue!=null)){\n");
            if (type.nativeType() != NativeType.ANY) {
                stringBuilder.append("vReturn.setValue(rootValue);\n");
            } else {
                stringBuilder.append("if(rootValue instanceof Integer){\n");
                stringBuilder.append("vReturn.setValue(((Integer)(rootValue)).intValue());\n");
                stringBuilder.append("}else if(rootValue instanceof Double){\n");
                stringBuilder.append("vReturn.setValue(((Double)(rootValue)).doubleValue());\n");
                stringBuilder.append("}else if(rootValue instanceof String){\n");
                stringBuilder.append("vReturn.setValue(((String)(rootValue)));\n");
                stringBuilder.append("}else if(rootValue instanceof Boolean){\n");
                stringBuilder.append("vReturn.setValue(((Boolean)(rootValue)));\n");
                stringBuilder.append("}");
            }

            stringBuilder.append("}\n");

        }

        stringBuilder.append("return vReturn ;\n");
        stringBuilder.append("}\n");
    }

    private void parseSubType(TypeDefinition typeDefinition) {
        if (typeDefinition.hasSubTypes()) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = typeDefinition.subTypes();
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                //System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
                if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                    if (!subTypeMap.containsKey(((TypeDefinitionLink) me.getValue()).linkedTypeName())) {
                        subTypeMap.put(((TypeDefinitionLink) me.getValue()).linkedTypeName(), ((TypeDefinitionLink) me.getValue()).linkedType());
                        parseSubType(((TypeDefinitionLink) me.getValue()).linkedType());
                    }
                }
            }
        }
    }
}
