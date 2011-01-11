/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliedummycreator.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Map.Entry;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author balint
 */
public class JolieDummyDocumentCreator
{
	private ProgramInspector inspector;
	private String directorySourceFile;
	private StringBuilder stringBuilder;
	private String nameSourceFile;

	public JolieDummyDocumentCreator( ProgramInspector inspector, String directorySourceFile )
	{

		this.inspector = inspector;
		this.directorySourceFile = directorySourceFile.substring( 0, directorySourceFile.lastIndexOf( "/" ) + 1 );
		this.nameSourceFile = directorySourceFile.substring( directorySourceFile.lastIndexOf( "/" ) + 1 );
		stringBuilder = new StringBuilder();
	}

	public void createDocument()
		throws FileNotFoundException, IOException
	{
		String lineOriginalFile = "include  \"string_utils.iol\"";
		System.out.print( "The directory of the file ol is " + directorySourceFile + "\n" );
		BufferedReader reader = new BufferedReader( new FileReader( directorySourceFile + nameSourceFile ) );

		while( !(lineOriginalFile.contains( "nullProcess" )) ) {

			stringBuilder.append( lineOriginalFile + "\n" );

			lineOriginalFile = reader.readLine();
			if ( (lineOriginalFile.contains( "include" )) && (lineOriginalFile.contains( "/" )) ) {

				int indexSlash = lineOriginalFile.indexOf( "/" );
				lineOriginalFile = lineOriginalFile.substring( 0, indexSlash ) + ".." + lineOriginalFile.substring( indexSlash );
			}

		}

		for( InputPortInfo inputPortInfo : inspector.getInputPorts() ) {
			if ( inputPortInfo.context().source().getSchemeSpecificPart().contains( directorySourceFile ) ) {
				System.out.print( "The port is part of the soa " + inputPortInfo.context().source().getSchemeSpecificPart() + "\n" );
				for( OperationDeclaration operationDeclaration : inputPortInfo.operations() ) {

					convertOperation( operationDeclaration, stringBuilder );
					//Input

				}


			} else {


				System.out.print( "The port is not part of the soa " + inputPortInfo.context().source().getSchemeSpecificPart() + "\n" );

			}
		}





		stringBuilder.append( "}" );
		System.out.print( stringBuilder.toString() );
		File pippo = new File( directorySourceFile + "test/" );
		pippo.mkdir();
		BufferedWriter writer = new BufferedWriter( new FileWriter( directorySourceFile + "test/" + nameSourceFile ) );
		writer.append( stringBuilder.toString() );
		writer.flush();
		writer.close();
	}

	private void convertInterfaces( InterfaceDefinition insterfaceDefinition, StringBuilder stringBuilder )
	{
	}

