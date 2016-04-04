/***************************************************************************
 * Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>    *
 * Copyright (C) 2012 by Michele Morgagni <mmorgagni@italianasoftware.com> *
 * Copyright (C) 2013 by Claudio Guidi <guidiclaudio@gmail.com>            *
 * Copyright (C) 2015 by Matthias Dieter Wallnöfer                         *
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
import jolie.runtime.FaultException;

public class JavaGWTDocumentCreator {

    private Vector<TypeDefinition> subclass;
    private boolean subtypePresent = false;
    private String namespace;
    private String targetPort;
    private LinkedHashMap<String, TypeDefinition> typeMap;
    private LinkedHashMap<String, TypeDefinition> subTypeMap;
    ProgramInspector inspector;
    private static HashMap<NativeType, String> javaNativeEquivalent = new HashMap<NativeType, String>();
    private static HashMap<NativeType, String> javaNativeMethod = new HashMap<NativeType, String>();
    private static HashMap<NativeType, String> javaNativeChecker = new HashMap<NativeType, String>();

    public JavaGWTDocumentCreator(ProgramInspector inspector, String namespace, String targetPort) {

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

        javaNativeChecker.put(NativeType.INT, "isInt()");
        javaNativeChecker.put(NativeType.BOOL, "isBool()");
        javaNativeChecker.put(NativeType.DOUBLE, "isDouble()");
        javaNativeChecker.put(NativeType.LONG, "isLong()");
        javaNativeChecker.put(NativeType.STRING, "isString()");
        javaNativeChecker.put(NativeType.RAW, "isByteArray()");
    }

    public void ConvertDocument() throws FaultException {

        typeMap = new LinkedHashMap<String, TypeDefinition>();
        subTypeMap = new LinkedHashMap<String, TypeDefinition>();
        subclass = new Vector<TypeDefinition>();

        try {
            // creating ZipOutputStream
            File jarFile = new File("archive.jar");
            FileOutputStream os = new FileOutputStream(jarFile);
            ZipOutputStream zipStream = new ZipOutputStream(os);


            TypeDefinition[] support = inspector.getTypes();
            InputPortInfo[] inputPorts = inspector.getInputPorts();
            OutputPortInfo[] outputPorts = inspector.getOutputPorts();
            OperationDeclaration operation;
            RequestResponseOperationDeclaration requestResponseOperation;

            for (InputPortInfo inputPort : inputPorts) {
                if (targetPort == null || inputPort.id().equals(targetPort)) {
                    ConvertInputPorts(inputPort, outputPorts, zipStream);
                    Map<String, OperationDeclaration> operations = inputPort.operationsMap();

                    for (int x = 0; x < inputPort.aggregationList().length; x++) {
                        int i = 0;
                        while (!inputPort.aggregationList()[x].outputPortList()[0].equals(outputPorts[i].id())) {
                            i++;
                        }
                        for (InterfaceDefinition interfaceDefinition : outputPorts[i].getInterfaceList()) {

                            for (Entry<String, OperationDeclaration> entry : interfaceDefinition.operationsMap().entrySet()) {
                                operations.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    for (Entry<String, OperationDeclaration> operationEntry : operations.entrySet()) {

                        if (operationEntry.getValue() instanceof RequestResponseOperationDeclaration) {
                            requestResponseOperation = (RequestResponseOperationDeclaration) operationEntry.getValue();
                            if (!typeMap.containsKey(requestResponseOperation.requestType().id())) {
                                typeMap.put(requestResponseOperation.requestType().id(), requestResponseOperation.requestType());
                            }
                            if (!typeMap.containsKey(requestResponseOperation.responseType().id())) {
                                typeMap.put(requestResponseOperation.responseType().id(), requestResponseOperation.responseType());
                            }
                        } else {
                            OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operationEntry.getValue();
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
                if (!typeMap.containsKey(subTypeEntry.getKey())) {
                    typeMap.put(subTypeEntry.getKey(), subTypeEntry.getValue());
                }

            }
            typeMapIterator = typeMap.entrySet().iterator();
            while (typeMapIterator.hasNext()) {
                Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
                if (!(typeEntry.getKey().equals("undefined"))) {
                    subclass = new Vector<TypeDefinition>();
                    subtypePresent = false;
                    ConvertTypes(typeEntry.getValue(), zipStream, namespace);
                }
            }

            zipStream.close();
            os.flush();
            os.close();

        } catch (IOException e) {
            throw new FaultException(e);
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

    public void ConvertInputPorts(InputPortInfo inputPortInfo, OutputPortInfo[] outputPorts, ZipOutputStream zipStream)
            throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder operationCallBuilder = new StringBuilder();
        stringBuilder.append("package ").append(namespace).append(".").append(inputPortInfo.id()).append(";\n");

        // adding imports
        stringBuilder.append("import joliex.gwt.client.JolieCallback;\n");
        stringBuilder.append("import joliex.gwt.client.JolieService;\n");
        stringBuilder.append("import joliex.gwt.client.Value;\n");
        Map<String, OperationDeclaration> operations = inputPortInfo.operationsMap();

        for (int x = 0; x < inputPortInfo.aggregationList().length; x++) {
            int i = 0;
            while (!inputPortInfo.aggregationList()[x].outputPortList()[0].equals(outputPorts[i].id())) {
                i++;
            }
            for (InterfaceDefinition interfaceDefinition : outputPorts[i].getInterfaceList()) {

                for (Entry<String, OperationDeclaration> entry : interfaceDefinition.operationsMap().entrySet()) {
                    operations.put(entry.getKey(), entry.getValue());
                }
            }
        }

        for (Entry<String, OperationDeclaration> operationEntry : operations.entrySet()) {
            OperationDeclaration operation;
            operation = operationEntry.getValue();

            if (operation instanceof RequestResponseOperationDeclaration) {
                RequestResponseOperationDeclaration requestResponseOperation = (RequestResponseOperationDeclaration) operation;
                stringBuilder.append("import ").append(namespace).append(".types.").append(requestResponseOperation.requestType().id()).append(";\n");
                stringBuilder.append("import ").append(namespace).append(".").append(inputPortInfo.id()).append(".callbacks.CallBack").append(requestResponseOperation.id()).append(";\n");
                operationCallBuilder.append(getPortOperationMethod(requestResponseOperation.id(), requestResponseOperation.requestType().id()));
                generateCallBackClass(requestResponseOperation, zipStream, inputPortInfo.id());
            } else {
                OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
                System.out.println("OneWay operation not supported for GWT: " + oneWayOperationDeclaration.id());
            }
        }
        stringBuilder.append("\n");

        // adding class
        stringBuilder.append("public class ").append(inputPortInfo.id()).append("Port {\n");

        // adding operation methods
        stringBuilder.append(operationCallBuilder);

        // adding private call method
        stringBuilder.append("private void call( String operation_name, Value request, JolieCallback callback ) {\nJolieService.Util.getInstance().call( operation_name, request, callback );\n}\n");
        // closing class
        stringBuilder.append("}\n;");

        String namespaceDir = namespace.replaceAll("\\.", "/");
        ZipEntry zipEntry = new ZipEntry(namespaceDir + "/" + inputPortInfo.id() + "/" + inputPortInfo.id() + "Port.java");;
        zipStream.putNextEntry(zipEntry);
        byte[] bb = stringBuilder.toString().getBytes();
        zipStream.write(bb, 0, bb.length);
        zipStream.closeEntry();

    }

    public void ConvertOperations(OperationDeclaration operationDeclaration, Writer writer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ConvertTypes(TypeDefinition typeDefinition, ZipOutputStream zipStream, String portName)
            throws IOException {
        StringBuilder builderHeaderclass = new StringBuilder();
        builderHeaderclass.append("package ").append(namespace).append(".types;\n");
        importsCreate(builderHeaderclass, typeDefinition);
        convertClass(typeDefinition, builderHeaderclass);

        String namespaceDir = namespace.replaceAll("\\.", "/");
        ZipEntry zipEntry = new ZipEntry(namespaceDir + "/types/" + typeDefinition.id() + ".java");;
        zipStream.putNextEntry(zipEntry);
        byte[] bb = builderHeaderclass.toString().getBytes();
        zipStream.write(bb, 0, bb.length);
        zipStream.closeEntry();

    }

    private void ConvertSubTypes(TypeDefinition typeDefinition, StringBuilder builderHeaderclass) {
        Set<Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
        Iterator i = supportSet.iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (Utils.hasSubTypes(((TypeDefinition) me.getValue())))) {
                convertClass((TypeDefinition) me.getValue(), builderHeaderclass);
            }
        }

    }

    private void convertClass(TypeDefinition typeDefinition, StringBuilder stringBuilder) {
        stringBuilder.append("public class ").append(typeDefinition.id()).append(" {" + "\n");
        if (Utils.hasSubTypes(typeDefinition)) {
            ConvertSubTypes(typeDefinition, stringBuilder);
        }
        variableCreate(stringBuilder, typeDefinition);
        constructorCreate(stringBuilder, typeDefinition/*, true*/);
        methodsCreate(stringBuilder, typeDefinition/*, true*/);
        addGetValueMethod(stringBuilder, typeDefinition);
        stringBuilder.append("}\n");
    }

    private void importsCreate(StringBuilder stringBuilder, TypeDefinition type) {
        stringBuilder.append("import joliex.gwt.client.Value;\n");
        if (Utils.hasSubTypes(type)) {
            subtypePresent = true;
            stringBuilder.append("import java.util.List;\n");
            stringBuilder.append("import java.util.LinkedList;\n");
            stringBuilder.append("import joliex.gwt.client.ByteArray;\n");
            stringBuilder.append("\n");
        }
    }

    private void generateCallBackClass(RequestResponseOperationDeclaration operation, ZipOutputStream zipStream, String portName) {
        try {

            // generate class
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("package ").append(namespace).append(".").append(portName).append(".callbacks;\n");

            // adding imports
            stringBuilder.append("import joliex.gwt.client.FaultException;\n");
            stringBuilder.append("import joliex.gwt.client.JolieCallback;\n");
            stringBuilder.append("import joliex.gwt.client.Value;\n");
            stringBuilder.append("import ").append(namespace).append(".types.").append(operation.responseType().id()).append(";\n");
            stringBuilder.append("\n");

            stringBuilder.append("public abstract class CallBack").append(operation.id()).append(" extends JolieCallback{\n");

            //adding onFault method
            stringBuilder.append("@Override\nprotected void onFault(FaultException fault) {\n");
            for (Entry<String, TypeDefinition> fault : operation.faults().entrySet()) {
                stringBuilder.append("if ( fault.faultName().equals(\"").append(fault.getKey()).append("\") ) {\n");
                stringBuilder.append("onFault").append(fault.getKey()).append("();\n");
                stringBuilder.append("}\n");
            }
            stringBuilder.append("}\n");

            // adding onSuccessfullReply method
            stringBuilder.append("@Override\npublic void onSuccess(Value response) {\nonSuccessfullReply( new ");
            stringBuilder.append(operation.responseType().id()).append("( response ) );\n}\n");



            // adding abstract methods to be implemented
            for (Entry<String, TypeDefinition> fault : operation.faults().entrySet()) {
                stringBuilder.append("public abstract void onFault").append(fault.getKey()).append("();\n");
            }
            stringBuilder.append("public abstract void onSuccessfullReply(").append(operation.responseType().id()).append(" response );\n");

            //closign class
            stringBuilder.append("}\n");

            String namespaceDir = namespace.replaceAll("\\.", "/");
            ZipEntry zipEntry = new ZipEntry(namespaceDir + "/" + portName + "/callbacks/" + "CallBack" + operation.id() + ".java");
            zipStream.putNextEntry(zipEntry);
            byte[] bb = stringBuilder.toString().getBytes();
            zipStream.write(bb, 0, bb.length);
            zipStream.closeEntry();


        } catch (IOException ex) {
            Logger.getLogger(JavaGWTDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private StringBuilder getPortOperationMethod(String operationName, String requestTypeName) {
        StringBuilder operationCallBuilder = new StringBuilder();
        operationCallBuilder.append("public void ").append(operationName).append("(").append(requestTypeName);
        operationCallBuilder.append(" message, CallBack").append(operationName).append(" callback ) {\n");
        operationCallBuilder.append("call( \"").append(operationName).append("\", message.getValue(), callback );\n}\n");
        return operationCallBuilder;
    }

    private void variableCreate(StringBuilder stringBuilder, TypeDefinition type) {

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
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

                    if (Utils.hasSubTypes(subType)) {

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
                        String javaCode = javaNativeEquivalent.get(Utils.nativeType(subType));
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

        if (Utils.nativeType(type) != NativeType.VOID) {
            stringBuilder.append("private " + javaNativeEquivalent.get(Utils.nativeType(type)) + " rootValue;\n");
        }

        // stringBuilder.append("private Value v ;\n");
        //stringBuilder.append("private Value vReturn=  new Value() ;\n");
        stringBuilder.append("\n");





    }

    private void constructorCreate(StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/) {

        //constructor with parameters

        stringBuilder.append("public " + type.id() + "( Value v ){\n");
        //stringBuilder.append("this.v=v;\n");

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
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
                        stringBuilder.append(((TypeDefinitionLink) subType).linkedTypeName() + " support").append(subType.id()).append(" = new " + ((TypeDefinitionLink) subType).linkedTypeName() + "(v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append("));\n");
                        stringBuilder.append("_" + subType.id() + ".add(support" + subType.id() + ");\n");
                        stringBuilder.append("}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    } else {
                        stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                        stringBuilder.append("_" + subType.id() + " = new " + ((TypeDefinitionLink) subType).linkedTypeName() + "( v.getFirstChild(\"" + subType.id() + "\"));" + "\n");
                        stringBuilder.append("}\n");
                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (Utils.hasSubTypes(subType)) {

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
                            stringBuilder.append(subType.id() + " support").append(subType.id()).append("=new " + subType.id() + "(v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append("));\n");
                            stringBuilder.append("_" + subType.id() + ".add(support" + subType.id() + ");\n");
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
                        String javaCode = javaNativeEquivalent.get(Utils.nativeType(subType));
                        String javaMethod = javaNativeMethod.get(Utils.nativeType(subType));

                        if (((TypeDefinition) subType).cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + javaCode + ">();" + "\n");

                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0; " + "counter" + subType.id() + "<v.getChildren(\"" + subType.id() + "\").size(); counter" + subType.id() + "++){\n");
                            if (Utils.nativeType(subType) != NativeType.ANY) {
                                stringBuilder.append("" + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append(")." + javaMethod + ";\n");
                                stringBuilder.append("_" + subType.id() + ".add(support" + subType.id() + ");\n");
                            } else {
	                        for (NativeType t : NativeType.class.getEnumConstants()) {
                                    if (!javaNativeChecker.containsKey(t))
                                        continue;
                                    stringBuilder.append(
                                        "if(v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ")." + javaNativeChecker.get(t) + "){\n"
                                        + javaCode + " support").append(subType.id()).append(" = v.getChildren(\"" + subType.id() + "\").get(counter" + subType.id() + ")." + javaNativeMethod.get(t) + ";\n"
                                        + "_" + subType.id() + ".add(support" + subType.id() + ");\n"
                                        + "}\n");
                                }
                            }
                            stringBuilder.append("}\n");
                            //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );
                            stringBuilder.append("}\n");
                        } else {
                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");


                            if (Utils.nativeType(subType) != NativeType.ANY) {
                                stringBuilder.append("_" + subType.id() + "= v.getFirstChild(\"" + subType.id() + "\")." + javaMethod + ";" + "\n");
                            } else {
                                for (NativeType t : NativeType.class.getEnumConstants()) {
                                    if (!javaNativeChecker.containsKey(t))
                                        continue;
                                    stringBuilder.append(
                                        "if(v.getFirstChild(\"" + subType.id() + "\")." + javaNativeChecker.get(t) + "){\n"
                                        + "_" + subType.id() + " = v.getFirstChild(\"" + subType.id() + "\")." + javaNativeMethod.get(t) + ";\n"
                                        + "}\n");
                                }
                            }
                            stringBuilder.append("}\n");
                        }

                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        if (Utils.nativeType(type) != NativeType.VOID) {

            String javaCode = javaNativeEquivalent.get(Utils.nativeType(type));
            String javaMethod = javaNativeMethod.get(Utils.nativeType(type));

            if (Utils.nativeType(type) != NativeType.ANY) {
                stringBuilder.append("rootValue = v." + javaMethod + ";" + "\n");
            } else {
                for (NativeType t : NativeType.class.getEnumConstants()) {
                    if (!javaNativeChecker.containsKey(t))
                        continue;
                    stringBuilder.append(
                        "if(v." + javaNativeChecker.get(t) + "){\n"
                        + "rootValue = v." + javaNativeMethod.get(t) + ";\n"
                        + "}\n");
                }
            }
        }
        stringBuilder.append("}\n");



        //constructor without parameters

        stringBuilder.append("public " + type.id() + "(){\n");

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
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

                    if (Utils.hasSubTypes(subType)) {

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
                        String javaCode = javaNativeEquivalent.get(Utils.nativeType(subType));
                        //String javaMethod = javaNativeMethod.get(Utils.nativeType(subType));

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

    private void addGetValueMethod(StringBuilder stringBuilder, TypeDefinition type) {

        stringBuilder.append("public Value getValue(){\n");
        stringBuilder.append("Value vReturn = new Value();\n");
        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
            Iterator subtypeIterator = supportSet.iterator();
            while (subtypeIterator.hasNext()) {
                TypeDefinition subType = (TypeDefinition) (((Map.Entry) subtypeIterator.next()).getValue());
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
                    if (Utils.hasSubTypes(subType)) {

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

                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("if(_").append(subType.id()).append("!=null){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0;" + "counter" + subType.id() + "<" + "_" + subType.id() + ".size();counter" + subType.id() + "++){\n");
                            if (Utils.nativeType(subType) != NativeType.ANY) {
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\").setValue(_" + subType.id() + ".get(counter" + subType.id() + "));\n");
                            } else {
                                for (NativeType t : NativeType.class.getEnumConstants()) {
                                    if (!javaNativeChecker.containsKey(t))
                                        continue;
                                    stringBuilder.append(
                                        "if(_" + subType.id() + ".get(counter" + subType.id() + ") instanceof " + javaNativeEquivalent.get(t) + "){\n"
                                        + "vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(_" + subType.id() + ".get(counter" + subType.id() + "));\n"
                                        + "}\n");
                                }
                            }
                            stringBuilder.append("}\n");
                            stringBuilder.append("}\n");

                        } else {
                            stringBuilder.append("if((_").append(subType.id()).append("!=null)){\n");
                            if (Utils.nativeType(subType) != NativeType.ANY) {
                                stringBuilder.append("vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(_" + subType.id() + ");\n");
                            } else {
                                for (NativeType t : NativeType.class.getEnumConstants()) {
                                    if (!javaNativeChecker.containsKey(t))
                                        continue;
                                    stringBuilder.append(
                                        "if(_" + subType.id() + " instanceof " + javaNativeEquivalent.get(t) + "){\n"
                                        + "vReturn.getNewChild(\"" + subType.id() + "\")" + ".setValue(_" + subType.id() + ");\n"
                                        + "}\n");
                                }
                            }
                            stringBuilder.append("}\n");
                        }
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
        }

        if (Utils.nativeType(type) != NativeType.VOID) {

            stringBuilder.append("if((rootValue!=null)){\n");
            if (Utils.nativeType(type) != NativeType.ANY) {
                stringBuilder.append("vReturn.setValue(rootValue);\n");
            } else {
                for (NativeType t : NativeType.class.getEnumConstants()) {
                    if (!javaNativeChecker.containsKey(t))
                        continue;
                    stringBuilder.append(
                        "if(rootValue instanceof " + javaNativeEquivalent.get(t) + "){\n"
                        + "vReturn.setValue(rootValue);\n"
                        + "}\n");
                }
            }

            stringBuilder.append("}\n");

        }

        stringBuilder.append("return vReturn;\n");
        stringBuilder.append("}\n");
    }

    private void methodsCreate(StringBuilder stringBuilder, TypeDefinition type) {

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                String nameVariable = subType.id();
                String startingChar = nameVariable.substring(0, 1);
                String remaningStr = nameVariable.substring(1, nameVariable.length());
                String nameVariableOp = startingChar.toUpperCase() + remaningStr;

                if (subType instanceof TypeDefinitionLink) {
                    //link

                    if (subType.cardinality().max() > 1) {

                        stringBuilder.append("public " + ((TypeDefinitionLink) subType).linkedTypeName() + " get" + nameVariableOp + "Value( int index ){\n");
                        stringBuilder.append("return " + "_" + nameVariable + ".get(index);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                        stringBuilder.append("return " + "_" + nameVariable + ".size();\n");
                        stringBuilder.append("}\n");


                        stringBuilder.append("public " + "void add" + nameVariableOp + "Value( " + ((TypeDefinitionLink) subType).linkedTypeName() + " value ){\n");
                        stringBuilder.append("_" + nameVariable + ".add(value);\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                        stringBuilder.append("_" + nameVariable + ".remove(index);\n");
                        stringBuilder.append("}\n");

                    } else {

                        stringBuilder.append("public " + ((TypeDefinitionLink) subType).linkedTypeName() + " get" + nameVariableOp + "(){\n");
                        stringBuilder.append("return " + "_" + nameVariable + ";\n");
                        stringBuilder.append("}\n");

                        stringBuilder.append("public " + "void set" + nameVariableOp + "( " + ((TypeDefinitionLink) subType).linkedTypeName() + " value ){\n");
                        stringBuilder.append("_" + nameVariable + " = value;\n");
                        stringBuilder.append("}\n");

                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (Utils.hasSubTypes(subType)) {

                        /*if(subType.nativeType()==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                        if (subType.cardinality().max() > 1) {

                            stringBuilder.append("public " + subType.id() + " get" + nameVariableOp + "Value( int index ){\n");
                            stringBuilder.append("return " + "_" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("return " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value( " + subType.id() + " value ){\n");
                            stringBuilder.append("_" + nameVariable + ".add(value);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            stringBuilder.append("_" + nameVariable + ".remove(index);\n");
                            stringBuilder.append("}\n");

                        } else {

                            stringBuilder.append("public " + subType.id() + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("return " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "( " + subType.id() + " value ){\n");
                            stringBuilder.append("_" + nameVariable + " = value;\n");
                            stringBuilder.append("}\n");

                        }

                    } else {
                        //native type

                        String javaCode = javaNativeEquivalent.get(Utils.nativeType(subType));
                        String javaMethod = javaNativeMethod.get(Utils.nativeType(subType));

                        if ( Utils.nativeType(subType) != NativeType.VOID) {

                            if (subType.cardinality().max() > 1) {

                                stringBuilder.append("public int get" + nameVariableOp + "Size(){\n");
                                stringBuilder.append("return " + "_" + nameVariable + ".size();\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + javaCode + " get" + nameVariableOp + "Value( int index ){\n");
                                stringBuilder.append("return " + "_" + nameVariable + ".get(index);\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void add" + nameVariableOp + "Value( " + javaCode + " value ){\n");
                                stringBuilder.append(javaCode + " support").append(nameVariable).append(" = value;\n");
                                stringBuilder.append("_" + nameVariable + ".add(" + "support" + nameVariable + " );\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                                stringBuilder.append("_" + nameVariable + ".remove(index);\n");
                                stringBuilder.append("}\n");

                            } else {
                                stringBuilder.append("public " + javaCode + " get" + nameVariableOp + "(){\n");
                                stringBuilder.append("return " + "_" + nameVariable + ";\n");
                                stringBuilder.append("}\n");

                                stringBuilder.append("public " + "void set" + nameVariableOp + "(" + javaCode + " value ){\n");
                                stringBuilder.append("_" + nameVariable + " = value;\n");
                                stringBuilder.append("}\n");
                            }



                        }
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link or an Inline Definition!");
                }
            }
            if (Utils.nativeType(type) != NativeType.VOID) {

                String javaCode = javaNativeEquivalent.get(Utils.nativeType(type));
                String javaMethod = javaNativeMethod.get(Utils.nativeType(type));

                stringBuilder.append("public " + javaCode + " getRootValue(){\n");
                stringBuilder.append("return " + "rootValue;\n");
                stringBuilder.append("}\n");

                stringBuilder.append("public void setRootValue( " + javaCode + " value ){\n");
                stringBuilder.append("rootValue = value;\n");
                stringBuilder.append("}\n");

            }

        }


    }

    private void parseSubType(TypeDefinition typeDefinition) {
        if (Utils.hasSubTypes(typeDefinition)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();

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
