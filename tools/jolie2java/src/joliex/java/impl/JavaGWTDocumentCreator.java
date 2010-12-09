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
import jolie.runtime.typing.Type.TypeLink;
import joliex.gwt.client.Value;
import joliex.java.support.GeneralDocumentCreator;
import joliex.java.support.GeneralProgramVisitor;
import joliex.java.support.treeOLObject;





/**
 *
 * @author balint
 */
public class JavaGWTDocumentCreator extends GeneralDocumentCreator
{
	private Vector<TypeDefinition> subclass;
	private boolean subtypePresent = false;

	public JavaGWTDocumentCreator( GeneralProgramVisitor visitor )
	{

		super( visitor );

	}

	@Override
	public void ConvertDocument()
	{
		List<treeOLObject> olTree = GetOlTree();
		for( treeOLObject treeObject : olTree ) {
			//for (int countrerInterftreeObject.GetLinkedObjetSize();
		}
		subclass = new Vector<TypeDefinition>();
		List<Map<String, TypeDefinition>> typesList = this.GetTypesSet();
		int counterSubClass;
		for( Map<String, TypeDefinition> typeDefinitionMap : typesList ) {
			subclass = new Vector<TypeDefinition>();
			subtypePresent=false;
			String nameFile = typeDefinitionMap.entrySet().iterator().next().getValue().id() + ".java";
			Writer writer;
			try {
				writer = new BufferedWriter( new FileWriter( nameFile ) );
				ConvertTypes( typeDefinitionMap.entrySet().iterator().next().getValue(), writer );
				counterSubClass = 0;
				while( counterSubClass < subclass.size() ) {
					System.out.print( "sono dentro al sub "+subclass.size()+"\n");
					ConvertTypes( subclass.get( counterSubClass ), writer );
					counterSubClass++;
				}
				if ( subtypePresent ) {

					closeClass( writer );

				}

				writer.flush();
				writer.close();
			} catch( IOException ex ) {
				Logger.getLogger( JavaGWTDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
			}




		}

	}

	@Override
	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void ConvertTypes( TypeDefinition typesDefinition, Writer writer )
		throws IOException
	{
		StringBuilder builderHeaderclass = new StringBuilder();
		if ( subtypePresent == false ) {
			//System.out.print( "sto creado il header " + subtypePresent + "\n" );
			ImportCreate( builderHeaderclass, typesDefinition );
		}
		builderHeaderclass.append( "public class" + typesDefinition.id() + " {" + "\n" );
		VariableCreate( builderHeaderclass, typesDefinition );
		ConstructorCreate( builderHeaderclass, typesDefinition );
		MethodsCreate( builderHeaderclass, typesDefinition );
		if ( subtypePresent == false ) {
			builderHeaderclass.append( " }\n" );
		}
		writer.append( builderHeaderclass.toString() );


	}

	private void closeClass( Writer writer )
	{
		StringBuilder builderHeaderclass = new StringBuilder();
		builderHeaderclass.append( " }\n" );
		try {
			writer.append( builderHeaderclass.toString() );
		} catch( IOException ex ) {
			Logger.getLogger( JavaGWTDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	private void ImportCreate( StringBuilder stringBuilder, TypeDefinition type )
	{





		String nameFile = type.context().sourceName();
		TypeDefinition supportType = type;
		//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
		List<String> a = new LinkedList<String>();
		boolean addListImport = false;



		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {
						addListImport = true;
					}
					stringBuilder.append( "import types." + ((TypeDefinitionLink) me.getValue()).linkedType().id() + "\n" );

				} else {

					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						addListImport = true;

					}
				}


			}
			Integer po;
			if ( addListImport ) {
				stringBuilder.append( "import java.util.List;\n" );
				stringBuilder.append( "import java.util.LinkedList;\n" );
			}


			stringBuilder.append( "import joliex.gwt.client.Value;\n" );
			stringBuilder.append( "\n" );
		}


	}

