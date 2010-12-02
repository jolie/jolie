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
import joliex.gwt.client.Value;


import support.GeneralDocumentCreator;
import support.GeneralProgramVisitor;
import support.treeOLObject;

/**
 *
 * @author balint
 */
public class JavaGWTDocumentCreator extends GeneralDocumentCreator
{
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
		List<Map<String, TypeDefinition>> typesList = this.GetTypesSet();
		for( Map<String, TypeDefinition> typeDefinitionMap : typesList ) {
			String nameFile = typeDefinitionMap.entrySet().iterator().next().getValue().id() + ".java";
			Writer writer;
			try {
				writer = new BufferedWriter( new FileWriter( nameFile ) );
				ConvertTypes( typeDefinitionMap.entrySet().iterator().next().getValue(), writer );
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
		ImportCreate( builderHeaderclass, typesDefinition );
		builderHeaderclass.append( "public " + typesDefinition.id() + " {" + "\n" );
		VariableCreate( builderHeaderclass, typesDefinition );
		ConstructorCreate( builderHeaderclass, typesDefinition );
		builderHeaderclass.append( " }\n" );
		writer.append( builderHeaderclass.toString() );
	}

	private void ImportCreate( StringBuilder stringBuilder, TypeDefinition type )
	{





		String nameFile = type.context().sourceName();
		TypeDefinition supportType = type;
		System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
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

				}else {

					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						addListImport = true;

					}
				}


			}
			Integer po;
			if (addListImport){
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
		System.out.print( "element of the list Oltree " + supportType.id() + "\n" );


		if ( supportType.hasSubTypes() ) {


			Set<Map.Entry<String, TypeDefinition>> supportSet = supportType.subTypes();
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();

				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					if ( ((TypeDefinitionLink) me.getValue()).cardinality().max() > 1 ) {
						stringBuilder.append( "private List< " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + "> " + ((TypeDefinitionLink) me.getValue()).id() + "\n" );


					} else {
						stringBuilder.append( "private " + ((TypeDefinitionLink) me.getValue()).linkedType().id() + " " + ((TypeDefinitionLink) me.getValue()).id() + "\n" );
					}
				} else {
					System.out.print( ((TypeDefinition) me.getValue()).cardinality().max()+"\n" );
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( "private List<Integer> " + ((TypeDefinition) me.getValue()).id() + "\n" );
						} else if ( typeName.equals( "double" ) ) {

							stringBuilder.append( "private List<Double> " + ((TypeDefinition) me.getValue()).id() + "\n" );


						} else if ( typeName.equals( "string" ) ) {
							stringBuilder.append( "private List<Double> " + ((TypeDefinition) me.getValue()).id() + "\n" );

						} 
						

					}else {


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
		System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
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

						stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );


					} else {
					   stringBuilder.append( nameVariable + "=new " + ((TypeDefinitionLink) me.getValue()).linkedTypeName() + "( v.getFirstChildren(\"" + nameVariable + "\"));" + "\n" );

					}
					

				} else {

				nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							stringBuilder.append( nameVariable +"= new LinkedList<Integer>();"+ "\n" );

						} else if ( typeName.equals( "double" ) ) {
                            stringBuilder.append( nameVariable +"= new LinkedList<Double>();"+ "\n" );



						} else if ( typeName.equals( "string" ) ) {

							 stringBuilder.append( nameVariable +"= new LinkedList<String>();"+ "\n" );



						} 
					}else {


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

						stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink) me.getValue()).linkedType().id() + ">();"+ "\n" );


					}
				}else{
				   nameVariable = ((TypeDefinition) me.getValue()).id();
					if ( ((TypeDefinition) me.getValue()).cardinality().max() > 1 ) {

						String typeName = ((TypeDefinition) me.getValue()).nativeType().id();
						if ( typeName.equals( "int" ) ) {
							//Value dsadda;
						  //dsadda.getChildren( typeName ).
							stringBuilder.append("\n");
							stringBuilder.append( "for(int counter"+nameVariable+"=0;"+"counter"+nameVariable+"<v.getChildren(\""+nameVariable+"\').size();counter"+nameVariable+"");
							stringBuilder.append( nameVariable +"= new LinkedList<Integer>();"+ "\n" );

						} else if ( typeName.equals( "double" ) ) {
                            stringBuilder.append( nameVariable +"= new LinkedList<Double>();"+ "\n" );



						} else if ( typeName.equals( "string" ) ) {

							 stringBuilder.append( nameVariable +"= new LinkedList<String>();"+ "\n" );



						}
					}



				}

			}

			stringBuilder.append( "}\n" );
		}


	}
}
