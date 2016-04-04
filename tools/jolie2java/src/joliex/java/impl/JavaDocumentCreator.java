/***************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>  *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author balint maschio & michele morgagni
 */
public class JavaDocumentCreator {

    private Vector<TypeDefinition> subclass;
    private boolean subtypePresent = false;
    private String packageName;
    private String targetPort;
    private boolean addSource;
    private String directoryPath;
    private LinkedHashMap<String, TypeDefinition> typeMap;
    private LinkedHashMap<String, TypeDefinition> subTypeMap;
    ProgramInspector inspector;
    private static HashMap<NativeType, String> javaNativeEquivalent = new HashMap<NativeType, String>();
    private static HashMap<NativeType, String> javaNativeMethod = new HashMap<NativeType, String>();
    private static HashMap<NativeType, String> javaNativeChecker = new HashMap<NativeType, String>();

    public JavaDocumentCreator(ProgramInspector inspector, String packageName, String targetPort, boolean addSource) {

        this.inspector = inspector;
        this.packageName = packageName;
        this.targetPort = targetPort;
        this.addSource = addSource;


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

    public void ConvertDocument() {
        typeMap = new LinkedHashMap<>();
        subTypeMap = new LinkedHashMap<>();
        subclass = new Vector<>();
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
        createPackageDirectory();
        createBuildFile();
        while (typeMapIterator.hasNext()) {
            Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
            if (!(typeEntry.getKey().equals("undefined"))) {
                subclass = new Vector<TypeDefinition>();
                subtypePresent = false;
                counterSubClass = 0;
                String nameFile = directoryPath + Constants.fileSeparator + typeEntry.getKey() + ".java";
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

    private void createBuildFile() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("project");
            doc.appendChild(rootElement);
            rootElement.setAttribute("name", "JolieConnector");
            rootElement.setAttribute("default", "compile");
            rootElement.setAttribute("basedir", ".");
	    /*Section that defines constants*/
            Element propertyElement = doc.createElement("property");
            propertyElement.setAttribute("name", "src");
            propertyElement.setAttribute("location", "src");
            rootElement.appendChild(propertyElement);
            propertyElement = doc.createElement("property");
            propertyElement.setAttribute("name", "dist");
            propertyElement.setAttribute("location", "dist");
            rootElement.appendChild(propertyElement);
            propertyElement = doc.createElement("property");
            propertyElement.setAttribute("name", "build");
            propertyElement.setAttribute("location", "built");
            rootElement.appendChild(propertyElement);
            propertyElement = doc.createElement("property");
            propertyElement.setAttribute("name", "lib");
            propertyElement.setAttribute("location", "lib");
            rootElement.appendChild(propertyElement);
            propertyElement = doc.createElement("property");
            propertyElement.setAttribute("environment", "env");
            rootElement.appendChild(propertyElement);

	    /*
	     This portion of the code is responsible for the dist target creation
	     */
            Element initElement = doc.createElement("target");
            initElement.setAttribute("name", "init");
            rootElement.appendChild(initElement);
            Element mkDirElement = doc.createElement("mkdir");
            mkDirElement.setAttribute("dir", "${build}");
            initElement.appendChild(mkDirElement);
            mkDirElement = doc.createElement("mkdir");
            mkDirElement.setAttribute("dir", "${dist}");
            initElement.appendChild(mkDirElement);
            mkDirElement = doc.createElement("mkdir");
            mkDirElement.setAttribute("dir", "${lib}");
            initElement.appendChild(mkDirElement);
            mkDirElement = doc.createElement("mkdir");
            mkDirElement.setAttribute("dir", "${dist}/lib");
            initElement.appendChild(mkDirElement);
            Element copyLib = doc.createElement("copy");
            copyLib.setAttribute("file", "${env.JOLIE_HOME}/jolie.jar");
            copyLib.setAttribute("tofile", "${lib}/jolie.jar");
            initElement.appendChild(copyLib);
            copyLib = doc.createElement("copy");
            copyLib.setAttribute("file", "${env.JOLIE_HOME}/lib/libjolie.jar");
            copyLib.setAttribute("tofile", "${lib}/libjolie.jar");
            initElement.appendChild(copyLib);
            copyLib = doc.createElement("copy");
            copyLib.setAttribute("file", "${env.JOLIE_HOME}/lib/jolie-java.jar");
            copyLib.setAttribute("tofile", "${lib}/jolie-java.jar");
            initElement.appendChild(copyLib);
            copyLib = doc.createElement("copy");
            copyLib.setAttribute("file", "${env.JOLIE_HOME}/extensions/sodep.jar");
            copyLib.setAttribute("tofile", "${lib}/sodep.jar");
            initElement.appendChild(copyLib);
            Element compileElement = doc.createElement("target");
            rootElement.appendChild(compileElement);
            compileElement.setAttribute("name", "compile");
            compileElement.setAttribute("depends", "init");
            Element javacElement = doc.createElement("javac");
            compileElement.appendChild(javacElement);
            javacElement.setAttribute("srcdir", "${src}");
            javacElement.setAttribute("destdir", "${build}");
            Element classPathElement = doc.createElement("classpath");
            javacElement.appendChild(classPathElement);
            Element jolieJar = doc.createElement("pathelement");
            classPathElement.appendChild(jolieJar);
            jolieJar.setAttribute("path", "./lib/jolie.jar");
            Element libJolieJar = doc.createElement("pathelement");
            classPathElement.appendChild(libJolieJar);
            libJolieJar.setAttribute("path", "./lib/libjolie.jar");
            Element distElement = doc.createElement("target");
            rootElement.appendChild(distElement);
            distElement.setAttribute("name", "dist");
            distElement.setAttribute("depends", "compile");
            Element jarElement = doc.createElement("jar");
            distElement.appendChild(jarElement);
            jarElement.setAttribute("jarfile", "${dist}/JolieConnector.jar");
            jarElement.setAttribute("basedir", "${build}");
            if (addSource) {
                Element filesetElement = doc.createElement("fileset");
                filesetElement.setAttribute("dir", "${src}");
                filesetElement.setAttribute("includes", "**/*.java");
                jarElement.appendChild(filesetElement);
            }
            copyLib = doc.createElement("copy");
            copyLib.setAttribute("toDir", "${dist}/lib");
            Element filesetElement = doc.createElement("fileset");
            filesetElement.setAttribute("dir", "${lib}");
            copyLib.appendChild(filesetElement);
            distElement.appendChild(copyLib);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File("build.xml"));
            transformer.transform(source, streamResult);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void createPackageDirectory() {
        String[] directoriesComponents = packageName.split("\\.");
        File f = new File(".");

        try {
            directoryPath = f.getCanonicalPath() + Constants.fileSeparator + "src";
            for (int counterDirectories = 0; counterDirectories < directoriesComponents.length; counterDirectories++) {
                directoryPath += Constants.fileSeparator + directoriesComponents[counterDirectories];
            }
            f = new File(directoryPath);
            f.mkdirs();
        } catch (IOException ex) {
            Logger.getLogger(JavaDocumentCreator.class.getName()).log(Level.SEVERE, null, ex);
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
        builderHeaderclass.append("package " + packageName + ";\n");
        importsCreate(builderHeaderclass, typeDefinition);
        builderHeaderclass.append("public class " + typeDefinition.id() + " {" + "\n");
        if (Utils.hasSubTypes(typeDefinition)) {
            ConvertSubTypes(typeDefinition, builderHeaderclass);
        }


        builderHeaderclass.append("}\n");

        writer.append(builderHeaderclass.toString());
    }

    private void ConvertSubTypes(TypeDefinition typeDefinition, StringBuilder builderHeaderclass) {

        Set<Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
        Iterator i = supportSet.iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            //System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
            /*if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink){
             typeMap.put(((TypeDefinitionLink) me.getValue()).linkedTypeName(),((TypeDefinitionLink)me.getValue()).linkedType());



             }else*/ if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (Utils.hasSubTypes((TypeDefinition) me.getValue()))) {
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

        if (Utils.hasSubTypes(supportType)) {
            subtypePresent = true;
            stringBuilder.append("import java.util.List;\n");
            stringBuilder.append("import java.util.LinkedList;\n");
            stringBuilder.append("import jolie.runtime.Value;\n");
            stringBuilder.append("import jolie.runtime.ByteArray;\n");
            stringBuilder.append("\n");
        }
    }

    private void variableCreate(StringBuilder stringBuilder, TypeDefinition type) {

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                if (subType instanceof TypeDefinitionLink) {

                    //link
                    if (subType.cardinality().max() > 1) {
                        stringBuilder.append("private List<" + ((TypeDefinitionLink) subType).linkedType().id() + "> " + "_" + ((TypeDefinitionLink) subType).id() + ";\n");
                    } else {
                        stringBuilder.append("private " + ((TypeDefinitionLink) subType).linkedType().id() + " " + "_" + ((TypeDefinitionLink) subType).id() + ";\n");
                    }

                } else if (subType instanceof TypeInlineDefinition) {

                    if (Utils.hasSubTypes(subType)) {

                        /*if(Utils.nativeType(subType)==NativeType.VOID){
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
                        String javaCode = javaNativeEquivalent.get(((TypeInlineDefinition) subType).nativeType());
                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("private List<" + javaCode + "> " + "_" + subType.id() + ";\n");
                        } else {
                            stringBuilder.append("private " + javaCode + " _" + subType.id() + ";\n");
                        }
                    }


                } else if (subType instanceof TypeChoiceDefinition){
                    System.out.println("WARNING: Type definition contains a choice variable which is not supported!");
                    if (subType.cardinality().max() > 1) {
                        stringBuilder.append("private List<Object> _" + subType.id() + ";\n");
                    } else {
                        stringBuilder.append("private Object " + subType.id() + ";\n");
                    }

                } else {
                    System.out.println("WARNING: variable is not a Link, a Choice or an Inline Definition!");
                }
            }
        }

        if (Utils.nativeType(type) != NativeType.VOID) {
            stringBuilder.append("private " + javaNativeEquivalent.get(Utils.nativeType(type)) + " rootValue;\n");
        }

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
                    if (subType.cardinality().max() > 1) {
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

                        /*if(Utils.nativeType(subType)==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                        if (subType.cardinality().max() > 1) {
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

                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + javaCode + ">();" + "\n");

                            stringBuilder.append("if (v.hasChildren(\"").append(subType.id()).append("\")){\n");
                            stringBuilder.append("for(int counter" + subType.id() + "=0; " + "counter" + subType.id() + "<v.getChildren(\"" + subType.id() + "\").size(); counter" + subType.id() + "++){\n");
                            if (Utils.nativeType(subType) != NativeType.ANY) {
                                stringBuilder.append(javaCode + " support").append(subType.id()).append(" = v.getChildren(\"").append(subType.id()).append("\").get(counter").append(subType.id()).append(")." + javaMethod + ";\n");
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

                } else if (subType instanceof TypeChoiceDefinition) {
                    throw new UnsupportedOperationException("Can't initialize variable with several possible types");
                } else {
                    System.out.println("WARNING: variable is not a Link, a Choice or an Inline Definition!");
                }
            }
        }

        if (Utils.nativeType(type) != NativeType.VOID) {

            String javaCode = javaNativeEquivalent.get(Utils.nativeType(type));
            String javaMethod = javaNativeMethod.get(Utils.nativeType(type));

            if (Utils.nativeType(type) != NativeType.ANY) {
                stringBuilder.append("rootValue = v." + javaMethod + ";" + "\n");
            } else if (type instanceof TypeChoiceDefinition) {
                for (NativeType t : getTypes(type)) {
                    if (!javaNativeChecker.containsKey(t))
                        continue;
                    stringBuilder.append(
                            "if(v." + javaNativeChecker.get(t) + "){\n"
                                    + "rootValue = v." + javaNativeMethod.get(t) + ";\n"
                                    + "}\n");
                }
            } else{
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
                    if (subType.cardinality().max() > 1) {
                        stringBuilder.append("_" + subType.id() + "= new LinkedList<" + ((TypeDefinitionLink) subType).linkedType().id() + ">();" + "\n");
                        //stringBuilder.append("}\n");
                    }
                } else if (subType instanceof TypeInlineDefinition) {

                    if (Utils.hasSubTypes(subType)) {

                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + subType.id() + ">();" + "\n");
                        }
                        /*if(Utils.nativeType(subType)==NativeType.VOID){
                         //manage type with subtypes and a rootValue
                         }else{
                         //manage type with subtypes without rootValue
                         }*/

                    } else {
                        //native type
                        String javaCode = javaNativeEquivalent.get(Utils.nativeType(subType));
                        //String javaMethod = javaNativeMethod.get(Utils.nativeType(subType));

                        if (subType.cardinality().max() > 1) {
                            stringBuilder.append("_" + subType.id() + "= new LinkedList<" + javaCode + ">();" + "\n");
                        }
                    }

                } else if (subType instanceof TypeChoiceDefinition) {
                    throw new UnsupportedOperationException("Can't initialize variable with several possible types");
                } else {
                    System.out.println("WARNING: variable is not a Link, a Choice or an Inline Definition!");
                }
            }
        }

        stringBuilder.append("}\n");
    }

    private void methodsCreate(StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/) {

        if (Utils.hasSubTypes(type)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {

                TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

                String nameVariable = subType.id();
                String startingChar = nameVariable.substring(0, 1);
                String remaningStr = nameVariable.substring(1, nameVariable.length());
                String nameVariableOp = startingChar.toUpperCase() + remaningStr;
                if (nameVariableOp.equals("Value")) {
                    nameVariableOp = "__Value";
                }

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

                        /*if(Utils.nativeType(subType)==NativeType.VOID){
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

                        if (Utils.nativeType(subType) != NativeType.VOID) {

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

                                stringBuilder.append("public " + "void set" + nameVariableOp + "( " + javaCode + " value ){\n");
                                stringBuilder.append("_" + nameVariable + " = value;\n");
                                stringBuilder.append("}\n");
                            }
                        }
                    }
                } else if (subType instanceof TypeChoiceDefinition) {
                    //How to manage creating getters and setters for variable that can be initialized with several types?
                    //public <type1> get <variable>
                    //public <type2> get <variable>

                } else {
                    System.out.println("WARNING: variable is not a Link, a Choice or an Inline Definition!");
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


            //getValue

            stringBuilder.append("public " + "Value getValue(){\n");
            stringBuilder.append("Value vReturn = Value.create();\n");

            if (Utils.hasSubTypes(type)) {
                Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(type);
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

                        if (Utils.hasSubTypes(subType)) {

                        /*if(Utils.nativeType(subType)==NativeType.VOID){
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

                            String javaCode = javaNativeEquivalent.get(Utils.nativeType(type));
                            String javaMethod = javaNativeMethod.get(Utils.nativeType(type));

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

                    } else if (subType instanceof TypeChoiceDefinition) {
                        //How to set value for choice types if the operation differs for link and inline types
                        //vReturn.getNewChild("x").setValue(_x); for inline
                        //vReturn.getNewChild("x").deepCopy(_x.getValue()); for link

                    } else {
                        System.out.println("WARNING: variable is not a Link, a Choice or an Inline Definition!");
                    }
                }
            }

            if (Utils.nativeType(type) != NativeType.VOID) {

                stringBuilder.append("if((rootValue!=null)){\n");
                if (Utils.nativeType(type) != NativeType.ANY) {
                    stringBuilder.append("vReturn.setValue(rootValue);\n");
                } else {
                    for (NativeType t : getTypes(type)) {
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

    private void parseSubType(TypeDefinition typeDefinition) {
        if (Utils.hasSubTypes(typeDefinition)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
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

    private Set<NativeType> getTypes(TypeDefinition typeDefinition){
        Set<NativeType> choiceTypes = new HashSet<>();
        if (typeDefinition instanceof TypeChoiceDefinition){
            choiceTypes = getTypes(((TypeChoiceDefinition) typeDefinition).left());
            Set<NativeType> right = getTypes(((TypeChoiceDefinition) typeDefinition).right());
            if (right!=null){
                choiceTypes.addAll(right);
            }
        } else if (typeDefinition instanceof TypeDefinitionLink){
            return getTypes(((TypeDefinitionLink) typeDefinition).linkedType());
        } else if (typeDefinition instanceof TypeInlineDefinition){
            choiceTypes.add(((TypeInlineDefinition) typeDefinition).nativeType());
        }
        return choiceTypes;
    }
}
