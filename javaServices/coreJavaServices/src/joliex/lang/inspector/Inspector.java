/*******************************************************************************
 *   Copyright (C) 2019 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/
package joliex.lang.inspector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.NativeType;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.Interfaces;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

public class Inspector extends JavaService
{
	private static final class ProgramInfoType
	{
		private static final String PORT = "port";
		private static final String SUBTYPE = "subtype";
	}

	private static final class PortInfoType
	{
		private static final String NAME = "name";
		private static final String LOCATION = "location";
		private static final String PROTOCOL = "protocol";
		private static final String INTERFACE = "interface";
		private static final String IS_OUTPUT = "isOutput";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class InterfaceInfoType
	{
		private static final String NAME = "name";
		private static final String OPERATION = "operation";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class OperationInfoType
	{
		private static final String NAME = "name";
		private static final String REQUEST_TYPE = "requestType";
		private static final String RESPONSE_TYPE = "responseType";
		private static final String FAULT = "fault";
		private static final String DOCUMENTATION = "documentation";
	}

	private static final class TypeInfoType
	{
		private static final String NAME = "name";
		private static final String IS_NATIVE = "isNative";
		private static final String IS_CHOICE = "isChoice";
		private static final String CODE = "code";
		private static final String DOCUMENTATION = "documentation";
		private static final String SUBTYPE = "subtype";
		private static final String ROOT_TYPE = "rootType";
		private static final String UNDEFINED_SUBTYPES = "undefinedSubtypes";
	}

	private static final class FaultInfoType
	{
		private static final String NAME = "name";
		private static final String TYPE = "faultType";
	}

	private static final String TYPE_DECLARATION_TOKEN = "type";
	private static final String UNDEFINED_TYPE = "undefined";
	private static final String TYPE_DEFINITION_TOKEN = ":";
	private static final String TYPE_CHOICE_TOKEN = "|";
	private static final String TYPE_SUBTYPE_OPEN = "{";
	private static final String TYPE_SUBTYPE_CLOSE = "}";
	private static final String TYPE_SUBTYPE_DEFINITON = ".";
	private static final String TYPE_CARDINALITY_OPEN = "[";
	private static final String TYPE_CARDINALITY_CLOSE = "]";
	private static final String TYPE_CARDINALITY_SEPARATOR = ",";
	private static final String TYPE_CARDINALITY_ZERO_TO_ONE = "?";
	private static final String TYPE_CARDINALITY_ZERO_TO_MANY = "*";
	private static final String TAB = "\t";
	private static final String NEW_LINE = "\n";
	
	@RequestResponse
	public Value inspectProgram( Value request ) throws FaultException {
		try {
			ProgramInspector inspector = getInspector( request.getFirstChild( "filename" ).strValue() );
			return buildProgramInfo( inspector );
		} catch( CommandLineException | IOException | ParserException ex ) {
			throw new FaultException( ex );
		} catch ( SemanticException ex ){
			throw new FaultException( 
				ex.getErrorList().stream().map( e -> e.getMessage() ).collect( Collectors.joining( "\n" ) )
				, ex 
			);
		}
	}
	
	@RequestResponse
	public Value inspectTypes( Value request ) throws FaultException {
		try {
			ProgramInspector inspector = getInspector( request.getFirstChild( "filename" ).strValue() );
			return buildProgramTypeInfo( inspector );
		} catch( CommandLineException | IOException | ParserException ex ) {
			throw new FaultException( ex );
		} catch ( SemanticException ex ){
			throw new FaultException( 
				ex.getErrorList().stream().map( e -> e.getMessage() ).collect( Collectors.joining( "\n" ) )
				, ex 
			);
		}
	}
	
		
	private static ProgramInspector getInspector( String filename ) throws CommandLineException, IOException, ParserException, SemanticException{
		SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
		configuration.setCheckForMain( false );
		CommandLineParser commandLineParser;
		String[] args = { filename };
		commandLineParser = new CommandLineParser( args, Inspector.class.getClassLoader() );
		Program program = ParsingUtils.parseProgram(
				commandLineParser.programStream(),
				commandLineParser.programFilepath().toURI(),
				commandLineParser.charset(),
				commandLineParser.includePaths(),
				commandLineParser.jolieClassLoader(),
				commandLineParser.definedConstants(),
				configuration,
				true
		);
		return ParsingUtils.createInspector( program );
	}
	
	private static Value buildProgramInfo( ProgramInspector inspector )
	{

		Value returnValue = Value.create();
		ValueVector ports = ValueVector.create();
		returnValue.children().put( ProgramInfoType.PORT, ports );

		for ( InputPortInfo portInfo : inspector.getInputPorts() ) {
			ports.add( buildPortInfo( portInfo, inspector ) );
		}

		for ( OutputPortInfo portInfo : inspector.getOutputPorts() ) {
			ports.add( buildPortInfo( portInfo, inspector ) );
		}

		return returnValue;
	}
	
	private static Value buildProgramTypeInfo( ProgramInspector inspector ){
		Value returnValue = Value.create();
		ValueVector types = ValueVector.create();
		returnValue.children().put( "type", types );
		for( TypeDefinition type : inspector.getTypes() ){
			types.add( buildTypeInfo( type) );
		}
		return returnValue;
	}

	private static Value buildPortInfo( InputPortInfo portInfo, ProgramInspector inspector )
	{
		Value returnValue = Value.create();
		returnValue.setFirstChild( PortInfoType.NAME, portInfo.id() );
		returnValue.setFirstChild( PortInfoType.IS_OUTPUT, false );
		if ( portInfo.location() != null ) {
			returnValue.setFirstChild( PortInfoType.LOCATION, portInfo.location().toString() );
		}
		if ( portInfo.protocolId() != null ) {
			returnValue.setFirstChild( PortInfoType.PROTOCOL, portInfo.protocolId() );
		}
		if ( portInfo.getDocumentation() != null ) {
			returnValue.setFirstChild( PortInfoType.DOCUMENTATION, portInfo.getDocumentation() );
		}
		ValueVector interfaces = ValueVector.create();
		returnValue.children().put( PortInfoType.INTERFACE, interfaces );
		portInfo.getInterfaceList().forEach( ( i ) -> {
			interfaces.add( buildInterfaceInfo( i ) );
		} );
		getAggregatedInterfaces( portInfo, inspector ).forEach( ( i ) -> {
			interfaces.add( buildInterfaceInfo( i ));
		} );
		ValueVector subtypes = ValueVector.create();
		returnValue.children().put( ProgramInfoType.SUBTYPE, subtypes );
		Set<String> subtypesSet = new HashSet<>();
		buildSubTypes( portInfo, subtypes, subtypesSet, inspector );

		return returnValue;
	}
	
	private static ArrayList<InterfaceDefinition> getAggregatedInterfaces( InputPortInfo portInfo, ProgramInspector inspector ){
		ArrayList<InterfaceDefinition> returnList = new ArrayList<>();
		for ( InputPortInfo.AggregationItemInfo aggregationItemInfo : portInfo.aggregationList() ) {
			for ( String outputPortName : aggregationItemInfo.outputPortList() ) {
				OutputPortInfo outputPortInfo
					= Arrays.stream( inspector.getOutputPorts() )
						.filter( ( outputPort ) -> outputPort.id().equals( outputPortName ) )
						.findFirst().get();
				outputPortInfo.getInterfaceList().forEach( ( aggregatedInterfaceInfo ) -> {
					returnList.add(
						Interfaces.extend( aggregatedInterfaceInfo, aggregationItemInfo.interfaceExtender(), portInfo.id() )
					);
				} );
			}
		}
		return returnList;
	}

	private static Value buildPortInfo( OutputPortInfo portInfo, ProgramInspector inspector )
	{
		Value returnValue = Value.create();
		returnValue.setFirstChild( PortInfoType.NAME, portInfo.id() );
		returnValue.setFirstChild( PortInfoType.IS_OUTPUT, true );
		if ( portInfo.location() != null ) {
			returnValue.setFirstChild( PortInfoType.LOCATION, portInfo.location() );
		}
		if ( portInfo.protocolId() != null ) {
			returnValue.setFirstChild( PortInfoType.PROTOCOL, portInfo.protocolId() );
		}
		if ( portInfo.getDocumentation() != null ) {
			returnValue.setFirstChild( PortInfoType.DOCUMENTATION, portInfo.getDocumentation() );
		}
		ValueVector interfaces = ValueVector.create();
		returnValue.children().put( PortInfoType.INTERFACE, interfaces );
		portInfo.getInterfaceList().forEach( ( i ) -> {
			interfaces.add( buildInterfaceInfo( i ) );
		} );
		ValueVector subtypes = ValueVector.create();
		returnValue.children().put( ProgramInfoType.SUBTYPE, subtypes );
		Set<String> subtypesSet = new HashSet<>();
		buildSubTypes( portInfo, subtypes, subtypesSet, inspector );
		return returnValue;
	}

	private static Value buildInterfaceInfo( InterfaceDefinition interfaceDefinition )
	{
		Value returnValue = Value.create();
		returnValue.setFirstChild( InterfaceInfoType.NAME, interfaceDefinition.name() );
		if ( interfaceDefinition.getDocumentation() != null ) {
			returnValue.setFirstChild( InterfaceInfoType.DOCUMENTATION, interfaceDefinition.getDocumentation() );
		}
		ValueVector operations = ValueVector.create();
		returnValue.children().put( InterfaceInfoType.OPERATION, operations );
		interfaceDefinition.operationsMap().entrySet().forEach( ( o ) -> {
			operations.add( buildOperationInfo( o.getValue() ) );
		} );
		return returnValue;
	}

	private static Value buildOperationInfo( OperationDeclaration operationDeclaration )
	{
		Value returnValue = Value.create();
		returnValue.setFirstChild( OperationInfoType.NAME, operationDeclaration.id() );
		if ( operationDeclaration.getDocumentation() != null ) {
			returnValue.setFirstChild( OperationInfoType.DOCUMENTATION, operationDeclaration.getDocumentation() );
		}
		if ( operationDeclaration instanceof RequestResponseOperationDeclaration ) {
			RequestResponseOperationDeclaration rrod = (RequestResponseOperationDeclaration) operationDeclaration;
			returnValue.getChildren( OperationInfoType.REQUEST_TYPE ).add( buildTypeInfo( rrod.requestType() ) );
			returnValue.getChildren( OperationInfoType.RESPONSE_TYPE ).add( buildTypeInfo( rrod.responseType() ) );
			if ( rrod.faults().size() > 0 ) {
				ValueVector faults = ValueVector.create();
				returnValue.children().put( OperationInfoType.FAULT, faults );
				rrod.faults().entrySet().forEach( ( fault ) -> {
					faults.add( buildFaultInfo( fault ) );
				} );
			}
		} else {
			OneWayOperationDeclaration owd = (OneWayOperationDeclaration) operationDeclaration;
			returnValue.getChildren( OperationInfoType.REQUEST_TYPE ).add( buildTypeInfo( owd.requestType() ) );
		}
		return returnValue;
	}

	private static Value buildFaultInfo( Entry<String, TypeDefinition> fault )
	{
		Value returnValue = Value.create();
		returnValue.setFirstChild( FaultInfoType.NAME, fault.getKey() );
		returnValue.getChildren( FaultInfoType.TYPE ).add( buildTypeInfo( fault.getValue() ) );
		return returnValue;
	}
	
	private static Value buildTypeInfo( TypeDefinition typeDefinition ){
		return buildTypeInfo( typeDefinition, true );
	}
	
	private static Value buildTypeInfo( TypeDefinition typeDefinition, boolean addCode )
	{
		Value returnValue = Value.create();
		if( !( typeDefinition instanceof TypeInlineDefinition ) ){
			returnValue.setFirstChild( TypeInfoType.NAME, typeDefinition.id() );
			returnValue.setFirstChild( TypeInfoType.IS_NATIVE, false );
		} else {
			TypeInlineDefinition tid = ( TypeInlineDefinition ) typeDefinition;
			returnValue.setFirstChild( TypeInfoType.NAME, tid.id() );
			if( tid.hasSubTypes() || tid.untypedSubTypes() ){
				returnValue.setFirstChild( TypeInfoType.IS_NATIVE, false );
			} else {
				returnValue.setFirstChild( TypeInfoType.IS_NATIVE, true );
			}
			if ( tid.untypedSubTypes() ) {
				returnValue.setFirstChild( TypeInfoType.UNDEFINED_SUBTYPES, true );
			}
			returnValue.setFirstChild( TypeInfoType.ROOT_TYPE, tid.nativeType().id() );
		}
		if( typeDefinition instanceof TypeChoiceDefinition ){
			returnValue.setFirstChild( TypeInfoType.IS_CHOICE, typeDefinition instanceof TypeChoiceDefinition );
		}
		if ( typeDefinition.getDocumentation() != null ) {
			returnValue.setFirstChild( TypeInfoType.DOCUMENTATION, typeDefinition.getDocumentation() );
		}
		
		ValueVector subtypes = buildSubtypes( typeDefinition );
		if( !subtypes.isEmpty() ){
			returnValue.children().put( TypeInfoType.SUBTYPE, subtypes );
		}
		
		if( addCode ){
			returnValue.setFirstChild( TypeInfoType.CODE, buildTypeCode( typeDefinition ) );
		}
		
		return returnValue;
	}
	
	private static ValueVector buildSubtypes( TypeDefinition typeDefinition ){
		ValueVector returnVector = ValueVector.create();
		
		if ( typeDefinition instanceof TypeChoiceDefinition ) {
			returnVector.add( buildTypeInfo( ( (TypeChoiceDefinition) typeDefinition ).left(), false ) );
			returnVector.get( returnVector.size() - 1 ).children().remove( TypeInfoType.NAME );
			returnVector.add( buildTypeInfo( ( (TypeChoiceDefinition) typeDefinition ).right(), false ) );
			returnVector.get( returnVector.size() - 1 ).children().remove( TypeInfoType.NAME );
		}
		if ( typeDefinition instanceof TypeInlineDefinition ) {
			TypeInlineDefinition tid = (TypeInlineDefinition) typeDefinition;
			if( tid.hasSubTypes() ){
				for ( Entry<String, TypeDefinition> subType : tid.subTypes() ) {
					returnVector.add( buildTypeInfo( subType.getValue(), false ) );
				}
			}
		}
		return returnVector;
	}

	private static String buildTypeCode( TypeDefinition typeDefinition )
	{
		return TYPE_DECLARATION_TOKEN + " "
			+ typeDefinition.id() + TYPE_DEFINITION_TOKEN + " " 
			+ buildSubTypeCode( typeDefinition );
	}

	private static String buildSubTypeCode( TypeDefinition typeDefinition )
	{
		String returnString = "";
		if ( typeDefinition instanceof TypeChoiceDefinition ) {
			returnString += buildSubTypeCode( ( (TypeChoiceDefinition) typeDefinition ).left() );
			returnString += " " + TYPE_CHOICE_TOKEN + " ";
			returnString += buildSubTypeCode( ( (TypeChoiceDefinition) typeDefinition ).right() );
		}
		if ( typeDefinition instanceof TypeDefinitionLink ) {
			returnString += ( (TypeDefinitionLink) typeDefinition ).linkedTypeName();
		}
		if ( typeDefinition instanceof TypeInlineDefinition ) {
			TypeInlineDefinition tid = (TypeInlineDefinition) typeDefinition;
			if ( tid.untypedSubTypes() ){
				returnString += UNDEFINED_TYPE;
			} else {
				returnString += tid.nativeType().id();
				if ( tid.hasSubTypes() ) {
					returnString += " " + TYPE_SUBTYPE_OPEN;
					returnString = tid.subTypes().stream().map(
						( subType )
						-> NEW_LINE + TYPE_SUBTYPE_DEFINITON + subType.getKey()
						+ getSubTypeCardinalityCode( subType.getValue() )
						+ TYPE_DEFINITION_TOKEN + " "
						+ buildSubTypeCode( subType.getValue() ) )
						.reduce( returnString, String::concat )
						.replaceAll( NEW_LINE, NEW_LINE + TAB );
					returnString += NEW_LINE + TYPE_SUBTYPE_CLOSE;
				}
			}
		}
		return returnString;
	}

	private static String getSubTypeCardinalityCode( TypeDefinition type )
	{
		switch( type.cardinality().min() ) {
			case 0:
				switch( type.cardinality().max() ) {
					case Integer.MAX_VALUE:
						return TYPE_CARDINALITY_ZERO_TO_MANY;
					case 1:
						return TYPE_CARDINALITY_ZERO_TO_ONE;
					default:
						return getCardinalityString( 0, type.cardinality().max() );
				}
			default:
				return getCardinalityString( type.cardinality().min(), type.cardinality().max() );
		}
	}

	private static String getCardinalityString( int min, int max )
	{
		if ( min == max && min == 1 ) {
			return "";
		}
		String returnString = TYPE_CARDINALITY_OPEN + min;
		if ( min != max ) {
			returnString += TYPE_CARDINALITY_SEPARATOR + max;
		}
		return returnString + TYPE_CARDINALITY_CLOSE;
	}

	private static void buildSubTypes( PortInfo p, ValueVector v, Set<String> s, ProgramInspector inspector )
	{
		p.getInterfaceList().forEach( ( i ) -> {
			buildSubTypes( i, v, s, inspector );
		} );
		if ( p instanceof InputPortInfo ){
			getAggregatedInterfaces( (InputPortInfo) p, inspector).forEach( ( i ) -> buildSubTypes( i, v, s, inspector ) );
		}
	}

	private static void buildSubTypes( InterfaceDefinition i, ValueVector v, Set<String> s, ProgramInspector inspector )
	{
		i.operationsMap().entrySet().forEach( ( Entry<String, OperationDeclaration> d ) -> {
			buildSubTypes( d.getValue(), v, s, inspector );
		} );
	}

	private static void buildSubTypes( OperationDeclaration o, ValueVector v, Set<String> s, ProgramInspector inspector )
	{
		if ( o instanceof OneWayOperationDeclaration ) {
			buildSubTypes( ( (OneWayOperationDeclaration) o ).requestType(), v, s, inspector );
		} else {
			buildSubTypes( ( (RequestResponseOperationDeclaration) o ).requestType(), v, s, inspector );
			buildSubTypes( ( (RequestResponseOperationDeclaration) o ).responseType(), v, s, inspector );
			( (RequestResponseOperationDeclaration) o ).faults().entrySet().forEach( ( f ) -> {
				buildSubTypes( f.getValue(), v, s, inspector );
			} );
		}
	}

	private static void buildSubTypes( TypeDefinition d, ValueVector v, Set<String> s, ProgramInspector inspector )
	{
		if ( d instanceof TypeChoiceDefinition ) {
			buildSubTypes( ( (TypeChoiceDefinition) d ).left(), v, s, inspector );
			buildSubTypes( ( (TypeChoiceDefinition) d ).right(), v, s, inspector );
		} else if ( d instanceof TypeDefinitionLink ) {
			TypeDefinitionLink tdl = (TypeDefinitionLink) d;
			if ( !s.contains( tdl.id() ) && Stream.of( inspector.getTypes() ).anyMatch( ( t ) -> t.id().equals( tdl.id() ) ) ) {
				s.add( tdl.id() );
				Value tv = Value.create();
				tv.setFirstChild( TypeInfoType.NAME, tdl.id() );
				tv.setFirstChild( TypeInfoType.IS_NATIVE, NativeType.isNativeTypeKeyword( tdl.id() ) );
				tv.setFirstChild( TypeInfoType.CODE, buildTypeCode( tdl ) );
				if ( tdl.getDocumentation() != null ) {
					tv.setFirstChild( TypeInfoType.DOCUMENTATION, tdl.getDocumentation() );
				}
				v.add( tv );
			}
		} else if ( d instanceof TypeInlineDefinition ) {
			TypeInlineDefinition tid = (TypeInlineDefinition) d;
			if ( tid.hasSubTypes() ) {
				tid.subTypes().forEach( ( td ) -> {
					buildSubTypes( td.getValue(), v, s, inspector );
				} );
			}
		}
	}

}