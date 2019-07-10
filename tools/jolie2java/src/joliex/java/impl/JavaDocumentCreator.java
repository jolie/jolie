/**
 * *************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com> *
 * Copyright (C) 2015 by Matthias Dieter Wallnöfer * * This program is free
 * software; you can redistribute it and/or modify * it under the terms of the
 * GNU Library General Public License as * published by the Free Software
 * Foundation; either version 2 of the * License, or (at your option) any later
 * version. * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General
 * Public License for more details. * * You should have received a copy of the
 * GNU Library General Public * License along with this program; if not, write
 * to the * Free Software Foundation, Inc., * 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *************************************************************************
 */
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
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author balint maschio & michele morgagni
 */
public class JavaDocumentCreator
{

	private final String packageName;
	private final String targetPort;
	private final boolean addSource;
	private final int INDENTATION_STEP = 1;
	private int indentation;
	private String directoryPath;
	private LinkedHashMap<String, TypeDefinition> typeMap;
	private LinkedHashMap<String, TypeDefinition> subTypeMap;
	ProgramInspector inspector;
	private String TYPESUFFIX = "Type";
	private static final HashMap<NativeType, String> javaNativeEquivalent = new HashMap<NativeType, String>();
	private static final HashMap<NativeType, String> javaNativeMethod = new HashMap<NativeType, String>();
	private static final HashMap<NativeType, String> javaNativeChecker = new HashMap<NativeType, String>();

	public JavaDocumentCreator( ProgramInspector inspector, String packageName, String targetPort, boolean addSource )
	{

		this.inspector = inspector;
		this.packageName = packageName;
		this.targetPort = targetPort;
		this.addSource = addSource;

		javaNativeEquivalent.put( NativeType.INT, "Integer" );
		javaNativeEquivalent.put( NativeType.BOOL, "Boolean" );
		javaNativeEquivalent.put( NativeType.DOUBLE, "Double" );
		javaNativeEquivalent.put( NativeType.LONG, "Long" );
		javaNativeEquivalent.put( NativeType.STRING, "String" );
		javaNativeEquivalent.put( NativeType.ANY, "Object" );
		javaNativeEquivalent.put( NativeType.RAW, "ByteArray" );

		javaNativeMethod.put( NativeType.INT, "intValue()" );
		javaNativeMethod.put( NativeType.BOOL, "boolValue()" );
		javaNativeMethod.put( NativeType.DOUBLE, "doubleValue()" );
		javaNativeMethod.put( NativeType.LONG, "longValue()" );
		javaNativeMethod.put( NativeType.STRING, "strValue()" );
		javaNativeMethod.put( NativeType.RAW, "byteArrayValue()" );

		javaNativeChecker.put( NativeType.INT, "isInt()" );
		javaNativeChecker.put( NativeType.BOOL, "isBool()" );
		javaNativeChecker.put( NativeType.DOUBLE, "isDouble()" );
		javaNativeChecker.put( NativeType.LONG, "isLong()" );
		javaNativeChecker.put( NativeType.STRING, "isString()" );
		javaNativeChecker.put( NativeType.RAW, "isByteArray()" );
	}

