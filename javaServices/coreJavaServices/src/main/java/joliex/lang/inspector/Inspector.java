/*
 * Copyright (C) 2019 Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 * Copyright (C) 2019 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package joliex.lang.inspector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.Interpreter;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.Interfaces;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

public class Inspector extends JavaService {
	private static final class PortInspectionResponse {
		private static final String INPUT_PORT = "inputPorts";
		private static final String OUTPUT_PORT = "outputPorts";
		private static final String REFERRED_TYPES = "referredTypes";
	}

	private static final class PortInfoType {
		private static final String NAME = "name";
		private static final String LOCATION = "location";
		private static final String PROTOCOL = "protocol";
		private static final String INTERFACE = "interfaces";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class InterfaceInfoType {
		private static final String NAME = "name";
		private static final String OPERATION = "operations";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class OperationInfoType {
		private static final String NAME = "name";
		private static final String REQUEST_TYPE = "requestType";
		private static final String RESPONSE_TYPE = "responseType";
		private static final String FAULT = "faults";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class TypeDefinitionInfoType {
		private static final String NAME = "name";
		private static final String TYPE = "type";
	}

	private static final class TypeInfoType {
		private static final String DOCUMENTATION = "documentation";
		private static final String FIELDS = "fields";
		private static final String UNTYPED_FIELDS = "untypedFields";
		private static final String NATIVE_TYPE = "nativeType";
		private static final String LINKED_TYPE_NAME = "linkedTypeName";
		private static final String LEFT = "left";
		private static final String RIGHT = "right";
	}

	private static final class FaultInfoType {
		private static final String NAME = "name";
		private static final String TYPE = "type";
	}

	private static final class FieldType {
		private static final String NAME = "name";
		private static final String RANGE = "range";
		private static final String TYPE = "type";
		private static final String MIN = "min";
		private static final String MAX = "max";
	}

	@RequestResponse
	public Value inspectPorts( Value request ) throws FaultException {
		try {
			final ProgramInspector inspector;
			String[] includePaths =
				request.getChildren( "includePaths" ).stream().map( Value::strValue ).toArray( String[]::new );
			if( request.hasChildren( "source" ) ) {
				inspector = getInspector( request.getFirstChild( "filename" ).strValue(),
					Optional.of( request.getFirstChild( "source" ).strValue() ), includePaths );
			} else {
				inspector =
					getInspector( request.getFirstChild( "filename" ).strValue(), Optional.empty(), includePaths );
			}
			return buildPortInspectionResponse( inspector );
		} catch( CommandLineException | IOException | ParserException | ModuleException ex ) {
			throw new FaultException( ex );
		} catch( SemanticException ex ) {
			throw new FaultException(
				"SemanticException",
				ex.getErrorList().stream().map( SemanticException.SemanticError::getMessage )
					.collect( Collectors.joining( "\n" ) ) );
		}
	}

	@RequestResponse
	public Value inspectTypes( Value request ) throws FaultException {
		String[] includePaths =
			request.getChildren( "includePaths" ).stream().map( Value::strValue ).toArray( String[]::new );
		try {
			ProgramInspector inspector =
				getInspector( request.getFirstChild( "filename" ).strValue(), Optional.empty(), includePaths );
			return buildProgramTypeInfo( inspector );
		} catch( CommandLineException | IOException | ParserException | ModuleException ex ) {
			throw new FaultException( ex );
		} catch( SemanticException ex ) {
			throw new FaultException(
				"SemanticException",
				ex.getErrorList().stream().map( SemanticException.SemanticError::getMessage )
					.collect( Collectors.joining( "\n" ) ) );
		}
	}

	private static ProgramInspector getInspector( String filename, Optional< String > source, String[] includePaths )
		throws CommandLineException, IOException, ParserException, SemanticException, ModuleException {
		SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
		configuration.setCheckForMain( false );
		String[] args = { filename };
		Interpreter.Configuration interpreterConfiguration =
			new CommandLineParser( args, Inspector.class.getClassLoader() ).getInterpreterConfiguration();
		final InputStream sourceIs;
		if( source.isPresent() ) {
			sourceIs = new ByteArrayInputStream( source.get().getBytes() );
		} else {
			sourceIs = interpreterConfiguration.inputStream();
		}
		Program program = ParsingUtils.parseProgram(
			sourceIs,
			interpreterConfiguration.programFilepath().toURI(),
			interpreterConfiguration.charset(),
			includePaths,
			interpreterConfiguration.packagePaths(),
			interpreterConfiguration.jolieClassLoader(),
			interpreterConfiguration.constants(),
			configuration,
			true );
		return ParsingUtils.createInspector( program );
	}

	private static Value buildPortInspectionResponse( ProgramInspector inspector ) {
		Value result = Value.create();
		ValueVector inputPorts = result.getChildren( PortInspectionResponse.INPUT_PORT );
		ValueVector outputPorts = result.getChildren( PortInspectionResponse.OUTPUT_PORT );
		ValueVector referredTypesValues = result.getChildren( PortInspectionResponse.REFERRED_TYPES );

		Set< String > referredTypes = new HashSet<>();
		for( InputPortInfo portInfo : inspector.getInputPorts() ) {
			inputPorts.add( buildPortInfo( portInfo, inspector, referredTypes ) );
		}

		for( OutputPortInfo portInfo : inspector.getOutputPorts() ) {
			outputPorts.add( buildPortInfo( portInfo, referredTypes ) );
		}

		Map< String, TypeDefinition > types = new HashMap<>();
		for( TypeDefinition t : inspector.getTypes() ) {
			types.put( t.id(), t );
		}

		referredTypes.stream().filter( types::containsKey )
			.forEach( typeName -> referredTypesValues.add( buildTypeDefinition( types.get( typeName ) ) ) );

		return result;
	}

	private static Value buildTypeDefinition( TypeDefinition typeDef ) {
		Value result = Value.create();
		result.setFirstChild( TypeDefinitionInfoType.NAME, typeDef.id() );
		result.getChildren( TypeDefinitionInfoType.TYPE ).add( buildTypeInfo( typeDef ) );
		return result;
	}

	private static Value buildProgramTypeInfo( ProgramInspector inspector ) {
		Value returnValue = Value.create();
		ValueVector types = ValueVector.create();
		returnValue.children().put( "types", types );
		for( TypeDefinition type : inspector.getTypes() ) {
			Value typeDefinition = Value.create();
			typeDefinition.setFirstChild( TypeDefinitionInfoType.NAME, type.id() );
			typeDefinition.getChildren( TypeDefinitionInfoType.TYPE ).add( buildTypeInfo( type ) );
			types.add( typeDefinition );
		}
		return returnValue;
	}

	private static Value buildPortInfo( InputPortInfo portInfo, ProgramInspector inspector,
		Set< String > referredTypesSet ) {
		Value result = Value.create();
		result.setFirstChild( PortInfoType.NAME, portInfo.id() );

		if( portInfo.location() != null ) {
			result.setFirstChild( PortInfoType.LOCATION, portInfo.location().toString() );
		}
		if( portInfo.protocol() != null ) {
			result.setFirstChild( PortInfoType.PROTOCOL, portInfo.protocolId() );
		}
		if( portInfo.getDocumentation() != null ) {
			result.setFirstChild( PortInfoType.DOCUMENTATION, portInfo.getDocumentation() );
		}

		ValueVector interfaces = result.getChildren( PortInfoType.INTERFACE );

		portInfo.getInterfaceList().forEach( i -> interfaces.add( buildInterfaceInfo( i, referredTypesSet ) ) );

		getAggregatedInterfaces( portInfo, inspector )
			.forEach( i -> interfaces.add( buildInterfaceInfo( i, referredTypesSet ) ) );

		return result;
	}

	private static ArrayList< InterfaceDefinition > getAggregatedInterfaces( InputPortInfo portInfo,
		ProgramInspector inspector ) {
		ArrayList< InterfaceDefinition > returnList = new ArrayList<>();
		for( InputPortInfo.AggregationItemInfo aggregationItemInfo : portInfo.aggregationList() ) {
			for( String outputPortName : aggregationItemInfo.outputPortList() ) {
				OutputPortInfo outputPortInfo = Arrays.stream( inspector.getOutputPorts() )
					.filter( ( outputPort ) -> outputPort.id().equals( outputPortName ) )
					.findFirst().get();
				outputPortInfo.getInterfaceList().forEach( ( aggregatedInterfaceInfo ) -> returnList.add(
					Interfaces.extend( aggregatedInterfaceInfo, aggregationItemInfo.interfaceExtender(),
						portInfo.id() ) ) );
			}
		}
		return returnList;
	}

	private static Value buildPortInfo( OutputPortInfo portInfo, Set< String > referredTypesSet ) {
		Value result = Value.create();
		result.setFirstChild( PortInfoType.NAME, portInfo.id() );

		if( portInfo.location() != null ) {
			result.setFirstChild( PortInfoType.LOCATION, portInfo.location().toString() );
		}
		if( portInfo.protocol() != null ) {
			result.setFirstChild( PortInfoType.PROTOCOL, portInfo.protocolId() );
		}
		if( portInfo.getDocumentation() != null ) {
			result.setFirstChild( PortInfoType.DOCUMENTATION, portInfo.getDocumentation() );
		}

		ValueVector interfaces = result.getChildren( PortInfoType.INTERFACE );

		portInfo.getInterfaceList().forEach( i -> interfaces.add( buildInterfaceInfo( i, referredTypesSet ) ) );

		return result;
	}

	private static Value buildInterfaceInfo( InterfaceDefinition interfaceDefinition, Set< String > referredTypesSet ) {
		Value result = Value.create();
		result.setFirstChild( InterfaceInfoType.NAME, interfaceDefinition.name() );
		if( interfaceDefinition.getDocumentation() != null ) {
			result.setFirstChild( InterfaceInfoType.DOCUMENTATION, interfaceDefinition.getDocumentation() );
		}
		ValueVector operations = result.getChildren( InterfaceInfoType.OPERATION );
		interfaceDefinition.operationsMap().entrySet()
			.forEach( o -> operations.add( buildOperationInfo( o.getValue(), referredTypesSet ) ) );
		return result;
	}

	private static Value buildOperationInfo( OperationDeclaration operationDeclaration,
		Set< String > referredTypesSet ) {
		Value result = Value.create();
		result.setFirstChild( OperationInfoType.NAME, operationDeclaration.id() );
		if( operationDeclaration.getDocumentation() != null ) {
			result.setFirstChild( OperationInfoType.DOCUMENTATION, operationDeclaration.getDocumentation() );
		}
		if( operationDeclaration instanceof RequestResponseOperationDeclaration ) {
			RequestResponseOperationDeclaration rrod = (RequestResponseOperationDeclaration) operationDeclaration;
			result.setFirstChild( OperationInfoType.REQUEST_TYPE, rrod.requestType().id() );
			referredTypesSet.add( rrod.requestType().id() );
			result.setFirstChild( OperationInfoType.RESPONSE_TYPE, rrod.responseType().id() );
			referredTypesSet.add( rrod.responseType().id() );
			if( !rrod.faults().isEmpty() ) {
				ValueVector faults = result.getChildren( OperationInfoType.FAULT );
				rrod.faults().forEach( ( faultName, faultType ) -> {
					Value faultInfo = Value.create();
					faultInfo.setFirstChild( FaultInfoType.NAME, faultName );
					faultInfo.setFirstChild( FaultInfoType.TYPE, faultType.id() );
					faults.add( faultInfo );
					referredTypesSet.add( faultType.id() );
				} );
			}
		} else {
			OneWayOperationDeclaration owd = (OneWayOperationDeclaration) operationDeclaration;
			result.setFirstChild( OperationInfoType.REQUEST_TYPE, owd.requestType().id() );
			referredTypesSet.add( owd.requestType().id() );
		}
		return result;
	}

	private static Value buildTypeInfo( TypeDefinition t ) {
		Value result = Value.create();
		if( t.getDocumentation() != null ) {
			result.setFirstChild( TypeInfoType.DOCUMENTATION, t.getDocumentation() );
		}

		if( t instanceof TypeDefinitionLink ) {
			result.setFirstChild( TypeInfoType.LINKED_TYPE_NAME, ((TypeDefinitionLink) t).linkedTypeName() );
		} else if( t instanceof TypeChoiceDefinition ) {
			TypeChoiceDefinition tc = (TypeChoiceDefinition) t;
			result.getChildren( TypeInfoType.LEFT ).add( buildTypeInfo( tc.left() ) );
			result.getChildren( TypeInfoType.RIGHT ).add( buildTypeInfo( tc.right() ) );
		} else if( t instanceof TypeInlineDefinition ) {
			TypeInlineDefinition ti = (TypeInlineDefinition) t;
			result.setFirstChild( TypeInfoType.NATIVE_TYPE, ti.nativeType().id() );
			result.setFirstChild( TypeInfoType.UNTYPED_FIELDS, ti.untypedSubTypes() );
			if( ti.hasSubTypes() ) {
				ValueVector fields = result.getChildren( TypeInfoType.FIELDS );
				ti.subTypes().forEach( entry -> {
					Value field = Value.create();
					field.setFirstChild( FieldType.NAME, entry.getKey() );

					Value range = field.getFirstChild( FieldType.RANGE );
					range.setFirstChild( FieldType.MIN, entry.getValue().cardinality().min() );
					range.setFirstChild( FieldType.MAX, entry.getValue().cardinality().max() );

					field.getChildren( FieldType.TYPE ).add( buildTypeInfo( entry.getValue() ) );

					fields.add( field );
				} );
			}
		}

		return result;
	}
}