	private void VariableCreate( StringBuilder stringBuilder, TypeDefinition type )
	{





		String nameFile = type.context().sourceName();
		TypeDefinition supportType = type;
		//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
          System.out.print( " the type under Observation is : "+type.id()+"\n" );

		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();
                System.out.print( ((TypeDefinition) me.getValue()).id()+"\n");

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {
						stringBuilder.append( "private List< " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + "> " + ((TypeDefinitionLink) me.getValue()).id() + "\n" );


					} else {
						stringBuilder.append( "private " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + " " + ((TypeDefinitionLink) me.getValue()).id() + "\n" );
					}
				} else if ( (((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (((TypeDefinition) me.getValue()).hasSubTypes()) ) {

					System.out.print( "tipo in line "+((TypeDefinition) me.getValue()).id()+"\n");
					StringBuilder supBuffer = new StringBuilder();
					System.out.print( "ho inserito un type in line " + ((TypeDefinition) me.getValue()).id() + "\n" );
					this.subclass.add( ((TypeInlineDefinition) me.getValue()) );
					if ( ((TypeInlineDefinition) me.getValue()).cardinality().max() > 1 ) {
						stringBuilder.append( "private List< " + ((TypeDefinition) me.getValue()).id() + "> " + ((TypeDefinition) me.getValue()).id() + "\n" );


					} else {
						stringBuilder.append( "private " + ((TypeDefinition) me.getValue()).id() + " " + ((TypeDefinition) me.getValue()).id() + "\n" );
					}
					subtypePresent = true;

				} else {
					//	System.out.print( ((TypeDefinition) me.getValue()).cardinality().max() + "\n" );
//					((TypeInlineDefinition)((TypeDefinition) me.getValue())).
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( "private List<Integer> " + ((TypeDefinition) me.getValue()).id() + "\n" );
						} else if ( typeName.equals( "double" ) ) {

							stringBuilder.append( "private List<Double> " + ((TypeDefinition) me.getValue()).id() + "\n" );


						} else if ( typeName.equals( "string" ) ) {
							stringBuilder.append( "private List<Double> " + ((TypeDefinition) me.getValue()).id() + "\n" );

						}


					} else {


						stringBuilder.append( "private " + ((TypeDefinition) me.getValue()).nativeType().id() + " " + ((TypeDefinition) me.getValue()).id() + "\n" );


					}
				}

			}
			stringBuilder.append( "private Value v \n" );
			stringBuilder.append( "\n" );
		}


	}

	private void ConstructorCreate( StringBuilder stringBuilder, TypeDefinition type )
	{



		TypeDefinition supportType = type;
		//	System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
		stringBuilder.append( "public " + supportType.id() + "(Value v){\n" );
		stringBuilder.append( "\n" );
		stringBuilder.append( "this.v=v;\n" );
		String nameVariable;

		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();

			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					nameVariable = ((TypeDefinitionLink) me.getValue()).id();
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {

						stringBuilder.append( nameVariable + "= new LinkedList<" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();" + "\n" );


					} else {
						stringBuilder.append( nameVariable + "=new " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "( v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );

					}


				} else {

					nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( nameVariable + "= new LinkedList<Integer>();" + "\n" );

						} else if ( typeName.equals( "double" ) ) {
							stringBuilder.append( nameVariable + "= new LinkedList<Double>();" + "\n" );



						} else if ( typeName.equals( "string" ) ) {

							stringBuilder.append( nameVariable + "= new LinkedList<String>();" + "\n" );



						}
					} else {


						//stringBuilder.append( "private " + ((TypeDefinition) me.getValue()).nativeType().id() + " " + ((TypeDefinition) me.getValue()).id() + "\n" );
						stringBuilder.append( nameVariable + "=v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );


					}
				}

			}
			i = supportSet.iterator();

			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();
				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					nameVariable = ((TypeDefinitionLink) me.getValue()).id();
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {



						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\t" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + " support" ).append( nameVariable ).append( "=new Double(v.getChildren(\"" ).append( nameVariable ).append( "\").get(counter" ).append( nameVariable ).append( ");\n" );
						stringBuilder.append( "\t\t" + nameVariable + ".add(support" + nameVariable + ");\n" );
						stringBuilder.append( "\t}\n" );
						//stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );


					}
				} else {
					nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {


						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							//Value dsadda;
							//dsadda.getChildren( typeName ).
							stringBuilder.append( "\n" );
							stringBuilder.append( "\t" + "for(int counter" ).append( nameVariable ).append( "=0;" + "counter" ).append( nameVariable ).append( "<v.getChildren(\"" ).append( nameVariable ).append( "\").size();counter" ).append( nameVariable ).append( "++){\n" );
							stringBuilder.append( "\t\t" + "Integer support" ).append( nameVariable ).append( "=new Integer(v.getChildren(\"" ).append( nameVariable ).append( "\").get(counter" ).append( nameVariable ).append( ").intValue);\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(support" + nameVariable + ");\n" );
							stringBuilder.append( "\t" + "}\n" );
						} else if ( typeName.equals( "double" ) ) {
							stringBuilder.append( nameVariable + "= new LinkedList<Double>();" + "\n" );
							stringBuilder.append( "\t" + "for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
							stringBuilder.append( "\t\t" + "Double support" ).append( nameVariable ).append( "=new Double(v.getChildren(\"" ).append( nameVariable ).append( "\").get(counter" ).append( nameVariable ).append( ").doubleValue);\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(support" + nameVariable + ");\n" );
							stringBuilder.append( "\t}\n" );


						} else if ( typeName.equals( "string" ) ) {

							stringBuilder.append( "for(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
							stringBuilder.append( "\t\t" + "String support" ).append( nameVariable ).append( "=new String(v.getChildren(\"" ).append( nameVariable ).append( "\").get(counter" ).append( nameVariable ).append( ").strValue);\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(support" + nameVariable + ");\n" );
							stringBuilder.append( "}\n" );


						}
					}



				}

			}

			stringBuilder.append( "}\n" );
		}