	public void ConvertDocument()
	{
		typeMap = new LinkedHashMap<>();
		subTypeMap = new LinkedHashMap<>();
		InputPortInfo[] inputPorts = inspector.getInputPorts();
		OperationDeclaration operation;
		RequestResponseOperationDeclaration requestResponseOperation;

		for( InputPortInfo inputPort : inputPorts ) {
			/* range over the input ports */

			if ( targetPort == null || inputPort.id().equals( targetPort ) ) {

				Collection<OperationDeclaration> operations = inputPort.operations();
				Iterator<OperationDeclaration> operatorIterator = operations.iterator();
				while( operatorIterator.hasNext() ) {
					operation = operatorIterator.next();
					if ( operation instanceof RequestResponseOperationDeclaration ) {
						requestResponseOperation = (RequestResponseOperationDeclaration) operation;
						if ( !typeMap.containsKey( requestResponseOperation.requestType().id() ) ) {
							typeMap.put( requestResponseOperation.requestType().id(), requestResponseOperation.requestType() );
						}
						if ( !typeMap.containsKey( requestResponseOperation.responseType().id() ) ) {
							typeMap.put( requestResponseOperation.responseType().id(), requestResponseOperation.responseType() );
						}
						for( Entry<String, TypeDefinition> fault : requestResponseOperation.faults().entrySet() ) {
							if ( !typeMap.containsKey( fault.getValue().id() ) ) {
								typeMap.put( fault.getValue().id(), fault.getValue() );
							}
						}
					} else {
						OneWayOperationDeclaration oneWayOperationDeclaration = (OneWayOperationDeclaration) operation;
						if ( !typeMap.containsKey( oneWayOperationDeclaration.requestType().id() ) ) {
							typeMap.put( oneWayOperationDeclaration.requestType().id(), oneWayOperationDeclaration.requestType() );
						}
					}
				}
			}
		}

		/* range over all the types */
		Iterator<Entry<String, TypeDefinition>> typeMapIterator = typeMap.entrySet().iterator();
		while( typeMapIterator.hasNext() ) {
			Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
			if ( !NativeType.isNativeTypeKeyword( typeEntry.getKey() ) && !isNativeTypeUndefined( typeEntry.getKey() ) ) {
				parseSubType( typeEntry.getValue() );
			}
		}

		/* put subtypes in the typeMap */
		Iterator<Entry<String, TypeDefinition>> subTypeMapIterator = subTypeMap.entrySet().iterator();
		while( subTypeMapIterator.hasNext() ) {
			Entry<String, TypeDefinition> subTypeEntry = subTypeMapIterator.next();
			typeMap.put( subTypeEntry.getKey(), subTypeEntry.getValue() );
		}

		/* start generation of java files for each type in typeMap */
		typeMapIterator = typeMap.entrySet().iterator();
		createPackageDirectory();
		createBuildFile();

		while( typeMapIterator.hasNext() ) {
			Entry<String, TypeDefinition> typeEntry = typeMapIterator.next();
			if ( !NativeType.isNativeTypeKeyword( typeEntry.getKey() ) && !isNativeTypeUndefined( typeEntry.getKey() ) ) {
				String nameFile = directoryPath + Constants.fileSeparator + typeEntry.getKey() + ".java";
				Writer writer;
				try {
					writer = new BufferedWriter( new FileWriter( nameFile ) );
					prepareOutputFile( typeEntry.getValue(), writer );
					writer.flush();
					writer.close();
				} catch( IOException ex ) {
					Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
		}
	}

	private void createBuildFile()
	{
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement( "project" );
			doc.appendChild( rootElement );
			rootElement.setAttribute( "name", "JolieConnector" );
			rootElement.setAttribute( "default", "compile" );
			rootElement.setAttribute( "basedir", "." );
			/*Section that defines constants*/
			Element propertyElement = doc.createElement( "property" );
			propertyElement.setAttribute( "name", "src" );
			propertyElement.setAttribute( "location", "src" );
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
	     This portion of the code is responsible for the dist target creation
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
			Element classPathElement = doc.createElement( "classpath" );
			javacElement.appendChild( classPathElement );
			Element jolieJar = doc.createElement( "pathelement" );
			classPathElement.appendChild( jolieJar );
			jolieJar.setAttribute( "path", "./lib/jolie.jar" );
			Element libJolieJar = doc.createElement( "pathelement" );
			classPathElement.appendChild( libJolieJar );
			libJolieJar.setAttribute( "path", "./lib/libjolie.jar" );
			Element distElement = doc.createElement( "target" );
			rootElement.appendChild( distElement );
			distElement.setAttribute( "name", "dist" );
			distElement.setAttribute( "depends", "compile" );
			Element jarElement = doc.createElement( "jar" );
			distElement.appendChild( jarElement );
			jarElement.setAttribute( "jarfile", "${dist}/JolieConnector.jar" );
			jarElement.setAttribute( "basedir", "${build}" );
			if ( addSource ) {
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
			StreamResult streamResult = new StreamResult( new File( directoryPath + "build.xml" ) );
			transformer.transform( source, streamResult );
		} catch( ParserConfigurationException ex ) {
			Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( TransformerConfigurationException ex ) {
			Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( TransformerException ex ) {
			Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}

	}

	private void createPackageDirectory()
	{
		String[] directoriesComponents = packageName.split( "\\." );
		File f = new File( "." );

		try {
			directoryPath = f.getCanonicalPath() + Constants.fileSeparator + "generated";
			for( int counterDirectories = 0; counterDirectories < directoriesComponents.length; counterDirectories++ ) {
				directoryPath += Constants.fileSeparator + directoriesComponents[ counterDirectories ];
			}
			f = new File( directoryPath );
			f.mkdirs();
		} catch( IOException ex ) {
			Logger.getLogger( JavaDocumentCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void prepareOutputFile( TypeDefinition typeDefinition, Writer writer )
		throws IOException
	{
		StringBuilder outputFileText = new StringBuilder();
		/* appending package */
		outputFileText.append( "package " ).append( packageName ).append( ";\n" );
		/* appending imports */
		appendingImportsIfNecessary( outputFileText, typeDefinition );

		/* writing main class */
		indentation = 0;
		appendingClass( outputFileText, typeDefinition, "Jolie2JavaInterface" );

		writer.append( outputFileText.toString() );
	}

	private void appendingClassBody( TypeDefinition typeDefinition, StringBuilder stringBuilder, String className )
	{

		Set<Entry<String, TypeDefinition>> supportSet = Utils.subTypes( typeDefinition );
		Iterator i = supportSet.iterator();

		/* inserting inner classes if present */
		while( i.hasNext() ) {
			Map.Entry me = (Map.Entry) i.next();
			/* TypeInLineDefinitions are converted into inner classes */
			if ( (((TypeDefinition) me.getValue()) instanceof TypeInlineDefinition) && (Utils.hasSubTypes( (TypeDefinition) me.getValue() )) ) {
				/* opening the inner class */
				appendingIndentation( stringBuilder );
				String clsName = ((TypeDefinition) me.getValue()).id() + TYPESUFFIX;
				stringBuilder.append( "public class " ).append( clsName ).append( " {" ).append( "\n" );
				incrementIndentation();
				appendingClassBody( (TypeDefinition) me.getValue(), stringBuilder, clsName );
				decrementIndentation();
				appendingIndentation( stringBuilder );
				stringBuilder.append( "}\n" );
			}
		}

		/* appending private variables */
		appendingPrivateVariables( stringBuilder, typeDefinition );
		stringBuilder.append( "\n" );

		/* create constructor */
		appendingConstructor( stringBuilder, typeDefinition, className );

		/* create methods */
		appendingMethods( stringBuilder, typeDefinition );

	}

	private void appendingClass( StringBuilder stringBuilder, TypeDefinition typeDefinition, String interfaceToBeImplemented )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public class " ).append( typeDefinition.id() );
		if ( interfaceToBeImplemented != null && Utils.hasSubTypes( typeDefinition ) ) {
			stringBuilder.append( " implements " ).append( interfaceToBeImplemented );
		}

		stringBuilder.append( " {" + "\n" );

		if ( Utils.hasSubTypes( typeDefinition ) ) {
			incrementIndentation();
			appendingClassBody( typeDefinition, stringBuilder, typeDefinition.id() );
			decrementIndentation();
		}

		/* closing main class */
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingImportsIfNecessary( StringBuilder stringBuilder, TypeDefinition type )
	{

		stringBuilder.append( "import jolie.runtime.embedding.Jolie2JavaInterface;\n" );
		//stringBuilder.append( "import ").append( packageName ).append(".*;\n");
		TypeDefinition supportType = type;
		if ( Utils.hasSubTypes( supportType ) ) {
			stringBuilder.append( "import java.util.List;\n" );
			stringBuilder.append( "import java.util.ArrayList;\n" );
			stringBuilder.append( "import jolie.runtime.Value;\n" );
			stringBuilder.append( "import jolie.runtime.ByteArray;\n" );
			stringBuilder.append( "\n" );
		}
	}

	private void appendingIndentation( StringBuilder stringBuilder )
	{
		for( int i = 0; i < indentation; i++ ) {
			stringBuilder.append( "\t" );
		}
	}

	private void appendingPrivateVariables( StringBuilder stringBuilder, TypeDefinition type )
	{

		if ( Utils.hasSubTypes( type ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( type );
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				String variableType = null;
				String variableName = null;
				TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());
				if ( subType instanceof TypeDefinitionLink ) {
					if ( ((TypeDefinitionLink) subType).linkedType() instanceof TypeDefinitionUndefined ) {
						variableName = ((TypeDefinitionLink) subType).id();
						variableType = "Value";
					} else {
						variableName = ((TypeDefinitionLink) subType).id();
						variableType = ((TypeDefinitionLink) subType).linkedTypeName();
					}
				} else if ( subType instanceof TypeInlineDefinition ) {
					if ( Utils.hasSubTypes( subType ) ) {
						variableName = subType.id();
						variableType = variableTypeFromVariableName( variableName );
					} else {
						variableName = subType.id();
						variableType = javaNativeEquivalent.get( ((TypeInlineDefinition) subType).nativeType() );
					}
				} else if ( subType instanceof TypeChoiceDefinition ) {
					System.out.println( "WARNING7: Type definition contains a choice variable which is not supported!" );
					variableName = subType.id();
					variableType = "Object";

				} else {
					System.out.println( "WARNING8: variable is not a Link, a Choice or an Inline Definition!" );
				}

				if ( subType.cardinality().max() > 1 ) {
					appendPrivateVariableList( variableType, variableName, stringBuilder );
				} else {
					appendPrivateVariable( variableType, variableName, stringBuilder );
				}
			}
		}

		if ( Utils.nativeType( type ) != NativeType.VOID ) {
			String variableName = "rootValue";
			String variableType = javaNativeEquivalent.get( Utils.nativeType( type ) );
			appendPrivateVariable( variableType, variableName, stringBuilder );
		}
	}

	private void appendingConstructorWithParameters( StringBuilder stringBuilder, TypeDefinition type, String className )
	{
		//constructor with parameters
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "( Value v ){\n" );

		if ( Utils.hasSubTypes( type ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( type );
			Iterator i = supportSet.iterator();

			incrementIndentation();
			while( i.hasNext() ) {
				TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());
				String variableName = getVariableName( subType );
				String variableNameType = getVariableTypeName( subType );

				// case where there are subnodes
				if ( Utils.hasSubTypes( subType ) ) {
					if ( subType.cardinality().max() > 1 ) {
						/* creating the list object */
						appendingIndentation( stringBuilder );
						stringBuilder.append( subType.id() ).append( "= new ArrayList<" );
						stringBuilder.append( variableNameType );
						stringBuilder.append( ">();" ).append( "\n" );

						/* checking if there are fields in the value */
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if ( v.hasChildren(\"" ).append( variableName );
						stringBuilder.append( "\")){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "for( int counter" ).append( variableName ).append( " = 0;" ).append( "counter" );
						stringBuilder.append( variableName );
						stringBuilder.append( " < v.getChildren( \"" ).append( variableName ).append( "\" ).size(); counter" );
						stringBuilder.append( variableName ).append( "++) { \n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( variableNameType ).append( " support" );
						stringBuilder.append( variableName ).append( " = new " );
						stringBuilder.append( variableNameType );
						stringBuilder.append( "( v.getChildren(\"" ).append( variableName ).append( "\").get(counter" );
						stringBuilder.append( subType.id() ).append( "));\n" );

						appendingIndentation( stringBuilder );
						stringBuilder.append( subType.id() ).append( ".add(support" ).append( variableName ).append( ");\n" );
						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );
					} else {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if (v.hasChildren(\"" ).append( variableName ).append( "\")){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( subType.id() ).append( " = new " ).append( variableNameType );
						stringBuilder.append( "( v.getFirstChild(\"" ).append( variableName ).append( "\"));" ).append( "\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );
					}
				} else {
					// case where there are no subnodes
					//native type
					String nativeTypeName = javaNativeEquivalent.get( Utils.nativeType( subType ) );
					if ( isNativeTypeUndefined( subType ) ) {
						// it is undefined
						nativeTypeName = "Value";
					}
					String javaMethod = javaNativeMethod.get( Utils.nativeType( subType ) );

					// it is a vector
					if ( subType.cardinality().max() > 1 ) {

						appendingIndentation( stringBuilder );
						stringBuilder.append( variableName ).append( "= new ArrayList<" ).append( nativeTypeName ).append( ">();\n" );

						appendingIndentation( stringBuilder );
						stringBuilder.append( "if (v.hasChildren(\"" ).append( variableName ).append( "\")){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "for(int counter" ).append( variableName ).append( "=0;counter" ).append( variableName );
						stringBuilder.append( "<v.getChildren(\"" ).append( variableName ).append( "\").size(); counter" ).append( variableName );
						stringBuilder.append( "++){\n" );
						incrementIndentation();
						if ( Utils.nativeType( subType ) != NativeType.ANY ) {
							appendingIndentation( stringBuilder );
							appendingAddToListNative( stringBuilder, variableName, javaMethod );
						} else if ( nativeTypeName.equals( "Value" ) ) {
							appendingIndentation( stringBuilder );
							appendingAddToListValue( stringBuilder, variableName );
						} else {
							for( NativeType t : NativeType.class.getEnumConstants() ) {
								if ( !javaNativeChecker.containsKey( t ) ) {
									continue;
								}
								appendingIndentation( stringBuilder );
								stringBuilder.append( "if(v.getChildren(\"" ).append( variableName ).append( "\").get(counter" );
								stringBuilder.append( variableName ).append( ")." ).append( javaNativeChecker.get( t ) ).append( "){\n" );

								incrementIndentation();
								appendingIndentation( stringBuilder );
								appendingAddToListNative( stringBuilder, variableName, javaNativeMethod.get( t ) );

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
						// it is a single element
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if (v.hasChildren(\"" ).append( variableName ).append( "\")){\n" );

						incrementIndentation();
						if ( Utils.nativeType( subType ) != NativeType.ANY ) {
							// all the common native types
							appendingIndentation( stringBuilder );
							stringBuilder.append( variableName ).append( "= v.getFirstChild(\"" ).append( variableName ).append( "\")." );
							stringBuilder.append( javaMethod );
							stringBuilder.append( ";\n" );
						} else if ( variableNameType.equals( "Value" ) ) {
							// in case of ANY and in case of undefined
							appendingIndentation( stringBuilder );
							stringBuilder.append( "if(v.getFirstChild(\"" ).append( variableName );
							stringBuilder.append( "\").isDefined()){\n" );

							incrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( variableName ).append( "= v.getFirstChild(\"" ).append( variableName );
							stringBuilder.append( "\")" ).append( ";\n" );

							decrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "}\n" );

						} else {
							for( NativeType t : NativeType.class.getEnumConstants() ) {
								if ( !javaNativeChecker.containsKey( t ) ) {
									continue;
								}
								appendingIndentation( stringBuilder );
								stringBuilder.append( "if(v.getFirstChild(\"" ).append( variableName );
								stringBuilder.append( "\")." ).append( javaNativeChecker.get( t ) ).append( "){\n" );

								incrementIndentation();
								appendingIndentation( stringBuilder );
								stringBuilder.append( variableName ).append( " = v.getFirstChild(\"" ).append( variableName ).append( "\")." );
								stringBuilder.append( javaNativeMethod.get( t ) ).append( ";\n" );

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

				if ( subType instanceof TypeChoiceDefinition ) {
					throw new UnsupportedOperationException( "Can't initialize variable with several possible types" );
				}
			}
			decrementIndentation();
			appendingIndentation( stringBuilder );
			stringBuilder.append( "}\n" );
		}
	}

	private void appendingConstructorWithoutParameters( StringBuilder stringBuilder, TypeDefinition type, String className )
	{
		//constructor without parameters
		stringBuilder.append( "\n\n" );
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( className ).append( "(){\n" );

		incrementIndentation();

		if ( Utils.hasSubTypes( type ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( type );
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());
				String variableName = subType.id();
				String variableType = "Value";
				if ( subType.cardinality().max() > 1 ) {
					if ( subType instanceof TypeDefinitionLink ) {
						if ( !isNativeTypeUndefined( subType ) ) {
							variableType = ((TypeDefinitionLink) subType).linkedType().id();
						}
					} else if ( subType instanceof TypeInlineDefinition ) {
						if ( Utils.hasSubTypes( subType ) ) {
							variableType = subType.id() + "Type";
						} else {
							variableType = javaNativeEquivalent.get( Utils.nativeType( subType ) );
						}
					}
					appendingIndentation( stringBuilder );
					stringBuilder.append( variableName ).append( "= new ArrayList<" ).append( variableType ).append( ">();\n" );
				} 
			}
		}

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );

	}

	private void appendingConstructor( StringBuilder stringBuilder, TypeDefinition type, String className )
	{

		appendingConstructorWithParameters( stringBuilder, type, className );
		appendingConstructorWithoutParameters( stringBuilder, type, className );
	}

	
	private void appendingGetValueMethod( StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/ )
	{
		//method getValue
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public Value getValue(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "Value vReturn = Value.create();\n" );

		if ( Utils.hasSubTypes( type ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( type );
			Iterator i = supportSet.iterator();

			while( i.hasNext() ) {
				TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());
				String variableName = getVariableName( subType );
				String variableNameType = getVariableTypeName( subType );

				if ( Utils.hasSubTypes( subType ) ) {
					//link
					if ( subType.cardinality().max() > 1 ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if(" ).append( variableName ).append( "!=null){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "for(int counter" ).append( variableName ).append( "=0;counter" ).append( variableName );
						stringBuilder.append( "<" ).append( variableName ).append( ".size();counter" ).append( variableName ).append( "++){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getChildren(\"" ).append( variableName ).append( "\").add(" ).append( variableName );
						stringBuilder.append( ".get(counter" ).append( variableName ).append( ").getValue());\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );

					} else {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "if((" ).append( variableName ).append( "!=null)){\n" );

						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getChildren(\"" ).append( variableName ).append( "\").add(" ).append( variableName );
						stringBuilder.append( ".getValue());\n" );

						decrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "}\n" );
					}
				} else if ( subType.cardinality().max() > 1 ) {
					appendingIndentation( stringBuilder );
					stringBuilder.append( "if(" ).append( variableName ).append( "!=null){\n" );

					incrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "for(int counter" ).append( variableName ).append( "=0;counter" ).append( variableName );
					stringBuilder.append( "<" ).append( variableName ).append( ".size();counter" ).append( variableName ).append( "++){\n" );

					incrementIndentation();
					if ( Utils.nativeType( subType ) != NativeType.ANY ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" ).append( variableName );
						stringBuilder.append( ".get(counter" ).append( variableName ).append( "));\n" );

					} else if ( isNativeTypeUndefined( subType ) ) {
						incrementIndentation();
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" );
						stringBuilder.append( variableName ).append( ".get(counter" ).append( variableName ).append( "));\n" );
						decrementIndentation();
					} else {
						for( NativeType t : NativeType.class.getEnumConstants() ) {
							if ( !javaNativeChecker.containsKey( t ) ) {
								continue;
							}
							appendingIndentation( stringBuilder );
							stringBuilder.append( "if(" ).append( variableName ).append( ".get(counter" ).append( variableName );
							stringBuilder.append( ") instanceof " ).append( javaNativeEquivalent.get( t ) ).append( "){\n" );

							incrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" );
							stringBuilder.append( variableName ).append( ".get(counter" ).append( variableName ).append( "));\n" );

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
					stringBuilder.append( "if((" ).append( variableName ).append( "!=null)){\n" );
					incrementIndentation();

					if ( Utils.nativeType( subType ) != NativeType.ANY ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" );
						stringBuilder.append( subType.id() ).append( ");\n" );
					} else if ( variableNameType.equals( "Value" ) ) {
						appendingIndentation( stringBuilder );
						stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" );
						stringBuilder.append( variableName ).append( ");\n" );
					} else {
						for( NativeType t : NativeType.class.getEnumConstants() ) {
							if ( !javaNativeChecker.containsKey( t ) ) {
								continue;
							}
							appendingIndentation( stringBuilder );
							stringBuilder.append( "if(" ).append( variableName ).append( " instanceof " );
							stringBuilder.append( javaNativeEquivalent.get( t ) ).append( "){\n" );

							incrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "vReturn.getNewChild(\"" ).append( variableName ).append( "\").setValue(" );
							stringBuilder.append( variableName ).append( ");\n" );

							decrementIndentation();
							appendingIndentation( stringBuilder );
							stringBuilder.append( "}\n" );
						}
					}
					decrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "}\n" );
				}

				if ( subType instanceof TypeChoiceDefinition ) {

				}
			}
		}

		if ( Utils.nativeType( type ) != NativeType.VOID ) {
			appendingIndentation( stringBuilder );
			stringBuilder.append( "if((rootValue!=null)){\n" );

			incrementIndentation();
			if ( Utils.nativeType( type ) != NativeType.ANY ) {
				appendingIndentation( stringBuilder );
				stringBuilder.append( "vReturn.setValue(rootValue);\n" );
			} else {
				for( NativeType t : getTypes( type ) ) {
					if ( !javaNativeChecker.containsKey( t ) ) {
						continue;
					}
					appendingIndentation( stringBuilder );
					stringBuilder.append( "if(rootValue instanceof " ).append( javaNativeEquivalent.get( t ) ).append( "){\n" );

					incrementIndentation();
					appendingIndentation( stringBuilder );
					stringBuilder.append( "vReturn.setValue(rootValue);\n" );

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
		stringBuilder.append( "return vReturn;\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n" );
	}

	private void appendingMethods( StringBuilder stringBuilder, TypeDefinition type/*, boolean naturalType*/ )
	{

		if ( Utils.hasSubTypes( type ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( type );
			Iterator i = supportSet.iterator();

			while( i.hasNext() ) {
				TypeDefinition subType = (TypeDefinition) (((Map.Entry) i.next()).getValue());

				String variableName = getVariableName( subType );
				String startingChar = variableName.substring( 0, 1 );
				String variableNameCapitalized = startingChar.toUpperCase().concat( variableName.substring( 1, variableName.length() ) );
				if ( variableNameCapitalized.equals( "Value" ) ) {
					variableNameCapitalized = "__Value";
				}
				String variableNameType = getVariableTypeName( subType );

				if ( Utils.hasSubTypes( subType ) ) {
					//link
					if ( subType.cardinality().max() > 1 ) {

						// get
						appendGetMethodWithIndex( stringBuilder, variableNameType, variableNameCapitalized, variableName );

						// size
						appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

						// add
						appendAddMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );

						// remove
						appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );

					} else {
						// get
						appendGetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
						//
						appendSetMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );
					}
				} else {
					//native type

					String nativeTypeName = javaNativeEquivalent.get( Utils.nativeType( subType ) );

					if ( Utils.nativeType( subType ) != NativeType.VOID ) {

						if ( subType.cardinality().max() > 1 ) {
							if ( variableNameType.equals( "Value" ) ) {
								appendGetMethodWithIndex( stringBuilder, variableNameType, variableNameCapitalized, variableName );

								// size
								appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

								// add
								appendAddMethod( stringBuilder, variableNameType, variableNameCapitalized, variableName );

								// remove
								appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );
							} else {
								appendGetMethodWithIndex( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );

								// size
								appendGetMethodSize( stringBuilder, variableNameCapitalized, variableName );

								// add
								appendAddMethod( stringBuilder, nativeTypeName, variableNameCapitalized, variableName );

								// remove
								appendRemoveMethod( stringBuilder, variableNameCapitalized, variableName );
							}

						} else if ( variableNameType.equals( "Value" ) ) {
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

				if ( subType instanceof TypeChoiceDefinition ) {
					//How to manage creating getters and setters for variable that can be initialized with several types?
					//public <type1> get <variable>
					//public <type2> get <variable>

				}
			}
		}

		appendingGetValueMethod( stringBuilder, type );
	}

	/*
				if ( Utils.nativeType( type ) != NativeType.VOID ) {

					String nativeTypeName = javaNativeEquivalent.get( Utils.nativeType( type ) );

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
			}
		}

		

	}
	 */
	private void parseSubType( TypeDefinition typeDefinition )
	{
		if ( Utils.hasSubTypes( typeDefinition ) ) {
			Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes( typeDefinition );
			Iterator i = supportSet.iterator();
			while( i.hasNext() ) {
				Map.Entry me = (Map.Entry) i.next();
				//System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
				if ( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink ) {
					if ( !subTypeMap.containsKey( ((TypeDefinitionLink) me.getValue()).linkedTypeName() ) ) {
						subTypeMap.put( ((TypeDefinitionLink) me.getValue()).linkedTypeName(), ((TypeDefinitionLink) me.getValue()).linkedType() );
						parseSubType( ((TypeDefinitionLink) me.getValue()).linkedType() );
					}
				}
			}
		}
	}

	private Set<NativeType> getTypes( TypeDefinition typeDefinition )
	{
		Set<NativeType> choiceTypes = new HashSet<>();
		if ( typeDefinition instanceof TypeChoiceDefinition ) {
			choiceTypes = getTypes( ((TypeChoiceDefinition) typeDefinition).left() );
			Set<NativeType> right = getTypes( ((TypeChoiceDefinition) typeDefinition).right() );
			if ( right != null ) {
				choiceTypes.addAll( right );
			}
		} else if ( typeDefinition instanceof TypeDefinitionLink ) {
			return getTypes( ((TypeDefinitionLink) typeDefinition).linkedType() );
		} else if ( typeDefinition instanceof TypeInlineDefinition ) {
			choiceTypes.add( ((TypeInlineDefinition) typeDefinition).nativeType() );
		}
		return choiceTypes;
	}

	private boolean isNativeTypeUndefined( String t )
	{
		return t.equals( "undefined" );
	}

	private boolean isNativeTypeUndefined( TypeDefinition t )
	{
		if ( t instanceof TypeDefinitionLink ) {
			TypeDefinitionLink tLink = (TypeDefinitionLink) t;
			if ( ((TypeDefinitionLink) t).linkedType() instanceof TypeDefinitionUndefined ) {
				return true;
			}
		}
		return false;
	}

	private String variableTypeFromVariableName( String variableName )
	{
		return variableName.concat( TYPESUFFIX );
	}

	private void appendPrivateVariable( String variableType, String variableName, StringBuilder stringBuilder )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "private " ).append( variableType ).append( " " ).append( variableName ).append( ";\n" );
	}

	private void appendPrivateVariableList( String variableType, String variableName, StringBuilder stringBuilder )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "private ArrayList<" ).append( variableType ).append( "> " ).append( variableName ).append( ";\n" );
	}

	private void appendGetMethodWithIndex( StringBuilder stringBuilder, String typeName, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( typeName );
		stringBuilder.append( " get" ).append( variableNameCapitalized ).append( "Value( int index ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( variableName ).append( ".get(index);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendGetMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( typeName );
		stringBuilder.append( " get" ).append( variableNameCapitalized ).append( "(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( variableName ).append( ";\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendGetMethodSize( StringBuilder stringBuilder, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public " ).append( "int get" ).append( variableNameCapitalized ).append( "Size(){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "return " ).append( variableName ).append( ".size();\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendAddMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void add" ).append( variableNameCapitalized ).append( "Value( " );
		stringBuilder.append( typeName ).append( " value ){\n" );
		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( variableName ).append( ".add(value);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendSetMethod( StringBuilder stringBuilder, String typeName, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void set" ).append( variableNameCapitalized ).append( "( " );
		stringBuilder.append( typeName ).append( " value ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( variableName ).append( " = value;\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendRemoveMethod( StringBuilder stringBuilder, String variableNameCapitalized, String variableName )
	{
		appendingIndentation( stringBuilder );
		stringBuilder.append( "public void remove" ).append( variableNameCapitalized ).append( "Value( int index ){\n" );

		incrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( variableName ).append( ".remove(index);\n" );

		decrementIndentation();
		appendingIndentation( stringBuilder );
		stringBuilder.append( "}\n\n" );
	}

	private void appendingAddToListNative( StringBuilder stringBuilder, String variableName, String javaMethod )
	{
		stringBuilder.append( variableName ).append( ".add(v.getChildren(\"" );
		stringBuilder.append( variableName ).append( "\").get(counter" ).append( variableName ).append( ")." );
		stringBuilder.append( javaMethod ).append( ");\n" );
	}

	private void appendingAddToListValue( StringBuilder stringBuilder, String variableName )
	{
		stringBuilder.append( variableName ).append( ".add(v.getChildren(\"" );
		stringBuilder.append( variableName ).append( "\").get(counter" ).append( variableName ).append( "));\n" );
	}

	private void incrementIndentation()
	{
		indentation = indentation + INDENTATION_STEP;
	}

	private void decrementIndentation()
	{
		indentation = indentation - INDENTATION_STEP;
	}

	private String getVariableName( TypeDefinition type )
	{
		if ( type instanceof TypeDefinitionLink ) {
			if ( ((TypeDefinitionLink) type).linkedType() instanceof TypeDefinitionUndefined ) {
				return ((TypeDefinitionLink) type).id();
			} else {
				return ((TypeDefinitionLink) type).id();
			}
		} else if ( type instanceof TypeInlineDefinition ) {
			if ( Utils.hasSubTypes( type ) ) {
				return type.id();
			} else {
				return type.id();
			}
		} else if ( type instanceof TypeChoiceDefinition ) {
			System.out.println( "WARNING: Type definition contains a choice variable which is not supported!" );
			return type.id();
		} else {
			System.out.println( "WARNING5: variable is not a Link, a Choice or an Inline Definition!" );
		}
		return "err";
	}

	private String getVariableTypeName( TypeDefinition type )
	{
		if ( type instanceof TypeDefinitionLink ) {
			if ( ((TypeDefinitionLink) type).linkedType() instanceof TypeDefinitionUndefined ) {
				return "Value";
			} else {
				return ((TypeDefinitionLink) type).linkedTypeName();
			}
		} else if ( type instanceof TypeInlineDefinition ) {
			if ( Utils.hasSubTypes( type ) ) {
				return variableTypeFromVariableName( getVariableName( type ) );
			} else {
				return javaNativeEquivalent.get( ((TypeInlineDefinition) type).nativeType() );
			}
		} else if ( type instanceof TypeChoiceDefinition ) {
			System.out.println( "WARNING: Type definition contains a choice variable which is not supported!" );
			return "Value";
		} else {
			System.out.println( "WARNING6: variable is not a Link, a Choice or an Inline Definition!" );
		}
		return "err";
	}

}
