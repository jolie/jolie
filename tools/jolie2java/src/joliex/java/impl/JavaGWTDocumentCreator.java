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

import java.util.Iterator;
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
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import javax.lang.model.SourceVersion;

/**
 *
 * @author balint
 */
public class JavaGWTDocumentCreator {

    private Vector<TypeDefinition> subclass;
    private boolean subtypePresent = false;
    private String namespace;
    ProgramInspector inspector;
	private int counterInLineDefinition = 0;

    public JavaGWTDocumentCreator(ProgramInspector inspector, String namespace) {

        this.inspector = inspector;
        this.namespace = namespace;
    }

    public void ConvertDocument() {



        subclass = new Vector<TypeDefinition>();
        int counterSubClass;
        TypeDefinition[] support = inspector.getTypes();
        for (TypeDefinition typeDefinition : inspector.getTypes()) {
            if (!(typeDefinition.id().equals("undefined"))) {
                subclass = new Vector<TypeDefinition>();
                subtypePresent = false;
                String nameFile = typeDefinition.id() + ".java";
                Writer writer;
                try {
                    writer = new BufferedWriter(new FileWriter(nameFile));
                    System.out.print(nameFile + "\n");
                    ConvertTypes(typeDefinition, writer);
                    counterSubClass = 0;

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
        ImportCreate(builderHeaderclass, typeDefinition);
        if (SourceVersion.isKeyword(typeDefinition.id())){
            builderHeaderclass.append("public class $" + typeDefinition.id() + " {" + "\n");
        }else{
             builderHeaderclass.append("public class " + typeDefinition.id() + " {" + "\n");

        }
        if (typeDefinition.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> supportSet = typeDefinition.subTypes();
            Iterator i = supportSet.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                System.out.print(((TypeDefinition) me.getValue()).id() + "\n");

                if ((((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) me.getValue()).hasSubTypes())) {
                    counterInLineDefinition++;

                    builderHeaderclass.append("public class ").append("$").append(counterInLineDefinition).append("$").append(((TypeDefinition) me.getValue()).id() + " {" + "\n");

                    VariableCreate(builderHeaderclass, ((TypeDefinition) me.getValue()));
                    ConstructorCreate(builderHeaderclass, ((TypeDefinition) me.getValue()), false, new Integer(counterInLineDefinition));
                    MethodsCreate(builderHeaderclass, ((TypeDefinition) me.getValue()), false);
                    builderHeaderclass.append("}\n");
                }

            }





            VariableCreate(builderHeaderclass, typeDefinition);
            ConstructorCreate(builderHeaderclass, typeDefinition, true, null);
            MethodsCreate(builderHeaderclass, typeDefinition, true);
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

    private void ImportCreate(StringBuilder stringBuilder, TypeDefinition type) {
		String nameFile = type.context().sourceName();
        TypeDefinition supportType = type;
        //System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
        List<String> a = new LinkedList<String>();
        boolean addListImport = false;



        if (supportType.hasSubTypes()) {
            subtypePresent = true;
            stringBuilder.append("\n");
        }


    }

    private void VariableCreate(StringBuilder stringBuilder, TypeDefinition type) {
        String nameFile = type.context().sourceName();
        TypeDefinition supportType = type;
        int counterInlineDefinition = 0;
        String a;


        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> typedefinitionSet = supportType.subTypes();
            Iterator interatorTypeDefinition = typedefinitionSet.iterator();
            while (interatorTypeDefinition.hasNext()) {
                Map.Entry typedefitionMapEntry = (Map.Entry) interatorTypeDefinition.next();


                if (((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeDefinitionLink) {
                    if (((TypeDefinitionLink) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            System.out.println("WARNING java reserved KEYWORD " + ((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + " in " + supportType.id() + "in" + supportType.context().sourceName());
                            stringBuilder.append("private List< ").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append("> ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).id()).append(";\n");
                        } else {
                            stringBuilder.append("private List< ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append("> ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).id()).append(";\n");
                        }

                    } else {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            System.out.println("WARNING java reserved KEYWORD " + ((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + " in " + supportType.id() + "in" + supportType.context().sourceName());
                            stringBuilder.append("private ").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(" " + " ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).id()).append(";\n");
                        } else {
                            stringBuilder.append("private ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(" " + "_").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).id()).append(";\n");
                        }
                    }
                } else if ((((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) typedefitionMapEntry.getValue()).hasSubTypes())) {




                    this.subclass.add(((TypeInlineDefinition) typedefitionMapEntry.getValue()));
                    if (((TypeInlineDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword(((TypeDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterInlineDefinition++;
                            System.out.println("WARNING java reserved KEYWORD " + ((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + " in " + supportType.id() + "in" + supportType.context().sourceName());
                            stringBuilder.append("private List< ").append("$").append(counterInlineDefinition).append("$").append(((TypeDefinition) typedefitionMapEntry.getValue()).id()).append(">  _").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).id()).append(";\n");
                        } else {
                            stringBuilder.append("private List< ").append(((TypeDefinition) typedefitionMapEntry.getValue()).id()).append("> " + "_").append(((TypeDefinition) typedefitionMapEntry.getValue()).id()).append(";\n");
                        }

                    } else {
                        if (SourceVersion.isKeyword(((TypeDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterInlineDefinition++;
                            System.out.println("WARNING java reserved KEYWORD " + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + " in " + supportType.id() + "in" + supportType.context().sourceName());
                            a = ((TypeDefinition) typedefitionMapEntry.getValue()).id();
                            stringBuilder.append("private ").append("$").append(counterInlineDefinition).append("$").append(a).append(" _").append(a).append(";\n");
                        } else {
                            stringBuilder.append("private ").append(((TypeDefinition) typedefitionMapEntry.getValue()).id()).append(" " + "_").append(((TypeDefinition) typedefitionMapEntry.getValue()).id()).append(";\n");
                        }
                    }
                    subtypePresent = true;

                } else {

                    if (((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {

                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("private java.util.List<java.lang.Integer> " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");
                        } else if (typeName.equals("double")) {

                            stringBuilder.append("private java.util.List<java.lang.Double> " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");


                        } else if (typeName.equals("string")) {
                            stringBuilder.append("private java.util.List<java.lang.String> " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");

                        }


                    } else {
                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("private java.lang.Integer " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");
                        } else if (typeName.equals("double")) {

                            stringBuilder.append("private java.lang.Double " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");


                        } else if (typeName.equals("string")) {
                            //stringBuilder.append( "private List<String> " + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n" );
                            stringBuilder.append("private java.lang.String " + " " + "_" + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n");

                        }
                        //stringBuilder.append( "private " + ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id() + " " + ((TypeDefinition) typedefitionMapEntry.getValue()).id() + ";\n" );


                    }
                }

            }
            stringBuilder.append("private joliex.gwt.client.Value v ;\n");
            stringBuilder.append("private joliex.gwt.client.Value vReturn= new Value() ;\n");
            stringBuilder.append("\n");

        }
        System.out.println("At the end o Variable creation");
        System.out.println(stringBuilder.toString());
    }

    private void ConstructorCreate(StringBuilder stringBuilder, TypeDefinition type, boolean naturalType, Integer counter) {



        TypeDefinition supportType = type;
        if (counter == null) {
            if (SourceVersion.isKeyword(supportType.id())) {
                stringBuilder.append("public ").append("$").append(supportType.id()).append("(joliex.gwt.client.Value v){\n");
            } else {
                stringBuilder.append("public ").append(supportType.id()).append("(joliex.gwt.client.Value v){\n");
            }
        } else {

            if (SourceVersion.isKeyword(supportType.id())) {
                stringBuilder.append("public ").append("$").append(counter).append("$").append(supportType.id()).append("(joliex.gwt.client.Value v){\n");
            } else {
                stringBuilder.append("public ").append(supportType.id()).append("(joliex.gwt.client.Value v){\n");
            }


        }
        stringBuilder.append("\n");
        stringBuilder.append("this.v=v;\n");
        String nameVariable;
        int counterKeyWords = 0;


        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> typedefinitionSet = supportType.subTypes();
            Iterator interatorTypeDefinition = typedefinitionSet.iterator();

            while (interatorTypeDefinition.hasNext()) {
                Map.Entry typedefitionMapEntry = (Map.Entry) interatorTypeDefinition.next();

                if (((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) typedefitionMapEntry.getValue()).id();
                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeDefinitionLink) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            stringBuilder.append(nameVariable).append("= new LinkedList<").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(">();" + "\n");
                        } else {
                            stringBuilder.append(nameVariable).append("= new LinkedList<").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(">();" + "\n");
                        }
                        stringBuilder.append("}\n");
                    } else {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            stringBuilder.append(nameVariable).append("=new ").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append("( v.getFirstChild(\"").append(nameVariable).append("\"));" + "\n");
                        } else {
                            stringBuilder.append(nameVariable).append("=new ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append("( v.getFirstChild(\"").append(nameVariable).append("\"));" + "\n");
                        }
                        stringBuilder.append("}\n");

                    }


                } else if ((((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) typedefitionMapEntry.getValue()).hasSubTypes()) {
                    nameVariable = ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id();
                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeInlineDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword(((TypeDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterKeyWords++;

                            stringBuilder.append("_").append(nameVariable).append("= new LinkedList<").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(">();" + "\n");
                        } else {
                            stringBuilder.append("_").append(nameVariable).append("= new LinkedList<").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(">();" + "\n");

                        }
                        stringBuilder.append("}\n");
                    } else {
                        if (SourceVersion.isKeyword(((TypeDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterKeyWords++;
                            stringBuilder.append("_").append(nameVariable).append("=new ").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append("( v.getFirstChild(\"").append(nameVariable).append("\"));" + "\n");
                        } else {
                            stringBuilder.append("_").append(nameVariable).append("=new ").append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append("( v.getFirstChild(\"").append(nameVariable).append("\"));" + "\n");
                        }
                        stringBuilder.append("}\n");
                    }



                } else {

                    nameVariable = ((TypeDefinition) typedefitionMapEntry.getValue()).id();

                    stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                    if (((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {

                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<java.lang.Integer>();" + "\n");

                        } else if (typeName.equals("double")) {
                            stringBuilder.append("_" + nameVariable + "= new LinkedList<java.lang.Double>();" + "\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("_" + nameVariable + "= new LinkedList<java.lang.String>();" + "\n");



                        }
                        stringBuilder.append("}\n");
                    } else {

                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        //stringBuilder.append( "if (v.hasChildren(\"").append( nameVariable).append( "\")){\n");
                        if (typeName.equals("int")) {

                            stringBuilder.append("_" + nameVariable + "=new java.lang.Integer(v.getFirstChild(\"" + nameVariable + "\").intValue());" + "\n");
                            stringBuilder.append("}\n");
                        } else if (typeName.equals("double")) {

                            stringBuilder.append("_" + nameVariable + "=new java.lang.Double(v.getFirstChild(\"" + nameVariable + "\").doubleValue());" + "\n");
                            stringBuilder.append("}\n");

                        } else if (typeName.equals("string")) {
                            stringBuilder.append("_" + nameVariable + "=v.getFirstChild(\"" + nameVariable + "\").strValue();" + "\n");
                            stringBuilder.append("}\n");


                        }

                    }
                }

            }
            interatorTypeDefinition = typedefinitionSet.iterator();

            while (interatorTypeDefinition.hasNext()) {
                Map.Entry typedefitionMapEntry = (Map.Entry) interatorTypeDefinition.next();
                if (((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) typedefitionMapEntry.getValue()).id();

                    if (((TypeDefinitionLink) typedefitionMapEntry.getValue()).cardinality().max() > 1) {


                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\t" + ((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + " support").append(nameVariable).append("=new " + ((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + "(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append("));\n");
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                        stringBuilder.append("\t}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    }

                } else if ((((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) typedefitionMapEntry.getValue()).hasSubTypes()) {
                    if (((TypeInlineDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {

                        nameVariable = ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id();

                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\t" + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + " support").append(nameVariable).append("=new " + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + "(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append("));\n");
                        stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                        stringBuilder.append("\t}\n");
                        //stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id() + ">();"+ "\n" );
                        stringBuilder.append("}\n");
                    }

                } else {
                    nameVariable = ((TypeDefinition) typedefitionMapEntry.getValue()).id();

                    if (((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {

                        stringBuilder.append("if (v.hasChildren(\"").append(nameVariable).append("\")){\n");
                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            //Value dsadda;
                            //dsadda.getChildren( typeName ).
                            stringBuilder.append("\n");
                            stringBuilder.append("\t" + "for(int counter").append(nameVariable).append("=0;" + "counter").append(nameVariable).append("<v.getChildren(\"").append(nameVariable).append("\").size();counter").append(nameVariable).append("++){\n");
                            stringBuilder.append("\t\t" + "java.lang.Integer support").append(nameVariable).append("=new java.lang.Integer(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").intValue());\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                            stringBuilder.append("\t" + "}\n");
                        } else if (typeName.equals("double")) {
                            stringBuilder.append(nameVariable + "= new LinkedList<Double>();" + "\n");
                            stringBuilder.append("\t" + "for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                            stringBuilder.append("\t\t" + "java.lang.Double support").append(nameVariable).append("=new java.lang.Double(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").doubleValue());\n");
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".add(support" + nameVariable + ");\n");
                            stringBuilder.append("\t}\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n");
                            stringBuilder.append("\t\t" + "java.lang.String support").append(nameVariable).append("=new java.lang.String(v.getChildren(\"").append(nameVariable).append("\").get(counter").append(nameVariable).append(").strValue());\n");
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
        counterKeyWords = 0;
        if (counter == null) {
            if (SourceVersion.isKeyword(supportType.id())) {
                stringBuilder.append("public ").append("$").append(supportType.id()).append("(){\n");
            } else {
                stringBuilder.append("public ").append(supportType.id()).append("(){\n");
            }
        } else {

            if (SourceVersion.isKeyword(supportType.id())) {
                stringBuilder.append("public ").append("$").append(counter).append("$").append(supportType.id()).append("(){\n");
            } else {
                stringBuilder.append("public ").append(supportType.id()).append(supportType.id()).append("(){\n");
            }
        }

        stringBuilder.append("\n");



        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> typedefinitionSet = supportType.subTypes();
            Iterator interatorTypeDefinition = typedefinitionSet.iterator();

            while (interatorTypeDefinition.hasNext()) {
                Map.Entry typedefitionMapEntry = (Map.Entry) interatorTypeDefinition.next();

                if (((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) typedefitionMapEntry.getValue()).id();
                    if (((TypeDefinitionLink) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            stringBuilder.append(nameVariable).append("= new LinkedList<").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(">();" + "\n");
                        } else {
                            stringBuilder.append(nameVariable).append("= new LinkedList<").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()).append(">();" + "\n");
                        }



                    }


                } else {

                    nameVariable = ((TypeDefinition) typedefitionMapEntry.getValue()).id();
                    if (((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {

                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        if (typeName.equals("int")) {
                            stringBuilder.append(nameVariable + "= new LinkedList<Integer>();" + "\n");

                        } else if (typeName.equals("double")) {
                            stringBuilder.append(nameVariable + "= new LinkedList<Double>();" + "\n");



                        } else if (typeName.equals("string")) {

                            stringBuilder.append(nameVariable + "= new LinkedList<String>();" + "\n");



                        }
                    }
                }

            }
        }
        stringBuilder.append("}\n");
        System.out.println("At the end of Constructor creation");
        System.out.println(stringBuilder.toString());
    }

    private void MethodsCreate(StringBuilder stringBuilder, TypeDefinition type, boolean naturalType) {
        TypeDefinition supportType = type;
        String nameVariable, nameVariableOp;
        int counterKeyWords = 0;
        if (supportType.hasSubTypes()) {


            Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
            Iterator interatorTypeDefinition = supportSet.iterator();

            while (interatorTypeDefinition.hasNext()) {
                Map.Entry typedefitionMapEntry = (Map.Entry) interatorTypeDefinition.next();

                if (((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeDefinitionLink) {
                    nameVariable = ((TypeDefinitionLink) typedefitionMapEntry.getValue()).id();
                    String startingChar = nameVariable.substring(0, 1);
                    String remaningStr = nameVariable.substring(1, nameVariable.length());
                    nameVariableOp = startingChar.toUpperCase() + remaningStr;

                    if (((TypeDefinitionLink) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedType().id()))) {

                            stringBuilder.append("public ").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" get").append(nameVariableOp).append("Value(int index){\n");
                            stringBuilder.append("\n\treturn " + "").append(nameVariable).append(".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get").append(nameVariableOp).append("Size(){\n");
                            stringBuilder.append("\n\treturn ").append(nameVariable).append(".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add").append(nameVariableOp).append("Value(").append("$").append(counterKeyWords).append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" value ){\n");
                            stringBuilder.append("\n\t\t" + "").append(nameVariable).append(".add(value);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove").append(nameVariableOp).append("Value( int index ){\n");
                            stringBuilder.append("\t\t" + "").append(nameVariable).append(".remove(index);\n");
                            stringBuilder.append("}\n");

                        } else {
                            stringBuilder.append("public ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" get").append(nameVariableOp).append("Value(int index){\n");
                            StringBuilder append = stringBuilder.append("\n\treturn " + "" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get").append(nameVariableOp).append("Size(){\n");
                            stringBuilder.append("\n\treturn " + "").append(nameVariable).append(".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add").append(nameVariableOp).append("Value(").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\n\t\t" + "").append(nameVariable).append(".add(value);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove").append(nameVariableOp).append("Value( int index ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "" + nameVariable + ".remove(index);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");
                        }

                    } else {
                        if (SourceVersion.isKeyword((((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()))) {

                            stringBuilder.append("public ").append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" get").append(nameVariableOp).append("(){\n");
                            stringBuilder.append("\n\treturn " + "").append(nameVariable).append(";\n");
                            stringBuilder.append("}\n");
                            stringBuilder.append("public " + "void set").append(nameVariableOp).append("(").append("$").append(counterKeyWords).append("$").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" value ){\n");
                            stringBuilder.append("\n\t" + "").append(nameVariable).append("=value;\n");
                            stringBuilder.append("}\n");
                        } else {

                            stringBuilder.append("public ").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" get").append(nameVariableOp).append("(){\n");
                            stringBuilder.append("\n\treturn " + "_").append(nameVariable).append(";\n");
                            stringBuilder.append("}\n");
                            stringBuilder.append("public " + "void set").append(nameVariableOp).append("(").append(((TypeDefinitionLink) typedefitionMapEntry.getValue()).linkedTypeName()).append(" value ){\n");
                            stringBuilder.append("\n\t" + "_").append(nameVariable).append("=value;\n");
                            stringBuilder.append("}\n");
                        }
                    }


                } else if ((((TypeDefinition) typedefitionMapEntry.getValue()) instanceof TypeInlineDefinition) && (naturalType) && ((TypeInlineDefinition) typedefitionMapEntry.getValue()).hasSubTypes()) {

                    nameVariable = ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id();
                    String startingChar = nameVariable.substring(0, 1);
                    String remaningStr = nameVariable.substring(1, nameVariable.length());
                    nameVariableOp = startingChar.toUpperCase() + remaningStr;
                    if (((TypeInlineDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        if (SourceVersion.isKeyword(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterKeyWords++;
                            stringBuilder.append("public ").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(" get").append(nameVariableOp).append("Value(int index){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add").append(nameVariableOp).append("Value( ").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(" value ){\n");
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

                            stringBuilder.append("public " + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + " get" + nameVariableOp + "Value(int index){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".get(index);\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");


                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value(" + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + " value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\n\t\t" + "_" + nameVariable + ".add(value);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void remove" + nameVariableOp + "Value( int index ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "_" + nameVariable + ".remove(index);\n");
                            //stringBuilder.append( "\t}\n" );
                            stringBuilder.append("}\n");


                        }

                    } else {
                        if (SourceVersion.isKeyword(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id())) {
                            counterKeyWords++;
                            stringBuilder.append("public ").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(" get").append(nameVariableOp).append("(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set").append(nameVariableOp).append("(").append("$").append(counterKeyWords).append("$").append(((TypeInlineDefinition) typedefitionMapEntry.getValue()).id()).append(" value ){\n");
                            stringBuilder.append("\n\t" + "_" + nameVariable + "=value;\n");
                            stringBuilder.append("}\n");
                        } else {

                            stringBuilder.append("public " + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "(" + ((TypeInlineDefinition) typedefitionMapEntry.getValue()).id() + " value ){\n");
                            stringBuilder.append("\n\t" + "_" + nameVariable + "=value;\n");
                            stringBuilder.append("}\n");


                        }


                    }


                } else {

                    nameVariable = ((TypeDefinition) typedefitionMapEntry.getValue()).id();
                    if (((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max() > 1) {
                        Integer maxIndex = new Integer(((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().max());
                        Integer minIndex = new Integer(((TypeDefinition) typedefitionMapEntry.getValue()).cardinality().min());
                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        String startingChar = nameVariable.substring(0, 1);
                        String remaningStr = nameVariable.substring(1, nameVariable.length());
                        nameVariableOp = startingChar.toUpperCase() + remaningStr;
                        if (typeName.equals("int")) {



                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "Size(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".size();\n");
                            stringBuilder.append("}\n");



                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value(int value ){\n");
                            //stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
                            stringBuilder.append("\t\t" + "java.lang.Integer support").append(nameVariable).append("=new java.lang.Integer(value);\n");
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
                            stringBuilder.append("\t\t" + "java.lang.Double support").append(nameVariable).append("=new java.lang.Double(value);\n");
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

                            stringBuilder.append("public " + "void add" + nameVariableOp + "Value( java.lang.String value ){\n");
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


                        String typeName = ((TypeDefinition) typedefitionMapEntry.getValue()).nativeType().id();
                        String startingChar = nameVariable.substring(0, 1);
                        String remaningStr = nameVariable.substring(1, nameVariable.length());
                        nameVariableOp = startingChar.toUpperCase() + remaningStr;
                        if (typeName.equals("int")) {
                            stringBuilder.append("public " + "int" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".intValue();\n");
                            stringBuilder.append("}\n");
                            Integer a = new Integer(2);


                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value(int value ){\n");
                            stringBuilder.append("\n\t" + "_" + nameVariable + "=new java.lang.Integer(value);\n");
                            stringBuilder.append("}\n");

                        } else if (typeName.equals("double")) {
                            //stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
                            stringBuilder.append("public " + "double" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ".doubleValue();\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value( double value ){\n");
                            stringBuilder.append("\n\t\t" + "_" + nameVariable + "=new java.lang.Double(value);\n");
                            stringBuilder.append("}\n");


                        } else if (typeName.equals("string")) {

                            stringBuilder.append("public " + "String" + " get" + nameVariableOp + "(){\n");
                            stringBuilder.append("\n\treturn " + "_" + nameVariable + ";\n");
                            stringBuilder.append("}\n");

                            stringBuilder.append("public " + "void set" + nameVariableOp + "Value( java.lang.String value ){\n");
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


                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\").setValue(" + "_" + nameVariable + ".get(counter" + nameVariable + "));\n");
                        stringBuilder.append("\t}");
                        //stringBuilder.append( "\n}\n" );
                    } else if (typeName.equals("string")) {
//						
                        stringBuilder.append("\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + "_" + nameVariable + ".size();counter" + nameVariable + "++){\n");
                        stringBuilder.append("\t\tvReturn.getNewChild(\"" + nameVariable + "\")" + ".setValue(" + "_" + nameVariable + ".get(counter" + nameVariable + "));\n");
                        stringBuilder.append("\t}");


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
}