	private void convertOperation( OperationDeclaration operatationDeclaration, StringBuilder stringBuilder )
	{

		if ( operatationDeclaration instanceof RequestResponseOperationDeclaration ) {
			//if (((RequestResponseOperationDeclaration)operatationDeclaration).requestType().id()
			stringBuilder.append( "[" ).append( operatationDeclaration.id() ).append( "(request)(response){\n" );
			stringBuilder.append( "\t" ).append( "valueToPrettyString@StringUtils( request )( s)" ).append( ";\n" );
			stringBuilder.append( "\t" ).append( "println@Console(s)()" );

			TypeDefinition typeDefinition = ((RequestResponseOperationDeclaration) operatationDeclaration).responseType();
			if ( typeDefinition.hasSubTypes() ) {
				stringBuilder.append( ";\n" );
				for( Entry<String, TypeDefinition> entry : typeDefinition.subTypes() ) {

					convertTypes( entry.getValue(), stringBuilder, "\tdummyResponse." + entry.getKey() );


				}
				stringBuilder.append( "response<<dummyResponse\n" );
			} else {

				if ( !(typeDefinition.nativeType().id().equals( "void" )) ) {
					stringBuilder.append( ";\n" );
					if ( typeDefinition.nativeType().id().equals( "int" ) ) {
						stringBuilder.append( "\tdummyRespose" ).append( "=" ).append( "42;\n" );
//
					} else if ( typeDefinition.nativeType().id().equals( "double" ) ) {

						stringBuilder.append( "\tdummyRespose" ).append( "=" ).append( "1.54;\n" );


					} else {

						stringBuilder.append( "\tdummyRespose" ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


					}
					stringBuilder.append( "response<<dummyResponse\n" );
				}

			}

			/// handeling of types here

			stringBuilder.append( "\n}]{nullProcess}\n" );

		}

	}

	private void convertTypes( TypeDefinition typeDefinition, StringBuilder stringBuilder, String nameVariable )
	{


		if ( typeDefinition instanceof TypeDefinitionLink ) {


			TypeDefinitionLink typeDefinitionLink = (TypeDefinitionLink) typeDefinition;

			if ( typeDefinitionLink.cardinality().max() > 1 ) {
				if ( typeDefinitionLink.cardinality().max() == 2147483647 ) {

					for( int cardinalityIndex = 0; cardinalityIndex < 4; cardinalityIndex++ ) {
						{
							if ( !(typeDefinitionLink.linkedType().nativeType().id().equals( "void" )) ) {
								if ( typeDefinitionLink.nativeType().id().equals( "int" ) ) {
									stringBuilder.append( "[" + (new Integer( cardinalityIndex )).toString() + "]" ).append( "=" ).append( "42;\n" );
//
								} else if ( typeDefinitionLink.nativeType().id().equals( "double" ) ) {

									stringBuilder.append( nameVariable ).append( "[" + (new Integer( cardinalityIndex )).toString() + "]" ).append( "=" ).append( "1.54;\n" );


								} else {

									stringBuilder.append( nameVariable ).append( "[" + (new Integer( cardinalityIndex )).toString() + "]" ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


								}
							}
							if (typeDefinitionLink.linkedType().hasSubTypes()){
							for( Entry<String, TypeDefinition> entry : typeDefinitionLink.linkedType().subTypes() ) {

								convertTypes( entry.getValue(), stringBuilder, nameVariable + "[" + (new Integer( cardinalityIndex )).toString() + "]" + "." + entry.getKey() );


							}
							}
						}
					}
				} else {
					for( int cardinalityIndex = 0; cardinalityIndex < typeDefinitionLink.cardinality().max(); cardinalityIndex++ ) {
						{
							if ( !(typeDefinitionLink.linkedType().nativeType().id().equals( "void" )) ) {
						if ( typeDefinitionLink.nativeType().id().equals( "int" ) ) {
							stringBuilder.append( "[" ).append( (new Integer( cardinalityIndex )).toString() ).append( "]").append( "=" ).append( "42;\n" );
//
						} else if ( typeDefinitionLink.nativeType().id().equals( "double" ) ) {

							stringBuilder.append( nameVariable ).append( "[" ).append( (new Integer( cardinalityIndex )).toString()).append( "]").append( "=" ).append( "1.54;\n" );


						} else {

							stringBuilder.append( nameVariable ).append( "[" ).append( (new Integer( cardinalityIndex )).toString()).append( "]").append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


						}
					}

							for( Entry<String, TypeDefinition> entry : typeDefinitionLink.linkedType().subTypes() ) {

								convertTypes( entry.getValue(), stringBuilder, nameVariable + "[" + (new Integer( cardinalityIndex )).toString() + "]" + "." + entry.getKey() );


							}
						}

					}

				}
			} else {
				if ( !(typeDefinitionLink.linkedType().nativeType().id().equals( "void" )) ) {
					if ( typeDefinitionLink.nativeType().id().equals( "int" ) ) {
						stringBuilder.append( nameVariable ).append( "=" ).append( "42;\n" );
//
					} else if ( typeDefinitionLink.nativeType().id().equals( "double" ) ) {

						stringBuilder.append( nameVariable ).append( "=" ).append( "1.54;\n" );


					} else {

						stringBuilder.append( nameVariable ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


					}
				}
				if (typeDefinitionLink.linkedType().hasSubTypes()){
				for( Entry<String, TypeDefinition> entry : typeDefinitionLink.linkedType().subTypes() ) {
					convertTypes( entry.getValue(), stringBuilder, nameVariable + "." + entry.getKey() );

				}
				}

			}
		} else if ( typeDefinition.untypedSubTypes() ) {
//			
		} else {


			if ( typeDefinition.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> entry : typeDefinition.subTypes() ) {

					convertTypes( entry.getValue(), stringBuilder, nameVariable + "." + entry.getKey() );


				}






			} else {

				if ( typeDefinition.cardinality().max() > 1 ) {
					if ( typeDefinition.cardinality().max() == 2147483647 ) {
						for( int cardinalityIndex = 0; cardinalityIndex < 4; cardinalityIndex++ ) {
							if ( typeDefinition.nativeType().id().equals( "int" ) ) {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( cardinalityIndex ).append( ";\n" );
//
							} else if ( typeDefinition.nativeType().id().equals( "double" ) ) {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( 1.54 + cardinalityIndex ).append( ";\n" );


							} else {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


							}
//
						}
					} else {

						for( int cardinalityIndex = 0; cardinalityIndex < typeDefinition.cardinality().max(); cardinalityIndex++ ) {
							if ( typeDefinition.nativeType().id().equals( "int" ) ) {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( cardinalityIndex ).append( ";\n" );
//
							} else if ( typeDefinition.nativeType().id().equals( "double" ) ) {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( 1.54 + cardinalityIndex ).append( ";\n" );


							} else {

								stringBuilder.append( nameVariable ).append( "[" ).append( cardinalityIndex ).append( "]" ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


							}

						}


					}


				} else {
					if ( typeDefinition.nativeType().id().equals( "int" ) ) {
						stringBuilder.append( nameVariable ).append( "=" ).append( "42;\n" );
//			 
					} else if ( typeDefinition.nativeType().id().equals( "double" ) ) {

						stringBuilder.append( nameVariable ).append( "=" ).append( "1.54;\n" );


					} else {

						stringBuilder.append( nameVariable ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );


					}




				}
			}

		}



	}
	//private void writeType( TypeDefinition type, boolean subType, Writer writer, int indetationLevel )
}
