/***************************************************************************
 *   Copyright (C) by Claudio Guidi                                        *
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

package joliex.meta;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.NativeType;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import jolie.util.Range;

/**
 *
 * @author claudio guidi
 */
public class MetaJolie extends JavaService
{

	private int MAX_CARD = 2147483647;

	private Value getNativeType( NativeType type )
	{
		Value response = Value.create();
		if ( type == NativeType.ANY ) {
			response.getFirstChild( "any_type" ).setValue( true );
		} else if ( type == NativeType.STRING ) {
			response.getFirstChild( "string_type" ).setValue( true );
		} else if ( type == NativeType.DOUBLE ) {
			response.getFirstChild( "double_type" ).setValue( true );
		} else if ( type == NativeType.INT ) {
			response.getFirstChild( "int_type" ).setValue( true );
		} else if ( type == NativeType.VOID ) {
			response.getFirstChild( "void_type" ).setValue( true );
		} else if ( type == NativeType.BOOL ) {
			response.getFirstChild( "bool_type" ).setValue( true );
		} else if ( type == NativeType.LONG ) {
			response.getFirstChild( "long_type" ).setValue( true );
		} else if ( type == NativeType.RAW ) {
			response.getFirstChild( "raw_type" ).setValue( true );
		}
		return response;
	}

	private boolean isNativeType( String type )
	{
		return type.equals( "any" )
			|| type.equals( "string" )
			|| type.equals( "double" )
			|| type.equals( "int" )
			|| type.equals( "void" )
			|| type.equals( "raw" )
			//|| type.equals("undefined")
			|| type.equals( "any" )
			|| type.equals( "bool" )
			|| type.equals( "long" );
	}

	private Value addCardinality( Range range )
	{
		Value response = Value.create();
		response.getFirstChild( "min" ).setValue( range.min() );
		if ( range.max() == MAX_CARD ) {
			response.getFirstChild( "infinite" ).setValue( 1 );
		} else {
			response.getFirstChild( "max" ).setValue( range.max() );
		}
		return response;
	}