///// constructor with out value
		stringBuilder.append( "public " + supportType.id() + "(){\n" );
		stringBuilder.append( "\n" );



		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();

			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					nameVariable = ((TypeDefinitionLink) me.getValue()).id();
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {

						stringBuilder.append( nameVariable + "= new LinkedList<" + ((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();" + "\n" );


					} else {
						//stringBuilder.append( nameVariable + "=new " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "( v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );
					}


				} else {

					nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( nameVariable + "= new LinkedList<Integer>();" + "\n" );

						} else if ( typeName.equals( "double" ) ) {
							stringBuilder.append( nameVariable + "= new LinkedList<Double>();" + "\n" );



						} else if ( typeName.equals( "string" ) ) {

							stringBuilder.append( nameVariable + "= new LinkedList<String>();" + "\n" );



						}
					} else {
						//stringBuilder.append( "private " + ((TypeDefinition) me.getValue()).nativeType().id() + " " + ((TypeDefinition) me.getValue()).id() + "\n" );
						//stringBuilder.append( nameVariable + "=v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );
					}
				}

			}
		}
		stringBuilder.append( "}\n" );
	}

	private void MethodsCreate( StringBuilder stringBuilder, TypeDefinition type )
	{
		TypeDefinition supportType = type;
		String nameVariable, nameVariableOp;
		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();

			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					nameVariable = ((TypeDefinitionLink) me.getValue()).id();
					String startingChar = nameVariable.substring( 0, 1 );
					String remaningStr = nameVariable.substring( 1, nameVariable.length() );
					nameVariableOp = startingChar.toUpperCase() + remaningStr;
					Integer maxIndex = new Integer( ((TypeDefinitionLink) me.getValue()).cardinality().max() );
					Integer minIndex = new Integer( ((TypeDefinitionLink) me.getValue()).cardinality().min() );
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {

						stringBuilder.append( "public " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " set" + nameVariableOp + "Value(int index){\n" );
						stringBuilder.append( "\n\treturn " + nameVariable + ".get(index);\n" );
						stringBuilder.append( "}\n" );



						stringBuilder.append( "public " + "void add" + nameVariableOp + "Value(" + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "value ){\n" );
						//stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
						stringBuilder.append( "\n\t\t" + nameVariable + ".add(value);\n" );
						//stringBuilder.append( "\t}\n" );
						stringBuilder.append( "}\n" );

						stringBuilder.append( "public " + "void remove" + nameVariableOp + "Value( int index ){\n" );
						//stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
						stringBuilder.append( "\t\t" + nameVariable + ".remove(index);\n" );
						//stringBuilder.append( "\t}\n" );
						stringBuilder.append( "}\n" );

					} else {

						stringBuilder.append( "public " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " get" + nameVariableOp + "(){\n" );
						stringBuilder.append( "\n\treturn " + nameVariable + ";\n" );
						stringBuilder.append( "}\n" );

						stringBuilder.append( "public " + "void set" + nameVariableOp + "(" + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + " value ){\n" );
						stringBuilder.append( "\n\t" + nameVariable + "=value;\n" );
						stringBuilder.append( "}\n" );

					}


				} else {

					nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {
						Integer maxIndex = new Integer( ((TypeDefinition) me.getValue()).cardinality().max() );
						Integer minIndex = new Integer( ((TypeDefinition) me.getValue()).cardinality().min() );
						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						String startingChar = nameVariable.substring( 0, 1 );
						String remaningStr = nameVariable.substring( 1, nameVariable.length() );
						nameVariableOp = startingChar.toUpperCase() + remaningStr;
						if ( typeName.equals( "int" ) ) {



							stringBuilder.append( "public " + "int" + " get" + nameVariableOp + "Value(int index){\n" );
							stringBuilder.append( "\treturn " + nameVariable + ".get(index).intValue;\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void add" + nameVariableOp + "Value(int value ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + "Integer support" ).append( nameVariable ).append( "=new Integer(value);\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(" + "support" + nameVariable + " );\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void remove" + nameVariableOp + "Value( int index ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".remove(index);\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );

						} else if ( typeName.equals( "double" ) ) {
							//stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
							stringBuilder.append( "public " + "double" + " get" + nameVariableOp + "Value(int index){\n" );
							stringBuilder.append( "\treturn " + nameVariable + ".get(index).doubleValue;\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void add" + nameVariableOp + "Value( double value ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + "Double support" ).append( nameVariable ).append( "=new Double(value);\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(" + "support" + nameVariable + " );\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void remove" + nameVariableOp + "Value( int index ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".remove(index);\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );


						} else if ( typeName.equals( "string" ) ) {

							stringBuilder.append( "public " + "String" + " get" + nameVariableOp + "Value(int index){\n" );
							stringBuilder.append( "\n\treturn " + nameVariable + ".get(index);\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void add" + nameVariableOp + "Value( String value ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()<" + maxIndex.toString() + "-" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".add(value);\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void remove" + nameVariableOp + "Value( int index ){\n" );
							//stringBuilder.append( "\tif ((" + nameVariable + ".size()>" + minIndex.toString() + ")){\n" );
							stringBuilder.append( "\t\t" + nameVariable + ".remove(index);\n" );
							//stringBuilder.append( "\t}\n" );
							stringBuilder.append( "}\n" );



						}
					} else {


						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						String startingChar = nameVariable.substring( 0, 1 );
						String remaningStr = nameVariable.substring( 1, nameVariable.length() );
						nameVariableOp = startingChar.toUpperCase() + remaningStr;
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( "public " + "int" + " get" + nameVariableOp + "(){\n" );
							stringBuilder.append( "\n\treturn " + nameVariable + ";\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void set" + nameVariableOp + "Value(int value ){\n" );
							stringBuilder.append( "\n\t" + nameVariable + "=value;\n" );
							stringBuilder.append( "}\n" );

						} else if ( typeName.equals( "double" ) ) {
							//stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
							stringBuilder.append( "public " + "double" + " get" + nameVariableOp + "(){\n" );
							stringBuilder.append( "\n\treturn " + nameVariable + ";\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void set" + nameVariableOp + "Value( double value ){\n" );
							stringBuilder.append( "\n\t\t" + nameVariable + "=value;\n" );
							stringBuilder.append( "}\n" );


						} else if ( typeName.equals( "string" ) ) {

							stringBuilder.append( "public " + "String" + " get" + nameVariableOp + "(){\n" );
							stringBuilder.append( "\n\treturn " + nameVariable + ";\n" );
							stringBuilder.append( "}\n" );

							stringBuilder.append( "public " + "void set" + nameVariableOp + "Value( String value ){\n" );
							stringBuilder.append( "\n\t\t" + nameVariable + "=value;\n" );
							stringBuilder.append( "}\n" );



						}


					}
				}

			}




		}
		//// getVALUE
		stringBuilder.append( "public " + "Value get" + "Value(){\n" );
		Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
		Iterator i = supportSet.iterator();

		while( i.hasNext() ) {
			Map.Entry me = (Map.Entry) i.next();
			//Value v
			//v.getNewChild( nameVariable ).deepCopy( v );
			//v.hasChildren( nameVariable );
			if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
				nameVariable = ((TypeDefinitionLink) me.getValue()).id();

				if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {
					stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
					stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
					stringBuilder.append( "\t\tv.getChildren(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + "," + nameVariable + ".get(counter" + nameVariable + ").getValue());\n" );
					stringBuilder.append( "\t}\n}else{\n" );
					stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + nameVariable + ".size();counter" + nameVariable + "++){\n" );
					stringBuilder.append( "\t\tv.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".get(counter" + nameVariable + ").getValue()));\n" );
					stringBuilder.append( "\t}" );
					stringBuilder.append( "\n}\n" );


					//a.intValue();


				} else {
					stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
					//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
					stringBuilder.append( "v.getChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".getValue());\n" );
					stringBuilder.append( "\t}else{\n" );
					//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
					stringBuilder.append( "v.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".getValue());\n" );
					//stringBuilder.append("\t}");
					stringBuilder.append( "}\n" );


				}


			} else {

				nameVariable = ((TypeDefinition) me.getValue()).id();
				if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

					String typeName = ((TypeDefinition) me.getValue()).nativeType().id();

					if ( typeName.equals( "int" ) ) {
						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getChildren(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + "," + nameVariable + ".get(counter" + nameVariable + ").intValue());\n" );
						stringBuilder.append( "\t}\n}else{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + nameVariable + ".size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".get(counter" + nameVariable + ").intValue());\n" );
						stringBuilder.append( "\t}" );
						stringBuilder.append( "\n}\n" );




					} else if ( typeName.equals( "double" ) ) {
						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getChildren(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + "," + nameVariable + ".get(counter" + nameVariable + ").doubleValue());\n" );
						stringBuilder.append( "\t}\n}else{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + nameVariable + ".size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".get(counter" + nameVariable + ").doubleValue());\n" );
						stringBuilder.append( "\t}" );
						stringBuilder.append( "\n}\n" );
					} else if ( typeName.equals( "string" ) ) {
						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getChildren(\"" + nameVariable + "\")" + ".set(counter" + nameVariable + "," + nameVariable + ".get(counter" + nameVariable + ").strValue());\n" );
						stringBuilder.append( "\t}\n}else{\n" );
						stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<" + nameVariable + ".size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "\t\tv.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".get(counter" + nameVariable + ").strValue());\n" );
						stringBuilder.append( "\t}" );
						stringBuilder.append( "\n}\n" );

					}
				} else {


					String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
					String startingChar = nameVariable.substring( 0, 1 );
					String remaningStr = nameVariable.substring( 1, nameVariable.length() );
					nameVariableOp = startingChar.toUpperCase() + remaningStr;
					if ( typeName.equals( "int" ) ) {
						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".intValue());\n" );
						stringBuilder.append( "\t}else{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".intValue());\n" );
						//stringBuilder.append("\t}");
						stringBuilder.append( "}\n" );
					} else if ( typeName.equals( "double" ) ) {
						//stringBuilder.append(nameVariable +"= new LinkedList<Double>();"+ "\n" );
						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".doubleValue()));\n" );
						stringBuilder.append( "\t}else{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".doubleValue());\n" );
						//stringBuilder.append("\t}");
						stringBuilder.append( "}\n" );


					} else if ( typeName.equals( "string" ) ) {

						stringBuilder.append( "if (v.hasChildren(" + nameVariable + ")" + "{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ");\n" );
						stringBuilder.append( "\t}else{\n" );
						//stringBuilder.append( "\tfor(int counter" + nameVariable + "=0;" + "counter" + nameVariable + "<v.getChildren(\"" + nameVariable + "\").size();counter" + nameVariable + "++){\n" );
						stringBuilder.append( "v.getNewChildren(\"" + nameVariable + "\")" + ".deepCopy(" + nameVariable + ".intValue());\n" );
						//stringBuilder.append("\t}");
						stringBuilder.append( "}\n" );




					}


				}
			}

		}




		stringBuilder.append( "return v ;\n" );
		stringBuilder.append( "}\n" );

	}
}
