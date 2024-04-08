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

	private final String namespace;
	private final String targetPort;
	private LinkedHashMap< String, TypeDefinition > typeMap;
	private LinkedHashMap< String, TypeDefinition > subTypeMap;
	final ProgramInspector inspector;
	private static final HashMap< NativeType, String > JAVA_NATIVE_EQUIVALENT = new HashMap<>();
	private static final HashMap< NativeType, String > JAVA_NATIVE_METHOD = new HashMap<>();
	private static final HashMap< NativeType, String > JAVA_NATIVE_CHECKER = new HashMap<>();

	public JavaGWTDocumentCreator( ProgramInspector inspector, String namespace, String targetPort ) {

		this.inspector = inspector;
		this.namespace = namespace;
		this.targetPort = targetPort;

		JAVA_NATIVE_EQUIVALENT.put( NativeType.INT, "Integer" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.BOOL, "Boolean" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.DOUBLE, "Double" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.LONG, "Long" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.STRING, "String" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.ANY, "Object" );
		JAVA_NATIVE_EQUIVALENT.put( NativeType.RAW, "ByteArray" );

		JAVA_NATIVE_METHOD.put( NativeType.INT, "intValue()" );
		JAVA_NATIVE_METHOD.put( NativeType.BOOL, "boolValue()" );
		JAVA_NATIVE_METHOD.put( NativeType.DOUBLE, "doubleValue()" );
		JAVA_NATIVE_METHOD.put( NativeType.LONG, "longValue()" );
		JAVA_NATIVE_METHOD.put( NativeType.STRING, "strValue()" );
		JAVA_NATIVE_METHOD.put( NativeType.RAW, "byteArrayValue()" );

		JAVA_NATIVE_CHECKER.put( NativeType.INT, "isInt()" );
		JAVA_NATIVE_CHECKER.put( NativeType.BOOL, "isBool()" );
		JAVA_NATIVE_CHECKER.put( NativeType.DOUBLE, "isDouble()" );
		JAVA_NATIVE_CHECKER.put( NativeType.LONG, "isLong()" );
		JAVA_NATIVE_CHECKER.put( NativeType.STRING, "isString()" );
		JAVA_NATIVE_CHECKER.put( NativeType.RAW, "isByteArray()" );
	}

	public void ConvertDocument() throws FaultException {

		typeMap = new LinkedHashMap<>();
		subTypeMap = new LinkedHashMap<>();

		try {
			// creating ZipOutputStream
			File jarFile = new File( "archive.jar" );
			FileOutputStream os = new FileOutputStream( jarFile );
			ZipOutputStream zipStream = new ZipOutputStream( os );


			InputPortInfo[] inputPorts = inspector.getInputPorts();
			OutputPortInfo[] outputPorts = inspector.getOutputPorts();
			RequestResponseOperationDeclaration requestResponseOperation;

			for( InputPortInfo inputPort : inputPorts ) {
				if( targetPort == null || inputPort.id().equals( targetPort ) ) {
					ConvertInputPorts( inputPort, outputPorts, zipStream );
					Map< String, OperationDeclaration > operations = inputPort.operationsMap();

					for( int x = 0; x < inputPort.aggregationList().length; x++ ) {
						int i = 0;
						while( !inputPort.aggregationList()[ x ].outputPortList()[ 0 ]
							.equals( outputPorts[ i ].id() ) ) {
							i++;
						}
						for( InterfaceDefinition interfaceDefinition : outputPorts[ i ].getInterfaceList() ) {

							for( Entry< String, OperationDeclaration > entry : interfaceDefinition.operationsMap()
								.entrySet() ) {
								operations.put( entry.getKey(), entry.getValue() );
							}
						}
					}

					for( Entry< String, OperationDeclaration > operationEntry : operations.entrySet() ) {

						if( operationEntry.getValue() instanceof RequestResponseOperationDeclaration ) {
							requestResponseOperation = (RequestResponseOperationDeclaration) operationEntry.getValue();
							if( !typeMap.containsKey( requestResponseOperation.requestType().name() ) ) {
								typeMap.put( requestResponseOperation.requestType().name(),
									requestResponseOperation.requestType() );
							}
							if( !typeMap.containsKey( requestResponseOperation.responseType().name() ) ) {
								typeMap.put( requestResponseOperation.responseType().name(),
									requestResponseOperation.responseType() );
							}
						} else {
							OneWayOperationDeclaration oneWayOperationDeclaration =
								(OneWayOperationDeclaration) operationEntry.getValue();
							if( !typeMap.containsKey( oneWayOperationDeclaration.requestType().name() ) ) {
								typeMap.put( oneWayOperationDeclaration.requestType().name(),
									oneWayOperationDeclaration.requestType() );
							}
						}
					}

				}
			}

			Iterator< Entry< String, TypeDefinition > > typeMapIterator = typeMap.entrySet().iterator();
			while( typeMapIterator.hasNext() ) {
				Entry< String, TypeDefinition > typeEntry = typeMapIterator.next();
				if( !(typeEntry.getKey().equals( "undefined" )) ) {
					parseSubType( typeEntry.getValue() );
				}
			}
			for( Entry< String, TypeDefinition > subTypeEntry : subTypeMap.entrySet() ) {

				if( !typeMap.containsKey( subTypeEntry.getKey() ) ) {
					typeMap.put( subTypeEntry.getKey(), subTypeEntry.getValue() );
				}

			}
			typeMapIterator = typeMap.entrySet().iterator();
			while( typeMapIterator.hasNext() ) {
				Entry< String, TypeDefinition > typeEntry = typeMapIterator.next();
				if( !(typeEntry.getKey().equals( "undefined" )) ) {
					ConvertTypes( typeEntry.getValue(), zipStream, namespace );
				}
			}

			zipStream.close();
			os.flush();
			os.close();

		} catch( IOException e ) {
			throw new FaultException( e );
		}

	}

	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertInputPorts( InputPortInfo inputPortInfo, OutputPortInfo[] outputPorts,
		ZipOutputStream zipStream )
		throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		StringBuilder operationCallBuilder = new StringBuilder();
		stringBuilder.append( "package " ).append( namespace ).append( "." ).append( inputPortInfo.id() )
			.append( ";\n" )
			// adding imports
			.append( "import joliex.gwt.client.JolieCallback;\n" )
			.append( "import joliex.gwt.client.JolieService;\n" )
			.append( "import joliex.gwt.client.Value;\n" );
		Map< String, OperationDeclaration > operations = inputPortInfo.operationsMap();

		for( int x = 0; x < inputPortInfo.aggregationList().length; x++ ) {
			int i = 0;
			while( !inputPortInfo.aggregationList()[ x ].outputPortList()[ 0 ].equals( outputPorts[ i ].id() ) ) {
				i++;
			}
			for( InterfaceDefinition interfaceDefinition : outputPorts[ i ].getInterfaceList() ) {

				for( Entry< String, OperationDeclaration > entry : interfaceDefinition.operationsMap().entrySet() ) {
					operations.put( entry.getKey(), entry.getValue() );
				}
			}
		}

		for( Entry< String, OperationDeclaration > operationEntry : operations.entrySet() ) {
			OperationDeclaration operation;
			operation = operationEntry.getValue();

			if( operation instanceof RequestResponseOperationDeclaration ) {
				RequestResponseOperationDeclaration requestResponseOperation =
					(RequestResponseOperationDeclaration) operation;
				stringBuilder.append( "import " ).append( namespace ).append( ".types." )
					.append( requestResponseOperation.requestType().name() ).append( ";\n" );
				stringBuilder.append( "import " ).append( namespace ).append( "." ).append( inputPortInfo.id() )
					.append( ".callbacks.CallBack" ).append( requestResponseOperation.id() ).append( ";\n" );
				operationCallBuilder.append( getPortOperationMethod( requestResponseOperation.id(),
					requestResponseOperation.requestType().name() ) );
				generateCallBackClass( requestResponseOperation, zipStream, inputPortInfo.id() );
			} else {
				OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
				System.out.println( "OneWay operation not supported for GWT: " + oneWayOperationDeclaration.id() );
			}
		}
		stringBuilder.append( "\n" )
			// adding class
			.append( "public class " ).append( inputPortInfo.id() ).append( "Port {\n" )
			// adding operation methods
			.append( operationCallBuilder )
			// adding private call method
			.append(
				"private void call( String operation_name, Value request, JolieCallback callback ) {\nJolieService.Util.getInstance().call( operation_name, request, callback );\n}\n" )
			// closing class
			.append( "}\n;" );

		String namespaceDir = namespace.replaceAll( "\\.", "/" );
		ZipEntry zipEntry =
			new ZipEntry( namespaceDir + "/" + inputPortInfo.id() + "/" + inputPortInfo.id() + "Port.java" );
		zipStream.putNextEntry( zipEntry );
		byte[] bb = stringBuilder.toString().getBytes();
		zipStream.write( bb, 0, bb.length );
		zipStream.closeEntry();

	}

	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertTypes( TypeDefinition typeDefinition, ZipOutputStream zipStream, String portName )
		throws IOException {
		StringBuilder builderHeaderclass = new StringBuilder();
		builderHeaderclass.append( "package " ).append( namespace ).append( ".types;\n" );
		importsCreate( builderHeaderclass, typeDefinition );
		convertClass( typeDefinition, builderHeaderclass );

		String namespaceDir = namespace.replaceAll( "\\.", "/" );
		ZipEntry zipEntry = new ZipEntry( namespaceDir + "/types/" + typeDefinition.name() + ".java" );
		zipStream.putNextEntry( zipEntry );
		byte[] bb = builderHeaderclass.toString().getBytes();
		zipStream.write( bb, 0, bb.length );
		zipStream.closeEntry();

	}

	private void ConvertSubTypes( TypeDefinition typeDefinition, StringBuilder builderHeaderclass ) {
		Set< Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );
		for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
			if( (stringTypeDefinitionEntry.getValue() instanceof TypeInlineDefinition)
				&& (Utils.hasSubTypes( stringTypeDefinitionEntry.getValue() )) ) {
				convertClass( stringTypeDefinitionEntry.getValue(), builderHeaderclass );
			}
		}

	}

	private void convertClass( TypeDefinition typeDefinition, StringBuilder stringBuilder ) {
		stringBuilder.append( "public class " ).append( typeDefinition.name() ).append( " {\n" );
		if( Utils.hasSubTypes( typeDefinition ) ) {
			ConvertSubTypes( typeDefinition, stringBuilder );
		}
		variableCreate( stringBuilder, typeDefinition );
		constructorCreate( stringBuilder, typeDefinition/* , true */ );
		methodsCreate( stringBuilder, typeDefinition/* , true */ );
		addGetValueMethod( stringBuilder, typeDefinition );
		stringBuilder.append( "}\n" );
	}

	private void importsCreate( StringBuilder stringBuilder, TypeDefinition type ) {
		stringBuilder.append( "import joliex.gwt.client.Value;\n" );
		if( Utils.hasSubTypes( type ) ) {
			stringBuilder.append( "import java.util.List;\n" );
			stringBuilder.append( "import java.util.LinkedList;\n" );
			stringBuilder.append( "import joliex.gwt.client.ByteArray;\n" );
			stringBuilder.append( "\n" );
		}
	}

	private void generateCallBackClass( RequestResponseOperationDeclaration operation, ZipOutputStream zipStream,
		String portName ) {
		try {

			// generate class
			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder.append( "package " ).append( namespace ).append( "." ).append( portName )
				.append( ".callbacks;\n" )
				// adding imports
				.append( "import joliex.gwt.client.FaultException;\n" )
				.append( "import joliex.gwt.client.JolieCallback;\n" )
				.append( "import joliex.gwt.client.Value;\n" )
				.append( "import " ).append( namespace ).append( ".types." )
				.append( operation.responseType().name() ).append( ";\n" )
				.append( "\n" )
				.append( "public abstract class CallBack" ).append( operation.id() )
				.append( " extends JolieCallback{\n" )
				// adding onFault method
				.append( "@Override\nprotected void onFault(FaultException fault) {\n" );
			for( Entry< String, TypeDefinition > fault : operation.faults().entrySet() ) {
				stringBuilder.append( "if ( fault.faultName().equals(\"" ).append( fault.getKey() )
					.append( "\") ) {\n" )
					.append( "onFault" ).append( fault.getKey() ).append( "();\n" )
					.append( "}\n" );
			}
			stringBuilder.append( "}\n" )
				// adding onSuccessfullReply method
				.append( "@Override\npublic void onSuccess(Value response) {\nonSuccessfullReply( new " )
				.append( operation.responseType().name() ).append( "( response ) );\n}\n" );

			// adding abstract methods to be implemented
			for( Entry< String, TypeDefinition > fault : operation.faults().entrySet() ) {
				stringBuilder.append( "public abstract void onFault" ).append( fault.getKey() ).append( "();\n" );
			}
			stringBuilder.append( "public abstract void onSuccessfullReply(" ).append( operation.responseType().name() )
				.append( " response );\n" )
				// closing class
				.append( "}\n" );

			String namespaceDir = namespace.replaceAll( "\\.", "/" );
			ZipEntry zipEntry =
				new ZipEntry( namespaceDir + "/" + portName + "/callbacks/" + "CallBack" + operation.id() + ".java" );
			zipStream.putNextEntry( zipEntry );
			byte[] bb = stringBuilder.toString().getBytes();
			zipStream.write( bb, 0, bb.length );
			zipStream.closeEntry();


		} catch( IOException ex ) {
			Logger.getLogger( JavaGWTDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	private StringBuilder getPortOperationMethod( String operationName, String requestTypeName ) {
		StringBuilder operationCallBuilder = new StringBuilder();
		operationCallBuilder.append( "public void " ).append( operationName ).append( "(" ).append( requestTypeName )
			.append( " message, CallBack" ).append( operationName ).append( " callback ) {\n" )
			.append( "call( \"" ).append( operationName )
			.append( "\", message.getValue(), callback );\n}\n" );
		return operationCallBuilder;
	}

	private void variableCreate( StringBuilder stringBuilder, TypeDefinition type ) {

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {

				TypeDefinition subType = stringTypeDefinitionEntry.getValue();

				if( subType instanceof TypeDefinitionLink ) {

					// link
					if( subType.cardinality().max() > 1 ) {
						stringBuilder.append( "private List<" )
							.append( ((TypeDefinitionLink) subType).linkedType().name() ).append( "> " ).append( "_" )
							.append( subType.name() ).append( ";\n" );
					} else {
						stringBuilder.append( "private " ).append( ((TypeDefinitionLink) subType).linkedType().name() )
							.append( " _" ).append( subType.name() ).append( ";\n" );
					}

				} else if( subType instanceof TypeInlineDefinition ) {

					if( Utils.hasSubTypes( subType ) ) {

						/*
						 * if(subType.nativeType()==NativeType.VOID){ //manage type with subtypes and a rootValue }else{
						 * //manage type with subtypes without rootValue }
						 */
						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "private List<" ).append( subType.name() ).append( "> " )
								.append( "_" )
								.append( subType.name() ).append( ";\n" );
						} else {
							stringBuilder.append( "private " ).append( subType.name() ).append( " _" )
								.append( subType.name() ).append( ";\n" );
						}

					} else {
						// native type
						String javaCode = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );
						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "private List<" ).append( javaCode ).append( "> " ).append( "_" )
								.append( subType.name() ).append( ";\n" );
						} else {
							stringBuilder.append( "private " ).append( javaCode ).append( " _" )
								.append( subType.name() )
								.append( ";\n" );
						}
					}


				} else {
					System.out.println( "WARNING: variable is not a Link or an Inline Definition!" );
				}
			}
		}

		if( Utils.nativeType( type ) != NativeType.VOID ) {
			stringBuilder
				.append( "private " ).append( JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( type ) ) )
				.append( " rootValue;\n" );
		}

		// stringBuilder.append("private Value v ;\n");
		// stringBuilder.append("private Value vReturn= new Value() ;\n");
		stringBuilder.append( "\n" );



	}

	private void constructorCreate( StringBuilder stringBuilder, TypeDefinition type/* , boolean naturalType */ ) {

		// constructor with parameters

		stringBuilder.append( "public " ).append( type.name() ).append( "( Value v ){\n" );
		// stringBuilder.append("this.v=v;\n");

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {

				TypeDefinition subType = stringTypeDefinitionEntry.getValue();

				if( subType instanceof TypeDefinitionLink ) {
					// link
					if( subType.cardinality().max() > 1 ) {
						stringBuilder.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
							.append( ((TypeDefinitionLink) subType).linkedType().name() ).append( ">();" )
							.append( "\n" );
						// stringBuilder.append("}\n");

						// to check:
						stringBuilder.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" )
							.append( "for(int counter" ).append( subType.name() ).append( "=0;" ).append( "counter" )
							.append( subType.name() ).append( "<v.getChildren(\"" ).append( subType.name() )
							.append( "\").size();counter" ).append( subType.name() ).append( "++){\n" )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() ).append( " support" )
							.append( subType.name() )
							.append( " = new " ).append( ((TypeDefinitionLink) subType).linkedTypeName() )
							.append( "(v.getChildren(\"" )
							.append( subType.name() ).append( "\").get(counter" ).append( subType.name() )
							.append( "));\n" )
							.append( "_" ).append( subType.name() ).append( ".add(support" )
							.append( subType.name() ).append( ");\n" )
							.append( "}\n" )
							// stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink)
							// me.getValue()).linkedType().id() + ">();").append( "\n" );
							.append( "}\n" );
					} else {
						stringBuilder.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" )

							.append( "_" ).append( subType.name() ).append( " = new " )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() )
							.append( "( v.getFirstChild(\"" ).append( subType.name() ).append( "\"));" ).append( "\n" )
							.append( "}\n" );
					}
				} else if( subType instanceof TypeInlineDefinition ) {

					if( Utils.hasSubTypes( subType ) ) {

						/*
						 * if(subType.nativeType()==NativeType.VOID){ //manage type with subtypes and a rootValue }else{
						 * //manage type with subtypes without rootValue }
						 */

						if( subType.cardinality().max() > 1 ) {
							stringBuilder
								.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
								.append( subType.name() ).append( ">();" ).append( "\n" )
								// to check:
								.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" )
								.append( "for(int counter" ).append( subType.name() ).append( "=0;" )
								.append( "counter" )
								.append( subType.name() ).append( "<v.getChildren(\"" )
								.append( subType.name() ).append( "\").size();counter" ).append( subType.name() )
								.append( "++){\n" )
								.append( subType.name() ).append( " support" ).append( subType.name() )
								.append( "=new " ).append( subType.name() ).append( "(v.getChildren(\"" )
								.append( subType.name() )
								.append( "\").get(counter" ).append( subType.name() ).append( "));\n" )
								.append( "_" ).append( subType.name() ).append( ".add(support" )
								.append( subType.name() )
								.append( ");\n" )
								.append( "}\n" )
								// stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink)
								// me.getValue()).linkedType().id() ).append( ">();").append( "\n" );
								.append( "}\n" );
						} else {
							stringBuilder.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" )
								.append( "_" ).append( subType.name() ).append( " = new " ).append( subType.name() )
								.append( "( v.getFirstChild(\"" ).append( subType.name() )
								.append( "\"));" ).append( "\n" )
								.append( "}\n" );
						}


					} else {
						// native type
						String javaCode = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );
						String javaMethod = JAVA_NATIVE_METHOD.get( Utils.nativeType( subType ) );

						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
								.append( javaCode ).append( ">();" ).append( "\n" );

							stringBuilder.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" );
							stringBuilder.append( "for(int counter" ).append( subType.name() ).append( "=0; " )
								.append( "counter" ).append( subType.name() ).append( "<v.getChildren(\"" )
								.append( subType.name() ).append( "\").size(); counter" ).append( subType.name() )
								.append( "++){\n" );
							if( Utils.nativeType( subType ) != NativeType.ANY ) {
								stringBuilder.append( javaCode ).append( " support" )
									.append( subType.name() )
									.append( " = v.getChildren(\"" ).append( subType.name() )
									.append( "\").get(counter" )
									.append( subType.name() ).append( ")." ).append( javaMethod ).append( ";\n" );
								stringBuilder.append( "_" ).append( subType.name() ).append( ".add(support" )
									.append( subType.name() ).append( ");\n" );
							} else {
								for( NativeType t : NativeType.class.getEnumConstants() ) {
									if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
										continue;
									stringBuilder.append(
										"if(v.getChildren(\"" ).append( subType.name() ).append( "\").get(counter" )
										.append( subType.name() ).append( ")." ).append( JAVA_NATIVE_CHECKER.get( t ) )
										.append( "){\n" ).append( javaCode )
										.append( " support" )
										.append( subType.name() )
										.append( " = v.getChildren(\"" ).append( subType.name() )
										.append( "\").get(counter" ).append( subType.name() ).append( ")." )
										.append( JAVA_NATIVE_METHOD.get( t ) ).append( ";\n" ).append( "_" )
										.append( subType.name() ).append( ".add(support" ).append( subType.name() )
										.append( ");\n" ).append( "}\n" );
								}
							}
							stringBuilder.append( "}\n" );
							// stringBuilder.append( nameVariable +"= new LinkedList<" +((TypeDefinitionLink)
							// me.getValue()).linkedType().id() ).append( ">();").append( "\n" );
							stringBuilder.append( "}\n" );
						} else {
							stringBuilder.append( "if (v.hasChildren(\"" ).append( subType.name() ).append( "\")){\n" );


							if( Utils.nativeType( subType ) != NativeType.ANY ) {
								stringBuilder.append( "_" ).append( subType.name() ).append( "= v.getFirstChild(\"" )
									.append( subType.name() ).append( "\")." )
									.append( javaMethod ).append( ";" ).append( "\n" );
							} else {
								for( NativeType t : NativeType.class.getEnumConstants() ) {
									if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
										continue;
									stringBuilder.append(
										"if(v.getFirstChild(\"" ).append( subType.name() ).append( "\")." )
										.append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n_" )
										.append( subType.name() ).append( " = v.getFirstChild(\"" )
										.append( subType.name() )
										.append( "\")." ).append( JAVA_NATIVE_METHOD.get( t ) )
										.append( ";\n" ).append( "}\n" );
								}
							}
							stringBuilder.append( "}\n" );
						}

					}

				} else {
					System.out.println( "WARNING: variable is not a Link or an Inline Definition!" );
				}
			}
		}

		if( Utils.nativeType( type ) != NativeType.VOID ) {

			String javaMethod = JAVA_NATIVE_METHOD.get( Utils.nativeType( type ) );

			if( Utils.nativeType( type ) != NativeType.ANY ) {
				stringBuilder.append( "rootValue = v." ).append( javaMethod ).append( ";" ).append( "\n" );
			} else {
				for( NativeType t : NativeType.class.getEnumConstants() ) {
					if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
						continue;
					stringBuilder.append(
						"if(v." ).append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n" ).append( "rootValue = v." )
						.append( JAVA_NATIVE_METHOD.get( t ) ).append( ";\n" ).append( "}\n" );
				}
			}
		}
		stringBuilder.append( "}\n" )
			// constructor without parameters
			.append( "public " ).append( type.name() ).append( "(){\n" );

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {

				TypeDefinition subType = stringTypeDefinitionEntry.getValue();

				if( subType instanceof TypeDefinitionLink ) {
					// link
					if( subType.cardinality().max() > 1 ) {
						stringBuilder.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
							.append( ((TypeDefinitionLink) subType).linkedType().name() ).append( ">();" )
							.append( "\n" );
						// stringBuilder.append("}\n");
					}
				} else if( subType instanceof TypeInlineDefinition ) {

					if( Utils.hasSubTypes( subType ) ) {

						if( subType.cardinality().max() > 1 ) {
							stringBuilder
								.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
								.append( subType.name() ).append( ">();" ).append( "\n" );
						}
						/*
						 * if(subType.nativeType()==NativeType.VOID){ //manage type with subtypes and a rootValue }else{
						 * //manage type with subtypes without rootValue }
						 */

					} else {
						// native type
						String javaCode = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );
						// String javaMethod = javaNativeMethod.get(Utils.nativeType(subType));

						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "_" ).append( subType.name() ).append( "= new LinkedList<" )
								.append( javaCode ).append( ">();" ).append( "\n" );
						}
					}

				} else {
					System.out.println( "WARNING: variable is not a Link or an Inline Definition!" );
				}
			}
		}

		stringBuilder.append( "}\n" );
	}

	private void addGetValueMethod( StringBuilder stringBuilder, TypeDefinition type ) {

		stringBuilder.append( "public Value getValue(){\n" )
			.append( "Value vReturn = new Value();\n" );
		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				TypeDefinition subType = stringTypeDefinitionEntry.getValue();
				if( subType instanceof TypeDefinitionLink ) {
					// link
					if( subType.cardinality().max() > 1 ) {
						stringBuilder.append( "if(_" ).append( subType.name() ).append( "!=null){\n" )
							.append( "for(int counter" ).append( subType.name() ).append( "=0;" ).append( "counter" )
							.append( subType.name() ).append( "<" ).append( "_" ).append( subType.name() )
							.append( ".size();counter" ).append( subType.name() ).append( "++){\n" )
							.append( "vReturn.getNewChild(\"" ).append( subType.name() ).append( "\").deepCopy(" )
							.append( "_" ).append( subType.name() ).append( ".get(counter" ).append( subType.name() )
							.append( ").getValue());\n" )
							.append( "}\n" )
							.append( "}\n" );
					} else {
						stringBuilder.append( "if((_" ).append( subType.name() ).append( "!=null)){\n" )
							.append( "vReturn.getNewChild(\"" ).append( subType.name() ).append( "\")" )
							.append( ".deepCopy(" ).append( "_" ).append( subType.name() ).append( ".getValue());\n" )
							.append( "}\n" );
					}
				} else if( subType instanceof TypeInlineDefinition ) {
					if( Utils.hasSubTypes( subType ) ) {

						/*
						 * if(subType.nativeType()==NativeType.VOID){ //manage type with subtypes and a rootValue }else{
						 * //manage type with subtypes without rootValue }
						 */
						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "if(_" ).append( subType.name() ).append( "!=null){\n" )
								.append( "for(int counter" ).append( subType.name() ).append( "=0;" )
								.append( "counter" )
								.append( subType.name() ).append( "<" ).append( "_" ).append( subType.name() )
								.append( ".size();counter" ).append( subType.name() ).append( "++){\n" )
								.append( "vReturn.getNewChild(\"" ).append( subType.name() ).append( "\").deepCopy(" )
								.append( "_" ).append( subType.name() ).append( ".get(counter" )
								.append( subType.name() )
								.append( ").getValue());\n" )
								.append( "}\n" )
								.append( "}\n" );
						} else {
							stringBuilder.append( "if((_" ).append( subType.name() ).append( "!=null)){\n" )
								.append( "vReturn.getNewChild(\"" ).append( subType.name() ).append( "\")" )
								.append( ".deepCopy(" ).append( "_" ).append( subType.name() )
								.append( ".getValue());\n" )
								.append( "}\n" );
						}

					} else {
						// native type

						if( subType.cardinality().max() > 1 ) {
							stringBuilder.append( "if(_" ).append( subType.name() ).append( "!=null){\n" )
								.append( "for(int counter" ).append( subType.name() ).append( "=0;" )
								.append( "counter" ).append( subType.name() ).append( "<" ).append( "_" )
								.append( subType.name() )
								.append( ".size();counter" ).append( subType.name() ).append( "++){\n" );
							if( Utils.nativeType( subType ) != NativeType.ANY ) {
								stringBuilder.append( "vReturn.getNewChild(\"" ).append( subType.name() )
									.append( "\").setValue(_" ).append( subType.name() ).append( ".get(counter" )
									.append( subType.name() ).append( "));\n" );
							} else {
								for( NativeType t : NativeType.class.getEnumConstants() ) {
									if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
										continue;
									stringBuilder.append(
										"if(_" ).append( subType.name() ).append( ".get(counter" )
										.append( subType.name() )
										.append( ") instanceof " ).append( JAVA_NATIVE_EQUIVALENT.get( t ) )
										.append( "){\n" ).append( "vReturn.getNewChild(\"" ).append( subType.name() )
										.append( "\")" ).append( ".setValue(_" ).append( subType.name() )
										.append( ".get(counter" ).append( subType.name() ).append( "));\n" )
										.append( "}\n" );
								}
							}
							stringBuilder.append( "}\n" )
								.append( "}\n" );

						} else {
							stringBuilder.append( "if((_" ).append( subType.name() ).append( "!=null)){\n" );
							if( Utils.nativeType( subType ) != NativeType.ANY ) {
								stringBuilder.append( "vReturn.getNewChild(\"" ).append( subType.name() )
									.append( "\")" )
									.append( ".setValue(_" ).append( subType.name() ).append( ");\n" );
							} else {
								for( NativeType t : NativeType.class.getEnumConstants() ) {
									if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
										continue;
									stringBuilder.append(
										"if(_" ).append( subType.name() ).append( " instanceof " )
										.append( JAVA_NATIVE_EQUIVALENT.get( t ) ).append( "){\n" )
										.append( "vReturn.getNewChild(\"" ).append( subType.name() ).append( "\")" )
										.append( ".setValue(_" ).append( subType.name() ).append( ");\n" )
										.append( "}\n" );
								}
							}
							stringBuilder.append( "}\n" );
						}
					}

				} else {
					System.out.println( "WARNING: variable is not a Link or an Inline Definition!" );
				}
			}
		}

		if( Utils.nativeType( type ) != NativeType.VOID ) {

			stringBuilder.append( "if((rootValue!=null)){\n" );
			if( Utils.nativeType( type ) != NativeType.ANY ) {
				stringBuilder.append( "vReturn.setValue(rootValue);\n" );
			} else {
				for( NativeType t : NativeType.class.getEnumConstants() ) {
					if( !JAVA_NATIVE_CHECKER.containsKey( t ) )
						continue;
					stringBuilder.append(
						"if(rootValue instanceof " ).append( JAVA_NATIVE_EQUIVALENT.get( t ) ).append( "){\n" )
						.append( "vReturn.setValue(rootValue);\n" ).append( "}\n" );
				}
			}

			stringBuilder.append( "}\n" );

		}

		stringBuilder.append( "return vReturn;\n" )
			.append( "}\n" );
	}

	private void methodsCreate( StringBuilder stringBuilder, TypeDefinition type ) {

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {

				TypeDefinition subType = stringTypeDefinitionEntry.getValue();

				String nameVariable = subType.name();
				String startingChar = nameVariable.substring( 0, 1 );
				String remaningStr = nameVariable.substring( 1 );
				String nameVariableOp = startingChar.toUpperCase() + remaningStr;

				if( subType instanceof TypeDefinitionLink ) {
					// link

					if( subType.cardinality().max() > 1 ) {

						stringBuilder.append( "public " )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() ).append( " get" )
							.append( nameVariableOp ).append( "Value( int index ){\n" )
							.append( "return " ).append( "_" ).append( nameVariable ).append( ".get(index);\n}\n" );

						stringBuilder.append( "public " ).append( "int" ).append( " get" )
							.append( nameVariableOp ).append( "Size(){\n" )
							.append( "return " ).append( "_" ).append( nameVariable ).append( ".size();\n" )
							.append( "}\n" )
							.append( "public " ).append( "void add" ).append( nameVariableOp ).append( "Value( " )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() ).append( " value ){\n" )
							.append( "_" ).append( nameVariable ).append( ".add(value);\n" )
							.append( "}\n" )
							.append( "public " ).append( "void remove" )
							.append( nameVariableOp ).append( "Value( int index ){\n" )
							.append( "_" ).append( nameVariable ).append( ".remove(index);\n" )
							.append( "}\n" );

					} else {

						stringBuilder.append( "public " )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() ).append( " get" )
							.append( nameVariableOp ).append( "(){\n" )
							.append( "return " ).append( "_" ).append( nameVariable ).append( ";\n" )
							.append( "}\n" )
							.append( "public " ).append( "void set" ).append( nameVariableOp ).append( "( " )
							.append( ((TypeDefinitionLink) subType).linkedTypeName() ).append( " value ){\n" )
							.append( "_" ).append( nameVariable ).append( " = value;\n" )
							.append( "}\n" );

					}
				} else if( subType instanceof TypeInlineDefinition ) {

					if( Utils.hasSubTypes( subType ) ) {

						/*
						 * if(subType.nativeType()==NativeType.VOID){ //manage type with subtypes and a rootValue }else{
						 * //manage type with subtypes without rootValue }
						 */

						if( subType.cardinality().max() > 1 ) {

							stringBuilder
								.append( "public " ).append( subType.name() ).append( " get" )
								.append( nameVariableOp ).append( "Value( int index ){\n" )
								.append( "return " ).append( "_" ).append( nameVariable ).append( ".get(index);\n" )
								.append( "}\n" )
								.append( "public " ).append( "int" ).append( " get" )
								.append( nameVariableOp ).append( "Size(){\n" )
								.append( "return " ).append( "_" ).append( nameVariable ).append( ".size();\n" )
								.append( "}\n" )
								.append(
									"public " )
								.append( "void add" ).append( nameVariableOp ).append( "Value( " )
								.append( subType.name() ).append( " value ){\n" )
								.append( "_" ).append( nameVariable ).append( ".add(value);\n" )
								.append( "}\n" )
								.append( "public " ).append( "void remove" )
								.append( nameVariableOp ).append( "Value( int index ){\n" )
								.append( "_" ).append( nameVariable ).append( ".remove(index);\n" )
								.append( "}\n" );

						} else {

							stringBuilder.append( "public " ).append( subType.name() ).append( " get" )
								.append( nameVariableOp ).append( "(){\n" )
								.append( "return _" ).append( nameVariable ).append( ";\n" )
								.append( "}\n" )
								.append(
									"public " )
								.append( "void set" ).append( nameVariableOp ).append( "( " )
								.append( subType.name() ).append( " value ){\n" )
								.append( "_" ).append( nameVariable ).append( " = value;\n}\n" );
						}

					} else {
						// native type

						String javaCode = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );

						if( Utils.nativeType( subType ) != NativeType.VOID ) {

							if( subType.cardinality().max() > 1 ) {

								stringBuilder.append( "public int get" ).append( nameVariableOp ).append( "Size(){\n" )
									.append( "return " ).append( "_" ).append( nameVariable ).append( ".size();\n" )
									.append( "}\n" )
									.append( "public " ).append( javaCode ).append( " get" ).append( nameVariableOp )
									.append( "Value( int index ){\n" )
									.append( "return " ).append( "_" ).append( nameVariable ).append( ".get(index);\n" )
									.append( "}\n" )
									.append(
										"public " )
									.append( "void add" ).append( nameVariableOp ).append( "Value( " )
									.append( javaCode ).append( " value ){\n" )
									.append( javaCode ).append( " support" ).append( nameVariable )
									.append( " = value;\n" )
									.append( "_" ).append( nameVariable ).append( ".add(" ).append( "support" )
									.append( nameVariable ).append( " );\n" )
									.append( "}\n" )
									.append( "public " ).append( "void remove" ).append( nameVariableOp )
									.append( "Value( int index ){\n" )
									.append( "_" ).append( nameVariable ).append( ".remove(index);\n" )
									.append( "}\n" );

							} else {
								stringBuilder.append( "public " ).append( javaCode ).append( " get" )
									.append( nameVariableOp ).append( "(){\n" )
									.append( "return " ).append( "_" ).append( nameVariable ).append( ";\n" )
									.append( "}\n" )
									.append( "public " ).append( "void set" ).append( nameVariableOp ).append( "(" )
									.append( javaCode ).append( " value ){\n" )
									.append( "_" ).append( nameVariable ).append( " = value;\n" )
									.append( "}\n" );
							}


						}
					}

				} else {
					System.out.println( "WARNING: variable is not a Link or an Inline Definition!" );
				}
			}
			if( Utils.nativeType( type ) != NativeType.VOID ) {

				String javaCode = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( type ) );

				stringBuilder.append( "public " ).append( javaCode ).append( " getRootValue(){\n" );
				stringBuilder.append( "return " ).append( "rootValue;\n" );
				stringBuilder.append( "}\n" );

				stringBuilder.append( "public void setRootValue( " ).append( javaCode ).append( " value ){\n" );
				stringBuilder.append( "rootValue = value;\n" );
				stringBuilder.append( "}\n" );

			}

		}


	}

	private void parseSubType( TypeDefinition typeDefinition ) {
		if( Utils.hasSubTypes( typeDefinition ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {

				if( stringTypeDefinitionEntry.getValue() instanceof TypeDefinitionLink ) {
					if( !subTypeMap.containsKey(
						((TypeDefinitionLink) stringTypeDefinitionEntry.getValue()).linkedTypeName() ) ) {
						subTypeMap.put(
							((TypeDefinitionLink) stringTypeDefinitionEntry.getValue()).linkedTypeName(),
							((TypeDefinitionLink) stringTypeDefinitionEntry.getValue()).linkedType() );
						parseSubType(
							((TypeDefinitionLink) stringTypeDefinitionEntry.getValue()).linkedType() );
					}
				}
			}
		}
	}
}