	private Value addTypeInLine( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition typedef )
	{
		Value response = Value.create();
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( typedef.id() );   // not useful, inserted for respecting Type
		if ( typedef instanceof TypeDefinitionLink ) {
			response.getFirstChild( "root_type" ).getFirstChild( "link" ).setValue( ((TypeDefinitionLink) typedef).linkedTypeName() );
			insertType( types, types_vector, name, ((TypeDefinitionLink) typedef).linkedType() );
		} else if ( typedef instanceof TypeInlineDefinition ) {
			final TypeInlineDefinition td = (TypeInlineDefinition) typedef;
			response.getFirstChild( "root_type" ).deepCopy( getNativeType( td.nativeType() ) );
			if ( td.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
					response.getChildren( "sub_type" ).add( addSubType( types, types_vector, name, entry.getValue() ) );
				}
			}
		} else {
			throw new UnsupportedOperationException( "choice types are still unsupported in MetaJolie" );
		}
		return response;
	}

	private Value addSubType( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition type )
	{
		Value response = Value.create();
		response.getFirstChild( "name" ).setValue( type.id() );
		response.getFirstChild( "cardinality" ).deepCopy( addCardinality( type.cardinality() ) );
		if ( type instanceof TypeDefinitionLink ) {
			response.getFirstChild( "type_link" ).deepCopy( setName( name ) );
			response.getFirstChild( "type_link" ).getFirstChild( "name" ).setValue( ((TypeDefinitionLink) type).linkedTypeName() );
			insertType( types, types_vector, name, ((TypeDefinitionLink) type).linkedType() );

		} else {
			response.getFirstChild( "type_inline" ).deepCopy( addTypeInLine( types, types_vector, name, type ) );
		}
		return response;
	}

	private void insertExtendedType( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition typedef, TypeDefinition extension )
	{
		// to be optimized, similar code with addType
		if ( !types.contains( typedef ) && !isNativeType( typedef.id() ) && !typedef.id().equals( "undefined" ) ) {
			types.add( typedef );
			Value type = Value.create();
			if ( typedef instanceof TypeDefinitionLink ) {
				type.getFirstChild( "name" ).deepCopy( setName( name ) );
				type.getFirstChild( "name" ).getFirstChild( "name" ).setValue( typedef.id() );
				type.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "name" ).setValue( ((TypeDefinitionLink) typedef).linkedTypeName() );
				insertExtendedType( types, types_vector, name, ((TypeDefinitionLink) typedef).linkedType(), extension );
			} else {
				TypeInlineDefinition td = (TypeInlineDefinition) typedef;
				type.getFirstChild( "name" ).deepCopy( setName( name ) );
				type.getFirstChild( "name" ).getFirstChild( "name" ).setValue( td.id() );
				type.getFirstChild( "root_type" ).deepCopy( getNativeType( td.nativeType() ) );
				if ( td.hasSubTypes() ) {
					int subtype_counter = 0;
					for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
						type.getChildren( "sub_type" ).get( subtype_counter ).deepCopy( addSubType( types, types_vector, name, entry.getValue() ) );
						subtype_counter++;
					}
				}
				// adding extension                
				if ( extension != null && extension instanceof TypeInlineDefinition ) {
					final TypeInlineDefinition typeInline = (TypeInlineDefinition) extension;
					int subtype_counter = type.getChildren( "sub_type" ).size();
					for( Entry<String, TypeDefinition> entry : typeInline.subTypes() ) {
						type.getChildren( "sub_type" ).get( subtype_counter ).deepCopy( addSubType( types, types_vector, name, entry.getValue() ) );
						subtype_counter++;
					}
				}

			}
			types_vector.add( type );
		}
	}

	private void insertType( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition typedef )
	{
		// to be optimized, similar code with addType
		if ( !types.contains( typedef ) && !isNativeType( typedef.id() ) && !typedef.id().equals( "undefined" ) ) {
			types.add( typedef );
			Value type = Value.create();
			if ( typedef instanceof TypeDefinitionLink ) {
				type.getFirstChild( "name" ).deepCopy( setName( name ) );
				type.getFirstChild( "name" ).getFirstChild( "name" ).setValue( typedef.id() );
				type.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "name" ).setValue( ((TypeDefinitionLink) typedef).linkedTypeName() );
				insertType( types, types_vector, name, ((TypeDefinitionLink) typedef).linkedType() );
			} else {

				TypeInlineDefinition td = (TypeInlineDefinition) typedef;
				type.getFirstChild( "name" ).deepCopy( setName( name ) );
				type.getFirstChild( "name" ).getFirstChild( "name" ).setValue( td.id() );
				type.getFirstChild( "root_type" ).deepCopy( getNativeType( td.nativeType() ) );
				if ( td.hasSubTypes() ) {
					int subtype_counter = 0;
					for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
						type.getChildren( "sub_type" ).get( subtype_counter ).deepCopy( addSubType( types, types_vector, name, entry.getValue() ) );
						subtype_counter++;
					}
				}

			}
			types_vector.add( type );
		}
	}

	private Value getSubType( TypeDefinition type, Value name )
	{
		Value response = Value.create();
		response.getFirstChild( "name" ).setValue( type.id() );
		response.getFirstChild( "cardinality" ).deepCopy( addCardinality( type.cardinality() ) );
		if ( type instanceof TypeDefinitionLink ) {

			response.getFirstChild( "type_link" ).deepCopy( setName( name ) );
			response.getFirstChild( "type_link" ).getFirstChild( "name" ).setValue( ((TypeDefinitionLink) type).linkedTypeName() );

		} else {
			response.getFirstChild( "type_inline" ).deepCopy( getType( type, name ) );
		}
		return response;
	}

	private Value getType( TypeDefinition typedef, Value name )
	{
		Value type = Value.create();

		type.getFirstChild( "name" ).deepCopy( setName( name ) );
		type.getFirstChild( "name" ).getFirstChild( "name" ).setValue( typedef.id() );

		if ( typedef instanceof TypeDefinitionLink ) {
			type.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "name" ).setValue( ((TypeDefinitionLink) typedef).linkedTypeName() );
			if ( name.getFirstChild( "domain" ).isDefined() ) {
				type.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "domain" ).setValue( name.getFirstChild( "domain" ).strValue() );
			}
		} else {
			TypeInlineDefinition td = (TypeInlineDefinition) typedef;
			type.getFirstChild( "root_type" ).deepCopy( getNativeType( td.nativeType() ) );
			if ( td.hasSubTypes() ) {
				int subtype_counter = 0;
				for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
					type.getChildren( "sub_type" ).get( subtype_counter ).deepCopy( getSubType( entry.getValue(), name ) );
					subtype_counter++;
				}
			}
		}
		return type;
	}

	private List<TypeDefinition> addType( List<TypeDefinition> types, TypeDefinition typedef )
	{

		if ( !typedef.id().equals( "undefined" ) ) {
			if ( !types.contains( typedef ) && !isNativeType( typedef.id() ) ) {
				types.add( typedef );
				if ( typedef instanceof TypeDefinitionLink ) {
					addType( types, ((TypeDefinitionLink) typedef).linkedType() );
				} else {
					TypeInlineDefinition td = (TypeInlineDefinition) typedef;
					if ( td.hasSubTypes() ) {
						for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
							addSubType( types, entry.getValue() );
						}
					}
				}
			}
		}
		return types;
	}

	private List<TypeDefinition> addSubType( List<TypeDefinition> types, TypeDefinition subtype )
	{
		if ( subtype instanceof TypeDefinitionLink ) {
			addType( types, ((TypeDefinitionLink) subtype).linkedType() );
		} else {
			TypeInlineDefinition td = (TypeInlineDefinition) subtype;
			if ( td.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> entry : td.subTypes() ) {
					addSubType( types, entry.getValue() );
				}
			}
		}

		return types;
	}

	private Value getInterface( InterfaceDefinition intf, Value name, List<TypeDefinition> types )
	{
		Value response = Value.create();

		// setting the name
		response.getFirstChild( "name" ).deepCopy( setName( name ) );
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( intf.name() );

		ValueVector operations = response.getChildren( "operations" );

		// scans operations and types
		Map< String, OperationDeclaration> operationMap = intf.operationsMap();

		for( Entry< String, OperationDeclaration> operationEntry : operationMap.entrySet() ) {
			Value current_operation = Value.create();
			if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
				OneWayOperationDeclaration oneWayOperation = (OneWayOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "documentation" ).setValue( oneWayOperation.getDocumentation() );
				current_operation.getFirstChild( "operation_name" ).setValue( oneWayOperation.id() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( oneWayOperation.requestType().id() );
				if ( !isNativeType( oneWayOperation.requestType().id() ) ) {
					addType( types, oneWayOperation.requestType() );
				}

			} else {
				RequestResponseOperationDeclaration requestResponseOperation = (RequestResponseOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "documentation" ).setValue( requestResponseOperation.getDocumentation() );
				current_operation.getFirstChild( "operation_name" ).setValue( requestResponseOperation.id() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( requestResponseOperation.requestType().id() );
				current_operation.getFirstChild( "output" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "output" ).getFirstChild( "name" ).setValue( requestResponseOperation.responseType().id() );
				if ( !isNativeType( requestResponseOperation.requestType().id() ) ) {
					addType( types, requestResponseOperation.requestType() );
				}
				if ( !isNativeType( requestResponseOperation.responseType().id() ) ) {
					addType( types, requestResponseOperation.responseType() );
				}
				Map<String, TypeDefinition> faults = requestResponseOperation.faults();
				int faultCounter = 0;
				for( Entry<String, TypeDefinition> f : faults.entrySet() ) {
					current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( f.getKey() );
					if ( f.getValue() != null ) {
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).deepCopy( setName( name ) );
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).getFirstChild( "name" ).setValue( f.getValue().id() );
						if ( !isNativeType( f.getValue().id() ) ) {
							addType( types, f.getValue() );
						}
					}
					faultCounter++;
				}
			}
			operations.add( current_operation );

		}
		return response;
	}

	private List<InterfaceDefinition> addInterfaceToList( List<InterfaceDefinition> list, InterfaceDefinition intf )
	{
		if ( !list.contains( intf ) ) {
			list.add( intf );
		}
		return list;
	}

	private Value setName( String name )
	{
		Value v = Value.create();
		v.getFirstChild( "name" ).setValue( name );
		return v;
	}

	private Value setName( String name, String domain )
	{
		Value v = setName( name );
		v.getFirstChild( "domain" ).setValue( domain );
		return v;

	}

	private Value setName( String name, String domain, String registry )
	{
		Value v = setName( name, domain );
		v.getFirstChild( "registry" ).setValue( registry );
		return v;
	}

	private Value setName( Value name )
	{
		Value v;
		if ( name.getFirstChild( "domain" ).isDefined() && name.getFirstChild( "registry" ).isDefined() ) {
			v = setName( name.getFirstChild( "name" ).strValue(), name.getFirstChild( "domain" ).strValue(), name.getFirstChild( "registry" ).strValue() );
		} else if ( name.getFirstChild( "domain" ).isDefined() && !name.getFirstChild( "registry" ).isDefined() ) {
			v = setName( name.getFirstChild( "name" ).strValue(), name.getFirstChild( "domain" ).strValue() );
		} else {
			v = setName( name.getFirstChild( "name" ).strValue() );
		}
		return v;
	}

	private Value getInputPort( InputPortInfo portInfo, Value name, OutputPortInfo[] outputPortList )
	{

		Value response = Value.create();
		response.getFirstChild( "name" ).deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( portInfo.id() );

		InputPortInfo port = (InputPortInfo) portInfo;
		response.getFirstChild( "location" ).setValue( port.location().toString() );
		if ( port.protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( port.protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			Value input_interface = response.getChildren( "interfaces" ).get( intf_index );
			addInterfaceToPortInfo( input_interface, interfaceDefinition, name );
		}

        // scanning aggregation
		// extracts interfaces from aggregated outputPorts
		for( int x = 0; x < portInfo.aggregationList().length; x++ ) {
			int i = 0;
			while( !portInfo.aggregationList()[ x ].outputPortList()[ 0 ].equals( outputPortList[ i ].id() ) ) {
				i++;
			}
			int curItfIndex = response.getChildren( "interfaces" ).size();
			InterfaceExtenderDefinition extender = null;
			OneWayOperationDeclaration owExtender = null;
			RequestResponseOperationDeclaration rrExtender = null;
			if ( portInfo.aggregationList()[ x ].interfaceExtender() != null ) {
                // the interfaces of the outputPort must be extended
				// only default extension is processed. TODO: extending also specific operation declaration

				extender = portInfo.aggregationList()[ x ].interfaceExtender();
				if ( extender.defaultOneWayOperation() != null ) {
					owExtender = extender.defaultOneWayOperation();
				}
				if ( extender.defaultRequestResponseOperation() != null ) {
					rrExtender = extender.defaultRequestResponseOperation();
				}
			}
			for( InterfaceDefinition interfaceDefinition : outputPortList[ i ].getInterfaceList() ) {
				Value inputInterface = response.getChildren( "interfaces" ).get( curItfIndex );
				if ( extender != null ) {
					addExtendedInterfaceToPortInfo( inputInterface, interfaceDefinition, name, owExtender, rrExtender );
				} else {
					addInterfaceToPortInfo( inputInterface, interfaceDefinition, name );
				}
				curItfIndex++;
			}

		}

		return response;

	}

	private Value getInputPort( InputPortInfo portInfo, Value name, OutputPortInfo[] outputPortList, List<InterfaceDefinition> interfaces )
	{

		Value response = Value.create();
		response.getFirstChild( "name" ).deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( portInfo.id() );

		InputPortInfo port = (InputPortInfo) portInfo;
		response.getFirstChild( "location" ).setValue( port.location().toString() );
		if ( port.protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( port.protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			response.getChildren( "interfaces" ).get( intf_index ).getFirstChild( "name" ).deepCopy( setName( name ) );
			response.getChildren( "interfaces" ).get( intf_index ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( interfaceDefinition.name() );
			addInterfaceToList( interfaces, interfaceDefinition );
		}

        // scanning aggregation
		// extracts interfaces from aggregated outputPorts
		for( int x = 0; x < portInfo.aggregationList().length; x++ ) {
			int i = 0;
			while( !portInfo.aggregationList()[ x ].outputPortList()[ 0 ].equals( outputPortList[ i ].id() ) ) {
				i++;
			}
			int intf = response.getChildren( "interfaces" ).size();
			for( InterfaceDefinition interfaceDefinition : outputPortList[ i ].getInterfaceList() ) {
				response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).deepCopy( setName( name ) );
				response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( interfaceDefinition.name() );
				addInterfaceToList( interfaces, interfaceDefinition );
				intf++;
			}
		}

		return response;

	}

	private Value getOutputPort( OutputPortInfo portInfo, Value name )
	{

		Value response = Value.create();
		response.getFirstChild( "name" ).deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( portInfo.id() );

		OutputPortInfo port = (OutputPortInfo) portInfo;
		response.getFirstChild( "location" ).setValue( port.location().toString() );

		if ( port.protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( port.protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			Value input_interface = response.getChildren( "interfaces" ).get( intf_index );
			addInterfaceToPortInfo( input_interface, interfaceDefinition, name );
		}

		return response;

	}

	private void addExtendedInterfaceToPortInfo(
		Value input_interface,
		InterfaceDefinition interfaceDefinition,
		Value name,
		OneWayOperationDeclaration owExtender,
		RequestResponseOperationDeclaration rrExtender )
	{

		ArrayList<TypeDefinition> types = new ArrayList<TypeDefinition>();

		input_interface.getFirstChild( "name" ).deepCopy( setName( name ) );
		input_interface.getFirstChild( "name" ).getFirstChild( "name" ).setValue( interfaceDefinition.name() );

		ValueVector operations = input_interface.getChildren( "operations" );
		ValueVector interface_types = input_interface.getChildren( "types" );

		// scans operations and types
		Map< String, OperationDeclaration> operationMap = interfaceDefinition.operationsMap();

		for( Entry< String, OperationDeclaration> operationEntry : operationMap.entrySet() ) {
			Value current_operation = Value.create();;
			if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
				OneWayOperationDeclaration oneWayOperation = (OneWayOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "operation_name" ).setValue( oneWayOperation.id() );
                                current_operation.getFirstChild( "documentation" ).setValue( oneWayOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( oneWayOperation.requestType().id() );
				if ( !isNativeType( oneWayOperation.requestType().id() ) ) {
					insertExtendedType( types, interface_types, name, oneWayOperation.requestType(), owExtender.requestType() );
				}

			} else {
				RequestResponseOperationDeclaration requestResponseOperation = (RequestResponseOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "operation_name" ).setValue( requestResponseOperation.id() );
                                current_operation.getFirstChild( "documentation" ).setValue( requestResponseOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( requestResponseOperation.requestType().id() );
				current_operation.getFirstChild( "output" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "output" ).getFirstChild( "name" ).setValue( requestResponseOperation.responseType().id() );
				if ( !isNativeType( requestResponseOperation.requestType().id() ) ) {
					insertExtendedType( types, interface_types, name, requestResponseOperation.requestType(), rrExtender.requestType() );
				}
				if ( !isNativeType( requestResponseOperation.responseType().id() ) ) {
					insertExtendedType( types, interface_types, name, requestResponseOperation.responseType(), rrExtender.responseType() );
				}
				Map<String, TypeDefinition> faults = requestResponseOperation.faults();
				int faultCounter = 0;
				for( Entry<String, TypeDefinition> f : faults.entrySet() ) {
					current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( f.getKey() );
					if ( f.getValue() != null ) {
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).deepCopy( setName( name ) );
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).getFirstChild( "name" ).setValue( f.getValue().id() );
						if ( !isNativeType( f.getValue().id() ) ) {
							insertExtendedType( types, interface_types, name, f.getValue(), rrExtender.faults().get( f.getKey() ) );
						}
					}
					faultCounter++;
				}
			}
			operations.add( current_operation );
		}
	}

	private void addInterfaceToPortInfo( Value input_interface, InterfaceDefinition interfaceDefinition, Value name )
	{
		ArrayList<TypeDefinition> types = new ArrayList<TypeDefinition>();

		input_interface.getFirstChild( "name" ).deepCopy( setName( name ) );
		input_interface.getFirstChild( "name" ).getFirstChild( "name" ).setValue( interfaceDefinition.name() );

		ValueVector operations = input_interface.getChildren( "operations" );
		ValueVector interface_types = input_interface.getChildren( "types" );

		// scans operations and types
		Map< String, OperationDeclaration> operationMap = interfaceDefinition.operationsMap();

		for( Entry< String, OperationDeclaration> operationEntry : operationMap.entrySet() ) {
			Value current_operation = Value.create();;
			if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
				OneWayOperationDeclaration oneWayOperation = (OneWayOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "operation_name" ).setValue( oneWayOperation.id() );
                                current_operation.getFirstChild( "documentation" ).setValue( oneWayOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( oneWayOperation.requestType().id() );
				if ( !isNativeType( oneWayOperation.requestType().id() ) ) {
					insertType( types, interface_types, name, oneWayOperation.requestType() );
				}

			} else {
				RequestResponseOperationDeclaration requestResponseOperation = (RequestResponseOperationDeclaration) operationEntry.getValue();
				current_operation.getFirstChild( "operation_name" ).setValue( requestResponseOperation.id() );
                                current_operation.getFirstChild( "documentation" ).setValue( requestResponseOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "input" ).getFirstChild( "name" ).setValue( requestResponseOperation.requestType().id() );
				current_operation.getFirstChild( "output" ).deepCopy( setName( name ) );
				current_operation.getFirstChild( "output" ).getFirstChild( "name" ).setValue( requestResponseOperation.responseType().id() );
				if ( !isNativeType( requestResponseOperation.requestType().id() ) ) {
					insertType( types, interface_types, name, requestResponseOperation.requestType() );
				}
				if ( !isNativeType( requestResponseOperation.responseType().id() ) ) {
					insertType( types, interface_types, name, requestResponseOperation.responseType() );
				}
				Map<String, TypeDefinition> faults = requestResponseOperation.faults();
				int faultCounter = 0;
				for( Entry<String, TypeDefinition> f : faults.entrySet() ) {
					current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( f.getKey() );
					if ( f.getValue() != null ) {
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).deepCopy( setName( name ) );
						current_operation.getChildren( "fault" ).get( faultCounter ).getFirstChild( "type_name" ).getFirstChild( "name" ).setValue( f.getValue().id() );
						if ( !isNativeType( f.getValue().id() ) ) {
							insertType( types, interface_types, name, f.getValue() );
						}
					}
					faultCounter++;
				}
			}
			operations.add( current_operation );
		}
	}

	private Value getPort( PortInfo portInfo, Value name, List<InterfaceDefinition> interfaces )
	{
		Value response = Value.create();

		// setting domain and registry from request
		response.getFirstChild( "name" ).deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( portInfo.id() );

		if ( portInfo instanceof InputPortInfo ) {
			InputPortInfo port = (InputPortInfo) portInfo;
			if ( port.location() != null ) {
				response.getFirstChild( "location" ).setValue( port.location().toString() );
			} else {
				response.getFirstChild( "location" ).setValue( "local" );
			}
			if ( port.protocolId() != null ) {
				response.getFirstChild( "protocol" ).setValue( port.protocolId() );
			} else {
				response.getFirstChild( "protocol" ).setValue( "" );
			}

		} else if ( portInfo instanceof OutputPortInfo ) {
			OutputPortInfo port = (OutputPortInfo) portInfo;
			if ( port.location() != null ) {
				response.getFirstChild( "location" ).setValue( port.location().toString() );
			} else {
				response.getFirstChild( "location" ).setValue( "local" );
			}
			if ( port.protocolId() != null ) {
				response.getFirstChild( "protocol" ).setValue( port.protocolId() );
			} else {
				response.getFirstChild( "protocol" ).setValue( "" );
			}
		}

		// scans interfaces
		List<InterfaceDefinition> interfaceList = portInfo.getInterfaceList();
		for( int intf = 0; intf < interfaceList.size(); intf++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf );

			// setting the name of the interface within the port response
			response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).deepCopy( setName( name ) );
			response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).getFirstChild( "name" ).setValue( interfaceDefinition.name() );
			interfaces = addInterfaceToList( interfaces, interfaceDefinition );

		}

		return response;
	}

	private String[] getArgs( String filename )
	{
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for( String s : interpreter().includePaths() ) {
			builder.append( s );
			if ( ++i < interpreter().includePaths().length ) {
				builder.append( jolie.lang.Constants.pathSeparator );
			}
		}
		
		return new String[] {
			"-i",
			builder.toString(),
			// String.join( jolie.lang.Constants.pathSeparator, interpreter().includePaths() ),
			filename
		};
	}

	@RequestResponse
	public Value checkNativeType( Value request )
	{
		Value response = Value.create();
		response.getFirstChild( "result" ).setValue( isNativeType( request.getFirstChild( "type_name" ).strValue() ) );
		return response;
	}

	@RequestResponse
	public Value parseRoles( Value request )
	{

		Value response = Value.create();
		try {

			response.getFirstChild( "name" ).deepCopy( setName( request.getFirstChild( "rolename" ) ) );
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );
			CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				cmdParser.programFilepath().toURI(), cmdParser.charset(),
				cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants() );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			URI originalFile = program.context().source();
			// scanning first inputport
			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
			Value input = response.getFirstChild( "input" );
			if ( inputPortList.length > 0 ) {
				InputPortInfo inputPort = inputPortList[ 0 ];
				input.deepCopy( getInputPort( inputPort, request.getFirstChild( "name" ), inspector.getOutputPorts() ) );
			}

			// scanning first outputPort if it exists
			OutputPortInfo[] outputPortList = inspector.getOutputPorts();
			if ( outputPortList.length > 0 ) {
				Value output = response.getFirstChild( "output" );
				output.deepCopy( getOutputPort( outputPortList[ 0 ], request.getFirstChild( "name" ) ) );
			}

		} catch( CommandLineException e ) {
			// TO DO
			e.printStackTrace();
		} catch( IOException e ) {
			// TO DO
			e.printStackTrace();
		} catch( ParserException e ) {
			// TO DO
			e.printStackTrace();
		} catch( SemanticException e ) {
			// TO DO
			e.printStackTrace();
		}
		return response;
	}

	@RequestResponse
	public Value getMetaData( Value request )
		throws FaultException
	{

		String domain = "";
		List<TypeDefinition> types = new ArrayList<TypeDefinition>();
		List<InterfaceDefinition> interfaces = new ArrayList<InterfaceDefinition>();
		Value response = Value.create();
		try {
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );

			if ( request.getFirstChild( "name" ).getFirstChild( "domain" ).isDefined() ) {
				domain = request.getFirstChild( "name" ).getFirstChild( "domain" ).strValue();
			}
			CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				cmdParser.programFilepath().toURI(), cmdParser.charset(),
				cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants() );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			URI originalFile = program.context().source();

			cmdParser.close();

			response.getFirstChild( "service" ).getFirstChild( "name" ).deepCopy( setName( request.getFirstChild( "name" ) ) );

			OutputPortInfo[] outputPortList = inspector.getOutputPorts();
			if ( outputPortList.length > 0 ) {
				ValueVector output = response.getChildren( "output" );
				for( int op = 0; op < outputPortList.length; op++ ) {
					OutputPortInfo outputPort = outputPortList[ op ];
					output.get( op ).deepCopy( getPort( outputPort, request.getFirstChild( "name" ), interfaces ) );
					response.getFirstChild( "service" ).getChildren( "output" ).get( op ).getFirstChild( "name" ).setValue( outputPort.id() );
					response.getFirstChild( "service" ).getChildren( "output" ).get( op ).getFirstChild( "domain" ).setValue( domain );
				}
			}

			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
			ValueVector input = response.getChildren( "input" );
			if ( inputPortList.length > 0 ) {
				for( int ip = 0; ip < inputPortList.length; ip++ ) {
					InputPortInfo inputPort = inputPortList[ ip ];
					input.get( ip ).deepCopy( getInputPort( inputPort, request.getFirstChild( "name" ), outputPortList, interfaces ) );
					response.getFirstChild( "service" ).getChildren( "input" ).get( ip ).getFirstChild( "name" ).setValue( inputPort.id() );
					response.getFirstChild( "service" ).getChildren( "input" ).get( ip ).getFirstChild( "domain" ).setValue( domain );
				}
			}

			// adding interfaces
			for( int intf = 0; intf < interfaces.size(); intf++ ) {
				InterfaceDefinition interfaceDefinition = interfaces.get( intf );
				response.getChildren( "interfaces" ).get( intf ).deepCopy( getInterface( interfaceDefinition, request.getFirstChild( "name" ), types ) );
			}

			// adding types
			for( int tp = 0; tp < types.size(); tp++ ) {
				TypeDefinition typeDefinition = types.get( tp );
				response.getChildren( "types" ).get( tp ).deepCopy( getType( typeDefinition, request.getFirstChild( "name" ) ) );
			}

			// adding embedded services
			EmbeddedServiceNode[] embeddedServices = inspector.getEmbeddedServices();
			for( int es = 0; es < embeddedServices.length; es++ ) {
				response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "type" ).setValue( embeddedServices[ es ].type().toString() );
				response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "servicepath" ).setValue( embeddedServices[ es ].servicePath() );
				response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "portId" ).setValue( embeddedServices[ es ].portId() );
			}

		} catch( CommandLineException e ) {
		} catch( IOException e ) {
		} catch( ParserException e ) {
			Value fault = Value.create();
			fault.getFirstChild( "message" ).setValue( e.getMessage() );
			fault.getFirstChild( "line" ).setValue( e.context().line() );
			fault.getFirstChild( "sourceName" ).setValue( e.context().sourceName() );
			throw new FaultException( "ParserException", fault );
		} catch( SemanticException e ) {
			Value fault = Value.create();
			int i = 0;
			for( SemanticException.SemanticError error : e.getErrorList() ) {
				fault.getChildren( "error" ).get( i ).getFirstChild( "message" ).setValue( error.getMessage() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "line" ).setValue( error.context().line() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "sourceName" ).setValue( error.context().sourceName() );
				i++;
			}
			throw new FaultException( "SemanticException", fault );
		}
		return response;
	}

	@RequestResponse
	public Value getInputPortMetaData( Value request ) throws FaultException
	{
		Value response = Value.create();
		try {
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );
			
			CommandLineParser cmdParser = new CommandLineParser( args, interpreter().getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				cmdParser.programFilepath().toURI(), cmdParser.charset(),
				cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants() );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			URI originalFile = program.context().source();

			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
			ValueVector input = response.getChildren( "input" );
			if ( inputPortList.length > 0 ) {
				for( int ip = 0; ip < inputPortList.length; ip++ ) {
					InputPortInfo inputPort = inputPortList[ ip ];
					input.get( ip ).deepCopy( getInputPort( inputPort, request.getFirstChild( "name" ), inspector.getOutputPorts() ) );
				}
			}
			cmdParser.close();

		} catch( CommandLineException e ) {
			throw new FaultException( "InputPortMetaDataFault", e );
		} catch( IOException e ) {
			throw new FaultException( "InputPortMetaDataFault", e );
		} catch( ParserException e ) {
			Value fault = Value.create();
			fault.getFirstChild( "message" ).setValue( e.getMessage() );
			fault.getFirstChild( "line" ).setValue( e.context().line() );
			fault.getFirstChild( "sourceName" ).setValue( e.context().sourceName() );
			throw new FaultException( "ParserException", fault );
		} catch( SemanticException e ) {
			Value fault = Value.create();
			List<SemanticException.SemanticError> errorList = e.getErrorList();
			for( int i = 0; i < errorList.size(); i++ ) {
				fault.getChildren( "error" ).get( i ).getFirstChild( "message" ).setValue( errorList.get( i ).getMessage() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "line" ).setValue( errorList.get( i ).context().line() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "sourceName" ).setValue( errorList.get( i ).context().sourceName() );
			}
			throw new FaultException( "SemanticException", fault );
		}

		return response;
	}

	private Value findType( ValueVector types, String typeName, String typeDomain )
	{
		Iterator iterator = types.iterator();
		boolean found = false;
		int index = 0;
		while( index < types.size() && !found ) {
			Value type = (Value) iterator.next();
			String name = type.getFirstChild( "name" ).getFirstChild( "name" ).strValue();
			String domain = type.getFirstChild( "name" ).getFirstChild( "domain" ).strValue();
			if ( name.equals( typeName ) && domain.equals( typeDomain ) ) {
				found = true;
			}
			index++;
		}
		return types.get( index - 1 );
	}

	private void castingSubType( ValueVector subTypes, String elementName, ValueVector messageVector, ValueVector types, Value response )
		throws FaultException
	{
		boolean found = false;
		int index = 0;
		while( !found && index < subTypes.size() ) {
			if ( subTypes.get( index ).getFirstChild( "name" ).strValue().equals( elementName ) ) {
				found = true;
			}
			index++;
		}
		if ( !found ) {
			throw new FaultException( "TypeMismatch" );
		} else {
			Value subType = subTypes.get( index - 1 );
			// check cardinality
			if ( messageVector.size() < subType.getFirstChild( "Cardinality" ).getFirstChild( "min" ).intValue() ) {
				throw new FaultException( "TypeMismatch" );
			}
			if ( subType.getFirstChild( "cardinality" ).getChildren( "max" ).size() > 0 ) {
				if ( messageVector.size() > subType.getFirstChild( "cardinality" ).getFirstChild( "max" ).intValue() ) {
					throw new FaultException( "TypeMismatch" );
				}
			}
			// casting all the elements
			for( int el = 0; el < messageVector.size(); el++ ) {
				if ( subType.getChildren( "type_inline" ).size() > 0 ) {
					castingType( subType.getFirstChild( "type_inline" ), messageVector.get( el ), types, response.getChildren( elementName ).get( el ) );
				} else if ( subType.getChildren( "type_link" ).size() > 0 ) {
					String name = subType.getFirstChild( "type_link" ).getFirstChild( "name" ).strValue();
					String domain = subType.getFirstChild( "type_link" ).getFirstChild( "domain" ).strValue();
					Value typeToCast = findType( types, name, domain );
					castingType( typeToCast, messageVector.get( el ), types, response.getChildren( elementName ).get( el ) );
				}
			}
		}

	}

	private void castingType( Value typeToCast, Value message, ValueVector types, Value response )
		throws FaultException
	{

		// casting root
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "string_type" ).size() > 0 ) {
			response.setValue( message.strValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
			response.setValue( message.intValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "double_type" ).size() > 0 ) {
			response.setValue( message.doubleValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "any_type" ).size() > 0 ) {
			response.setValue( message.strValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
			response.setValue( message.intValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "void_type" ).size() > 0 ) {
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "long_type" ).size() > 0 ) {
			response.setValue( message.longValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
			response.setValue( message.intValue() );
		}
		if ( typeToCast.getFirstChild( "root_type" ).getChildren( "link" ).size() > 0 ) {
			String domain = "";
			if ( typeToCast.getFirstChild( "root_type" ).getFirstChild( "link" ).getChildren( "domain" ).size() > 0 ) {
				domain = typeToCast.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "domain" ).strValue();
			}
			Value linkRootType = findType( types, typeToCast.getFirstChild( "root_type" ).getFirstChild( "link" ).getFirstChild( "name" ).strValue(), domain ).getFirstChild( "root_type" );
			castingType( linkRootType, message, types, response );
		}

		// casting subTypes
		if ( typeToCast.getChildren( "sub_type" ).size() > 0 ) {
			// ranging over all the subfields of the message
			for( Entry<String, ValueVector> e : message.children().entrySet() ) {
				castingSubType( typeToCast.getChildren( "sub_type" ), e.getKey(), message.getChildren( e.getKey() ), types, response );
			}
		}

	}

	@RequestResponse
	public Value messageTypeCast( Value request )
		throws FaultException
	{
		Value message = request.getFirstChild( "message" );
		String messageTypeName = request.getFirstChild( "types" ).getFirstChild( "messageTypeName" ).getFirstChild( "name" ).strValue();
		String messageTypeDomain = request.getFirstChild( "types" ).getFirstChild( "messageTypeName" ).getFirstChild( "domain" ).strValue();
		ValueVector types = request.getFirstChild( "types" ).getChildren( "types" );
		Value response = Value.create();

		// get message type
		Value messageType = findType( types, messageTypeName, messageTypeDomain );
		// casting root node
		castingType( messageType, message, types, response.getFirstChild( "message" ) );

		return response;
	}
}
