/**
 * *************************************************************************
 * Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>
 * Copyright (C) 2011 by Michele Morgagni Copyright (C) 2015 by Matthias Dieter
 * Wallnöfer Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
package joliex.java.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;

public class JavaDocumentCreator {

	private final String packageName;
	private final String targetPort;
	private final boolean addSource;
	private final boolean buildXml;
	private final boolean javaservice;
	private static final int INDENTATION_STEP = 1;
	private static final String TYPEFOLDER = "types";
	private int indentation;
	private String outputDirectory;
	private String generatedPath;
	private String directoryPathTypes;
	private LinkedHashMap< String, TypeDefinition > typeMap;
	private LinkedHashMap< String, TypeDefinition > faultMap;
	private LinkedHashMap< String, TypeDefinition > subTypeMap;
	final ProgramInspector inspector;
	private static final String TYPESUFFIX = "Type";
	private static final String CHOICEVARIABLENAME = "choice";
	private static final HashMap< NativeType, String > JAVA_NATIVE_EQUIVALENT = new HashMap<>();
	private static final HashMap< NativeType, String > JAVA_NATIVE_METHOD = new HashMap<>();
	private static final HashMap< NativeType, String > JAVA_NATIVE_CHECKER = new HashMap<>();

	public static final String DEFAULT_OUTPUT_DIRECTORY = "./generated";

	static final String[] KEYWORDS = { "abstract", "assert", "boolean",
		"break", "byte", "case", "catch", "char", "class", "const",
		"continue", "default", "do", "double", "else", "extends", "false",
		"final", "finally", "float", "for", "goto", "if", "implements",
		"import", "instanceof", "int", "interface", "long", "native",
		"new", "null", "package", "private", "protected", "public",
		"return", "short", "static", "strictfp", "super", "switch",
		"synchronized", "this", "throw", "throws", "transient", "true",
		"try", "void", "volatile", "while" };

	public JavaDocumentCreator( ProgramInspector inspector, String packageName, String targetPort, boolean addSource,
		String outputDirectory, boolean buildXml, boolean javaservice ) {

		this.inspector = inspector;
		this.packageName = packageName.replaceAll( "-", "_" );
		this.targetPort = targetPort;
		this.addSource = addSource;
		this.buildXml = buildXml;
		this.javaservice = javaservice;

		if( outputDirectory == null ) {
			this.outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
		} else {
			this.outputDirectory = (outputDirectory.endsWith( Constants.FILE_SEPARATOR ))
				? outputDirectory.substring( 0, outputDirectory.length() - 1 )
				: outputDirectory;
		}

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

	public void ConvertDocument() {
		typeMap = new LinkedHashMap<>();
		faultMap = new LinkedHashMap<>();
		subTypeMap = new LinkedHashMap<>();
		OutputPortInfo[] outputPorts = inspector.getOutputPorts();
		OperationDeclaration operation;
		RequestResponseOperationDeclaration requestResponseOperation;

		for( OutputPortInfo outputPort : outputPorts ) {
			/* range over the input ports */
			if( targetPort == null || outputPort.id().equals( targetPort ) ) {

				Collection< OperationDeclaration > operations = outputPort.operations();
				for( OperationDeclaration operationDeclaration : operations ) {
					operation = operationDeclaration;
					if( operation instanceof RequestResponseOperationDeclaration ) {
						requestResponseOperation = (RequestResponseOperationDeclaration) operation;
						if( !typeMap.containsKey( requestResponseOperation.requestType().name() ) ) {
							typeMap.put( requestResponseOperation.requestType().name(),
								requestResponseOperation.requestType() );
						}
						if( !typeMap.containsKey( requestResponseOperation.responseType().name() ) ) {
							typeMap.put( requestResponseOperation.responseType().name(),
								requestResponseOperation.responseType() );
						}
						for( Entry< String, TypeDefinition > fault : requestResponseOperation.faults().entrySet() ) {
							if( !typeMap.containsKey( fault.getValue().name() ) ) {
								typeMap.put( fault.getValue().name(), fault.getValue() );
							}
							faultMap.put( getExceptionName( requestResponseOperation.id(), fault.getKey() ),
								fault.getValue() );
						}
					} else {
						OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
						if( !typeMap.containsKey( oneWayOperationDeclaration.requestType().name() ) ) {
							typeMap.put( oneWayOperationDeclaration.requestType().name(),
								oneWayOperationDeclaration.requestType() );
						}
					}
				}
			}
		}

		/* range over all the types */
		Iterator< Entry< String, TypeDefinition > > typeMapIterator = typeMap.entrySet().iterator();
		while( typeMapIterator.hasNext() ) {
			Entry< String, TypeDefinition > typeEntry = typeMapIterator.next();
			if( !NativeType.isNativeTypeKeyword( typeEntry.getKey() )
				&& !isNativeTypeUndefined( typeEntry.getKey() ) ) {
				parseSubType( typeEntry.getValue() );
			}
		}

		/* put subtypes in the typeMap */
		for( Entry< String, TypeDefinition > subTypeEntry : subTypeMap.entrySet() ) {
			typeMap.put( subTypeEntry.getKey(), subTypeEntry.getValue() );
		}

		/* start generation of java files for each type in typeMap */
		typeMapIterator = typeMap.entrySet().iterator();
		createPackageDirectory();

		if( buildXml ) {
			createBuildFile();
		}

		// prepare types
		while( typeMapIterator.hasNext() ) {
			Entry< String, TypeDefinition > typeEntry = typeMapIterator.next();
			if( !NativeType.isNativeTypeKeyword( typeEntry.getKey() )
				&& !isNativeTypeUndefined( typeEntry.getKey() ) ) {
				String nameFile = directoryPathTypes + Constants.FILE_SEPARATOR + typeEntry.getKey() + ".java";
				Writer writer;
				try {
					writer = new BufferedWriter( new FileWriter( nameFile ) );
					prepareTypeOutputFile( typeEntry.getValue(), writer );
					writer.flush();
					writer.close();
				} catch( IOException ex ) {
					Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
		}

		// prepare exceptions

		for( Entry< String, TypeDefinition > faultEntry : faultMap.entrySet() ) {
			String nameFile = directoryPathTypes + Constants.FILE_SEPARATOR + faultEntry.getKey() + ".java";
			Writer writer;
			try {
				writer = new BufferedWriter( new FileWriter( nameFile ) );
				prepareException( faultEntry, writer );
				writer.flush();
				writer.close();
			} catch( IOException ex ) {
				Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
			}

		}

		// prepare interfaces
		for( OutputPortInfo outputPort : outputPorts ) {
			/* range over the input ports */
			if( targetPort == null || outputPort.id().equals( targetPort ) ) {

				String nameFile = outputDirectory + Constants.FILE_SEPARATOR + outputPort.id() + "Interface.java";
				Writer writer;
				try {
					writer = new BufferedWriter( new FileWriter( nameFile ) );
					prepareInterface( outputPort, writer );
					writer.flush();
					writer.close();
				} catch( IOException ex ) {
					Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
		}

		// prepare interfaceImpl
		if( !javaservice ) {
			for( OutputPortInfo outputPort : outputPorts ) {
				/* range over the input ports */
				if( targetPort == null || outputPort.id().equals( targetPort ) ) {

					String nameFile = outputDirectory + Constants.FILE_SEPARATOR + outputPort.id() + "Impl.java";
					Writer writer;
					try {
						writer = new BufferedWriter( new FileWriter( nameFile ) );
						prepareInterfaceImpl( outputPort, writer );
						writer.flush();
						writer.close();
					} catch( IOException ex ) {
						Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
					}
				}
			}
		}

		// prepare JolieClient
		if( !javaservice ) {
			String nameFile = outputDirectory + Constants.FILE_SEPARATOR + "JolieClient.java";
			Writer writer;
			try {
				writer = new BufferedWriter( new FileWriter( nameFile ) );
				prepareJolieClient( writer );
				writer.flush();
				writer.close();
			} catch( IOException ex ) {
				Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
			}
		}

		// prepare Controller
		if( !javaservice ) {
			String nameFile = outputDirectory + Constants.FILE_SEPARATOR + "Controller.java";
			Writer writer;
			try {
				writer = new BufferedWriter( new FileWriter( nameFile ) );
				prepareController( writer );
				writer.flush();
				writer.close();
			} catch( IOException ex ) {
				Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
			}
		}
	}

	private void prepareExceptionConstructorAndGet( StringBuilder stringBuilder, String faultTypeName,
		String exceptionName, String nativeMethod ) {
		if( faultTypeName != null ) {
			stringBuilder.append( "private " ).append( faultTypeName ).append( " fault" ).append( ";\n" );
			appendingIndentation( stringBuilder );
			if( !faultTypeName.equals( "Value" ) ) {
				stringBuilder.append( "public " ).append( exceptionName )
					.append( "( Value v ) throws TypeCheckingException{\n" );
				incrementIndentation();

				if( !faultTypeName.equals( "Object" ) ) {
					if( !faultTypeName.equals( "ByteArray" ) ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "fault = new " ).append( faultTypeName ).append( "(v" );
						if( nativeMethod != null ) {
							stringBuilder.append( "." ).append( nativeMethod );
						}
						stringBuilder.append( ");\n" );
					} else {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "fault = v.byteArrayValue();\n" );
					}
				} else {
					for( NativeType t : NativeType.class.getEnumConstants() ) {
						if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
							continue;
						}
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if ( v." ).append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n" );
						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "fault = v." ).append( JAVA_NATIVE_METHOD.get( t ) ).append( ";\n" );
						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );
					}
				}
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
			appendingIndentation( stringBuilder );
			stringBuilder.append( "public " ).append( exceptionName ).append( "( " ).append( faultTypeName )
				.append( " f) {\n" );
			incrementIndentation();

			appendingIndentation( stringBuilder );
			stringBuilder.append( "fault = f;\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
			appendingIndentation( stringBuilder );
			stringBuilder.append( "public " ).append( faultTypeName ).append( " getFault(){\n" );
			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "return fault;\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
			decrementIndentation();

		} else {
			stringBuilder.append( "public " ).append( exceptionName ).append( "( Value v ){}\n" );
		}

	}

	private void prepareException( Entry< String, TypeDefinition > fault, Writer writer ) throws IOException {

		StringBuilder outputFileText = new StringBuilder();
		/* appending package */
		outputFileText.append( "package " ).append( packageName ).append( "." ).append( TYPEFOLDER ).append( ";\n" )
			.append( "import jolie.runtime.Value;\n" )
			.append( "import jolie.runtime.ByteArray;\n" )
			.append( "import jolie.runtime.FaultException;\n" )
			.append( "import jolie.runtime.typing.TypeCheckingException;\n" )
			.append( "public class " ).append( fault.getKey() ).append( " extends Exception {\n" );

		indentation = 0;
		incrementIndentation();
		appendingIndentation( outputFileText );
		String faultTypeName = "";
		String nativeMethod = null;
		if( (Utils.hasSubTypes( fault.getValue() ) || !NativeType.isNativeTypeKeyword( fault.getValue().name() ))
			&& !(fault.getValue() instanceof TypeDefinitionUndefined) ) {
			faultTypeName = fault.getValue().name();
		} else if( fault.getValue() instanceof TypeDefinitionUndefined ) {
			faultTypeName = "Value";
		} else if( Utils.nativeType( fault.getValue() ) != NativeType.VOID ) {
			faultTypeName = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( fault.getValue() ) );
			nativeMethod = JAVA_NATIVE_METHOD.get( Utils.nativeType( fault.getValue() ) );
		} else {
			faultTypeName = null;
		}
		prepareExceptionConstructorAndGet( outputFileText, faultTypeName, fault.getKey(), nativeMethod );
		decrementIndentation();
		appendingIndentation( outputFileText );
		outputFileText.append( "}\n" );

		writer.append( outputFileText );
	}

	private String getRequestType( OperationDeclaration operation ) {
		String requestType = "";
		RequestResponseOperationDeclaration requestResponseOperation;
		if( operation instanceof RequestResponseOperationDeclaration ) {
			requestResponseOperation = (RequestResponseOperationDeclaration) operation;
			if( requestResponseOperation.requestType() instanceof TypeDefinitionUndefined ) {
				requestType = "Value";
			} else if( NativeType.isNativeTypeKeyword( requestResponseOperation.requestType().name() ) ) {
				requestType = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( requestResponseOperation.requestType() ) );
				if( requestType == null ) {
					requestType = "";
				}
			} else {
				requestType = requestResponseOperation.requestType().name();
			}

		} else {
			OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
			if( oneWayOperationDeclaration.requestType() instanceof TypeDefinitionUndefined ) {
				requestType = "Value";
			} else if( NativeType.isNativeTypeKeyword( oneWayOperationDeclaration.requestType().name() ) ) {
				requestType =
					JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( oneWayOperationDeclaration.requestType() ) );
				if( requestType == null ) {
					requestType = "";
				}
			} else {
				requestType = oneWayOperationDeclaration.requestType().name();
			}
		}
		return requestType;
	}

	private String getResponseType( OperationDeclaration operation ) {
		String responseType = "";
		RequestResponseOperationDeclaration requestResponseOperation;
		if( operation instanceof RequestResponseOperationDeclaration ) {
			requestResponseOperation = (RequestResponseOperationDeclaration) operation;

			if( requestResponseOperation.responseType() instanceof TypeDefinitionUndefined ) {
				responseType = "Value";
			} else if( NativeType.isNativeTypeKeyword( requestResponseOperation.responseType().name() ) ) {
				responseType =
					JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( requestResponseOperation.responseType() ) );
				if( responseType == null ) {
					responseType = "void";
				}
			} else {
				responseType = requestResponseOperation.responseType().name();
			}

		} else {
			responseType = "void";

		}
		return responseType;
	}

	private HashMap< String, String > getExceptionList( OperationDeclaration operation ) {
		HashMap< String, String > exceptionList = new HashMap<>();
		RequestResponseOperationDeclaration requestResponseOperation;
		if( operation instanceof RequestResponseOperationDeclaration ) {
			requestResponseOperation = (RequestResponseOperationDeclaration) operation;

			requestResponseOperation.faults().entrySet().stream().forEach( ( fault ) -> exceptionList
				.put( fault.getKey(), getExceptionName( requestResponseOperation.id(), fault.getKey() ) ) );
		}
		return exceptionList;
	}

	private void prepareInterface( OutputPortInfo outputPort, Writer writer ) throws IOException {
		OperationDeclaration operation;

		StringBuilder outputFileText = new StringBuilder();
		/* appending package */
		outputFileText.append( "package " ).append( packageName ).append( ";\n" )
			.append( "import " ).append( packageName ).append( "." ).append( TYPEFOLDER ).append( ".*;\n" )
			.append( "import jolie.runtime.FaultException;\n" )
			.append( "import jolie.runtime.Value;\n" )
			.append( "import jolie.runtime.ByteArray;\n" );
		if( !javaservice ) {
			outputFileText.append( "import java.io.IOException;\n" );
		}



		/* writing main class */
		indentation = 0;
		outputFileText.append( "public interface " ).append( outputPort.id() ).append( "Interface {\n" );

		incrementIndentation();

		Collection< OperationDeclaration > operations = outputPort.operations();
		for( OperationDeclaration operationDeclaration : operations ) {
			operation = operationDeclaration;
			String requestType = getRequestType( operation );
			String responseType = getResponseType( operation );
			HashMap< String, String > exceptionList = getExceptionList( operation );
			appendingOperationdeclaration( outputFileText, operation.id(), requestType, responseType, exceptionList );
			outputFileText.append( ";\n" );
		}

		decrementIndentation();
		appendingIndentation( outputFileText );
		outputFileText.append( "}\n" );
		writer.append( outputFileText.toString() );
	}

	private void prepareInterfaceImpl( OutputPortInfo outputPort, Writer writer ) throws IOException {
		OperationDeclaration operation;
		RequestResponseOperationDeclaration requestResponseOperation;

		StringBuilder outputFileText = new StringBuilder();
		/* appending package */
		outputFileText.append( "package " ).append( packageName ).append( ";\n" )
			.append( "import " ).append( packageName ).append( "." ).append( TYPEFOLDER ).append( ".*;\n" )
			.append( "import java.io.IOException;\n" )
			.append( "import jolie.runtime.FaultException;\n" )
			.append( "import jolie.runtime.Value;\n" )
			.append( "import jolie.runtime.ByteArray;\n" );


		/* writing main class */
		indentation = 0;
		outputFileText.append( "public class " ).append( outputPort.id() ).append( "Impl implements " )
			.append( outputPort.id() ).append( "Interface {\n" );

		incrementIndentation();

		Collection< OperationDeclaration > operations = outputPort.operations();
		Iterator< OperationDeclaration > operatorIterator = operations.iterator();
		TypeDefinition requestTypeDefinition;
		while( operatorIterator.hasNext() ) {
			operation = operatorIterator.next();
			String requestType = getRequestType( operation );
			String responseType = getResponseType( operation );
			HashMap< String, String > exceptionList = getExceptionList( operation );

			if( operation instanceof RequestResponseOperationDeclaration ) {
				requestResponseOperation = (RequestResponseOperationDeclaration) operation;
				requestTypeDefinition = requestResponseOperation.requestType();
			} else {
				OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
				requestTypeDefinition = oneWayOperationDeclaration.requestType();
			}
			appendingOperationdeclaration( outputFileText, operation.id(), requestType, responseType, exceptionList );
			outputFileText.append( "{\n" );
			incrementIndentation();
			appendingIndentation( outputFileText );
			outputFileText.append( "final Controller controller = new Controller();\n" );
			if( NativeType.isNativeTypeKeyword( requestTypeDefinition.name() ) ) {
				appendingIndentation( outputFileText );
				outputFileText.append( "Value requestValue = Value.create();\n" );
				if( !requestType.isEmpty() ) {
					appendingIndentation( outputFileText );
					outputFileText.append( "requestValue.setValue( request );\n" );
				}
				appendingIndentation( outputFileText );
				outputFileText.append( "JolieClient.call(requestValue, \"" ).append( operation.id() )
					.append( "\", controller );\n" );
			} else {
				appendingIndentation( outputFileText );
				if( requestType.equals( "Value" ) ) {
					outputFileText.append( "JolieClient.call(request, \"" ).append( operation.id() )
						.append( "\", controller );\n" );
				} else {
					outputFileText.append( "JolieClient.call(request.getValue(), \"" ).append( operation.id() )
						.append( "\", controller );\n" );
				}
			}
			appendingIndentation( outputFileText );
			outputFileText.append( "if ( controller.getFault() != null ) {\n" );
			incrementIndentation();
			exceptionList.entrySet().stream().forEach( f -> {
				appendingIndentation( outputFileText );
				outputFileText.append( "if ( controller.getFault().faultName().equals(\"" ).append( f.getKey() )
					.append( "\")) {\n" );
				incrementIndentation();
				appendingIndentation( outputFileText );
				outputFileText.append( "throw new " ).append( f.getValue() )
					.append( "( controller.getFault().value() );\n" );
				decrementIndentation();
				appendingIndentation( outputFileText );
				outputFileText.append( "}\n" );
			} );
			appendingIndentation( outputFileText );
			outputFileText.append( "throw new Exception( controller.getFault().faultName() );\n" );
			decrementIndentation();
			appendingIndentation( outputFileText );
			outputFileText.append( "}\n" );
			appendingIndentation( outputFileText );
			outputFileText.append( "if ( controller.getException() != null ) {\n" );
			incrementIndentation();
			appendingIndentation( outputFileText );
			outputFileText.append( "throw controller.getException();\n" );
			decrementIndentation();
			appendingIndentation( outputFileText );
			outputFileText.append( "}\n" );
			if( operation instanceof RequestResponseOperationDeclaration && !responseType.equals( "void" ) ) {

				if( NativeType
					.isNativeTypeKeyword( ((RequestResponseOperationDeclaration) operation).responseType().name() ) ) {
					if( Utils.nativeType(
						((RequestResponseOperationDeclaration) operation).responseType() ) != NativeType.ANY ) {
						String nativeMethod = "." + JAVA_NATIVE_METHOD.get(
							Utils.nativeType( ((RequestResponseOperationDeclaration) operation).responseType() ) );
						appendingIndentation( outputFileText );
						outputFileText.append( "return " ).append( "controller.getResponse()" ).append( nativeMethod )
							.append( ";\n" );
					} else {
						for( NativeType t : NativeType.class.getEnumConstants() ) {
							if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
								continue;
							}
							appendingIndentation( outputFileText );
							outputFileText.append( "if(controller.getResponse()." )
								.append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n" );

							incrementIndentation();
							String nativeMethod = "." + JAVA_NATIVE_METHOD.get( t );
							appendingIndentation( outputFileText );
							outputFileText.append( "return " ).append( "controller.getResponse()" )
								.append( nativeMethod ).append( ";\n" );

							decrementIndentation();
							appendingIndentation( outputFileText );
							outputFileText.append( "}\n" );
						}
						appendingIndentation( outputFileText );
						outputFileText.append( "return null;\n" );
					}
				} else if( responseType.equals( "Value" ) ) {
					appendingIndentation( outputFileText );
					outputFileText.append( "return " ).append( " controller.getResponse()" ).append( ";\n" );
				} else {
					appendingIndentation( outputFileText );
					outputFileText.append( "return new " ).append( responseType ).append( "( controller.getResponse()" )
						.append( ");\n" );
				}
			}
			decrementIndentation();
			appendingIndentation( outputFileText );
			outputFileText.append( "}\n" );
		}

		decrementIndentation();
		appendingIndentation( outputFileText );
		outputFileText.append( "}\n" );
		writer.append( outputFileText.toString() );
	}

	@SuppressWarnings( "PMD" ) // TODO: use appends instead of string + string here.
	private void prepareJolieClient( Writer writer ) throws IOException {

		StringBuilder outputFileText = new StringBuilder();
		outputFileText
			.append( "/**\n" + " * *************************************************************************\n"
				+ " * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>\n" + " *\n" + " *\n"
				+ " * This program is free software; you can redistribute it and/or modify it under\n"
				+ " * the terms of the GNU Library General Public License as published by the Free\n"
				+ " * Software Foundation; either version 2 of the License, or (at your option) any\n"
				+ " * later version. This program is distributed in the hope that it will be\n"
				+ " * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
				+ " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General\n"
				+ " * Public License for more details. You should have received a copy of the GNU\n"
				+ " * Library General Public License along with this program; if not, write to the\n"
				+ " * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA\n"
				+ " * 02111-1307, USA. For details about the authors of this software, see the\n" + " * AUTHORS file.\n"
				+ " * *************************************************************************\n" + " */\n"
				+ "package " )
			.append( packageName ).append( ";\n" ).append( "\n" ).append( "import java.io.IOException;\n" )
			.append( "import java.net.URI;\n" ).append( "import java.util.concurrent.CountDownLatch;\n" )
			.append( "import jolie.runtime.FaultException;\n" ).append( "import jolie.runtime.Value;\n" )
			.append( "import joliex.java.Callback;\n" ).append( "import joliex.java.Protocols;\n" )
			.append( "import joliex.java.Service;\n" ).append( "import joliex.java.ServiceFactory;\n" ).append( "\n" )
			.append( "public class JolieClient\n" ).append( "{\n" ).append( "	private static String ip = null;\n" )
			.append( "	private static int servicePort = 0;\n" ).append( "\n" )
			.append( "	public static void init( String ipAddress, int port )\n" ).append( "	{\n" )
			.append( "		ip = ipAddress;\n" ).append( "		servicePort = port;\n" ).append( "	}\n" )
			.append( "\n" )
			.append(
				"	public static void call( Value request, String operation, final Controller controller ) throws IOException, InterruptedException, Exception\n" )
			.append( "	{\n" ).append( "		if (ip != null && servicePort > 0  ) {\n" )
			.append( "			final CountDownLatch latch = new CountDownLatch( 1 ); // just one time\n" )
			.append( "			ServiceFactory serviceFactory = new ServiceFactory();\n" )
			.append( "			String location = \"socket://\" + ip + \":\" + servicePort;\n" )
			.append(
				"			Service service = serviceFactory.create( URI.create( location ), Protocols.SODEP, Value.create() );\n" )
			.append( "			if ( service != null ) {\n" )
			.append( "				service.callRequestResponse( operation, request, new Callback()\n" )
			.append( "				{\n" ).append( "					@Override\n" )
			.append( "					public void onSuccess( Value response )\n" ).append( "					{\n" )
			.append( "						controller.setResponse( response );\n" )
			.append( "						latch.countDown();\n" ).append( "\n" ).append( "					}\n" )
			.append( "\n" ).append( "					@Override\n" )
			.append( "					public void onFault( FaultException fault )\n" )
			.append( "					{\n" ).append( "						controller.setFault( fault );\n" )
			.append( "						latch.countDown();\n" ).append( "					}\n" ).append( "\n" )
			.append( "					@Override\n" )
			.append( "					public void onError( IOException exception )\n" )
			.append( "					{\n" )
			.append( "						controller.setException( exception );\n" )
			.append( "						latch.countDown();\n" ).append( "					}\n" )
			.append( "				} );\n" ).append( "			}\n" ).append( "\n" )
			.append( "			latch.await();\n" ).append( "			service.close();\n" )
			.append( "			serviceFactory.shutdown();\n" ).append( "		} else {\n" )
			.append(
				"			throw new Exception( \"IP and servicePort not initialized, initialize them using static init() method\" );\n" )
			.append( "		}\n" ).append( "\n" ).append( "	}\n" ).append( "\n" ).append( "}\n" );

		writer.append( outputFileText.toString() );
	}

	private void prepareController( Writer writer ) throws IOException {

		StringBuilder outputFileText = new StringBuilder();
		outputFileText
			.append( "/**\n" + " * *************************************************************************\n"
				+ " * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>\n" + " *\n" + " *\n"
				+ " * This program is free software; you can redistribute it and/or modify it under\n"
				+ " * the terms of the GNU Library General Public License as published by the Free\n"
				+ " * Software Foundation; either version 2 of the License, or (at your option) any\n"
				+ " * later version. This program is distributed in the hope that it will be\n"
				+ " * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
				+ " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General\n"
				+ " * Public License for more details. You should have received a copy of the GNU\n"
				+ " * Library General Public License along with this program; if not, write to the\n"
				+ " * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA\n"
				+ " * 02111-1307, USA. For details about the authors of this software, see the\n" + " * AUTHORS file.\n"
				+ " * *************************************************************************\n" + " */\n"
				+ "package " )
			.append( packageName ).append( ";\n" ).append( "import jolie.runtime.FaultException;\n" )
			.append( "import jolie.runtime.Value;\n" ).append( "\n" ).append( "public class Controller\n" )
			.append( "{	\n" ).append( "	private FaultException fault = null;\n" )
			.append( "	private Exception exception = null;\n" ).append( "	private Value response = null;\n" )
			.append( "		\n" ).append( "	public void setFault( FaultException f ) {\n" )
			.append( "		fault = f;\n" ).append( "	}\n" ).append( "	\n" )
			.append( "	public FaultException getFault() {\n" ).append( "		return fault;\n" ).append( "	}\n" )
			.append( "	\n" ).append( "	public void setException( Exception e ) {\n" )
			.append( "		exception = e;\n" ).append( "	}\n" ).append( "	\n" )
			.append( "	public Exception getException() {\n" ).append( "		return exception;\n" )
			.append( "	}\n" ).append( "	\n" ).append( "	public void setResponse( Value v ) {\n" )
			.append( "		response = v;\n" ).append( "	}\n" ).append( "	\n" )
			.append( "	public Value getResponse() {\n" ).append( "		return response;\n" ).append( "	}\n" )
			.append( "}" );

		writer.append( outputFileText.toString() );
	}

	private void createBuildFile() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement( "project" );
			doc.appendChild( rootElement );
			rootElement.setAttribute( "name", "JolieClient" );
			rootElement.setAttribute( "default", "compile" );
			rootElement.setAttribute( "basedir", "." );
			/* Section that defines constants */
			Element propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "name", "src" );
			propertyElement.setAttribute( "location", "." );
			rootElement.appendChild( propertyElement );
			propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "name", "dist" );
			propertyElement.setAttribute( "location", "dist" );
			rootElement.appendChild( propertyElement );
			propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "name", "build" );
			propertyElement.setAttribute( "location", "built" );
			rootElement.appendChild( propertyElement );
			propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "name", "lib" );
			propertyElement.setAttribute( "location", "lib" );
			rootElement.appendChild( propertyElement );
			propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "environment", "env" );
			rootElement.appendChild( propertyElement );

			/*
			 * This portion of the code is responsible for the dist target creation
			 */
			Element initElement = doc.createElement( "target" );
			initElement.setAttribute( "name", "init" );
			rootElement.appendChild( initElement );
			Element mkDirElement = doc.createElement( "mkdir" );
			mkDirElement.setAttribute( "dir", "${build}" );
			initElement.appendChild( mkDirElement );
			mkDirElement = doc.createElement( "mkdir" );
			mkDirElement.setAttribute( "dir", "${dist}" );
			initElement.appendChild( mkDirElement );
			mkDirElement = doc.createElement( "mkdir" );
			mkDirElement.setAttribute( "dir", "${lib}" );
			initElement.appendChild( mkDirElement );
			mkDirElement = doc.createElement( "mkdir" );
			mkDirElement.setAttribute( "dir", "${dist}/lib" );
			initElement.appendChild( mkDirElement );
			Element copyLib = doc.createElement( "copy" );
			copyLib.setAttribute( "file", "${env.JOLIE_HOME}/jolie.jar" );
			copyLib.setAttribute( "tofile", "${lib}/jolie.jar" );
			initElement.appendChild( copyLib );
			copyLib = doc.createElement( "copy" );
			copyLib.setAttribute( "file", "${env.JOLIE_HOME}/lib/libjolie.jar" );
			copyLib.setAttribute( "tofile", "${lib}/libjolie.jar" );
			initElement.appendChild( copyLib );
			copyLib = doc.createElement( "copy" );
			copyLib.setAttribute( "file", "${env.JOLIE_HOME}/lib/jolie-java.jar" );
			copyLib.setAttribute( "tofile", "${lib}/jolie-java.jar" );
			initElement.appendChild( copyLib );
			copyLib = doc.createElement( "copy" );
			copyLib.setAttribute( "file", "${env.JOLIE_HOME}/extensions/sodep.jar" );
			copyLib.setAttribute( "tofile", "${lib}/sodep.jar" );
			initElement.appendChild( copyLib );
			Element compileElement = doc.createElement( "target" );
			rootElement.appendChild( compileElement );
			compileElement.setAttribute( "name", "compile" );
			compileElement.setAttribute( "depends", "init" );
			Element javacElement = doc.createElement( "javac" );
			compileElement.appendChild( javacElement );
			javacElement.setAttribute( "srcdir", "${src}" );
			javacElement.setAttribute( "destdir", "${build}" );
			javacElement.setAttribute( "includeantruntime", "false" );
			Element classPathElement = doc.createElement( "classpath" );
			javacElement.appendChild( classPathElement );
			Element jolieJar = doc.createElement( "pathelement" );
			classPathElement.appendChild( jolieJar );
			jolieJar.setAttribute( "path", "./lib/jolie.jar" );
			Element libJolieJar = doc.createElement( "pathelement" );
			classPathElement.appendChild( libJolieJar );
			libJolieJar.setAttribute( "path", "./lib/libjolie.jar" );
			Element jolieJavaJar = doc.createElement( "pathelement" );
			classPathElement.appendChild( jolieJavaJar );
			jolieJavaJar.setAttribute( "path", "./lib/jolie-java.jar" );
			Element distElement = doc.createElement( "target" );
			rootElement.appendChild( distElement );
			distElement.setAttribute( "name", "dist" );
			distElement.setAttribute( "depends", "compile" );
			Element jarElement = doc.createElement( "jar" );
			distElement.appendChild( jarElement );
			jarElement.setAttribute( "jarfile", "${dist}/JolieClient.jar" );
			jarElement.setAttribute( "basedir", "${build}" );
			if( addSource ) {
				Element filesetElement = doc.createElement( "fileset" );
				filesetElement.setAttribute( "dir", "${src}" );
				filesetElement.setAttribute( "includes", "**/*.java" );
				jarElement.appendChild( filesetElement );
			}
			copyLib = doc.createElement( "copy" );
			copyLib.setAttribute( "toDir", "${dist}/lib" );
			Element filesetElement = doc.createElement( "fileset" );
			filesetElement.setAttribute( "dir", "${lib}" );
			copyLib.appendChild( filesetElement );
			distElement.appendChild( copyLib );
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource( doc );
			StreamResult streamResult = new StreamResult( new File( generatedPath + "/build.xml" ) );
			transformer.transform( source, streamResult );
		} catch( ParserConfigurationException | TransformerException ex ) {
			Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}

	}

	private void createPackageDirectory() {
		File f = new File( "." );

		generatedPath = outputDirectory;
		for( String directoryComponent : packageName.split( "\\." ) ) {
			outputDirectory += Constants.FILE_SEPARATOR + directoryComponent;
		}
		f = new File( outputDirectory );
		f.mkdirs();
		directoryPathTypes = outputDirectory + File.separator + TYPEFOLDER;
		File f2 = new File( directoryPathTypes );
		f2.mkdir();
	}

	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void prepareTypeOutputFile( TypeDefinition typeDefinition, Writer writer )
		throws IOException {
		StringBuilder outputFileText = new StringBuilder();
		/* appending package */
		outputFileText.append( "package " ).append( packageName ).append( "." ).append( TYPEFOLDER ).append( ";\n" );
		/* appending imports */
		appendingImportsIfNecessary( outputFileText );

		/* writing main class */
		indentation = 0;
		appendingClass( outputFileText, typeDefinition, "Jolie2JavaInterface" );

		writer.append( outputFileText.toString() );
	}

	private void appendingOperationdeclaration( StringBuilder stringBuilder, String operationName, String requestType,
		String responseType, HashMap< String, String > exceptionList ) {
		appendingIndentation( stringBuilder );
		String requestArgument = "";
		if( !requestType.isEmpty() ) {
			requestArgument = requestType + " request";
		}
		stringBuilder.append( "public " ).append( responseType ).append( " " ).append( operationName ).append( "(" )
			.append( requestArgument ).append( ") throws FaultException" );
		if( !javaservice ) {
			stringBuilder.append( ", IOException, InterruptedException, Exception" );
		}
		if( !exceptionList.isEmpty() ) {
			exceptionList.entrySet().stream().forEach( f -> stringBuilder.append( ", " ).append( f.getValue() ) );
		}
	}

	private void appendingSubClassBody( TypeDefinition typeDefinition, StringBuilder stringBuilder, String clsName ) {
		/* TypeInLineDefinitions are converted into inner classes */
		if( (typeDefinition instanceof TypeInlineDefinition) && (Utils.hasSubTypes( typeDefinition )) ) {
			/* opening the inner class */
			appendingIndentation( stringBuilder );
			stringBuilder.append( "public class " ).append( clsName ).append( " {" ).append( "\n" );
			incrementIndentation();
			appendingClassBody( typeDefinition, stringBuilder, clsName );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
		}
	}

	private void appendingClassBody( TypeDefinition typeDefinition, StringBuilder stringBuilder, String className ) {
		if( typeDefinition instanceof TypeChoiceDefinition ) {
			appendingClassBodyTypeChoiceDefinition( (TypeChoiceDefinition) typeDefinition, stringBuilder, className );
		} else {
			Set< Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );

			if( supportSet != null ) {
				/* inserting inner classes if present */
				supportSet.stream().forEach( ( me ) -> {
					TypeDefinition tdef = me.getValue();
					appendingSubClassBody( tdef, stringBuilder, tdef.name() + TYPESUFFIX );
				} );
			}

			/* appending private variables */
			appendingPrivateVariables( stringBuilder, typeDefinition );
			stringBuilder.append( "\n" );

			/* create constructor */
			appendingConstructor( stringBuilder, typeDefinition, className );

			/* create methods */
			appendingMethods( stringBuilder, typeDefinition );
		}
	}

	private void appendingChoicesVaiableDeclaration( TypeDefinition type, StringBuilder stringBuilder,
		int choiceCount ) {
		if( Utils.hasSubTypes( type ) ) {
			String variableTypeName = getVariableTypeName( type );
			if( type instanceof TypeInlineDefinition ) {
				variableTypeName = variableTypeName + choiceCount;
			}
			appendingIndentation( stringBuilder );
			stringBuilder.append( variableTypeName ).append( " " ).append( CHOICEVARIABLENAME ).append( choiceCount )
				.append( ";\n" );
			appendingSubClassBody( type, stringBuilder, variableTypeName );
		} else {
			NativeType nativeType = Utils.nativeType( type );
			if( nativeType != NativeType.VOID ) {
				appendingIndentation( stringBuilder );

				stringBuilder.append( JAVA_NATIVE_EQUIVALENT.get( nativeType ) ).append( " " )
					.append( CHOICEVARIABLENAME ).append( choiceCount ).append( ";\n" );
			} else {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "Object " ).append( " " ).append( CHOICEVARIABLENAME ).append( choiceCount )
					.append( ";\n" );
			}
		}
	}

	private void appendingReturnValueForChoice( StringBuilder stringBuilder, TypeDefinition type, int choiceCount ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "if ( choice" ).append( choiceCount ).append( " != null ) {\n" );
		incrementIndentation();
		if( Utils.hasSubTypes( type ) ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "return choice" ).append( choiceCount ).append( ".getValue();\n" );
		} else {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "Value returnValue = Value.create();\n" );
			if( Utils.nativeType( type ) != NativeType.VOID ) {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "returnValue.setValue( choice" ).append( choiceCount ).append( " );\n" );
			}
			appendingIndentation( stringBuilder );
			stringBuilder.append( "return returnValue;\n" );
		}
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingReturnValueForTypeChoice( StringBuilder stringBuilder, TypeChoiceDefinition type,
		int choiceCount ) {
		appendingReturnValueForChoice( stringBuilder, type.left(), choiceCount );
		if( type.right() instanceof TypeChoiceDefinition ) {
			appendingReturnValueForTypeChoice( stringBuilder, (TypeChoiceDefinition) type.right(), choiceCount + 1 );
		} else {
			appendingReturnValueForChoice( stringBuilder, type.right(), choiceCount + 1 );
		}

	}

	private void appendingSetChoiceMethod( StringBuilder stringBuilder, TypeDefinition type, int choiceCount ) {
		appendingIndentation( stringBuilder );
		if( Utils.hasSubTypes( type ) ) {
			String variableTypeName = getVariableTypeName( type );
			if( type instanceof TypeInlineDefinition ) {
				variableTypeName = variableTypeName + choiceCount;
			}
			stringBuilder.append( "public void set(" ).append( variableTypeName ).append( " c ) {\n" );
			incrementIndentation();
		} else {
			NativeType nativeType = Utils.nativeType( type );
			if( nativeType != NativeType.VOID ) {
				stringBuilder.append( "public void set(" ).append( JAVA_NATIVE_EQUIVALENT.get( nativeType ) )
					.append( " c ) {\n" );
				incrementIndentation();
			} else {
				stringBuilder.append( "public void set() {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "Object c = null;\n" );
			}
		}
		appendingIndentation( stringBuilder );
		stringBuilder.append( CHOICEVARIABLENAME ).append( choiceCount ).append( " = c;\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingSetChoiceMethods( StringBuilder stringBuilder, TypeChoiceDefinition type, int choiceCount ) {
		appendingSetChoiceMethod( stringBuilder, type.left(), choiceCount );
		choiceCount++;
		if( type.right() instanceof TypeChoiceDefinition ) {
			appendingSetChoiceMethods( stringBuilder, (TypeChoiceDefinition) type.right(), choiceCount );
		} else {
			appendingSetChoiceMethod( stringBuilder, type.right(), choiceCount );
		}
	}

	private void appendingGetChoiceMethod( StringBuilder stringBuilder, TypeDefinition type, int choiceCount ) {

		if( Utils.hasSubTypes( type ) ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "if ( " ).append( CHOICEVARIABLENAME ).append( choiceCount )
				.append( " != null ) {\n" );
			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "return " ).append( CHOICEVARIABLENAME ).append( choiceCount ).append( ";\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
		} else {
			NativeType nativeType = Utils.nativeType( type );
			if( nativeType != NativeType.VOID ) {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "if ( " ).append( CHOICEVARIABLENAME ).append( choiceCount )
					.append( " != null ) {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "return " ).append( CHOICEVARIABLENAME ).append( choiceCount ).append( ";\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
		}

	}

	private void appendingGetChoiceMethods( StringBuilder stringBuilder, TypeChoiceDefinition type, int choiceCount ) {
		appendingGetChoiceMethod( stringBuilder, type.left(), choiceCount );
		choiceCount++;
		if( type.right() instanceof TypeChoiceDefinition ) {
			appendingGetChoiceMethods( stringBuilder, (TypeChoiceDefinition) type.right(), choiceCount );
		} else {
			appendingGetChoiceMethod( stringBuilder, type.right(), choiceCount );
		}
	}

	private void appendingChoiceMethods( StringBuilder stringBuilder, TypeChoiceDefinition type ) {

		// getValue method
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public Value getValue() {\n" );
		incrementIndentation();
		appendingReturnValueForTypeChoice( stringBuilder, type, 1 );
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return Value.create();\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );

		// set methods
		appendingSetChoiceMethods( stringBuilder, type, 1 );
		// get method
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public Object get() {\n" );
		incrementIndentation();
		appendingGetChoiceMethods( stringBuilder, type, 1 );
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return null;\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingChoicesVariables( TypeChoiceDefinition typeChoiceDefinition, StringBuilder stringBuilder,
		String className, int choiceCount ) {
		appendingChoicesVaiableDeclaration( typeChoiceDefinition.left(), stringBuilder, choiceCount );
		if( typeChoiceDefinition.right() instanceof TypeChoiceDefinition ) {
			appendingChoicesVariables( (TypeChoiceDefinition) typeChoiceDefinition.right(), stringBuilder, className,
				choiceCount + 1 );
		} else {
			appendingChoicesVaiableDeclaration( typeChoiceDefinition.right(), stringBuilder,
				choiceCount + 1 );
		}

	}

	private void appendingClassBodyTypeChoiceDefinition( TypeChoiceDefinition typeChoiceDefinition,
		StringBuilder stringBuilder, String className ) {

		appendingChoicesVariables( typeChoiceDefinition, stringBuilder, className, 1 );

		/* create constructor */
		appendingChoiceConstructor( stringBuilder, typeChoiceDefinition, className, 1 );

		/* create methods */
		appendingChoiceMethods( stringBuilder, typeChoiceDefinition );
	}

	private void appendingClass( StringBuilder stringBuilder, TypeDefinition typeDefinition,
		String interfaceToBeImplemented ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public class " ).append( typeDefinition.name() ).append( " implements " )
			.append( interfaceToBeImplemented ).append( ", ValueConverter" )
			.append( " {" + "\n" );

		incrementIndentation();

		appendingStaticMethodsForValueConverterJavaServiceInterface( stringBuilder, typeDefinition.name() );

		appendingClassBody( typeDefinition, stringBuilder, typeDefinition.name() );
		decrementIndentation();


		/* closing main class */
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingImportsIfNecessary( StringBuilder stringBuilder ) {

		stringBuilder.append( "import jolie.runtime.embedding.Jolie2JavaInterface;\n" )
			.append( "import jolie.runtime.Value;\n" )
			.append( "import jolie.runtime.ValueVector;\n" )
			.append( "import jolie.runtime.ByteArray;\n" )
			.append( "import jolie.runtime.typing.TypeCheckingException;\n" )
			.append( "import java.util.List;\n" )
			.append( "import java.util.ArrayList;\n" )
			.append( "import java.util.Map.Entry;\n" )
			.append( "import jolie.runtime.JavaService.ValueConverter;\n" )
			.append( "\n" );

	}

	private void appendingIndentation( StringBuilder stringBuilder ) {
		for( int i = 0; i < indentation; i++ ) {
			stringBuilder.append( "\t" );
		}
	}

	private void appendingPrivateVariables( StringBuilder stringBuilder, TypeDefinition type ) {

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				String variableType = null;
				String variableName = null;
				TypeDefinition subType =
					stringTypeDefinitionEntry.getValue();
				if( subType instanceof TypeDefinitionLink ) {
					if( ((TypeDefinitionLink) subType).linkedType() instanceof TypeDefinitionUndefined ) {
						variableName = checkReservedKeywords( subType.name() );
						variableType = "Value";
					} else {
						variableName = checkReservedKeywords( subType.name() );
						variableType = ((TypeDefinitionLink) subType).linkedTypeName();
					}
				} else if( subType instanceof TypeInlineDefinition ) {
					if( Utils.hasSubTypes( subType ) ) {
						variableName = checkReservedKeywords( subType.name() );
						variableType = variableTypeFromVariableName( variableName );
					} else {
						variableName = checkReservedKeywords( subType.name() );
						variableType =
							JAVA_NATIVE_EQUIVALENT.get( ((TypeInlineDefinition) subType).basicType().nativeType() );
					}
				}

				if( subType.cardinality().max() > 1 ) {
					appendPrivateVariableList( variableType, variableName, stringBuilder );
				} else {
					appendPrivateVariable( variableType, variableName, stringBuilder );
				}
			}
		}

		if( Utils.nativeType( type ) != NativeType.VOID ) {
			String variableName = "rootValue";
			String variableType = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( type ) );
			appendPrivateVariable( variableType, variableName, stringBuilder );
		}
	}

	private void appendingIfHasChildren( StringBuilder stringBuilder, String variableName, TypeDefinition type,
		StringBuilder ifBody ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "if ( v.hasChildren(\"" ).append( variableName ).append( "\")){\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "if ( v.getChildren(\"" ).append( variableName ).append( "\").size() <= " )
			.append( type.cardinality().max() ).append( " && v.getChildren(\"" ).append( variableName )
			.append( "\").size() >= " ).append( type.cardinality().min() ).append( " ){\n" );
		incrementIndentation();
		stringBuilder.append( ifBody );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "} else {\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "throw new TypeCheckingException(\"cardinality does not correspond for node " )
			.append( variableName ).append( " \");\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingStaticMethodsForValueConverterJavaServiceInterface( StringBuilder stringBuilder,
		String className ) {
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public static " ).append( className )
			.append( " fromValue( Value value ) throws TypeCheckingException {\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return new " ).append( className ).append( "( value );\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public static Value toValue( " ).append( className ).append( " t ){\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return t.getValue();\n" );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingConstructorWithParameters( StringBuilder stringBuilder, TypeDefinition type,
		String className ) {
		// constructor with parameters
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "( Value v ) throws TypeCheckingException {\n" );
		incrementIndentation();

		if( Utils.hasSubTypes( type ) ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "ArrayList<String> __fieldList__ = new ArrayList<>();\n" );
			Utils.subTypes( type ).stream()
				.forEach( t -> {
					appendingIndentation( stringBuilder );
					stringBuilder.append( "__fieldList__.add(\"" ).append( t.getValue().name() ).append( "\");\n" );
				} );

			appendingIndentation( stringBuilder );
			stringBuilder.append( "for( Entry<String,ValueVector> __vv : v.children().entrySet() ) {\n" );
			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "if ( !__fieldList__.contains( __vv.getKey() ) ) { \n" );
			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "throw new TypeCheckingException(\"field \" + __vv.getKey() + \" not found\");\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );

			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				TypeDefinition subType = stringTypeDefinitionEntry.getValue();
				String variableName = getVariableName( subType );
				String variableNameType = getVariableTypeName( subType );

				// case where there are subnodes
				if( Utils.hasSubTypes( subType ) ) {
					if( subType.cardinality().max() > 1 ) {
						/* creating the list object */
						appendingIndentation( stringBuilder );
						stringBuilder.append( checkReservedKeywords( subType.name() ) ).append( "= new ArrayList<" );
						stringBuilder.append( variableNameType );
						stringBuilder.append( ">();" ).append( "\n" );

						/* checking if there are fields in the value */
						StringBuilder ifbody = new StringBuilder();
						incrementIndentation( 2 );
						appendingIndentation( ifbody );
						ifbody.append( "for( int counter" ).append( variableName ).append( " = 0;" )
							.append( "counter" )
							.append( variableName )
							.append( " < v.getChildren( \"" ).append( variableName ).append( "\" ).size(); counter" )
							.append( variableName ).append( "++) { \n" );

						incrementIndentation();
						appendingIndentation( ifbody );
						ifbody.append( variableNameType ).append( " support" )
							.append( variableName ).append( " = new " )
							.append( variableNameType )
							.append( "( v.getChildren(\"" ).append( variableName ).append( "\").get(counter" )
							.append( subType.name() ).append( "));\n" );
						appendingIndentation( ifbody );
						ifbody.append( subType.name() ).append( ".add(support" ).append( variableName )
							.append( ");\n" );
						decrementIndentation();
						appendingIndentation( ifbody );
						ifbody.append( "}\n" );
						decrementIndentation( 2 );
						appendingIfHasChildren( stringBuilder, variableName, subType, ifbody );
					} else {
						StringBuilder ifbody = new StringBuilder();
						incrementIndentation( 2 );
						appendingIndentation( ifbody );
						ifbody.append( checkReservedKeywords( subType.name() ) ).append( " = new " )
							.append( variableNameType )
							.append( "( v.getFirstChild(\"" ).append( variableName ).append( "\"));" ).append( "\n" );
						decrementIndentation( 2 );
						appendingIfHasChildren( stringBuilder, variableName, subType, ifbody );
					}
				} else {
					// case where there are no subnodes
					// native type
					String nativeTypeName = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );
					if( isNativeTypeUndefined( subType ) ) {
						// it is undefined
						nativeTypeName = "Value";
					}
					String javaMethod = JAVA_NATIVE_METHOD.get( Utils.nativeType( subType ) );

					// it is a vector
					if( subType.cardinality().max() > 1 ) {

						appendingIndentation( stringBuilder );
						stringBuilder.append( variableName ).append( "= new ArrayList<" ).append( nativeTypeName )
							.append( ">();\n" );

						StringBuilder ifbody = new StringBuilder();
						incrementIndentation( 2 );
						appendingIndentation( ifbody );
						ifbody.append( "for(int counter" ).append( variableName ).append( "=0;counter" )
							.append( variableName )
							.append( "<v.getChildren(\"" ).append( variableName ).append( "\").size(); counter" )
							.append( variableName )
							.append( "++){\n" );
						incrementIndentation();
						if( Utils.nativeType( subType ) != NativeType.ANY ) {
							appendingIndentation( ifbody );
							appendingAddToListNative( ifbody, variableName, javaMethod );
						} else if( nativeTypeName.equals( "Value" ) ) {
							appendingIndentation( ifbody );
							appendingAddToListValue( ifbody, variableName );
						} else {
							for( NativeType t : NativeType.class.getEnumConstants() ) {
								if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
									continue;
								}
								appendingIndentation( ifbody );
								ifbody.append( "if(v.getChildren(\"" ).append( variableName )
									.append( "\").get(counter" );
								ifbody.append( variableName ).append( ")." ).append( JAVA_NATIVE_CHECKER.get( t ) )
									.append( "){\n" );

								incrementIndentation();
								appendingIndentation( ifbody );
								appendingAddToListNative( ifbody, variableName, JAVA_NATIVE_METHOD.get( t ) );

								decrementIndentation();
								appendingIndentation( ifbody );
								ifbody.append( "}\n" );
							}
						}
						decrementIndentation();
						appendingIndentation( ifbody );
						ifbody.append( "}\n" );
						decrementIndentation( 2 );
						appendingIfHasChildren( stringBuilder, variableName, subType, ifbody );

					} else {
						// it is a single element
						StringBuilder ifbody = new StringBuilder();
						incrementIndentation( 2 );
						if( Utils.nativeType( subType ) != NativeType.ANY ) {
							// all the common native types
							appendingIndentation( ifbody );
							ifbody.append( checkReservedKeywords( variableName ) ).append( "= v.getFirstChild(\"" )
								.append( variableName ).append( "\")." );
							ifbody.append( javaMethod );
							ifbody.append( ";\n" );
						} else if( variableNameType.equals( "Value" ) ) {
							// in case of ANY and in case of undefined
							appendingIndentation( ifbody );
							ifbody.append( checkReservedKeywords( variableName ) ).append( "= v.getFirstChild(\"" )
								.append( variableName );
							ifbody.append( "\")" ).append( ";\n" );

						} else {
							for( NativeType t : NativeType.class.getEnumConstants() ) {
								if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
									continue;
								}
								appendingIndentation( ifbody );
								ifbody.append( "if(v.getFirstChild(\"" ).append( variableName );
								ifbody.append( "\")." ).append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n" );

								incrementIndentation();
								appendingIndentation( ifbody );
								ifbody.append( checkReservedKeywords( variableName ) ).append( " = v.getFirstChild(\"" )
									.append( variableName ).append( "\")." );
								ifbody.append( JAVA_NATIVE_METHOD.get( t ) ).append( ";\n" );

								decrementIndentation();
								appendingIndentation( ifbody );
								ifbody.append( "}\n" );
							}
						}
						decrementIndentation( 2 );
						appendingIfHasChildren( stringBuilder, variableName, subType, ifbody );
					}

				}

				if( subType instanceof TypeChoiceDefinition ) {
					throw new UnsupportedOperationException( "Can't initialize variable with several possible types" );
				}
			}
		}
		if( Utils.nativeType( type ) != NativeType.VOID ) {
			String variableName = "rootValue";
			String javaMethod = JAVA_NATIVE_METHOD.get( Utils.nativeType( type ) );
			if( javaMethod == null ) {
				// case any
				for( NativeType t : NativeType.class.getEnumConstants() ) {
					if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
						continue;
					}
					appendingIndentation( stringBuilder );
					stringBuilder.append( "if ( v." ).append( JAVA_NATIVE_CHECKER.get( t ) ).append( "){\n" );
					incrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "rootValue = v." ).append( JAVA_NATIVE_METHOD.get( t ) ).append( ";\n" );

					decrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "}\n" );
				}

			} else {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "if (!v." ).append( JAVA_NATIVE_CHECKER.get( Utils.nativeType( type ) ) )
					.append( ") {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "throw new TypeCheckingException(\"root value wrong type\");\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "} else {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( checkReservedKeywords( variableName ) ).append( " = v." ).append( javaMethod )
					.append( ";\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
		} else {
			appendingIndentation( stringBuilder );
			stringBuilder.append(
				"if ( v.isString() || v.isInt() || v.isDouble() || v.isByteArray() || v.isBool() || v.isLong() ) {\n" );
			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "throw new TypeCheckingException(\"root value wrong type\");\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );

		}
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingConstructorWithoutParameters( StringBuilder stringBuilder, TypeDefinition type,
		String className ) {
		// constructor without parameters
		stringBuilder.append( "\n\n" );
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "(){\n" );

		incrementIndentation();

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				TypeDefinition subType = stringTypeDefinitionEntry.getValue();
				String variableName = subType.name();
				String variableType = "Value";
				if( subType.cardinality().max() > 1 ) {
					if( subType instanceof TypeDefinitionLink ) {
						if( !isNativeTypeUndefined( subType ) ) {
							variableType = ((TypeDefinitionLink) subType).linkedType().name();
						}
					} else if( subType instanceof TypeInlineDefinition ) {
						if( Utils.hasSubTypes( subType ) ) {
							variableType = subType.name() + "Type";
						} else {
							variableType = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );
						}
					}
					appendingIndentation( stringBuilder );
					stringBuilder.append( checkReservedKeywords( variableName ) ).append( "= new ArrayList<" )
						.append( variableType ).append( ">();\n" );
				}
			}
		}

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );

	}

	private void appendingChoiceConstructorWithoutParameters( StringBuilder stringBuilder, String className ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "( ) {\n" );
		incrementIndentation();
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingInitChoiceVariable( StringBuilder stringBuilder, String variableTypeName, int choiceCount ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( CHOICEVARIABLENAME ).append( choiceCount ).append( " = new " )
			.append( variableTypeName ).append( "( v );\n" );
	}

	private void appendingInitChoiceVariableNativeType( StringBuilder stringBuilder, NativeType nativeType,
		int choiceCount ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( CHOICEVARIABLENAME ).append( choiceCount ).append( " = v." )
			.append( JAVA_NATIVE_METHOD.get( nativeType ) ).append( ";\n" );
	}

	private void appendingChoiceTryCatchForInitTypeRight( StringBuilder stringBuilder, TypeDefinition type,
		int choiceCount ) {
		if( type instanceof TypeChoiceDefinition ) {
			appendingChoiceTryCatchForInitType( stringBuilder, (TypeChoiceDefinition) type, choiceCount );
		} else if( Utils.hasSubTypes( type ) ) {
			String variableTypeName = getVariableTypeName( type );
			if( type instanceof TypeInlineDefinition ) {
				variableTypeName = variableTypeName + choiceCount;
			}
			appendingInitChoiceVariable( stringBuilder, variableTypeName, choiceCount );
		} else {
			NativeType nativeType = Utils.nativeType( type );
			if( nativeType != NativeType.VOID ) {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "if ( v." ).append( JAVA_NATIVE_CHECKER.get( nativeType ) )
					.append( " && !v.hasChildren()) {\n" );
				incrementIndentation();
				appendingInitChoiceVariableNativeType( stringBuilder, nativeType, choiceCount );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "} else {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "throw new TypeCheckingException(\"no native type\");\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			} else {
				appendingIndentation( stringBuilder );
				stringBuilder.append(
					"if ( !v.isString() && !v.isInt() && !v.isDouble() && !v.isLong() && !v.isByteArray() && !v.isBool() && !v.hasChildren()) {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( CHOICEVARIABLENAME ).append( choiceCount ).append( " = new Object();\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "} else {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "throw new TypeCheckingException(\"no native type\");\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
		}
	}

	private void appendingChoiceTryCatchForInitType( StringBuilder stringBuilder, TypeChoiceDefinition type,
		int choiceCount ) {
		if( Utils.hasSubTypes( type.left() ) ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "try {\n" );
			incrementIndentation();
			String variableTypeName = getVariableTypeName( type.left() );
			if( type.left() instanceof TypeInlineDefinition ) {
				variableTypeName = variableTypeName + choiceCount;
			}
			appendingInitChoiceVariable( stringBuilder, variableTypeName, choiceCount );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "} catch ( TypeCheckingException e" ).append( choiceCount ).append( " ) {\n" );
			incrementIndentation();
			choiceCount++;
			appendingChoiceTryCatchForInitTypeRight( stringBuilder, type.right(), choiceCount );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );

		} else {
			NativeType nativeType = Utils.nativeType( type.left() );
			if( nativeType != NativeType.VOID ) {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "if ( v." ).append( JAVA_NATIVE_CHECKER.get( nativeType ) )
					.append( " && !v.hasChildren()) {\n" );
				incrementIndentation();
				appendingInitChoiceVariableNativeType( stringBuilder, nativeType, choiceCount );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "} else {\n" );
				incrementIndentation();
				choiceCount++;
				appendingChoiceTryCatchForInitTypeRight( stringBuilder, type.right(), choiceCount );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			} else {
				appendingIndentation( stringBuilder );
				stringBuilder.append(
					"if ( !v.isString() && !v.isInt() && !v.isDouble() && !v.isLong() && !v.isByteArray() && !v.isBool() && !v.hasChildren()) {\n" );
				incrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( CHOICEVARIABLENAME ).append( choiceCount ).append( " = new Object();\n" );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "} else {\n" );
				incrementIndentation();
				choiceCount++;
				appendingChoiceTryCatchForInitTypeRight( stringBuilder, type.right(), choiceCount );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
		}

	}

	private void appendingChoiceConstructorWithParameters( StringBuilder stringBuilder, TypeChoiceDefinition type,
		String className, int choiceCount ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "( Value v ) throws TypeCheckingException {\n" );
		incrementIndentation();
		appendingChoiceTryCatchForInitType( stringBuilder, type, choiceCount );
		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingChoiceConstructor( StringBuilder stringBuilder, TypeChoiceDefinition type, String className,
		int choiceCounter ) {
		appendingChoiceConstructorWithParameters( stringBuilder, type, className, choiceCounter );
		appendingChoiceConstructorWithoutParameters( stringBuilder, className );
	}

	private void appendingConstructor( StringBuilder stringBuilder, TypeDefinition type, String className ) {

		appendingConstructorWithParameters( stringBuilder, type, className );
		appendingConstructorWithoutParameters( stringBuilder, type, className );
	}

	private void appendingGetValueMethod( StringBuilder stringBuilder,
		TypeDefinition type/* , boolean naturalType */ ) {
		// method getValue
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public Value getValue(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "Value vReturn = Value.create();\n" );

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				TypeDefinition subType = stringTypeDefinitionEntry.getValue();
				String variableName = getVariableName( subType );
				String variableNameType = getVariableTypeName( subType );

				if( Utils.hasSubTypes( subType ) ) {
					// link
					if( subType.cardinality().max() > 1 ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if(" ).append( checkReservedKeywords( variableName ) )
							.append( "!=null){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "for(int counter" ).append( variableName ).append( "=0;counter" )
							.append( variableName );
						stringBuilder.append( "<" ).append( variableName ).append( ".size();counter" )
							.append( variableName ).append( "++){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getChildren(\"" ).append( variableName ).append( "\").add(" )
							.append( checkReservedKeywords( variableName ) );
						stringBuilder.append( ".get(counter" ).append( variableName ).append( ").getValue());\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );

					} else {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if((" ).append( checkReservedKeywords( variableName ) )
							.append( "!=null)){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getChildren(\"" ).append( variableName ).append( "\").add(" )
							.append( checkReservedKeywords( variableName ) );
						stringBuilder.append( ".getValue());\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );
					}
				} else if( subType.cardinality().max() > 1 ) {
					appendingIndentation( stringBuilder );
					stringBuilder.append( "if(" ).append( checkReservedKeywords( variableName ) )
						.append( "!=null){\n" );

					incrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "for(int counter" ).append( variableName ).append( "=0;counter" )
						.append( variableName );
					stringBuilder.append( "<" ).append( variableName ).append( ".size();counter" )
						.append( variableName ).append( "++){\n" );

					incrementIndentation();
					if( Utils.nativeType( subType ) != NativeType.ANY ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName )
							.append( "\").setValue(" ).append( checkReservedKeywords( variableName ) );
						stringBuilder.append( ".get(counter" ).append( variableName ).append( "));\n" );

					} else if( isNativeTypeUndefined( subType ) ) {
						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName )
							.append( "\").setValue(" );
						stringBuilder.append( checkReservedKeywords( variableName ) ).append( ".get(counter" )
							.append( variableName ).append( "));\n" );
						decrementIndentation();
					} else {
						for( NativeType t : NativeType.class.getEnumConstants() ) {
							if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
								continue;
							}
							appendingIndentation( stringBuilder );
							stringBuilder.append( "if(" ).append( checkReservedKeywords( variableName ) )
								.append( ".get(counter" ).append( variableName );
							stringBuilder.append( ") instanceof " ).append( JAVA_NATIVE_EQUIVALENT.get( t ) )
								.append( "){\n" );

							incrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName )
								.append( "\").setValue(" );
							stringBuilder.append( checkReservedKeywords( variableName ) ).append( ".get(counter" )
								.append( variableName ).append( "));\n" );

							decrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "}\n" );
						}
					}
					decrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "}\n" );

					decrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "}\n" );

				} else {
					appendingIndentation( stringBuilder );
					stringBuilder.append( "if((" ).append( checkReservedKeywords( variableName ) )
						.append( "!=null)){\n" );
					incrementIndentation();

					if( Utils.nativeType( subType ) != NativeType.ANY ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName )
							.append( "\").setValue(" );
						stringBuilder.append( checkReservedKeywords( subType.name() ) ).append( ");\n" );
					} else if( variableNameType.equals( "Value" ) ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getChildren(\"" ).append( variableName ).append( "\").add(" );
						stringBuilder.append( checkReservedKeywords( variableName ) ).append( ");\n" );
					} else {
						for( NativeType t : NativeType.class.getEnumConstants() ) {
							if( !JAVA_NATIVE_CHECKER.containsKey( t ) ) {
								continue;
							}
							appendingIndentation( stringBuilder );
							stringBuilder.append( "if(" ).append( checkReservedKeywords( variableName ) )
								.append( " instanceof " );
							stringBuilder.append( JAVA_NATIVE_EQUIVALENT.get( t ) ).append( "){\n" );

							incrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName )
								.append( "\").setValue(" );
							stringBuilder.append( checkReservedKeywords( variableName ) ).append( ");\n" );

							decrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "}\n" );
						}
					}
					decrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "}\n" );
				}
			}
		}

		if( Utils.nativeType( type ) != NativeType.VOID ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "if((rootValue!=null)){\n" );

			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "vReturn.setValue(rootValue);\n" );
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
		}
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return vReturn;\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingMethods( StringBuilder stringBuilder, TypeDefinition type/* , boolean naturalType */ ) {

		if( Utils.hasSubTypes( type ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( type );

			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				TypeDefinition subType = stringTypeDefinitionEntry.getValue();

				String variableName = getVariableName( subType );
				String startingChar = variableName.substring( 0, 1 );
				String variableNameCapitalized =
					startingChar.toUpperCase().concat( variableName.substring( 1 ) );
				if( variableNameCapitalized.equals( "Value" ) ) {
					variableNameCapitalized = "__Value";
				}
				String variableNameType = getVariableTypeName( subType );

				if( Utils.hasSubTypes( subType ) ) {
					// link
					if( subType.cardinality().max() > 1 ) {

						// get
						appendGetMethodWithIndex( stringBuilder, variableNameType, variableNameCapitalized,
							variableName );

						// size
						appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

						// add
						appendAddMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );

						// remove
						appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );

						// get
						variableNameType = "ArrayList<" + variableNameType + ">";
						appendGetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );

					} else {
						// get
						appendGetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
						//
						appendSetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
					}
				} else {
					// native type

					String nativeTypeName = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( subType ) );

					if( Utils.nativeType( subType ) != NativeType.VOID ) {

						if( subType.cardinality().max() > 1 ) {
							if( variableNameType.equals( "Value" ) ) {
								appendGetMethodWithIndex( stringBuilder, variableNameType, variableNameCapitalized,
									variableName );

								// size
								appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

								// add
								appendAddMethod( stringBuilder, variableNameType, variableNameCapitalized,
									variableName );

								// remove
								appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );

								// get
								variableNameType = "ArrayList<" + variableNameType + ">";
								appendGetMethod( stringBuilder, variableNameType, variableNameCapitalized,
									variableName );
							} else {
								appendGetMethodWithIndex( stringBuilder, nativeTypeName, variableNameCapitalized,
									variableName );

								// size
								appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

								// add
								appendAddMethod( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );

								// remove
								appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );

								// get
								nativeTypeName = "ArrayList<" + nativeTypeName + ">";
								appendGetMethod( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );
							}

						} else if( variableNameType.equals( "Value" ) ) {
							// add
							appendGetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
							// set
							appendSetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
						} else {
							// add
							appendGetMethod( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );
							// set
							appendSetMethod( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );
						}
					}
				}

			}
		}

		appendingGetValueMethod( stringBuilder, type );

		if( Utils.nativeType( type ) != NativeType.VOID ) {

			String nativeTypeName = JAVA_NATIVE_EQUIVALENT.get( Utils.nativeType( type ) );

			appendingIndentation( stringBuilder );
			stringBuilder.append( "public " ).append( nativeTypeName ).append( " getRootValue(){\n" );

			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "return " + "rootValue;\n" );

			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );

			appendingIndentation( stringBuilder );
			stringBuilder.append( "public void setRootValue( " ).append( nativeTypeName ).append( " value ){\n" );

			incrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "rootValue = value;\n" );

			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );

		}

	}

	private void parseSubType( TypeDefinition typeDefinition ) {
		if( Utils.hasSubTypes( typeDefinition ) ) {
			Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : supportSet ) {
				// System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
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

	private boolean isNativeTypeUndefined( String t ) {
		return t.equals( "undefined" );
	}

	private boolean isNativeTypeUndefined( TypeDefinition t ) {
		if( t instanceof TypeDefinitionLink ) {
			return ((TypeDefinitionLink) t).linkedType() instanceof TypeDefinitionUndefined;
		}
		return false;
	}

	private String variableTypeFromVariableName( String variableName ) {
		return variableName.concat( TYPESUFFIX );
	}

	private void appendPrivateVariable( String variableType, String variableName, StringBuilder stringBuilder ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "private " ).append( variableType ).append( " " ).append( variableName ).append( ";\n" );
	}

	private void appendPrivateVariableList( String variableType, String variableName, StringBuilder stringBuilder ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "private ArrayList<" ).append( variableType ).append( "> " ).append( variableName )
			.append( ";\n" );
	}

	private void appendGetMethodWithIndex( StringBuilder stringBuilder, String typeName, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( typeName )
			.append( " get" ).append( variableNameCapitalized ).append( "Value( int index ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( checkReservedKeywords( variableName ) ).append( ".get(index);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendGetMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( typeName )
			.append( " get" ).append( variableNameCapitalized ).append( "(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( checkReservedKeywords( variableName ) ).append( ";\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendGetMethodSize( StringBuilder stringBuilder, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( "int get" ).append( variableNameCapitalized ).append( "Size(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( checkReservedKeywords( variableName ) ).append( ".size();\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendAddMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void add" ).append( variableNameCapitalized ).append( "Value( " )
			.append( typeName ).append( " value ){\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( checkReservedKeywords( variableName ) ).append( ".add(value);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendSetMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void set" ).append( variableNameCapitalized ).append( "( " )
			.append( typeName ).append( " value ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( checkReservedKeywords( variableName ) ).append( " = value;\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendRemoveMethod( StringBuilder stringBuilder, String variableNameCapitalized,
		String variableName ) {
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void remove" ).append( variableNameCapitalized )
			.append( "Value( int index ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( checkReservedKeywords( variableName ) ).append( ".remove(index);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendingAddToListNative( StringBuilder stringBuilder, String variableName, String javaMethod ) {
		stringBuilder.append( variableName ).append( ".add(v.getChildren(\"" )
			.append( variableName ).append( "\").get(counter" ).append( variableName ).append( ")." )
			.append( javaMethod ).append( ");\n" );
	}

	private void appendingAddToListValue( StringBuilder stringBuilder, String variableName ) {
		stringBuilder.append( variableName ).append( ".add(v.getChildren(\"" )
			.append( variableName ).append( "\").get(counter" ).append( variableName ).append( "));\n" );
	}

	private void incrementIndentation() {
		indentation = indentation + INDENTATION_STEP;
	}

	private void incrementIndentation( int times ) {
		for( int i = 0; i < times; i++ ) {
			incrementIndentation();
		}
	}

	private void decrementIndentation() {
		indentation = indentation - INDENTATION_STEP;
	}

	private void decrementIndentation( int times ) {
		for( int i = 0; i < times; i++ ) {
			decrementIndentation();
		}
	}

	private String getVariableName( TypeDefinition type ) {
		if( type instanceof TypeDefinitionLink ) {
			if( ((TypeDefinitionLink) type).linkedType() instanceof TypeDefinitionUndefined ) {
				return type.name();
			} else {
				return type.name();
			}
		} else if( type instanceof TypeInlineDefinition ) {
			if( Utils.hasSubTypes( type ) ) {
				return type.name();
			} else {
				return type.name();
			}
		} else if( type instanceof TypeChoiceDefinition ) {
			System.out.println( "WARNING: Type definition contains a choice variable which is not supported!" );
			return type.name();
		} else {
			System.out.println( "WARNING5: variable is not a Link, a Choice or an Inline Definition!" );
		}
		return "err";
	}

	private String getVariableTypeName( TypeDefinition type ) {
		if( type instanceof TypeDefinitionLink ) {
			if( ((TypeDefinitionLink) type).linkedType() instanceof TypeDefinitionUndefined ) {
				return "Value";
			} else {
				return ((TypeDefinitionLink) type).linkedTypeName();
			}
		} else if( type instanceof TypeInlineDefinition ) {
			if( Utils.hasSubTypes( type ) ) {
				return variableTypeFromVariableName( getVariableName( type ) );
			} else {
				return JAVA_NATIVE_EQUIVALENT.get( ((TypeInlineDefinition) type).basicType().nativeType() );
			}
		}
		return "err";
	}

	private String getExceptionName( String operationName, String faultName ) {
		return operationName + faultName + "Exception";
	}

	private String checkReservedKeywords( String id ) {
		if( Arrays.binarySearch( KEYWORDS, id ) >= 0 ) {
			return "_" + id;
		} else {
			return id;
		}
	}

}
