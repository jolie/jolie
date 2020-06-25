/** *************************************************************************
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
 ************************************************************************** */
package joliex.meta;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
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
public class MetaJolie extends JavaService {

	private static final int MAX_CARD = Integer.MAX_VALUE;
	private ArrayList< TypeDefinition > listOfGeneratedTypesInTypeDefinition;
	private ArrayList< Value > listOfGeneratedTypesInValues;
	private final ArrayList< String > nativeTypeList =
		new ArrayList<>( Arrays.asList( "any", "string", "double", "int", "void", "bool", "long", "raw" ) );

	private static class ValueTypeComparator implements Comparator< Value > {

		@Override
		public int compare( Value o1, Value o2 ) {
			return o1.getFirstChild( "name" ).strValue().compareTo( o2.getFirstChild( "name" ).strValue() );
		}
	}

	private Value getNativeType( NativeType type ) {
		Value response = Value.create();
		if( null != type )
			switch( type ) {
			case ANY:
				response.getFirstChild( "any_type" ).setValue( true );
				break;
			case STRING:
				response.getFirstChild( "string_type" ).setValue( true );
				break;
			case DOUBLE:
				response.getFirstChild( "double_type" ).setValue( true );
				break;
			case INT:
				response.getFirstChild( "int_type" ).setValue( true );
				break;
			case VOID:
				response.getFirstChild( "void_type" ).setValue( true );
				break;
			case BOOL:
				response.getFirstChild( "bool_type" ).setValue( true );
				break;
			case LONG:
				response.getFirstChild( "long_type" ).setValue( true );
				break;
			case RAW:
				response.getFirstChild( "raw_type" ).setValue( true );
				break;
			default:
				break;
			}
		return response;
	}

	private Value getNativeType( String type ) {
		Value response = Value.create();
		if( null != type )
			switch( type ) {
			case "any":
				response.getFirstChild( "any_type" ).setValue( true );
				break;
			case "string":
				response.getFirstChild( "string_type" ).setValue( true );
				break;
			case "double":
				response.getFirstChild( "double_type" ).setValue( true );
				break;
			case "int":
				response.getFirstChild( "int_type" ).setValue( true );
				break;
			case "void":
				response.getFirstChild( "void_type" ).setValue( true );
				break;
			case "bool":
				response.getFirstChild( "bool_type" ).setValue( true );
				break;
			case "long":
				response.getFirstChild( "long_type" ).setValue( true );
				break;
			case "raw":
				response.getFirstChild( "raw_type" ).setValue( true );
				break;
			default:
				break;
			}
		return response;
	}

	private boolean isNativeType( String type ) {
		return nativeTypeList.contains( type );
	}

	private Value addCardinality( Range range ) {
		Value response = Value.create();
		response.getFirstChild( "min" ).setValue( range.min() );
		if( range.max() == MAX_CARD ) {
			response.getFirstChild( "infinite" ).setValue( 1 );
		} else {
			response.getFirstChild( "max" ).setValue( range.max() );
		}
		return response;
	}

	private Value getSubType( TypeDefinition type, boolean insertTypeInInterfaceList, TypeDefinition extension ) {
		Value response = Value.create();
		response.getFirstChild( "name" ).setValue( type.id() );
		response.getFirstChild( "cardinality" ).deepCopy( addCardinality( type.cardinality() ) );
		response.getFirstChild( "type" ).deepCopy( getType( type, insertTypeInInterfaceList, extension ) );
		if( !type.getDocumentation().isEmpty() ) {
			response.getFirstChild( "documentation" ).setValue( type.getDocumentation() );
		}
		return response;
	}

	private Value getChoiceType( TypeChoiceDefinition typedef, boolean insertInInterfaceList,
		TypeDefinition extension ) {
		Value type = Value.create();
		Value left = type.getFirstChild( "choice" ).getFirstChild( "left_type" );
		Value right = type.getFirstChild( "choice" ).getFirstChild( "right_type" );

		left.deepCopy( getType( typedef.left(), insertInInterfaceList, extension ) );
		right.deepCopy( getType( typedef.right(), insertInInterfaceList, extension ) );

		return type;
	}

	private void insertTypeDefinition( TypeDefinition typedef, TypeDefinition extension ) {
		// to be optimized, similar code with addType
		if( !listOfGeneratedTypesInTypeDefinition.contains( typedef ) && !isNativeType( typedef.id() )
			&& !typedef.id().equals( "undefined" ) ) {
			listOfGeneratedTypesInTypeDefinition.add( typedef );
			listOfGeneratedTypesInValues.add( getTypeDefinition( typedef, true, extension ) );
		}
	}

	private Value getTypeDefinition( TypeDefinition typedef, boolean insertInInterfaceList, TypeDefinition extension ) {
		Value type = Value.create();
		type.getFirstChild( "type" ).deepCopy( getType( typedef, insertInInterfaceList, extension ) );
		type.getFirstChild( "name" ).setValue( typedef.id() );
		if( !typedef.getDocumentation().isEmpty() ) {
			type.getFirstChild( "documentation" ).setValue( typedef.getDocumentation() );
		}
		return type;
	}

	private Value getTypeLink( TypeDefinitionLink typedef, boolean insertInInterfaceList, TypeDefinition extension ) {
		Value type = Value.create();
		type.getFirstChild( "link_name" ).setValue( ((TypeDefinitionLink) typedef).linkedTypeName() );
		if( insertInInterfaceList ) {
			insertTypeDefinition( ((TypeDefinitionLink) typedef).linkedType(), extension );
		}
		return type;
	}

	private Value getTypeInlineDefinition( TypeInlineDefinition typedef, boolean insertInInterfaceList,
		TypeDefinition extension ) {
		Value type = Value.create();
		type.getFirstChild( "root_type" ).deepCopy( getNativeType( typedef.nativeType() ) );
		if( typedef.hasSubTypes() ) {
			int subtype_counter = 0;
			for( Entry< String, TypeDefinition > entry : typedef.subTypes() ) {
				type.getChildren( "sub_type" ).get( subtype_counter )
					.deepCopy( getSubType( entry.getValue(), insertInInterfaceList, null ) );
				subtype_counter++;
			}
		}
		if( extension != null && extension instanceof TypeInlineDefinition ) {
			final TypeInlineDefinition extensionTypeInline = (TypeInlineDefinition) extension;
			int subtype_counter = type.getChildren( "sub_type" ).size();
			if( extensionTypeInline.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : extensionTypeInline.subTypes() ) {
					type.getChildren( "sub_type" ).get( subtype_counter )
						.deepCopy( getSubType( entry.getValue(), true, null ) );
					subtype_counter++;
				}
			}
		}
		return type;
	}

	private Value getTypeUndefined() {
		Value type = Value.create();
		type.getFirstChild( "undefined" ).setValue( true );
		return type;
	}

	private Value getType( TypeDefinition typedef, boolean insertInInterfaceList, TypeDefinition extension ) {
		if( typedef instanceof TypeChoiceDefinition ) {
			return getChoiceType( (TypeChoiceDefinition) typedef, insertInInterfaceList, extension );
		} else if( typedef instanceof TypeDefinitionLink ) {
			if( ((TypeDefinitionLink) typedef).linkedType() instanceof TypeDefinitionUndefined ) {
				return getTypeUndefined();
			} else {
				return getTypeLink( (TypeDefinitionLink) typedef, insertInInterfaceList, extension );
			}
		} else {
			return getTypeInlineDefinition( (TypeInlineDefinition) typedef, insertInInterfaceList, extension );
		}
	}

	private List< TypeDefinition > addTypeDefinition( List< TypeDefinition > types, TypeDefinition typedef ) {

		if( !typedef.id().equals( "undefined" ) ) {
			if( !types.contains( typedef ) && !isNativeType( typedef.id() ) ) {
				types.add( typedef );
				if( typedef instanceof TypeDefinitionLink ) {
					addTypeDefinition( types, ((TypeDefinitionLink) typedef).linkedType() );
				} else if( typedef instanceof TypeInlineDefinition ) {
					TypeInlineDefinition td = (TypeInlineDefinition) typedef;
					if( td.hasSubTypes() ) {
						for( Entry< String, TypeDefinition > entry : td.subTypes() ) {
							addSubTypeDefinition( types, entry.getValue() );
						}
					}
				} else if( typedef instanceof TypeChoiceDefinition ) {
					addTypeDefinition( types, ((TypeChoiceDefinition) typedef).left() );
					addTypeDefinition( types, ((TypeChoiceDefinition) typedef).right() );
				}
			}
		}
		return types;
	}

	private List< TypeDefinition > addSubTypeDefinition( List< TypeDefinition > types, TypeDefinition subtype ) {
		if( subtype instanceof TypeDefinitionLink ) {
			addTypeDefinition( types, ((TypeDefinitionLink) subtype).linkedType() );
		} else {
			TypeInlineDefinition td = (TypeInlineDefinition) subtype;
			if( td.hasSubTypes() ) {
				for( Entry< String, TypeDefinition > entry : td.subTypes() ) {
					addSubTypeDefinition( types, entry.getValue() );
				}
			}
		}

		return types;
	}

	private Value getInterface(
		InterfaceDefinition interfaceDefinition,
		OneWayOperationDeclaration owExtender,
		RequestResponseOperationDeclaration rrExtender ) {
		Value itf = Value.create();

		itf.getFirstChild( "name" ).setValue( interfaceDefinition.name() );
		if( !interfaceDefinition.getDocumentation().isEmpty() ) {
			itf.getFirstChild( "documentation" ).setValue( interfaceDefinition.getDocumentation() );
		}

		ValueVector operations = itf.getChildren( "operations" );

		// scans operations and types
		Map< String, OperationDeclaration > operationMap = interfaceDefinition.operationsMap();
		ArrayList< String > opkeylist = new ArrayList<>();
		opkeylist.addAll( operationMap.keySet() );
		Collections.sort( opkeylist );


		for( String operationName : opkeylist ) {
			Value current_operation = Value.create();
			OperationDeclaration operationDeclaration = operationMap.get( operationName );
			if( operationDeclaration instanceof OneWayOperationDeclaration ) {
				OneWayOperationDeclaration oneWayOperation = (OneWayOperationDeclaration) operationDeclaration;
				current_operation.getFirstChild( "operation_name" ).setValue( oneWayOperation.id() );
				current_operation.getFirstChild( "documentation" ).setValue( oneWayOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).setValue( oneWayOperation.requestType().id() );

				if( !isNativeType( oneWayOperation.requestType().id() ) ) {
					if( owExtender == null ) {
						insertTypeDefinition( oneWayOperation.requestType(), null );
					} else {
						insertTypeDefinition( oneWayOperation.requestType(), owExtender.requestType() );
					}
				}

			} else {
				RequestResponseOperationDeclaration requestResponseOperation =
					(RequestResponseOperationDeclaration) operationDeclaration;
				current_operation.getFirstChild( "operation_name" ).setValue( requestResponseOperation.id() );
				current_operation.getFirstChild( "documentation" )
					.setValue( requestResponseOperation.getDocumentation() );
				current_operation.getFirstChild( "input" ).setValue( requestResponseOperation.requestType().id() );
				current_operation.getFirstChild( "output" ).setValue( requestResponseOperation.responseType().id() );
				if( !isNativeType( requestResponseOperation.requestType().id() ) ) {
					if( rrExtender == null ) {
						insertTypeDefinition( requestResponseOperation.requestType(), null );
					} else {
						insertTypeDefinition( requestResponseOperation.requestType(), rrExtender.requestType() );
					}
				}
				if( !isNativeType( requestResponseOperation.responseType().id() ) ) {
					if( rrExtender == null ) {
						insertTypeDefinition( requestResponseOperation.responseType(), null );
					} else {
						insertTypeDefinition( requestResponseOperation.responseType(), rrExtender.responseType() );
					}
				}
				Map< String, TypeDefinition > faults = requestResponseOperation.faults();
				int faultCounter = 0;
				for( Entry< String, TypeDefinition > f : faults.entrySet() ) {
					if( rrExtender == null ) {
						current_operation.getChildren( "fault" ).get( faultCounter )
							.deepCopy( getFault( f.getKey(), f.getValue(), null ) );
					} else {
						current_operation.getChildren( "fault" ).get( faultCounter )
							.deepCopy( getFault( f.getKey(), f.getValue(), rrExtender.faults().get( f.getKey() ) ) );
					}
					faultCounter++;
				}
			}
			operations.add( current_operation );
		}

		return itf;
	}



	private List< InterfaceDefinition > addInterfaceToList( List< InterfaceDefinition > list,
		InterfaceDefinition intf ) {
		if( !list.contains( intf ) ) {
			list.add( intf );
		}
		return list;
	}


	private Value getOutputPort( OutputPortInfo portInfo ) {

		Value response = Value.create();
		// setting the name of the port
		response.getFirstChild( "name" ).setValue( portInfo.id() );

		if( ((OutputPortInfo) portInfo).location() != null ) {
			response.getFirstChild( "location" ).setValue( ((OutputPortInfo) portInfo).location().toString() );
		} else {
			response.getFirstChild( "location" ).setValue( "undefined" );
		}
		if( ((OutputPortInfo) portInfo).protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( ((OutputPortInfo) portInfo).protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			response.getChildren( "interfaces" ).get( intf_index )
				.deepCopy( getInterfaceAndInsertTypes( interfaceDefinition, null, null ) );

		}

		return response;

	}

	private Value getInputPort( InputPortInfo portInfo, OutputPortInfo[] outputPortList ) {

		Value response = Value.create();
		// setting the name of the port
		response.getFirstChild( "name" ).setValue( portInfo.id() );

		response.getFirstChild( "location" ).setValue( ((InputPortInfo) portInfo).location().toString() );
		if( ((InputPortInfo) portInfo).protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( ((InputPortInfo) portInfo).protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			response.getChildren( "interfaces" ).get( intf_index )
				.deepCopy( getInterfaceAndInsertTypes( interfaceDefinition, null, null ) );

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
			if( portInfo.aggregationList()[ x ].interfaceExtender() != null ) {
				// the interfaces of the outputPort must be extended
				// only default extension is processed. TODO: extending also specific operation declaration

				extender = portInfo.aggregationList()[ x ].interfaceExtender();
				if( extender.defaultOneWayOperation() != null ) {
					owExtender = extender.defaultOneWayOperation();
				}
				if( extender.defaultRequestResponseOperation() != null ) {
					rrExtender = extender.defaultRequestResponseOperation();
				}
			}
			for( InterfaceDefinition interfaceDefinition : outputPortList[ i ].getInterfaceList() ) {
				Value inputInterface = response.getChildren( "interfaces" ).get( curItfIndex );
				if( extender != null ) {
					inputInterface
						.deepCopy( getInterfaceAndInsertTypes( interfaceDefinition, owExtender, rrExtender ) );
				} else {
					inputInterface.deepCopy( getInterfaceAndInsertTypes( interfaceDefinition, null, null ) );
				}
				curItfIndex++;
			}

		}

		return response;

	}

	private Value getInputPortInfoAndAddInterfacesToList( InputPortInfo portInfo, OutputPortInfo[] outputPortList,
		List< InterfaceDefinition > interfaces ) {

		Value response = Value.create();
		// setting the name of the port
		response.getFirstChild( "name" ).setValue( portInfo.id() );

		response.getFirstChild( "location" ).setValue( ((InputPortInfo) portInfo).location().toString() );
		if( ((InputPortInfo) portInfo).protocolId() != null ) {
			response.getFirstChild( "protocol" ).setValue( ((InputPortInfo) portInfo).protocolId() );
		} else {
			response.getFirstChild( "protocol" ).setValue( "" );
		}

		// scan all the interfaces of the inputPort
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			response.getChildren( "interfaces" ).get( intf_index ).getFirstChild( "name" )
				.setValue( interfaceDefinition.name() );
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
				response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" )
					.setValue( interfaceDefinition.name() );
				addInterfaceToList( interfaces, interfaceDefinition );
				intf++;
			}
		}

		return response;

	}

	private Value getFault( String faultname, TypeDefinition typedef, TypeDefinition extension ) {
		Value type = Value.create();
		type.getFirstChild( "name" ).setValue( faultname );
		if( typedef != null ) {
			if( typedef.id().equals( "undefined" ) ) {
				type.getFirstChild( "type" ).deepCopy( getTypeUndefined() );
			} else if( !isNativeType( typedef.id() ) ) {
				type.getFirstChild( "type" ).getFirstChild( "link_name" ).setValue( typedef.id() );
				insertTypeDefinition( typedef, extension );
			} else {
				type.getFirstChild( "type" ).deepCopy( getNativeType( typedef.id() ) );
			}
		}
		return type;
	}

	private Value getInterfaceAndInsertTypes(
		InterfaceDefinition interfaceDefinition,
		OneWayOperationDeclaration owExtender,
		RequestResponseOperationDeclaration rrExtender ) {
		Value inputInterface = Value.create();
		listOfGeneratedTypesInTypeDefinition = new ArrayList<>();
		listOfGeneratedTypesInValues = new ArrayList<>();

		inputInterface.deepCopy( getInterface( interfaceDefinition, owExtender, rrExtender ) );


		listOfGeneratedTypesInValues.sort( new ValueTypeComparator() );
		listOfGeneratedTypesInValues.stream().forEach( v -> inputInterface.getChildren( "types" ).add( v ) );

		return inputInterface;

	}

	private Value getPort( PortInfo portInfo, List< InterfaceDefinition > interfaces ) {
		Value response = Value.create();

		// setting the name of the port
		response.getFirstChild( "name" ).setValue( portInfo.id() );

		if( portInfo instanceof InputPortInfo ) {
			InputPortInfo port = (InputPortInfo) portInfo;
			if( port.location() != null ) {
				response.getFirstChild( "location" ).setValue( port.location().toString() );
			} else {
				response.getFirstChild( "location" ).setValue( "local" );
			}
			if( port.protocolId() != null ) {
				response.getFirstChild( "protocol" ).setValue( port.protocolId() );
			} else {
				response.getFirstChild( "protocol" ).setValue( "" );
			}

		} else if( portInfo instanceof OutputPortInfo ) {
			OutputPortInfo port = (OutputPortInfo) portInfo;
			if( port.location() != null ) {
				response.getFirstChild( "location" ).setValue( port.location().toString() );
			} else {
				response.getFirstChild( "location" ).setValue( "local" );
			}
			if( port.protocolId() != null ) {
				response.getFirstChild( "protocol" ).setValue( port.protocolId() );
			} else {
				response.getFirstChild( "protocol" ).setValue( "" );
			}
		}

		// scans interfaces
		List< InterfaceDefinition > interfaceList = portInfo.getInterfaceList();
		for( int intf = 0; intf < interfaceList.size(); intf++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf );

			// setting the name of the interface within the port response
			response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" )
				.setValue( interfaceDefinition.name() );
			interfaces = addInterfaceToList( interfaces, interfaceDefinition );

		}

		return response;
	}

	private String[] getArgs( String filename ) {
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for( String s : interpreter().includePaths() ) {
			builder.append( s );
			if( ++i < interpreter().includePaths().length ) {
				builder.append( jolie.lang.Constants.PATH_SEPARATOR );
			}
		}

		return new String[] {
			"-i",
			builder.toString(),
			// String.join( jolie.lang.Constants.pathSeparator, interpreter().includePaths() ),
			filename
		};
	}

	private Value findType( ValueVector types, String typeName ) {
		Iterator< Value > iterator = types.iterator();
		boolean found = false;
		int index = 0;
		while( index < types.size() && !found ) {
			Value type = (Value) iterator.next();
			String name = type.getFirstChild( "name" ).strValue();
			if( name.equals( typeName ) ) {
				found = true;
			}
			index++;
		}
		return types.get( index - 1 ).getFirstChild( "type" );
	}

	private void castingSubType( ValueVector subTypes, String elementName, ValueVector messageVector, ValueVector types,
		Value response )
		throws FaultException {
		boolean found = false;
		int index = 0;
		while( !found && index < subTypes.size() ) {
			if( subTypes.get( index ).getFirstChild( "name" ).strValue().equals( elementName ) ) {
				found = true;
			}
			index++;
		}
		if( !found ) {
			throw new FaultException( "TypeMismatch" );
		} else {
			Value subType = subTypes.get( index - 1 );
			// check cardinality
			if( messageVector.size() < subType.getFirstChild( "cardinality" ).getFirstChild( "min" ).intValue() ) {
				throw new FaultException( "TypeMismatch",
					"the min cardinality does not correspond for field " + elementName );
			}
			if( subType.getFirstChild( "cardinality" ).getChildren( "max" ).size() > 0 ) {
				if( messageVector.size() > subType.getFirstChild( "cardinality" ).getFirstChild( "max" ).intValue() ) {
					throw new FaultException( "TypeMismatch",
						" the max cardinality does not correspond for field \" + elementName" );
				}
			}
			// casting all the elements
			for( int el = 0; el < messageVector.size(); el++ ) {
				castingType( subType.getFirstChild( "type" ), messageVector.get( el ), types,
					response.getChildren( elementName ).get( el ) );
			}
		}

	}

	private void castingType( Value typeToCast, Value message, ValueVector types, Value response )
		throws FaultException {

		// casting root
		if( typeToCast.getChildren( "root_type" ).size() > 0 ) {
			// TypeInLine
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "string_type" ).size() > 0 ) {
				response.setValue( message.strValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "bool_type" ).size() > 0 ) {
				response.setValue( message.boolValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
				response.setValue( message.intValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "double_type" ).size() > 0 ) {
				response.setValue( message.doubleValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "any_type" ).size() > 0 ) {
				response.setValue( message.strValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
				response.setValue( message.intValue() );
			}
			// if( typeToCast.getFirstChild( "root_type" ).getChildren( "void_type" ).size() > 0 ) {
			// }
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "long_type" ).size() > 0 ) {
				response.setValue( message.longValue() );
			}
			if( typeToCast.getFirstChild( "root_type" ).getChildren( "int_type" ).size() > 0 ) {
				response.setValue( message.intValue() );
			}

			// casting subTypes
			if( typeToCast.getChildren( "sub_type" ).size() > 0 ) {
				// ranging over all the subfields of the message
				if( message.children().size() == 0 ) {
					throw new FaultException( "TypeMismatch", "No sub fields found when type requests them" );
				} else {
					ArrayList< String > foundSubTypes = new ArrayList<>();
					for( Entry< String, ValueVector > e : message.children().entrySet() ) {
						castingSubType( typeToCast.getChildren( "sub_type" ), e.getKey(),
							message.getChildren( e.getKey() ), types, response );
						foundSubTypes.add( e.getKey() );
					}
					for( Value subtype : typeToCast.getChildren( "sub_type" ) ) {
						if( !foundSubTypes.contains( subtype.getFirstChild( "name" ).strValue() )
							&& subtype.getFirstChild( "cardinality" ).getFirstChild( "min" ).intValue() > 0 ) {
							throw new FaultException( "TypeMismatch", "Field "
								+ subtype.getFirstChild( "name" ).strValue() + " must be present in the message" );
						}
					}
				}
			} else if( message.children().size() > 0 ) {
				throw new FaultException( "TypeMismatch",
					"The message contains fields for a type defines with no fields" );
			}
		} else if( typeToCast.getChildren( "link_name" ).size() > 0 ) {
			// TypeLink
			Value linkedType = findType( types, typeToCast.getFirstChild( "link_name" ).strValue() );
			castingType( linkedType, message, types, response );
		} else if( typeToCast.getChildren( "choice" ).size() > 0 ) {
			// TypeChoice
			try {
				castingType( typeToCast.getFirstChild( "choice" ).getFirstChild( "left_type" ), message, types,
					response );
			} catch( FaultException f ) {
				if( f.faultName().equals( "TypeMismatch" ) ) {
					castingType( typeToCast.getFirstChild( "choice" ).getFirstChild( "right_type" ), message, types,
						response );
				}
			}
		}



	}

	@RequestResponse
	public Value getInputPortMetaData( Value request ) throws FaultException {
		Value response = Value.create();
		try {
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );

			CommandLineParser cmdParser = new CommandLineParser( args, interpreter().getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterParameters().inputStream(),
				cmdParser.getInterpreterParameters().programFilepath().toURI(),
				cmdParser.getInterpreterParameters().charset(),
				cmdParser.getInterpreterParameters().includePaths(),
				cmdParser.getInterpreterParameters().jolieClassLoader(),
				cmdParser.getInterpreterParameters().constants(), true );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			URI originalFile = program.context().source();

			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
			ValueVector input = response.getChildren( "input" );

			if( inputPortList.length > 0 ) {
				for( int ip = 0; ip < inputPortList.length; ip++ ) {
					InputPortInfo inputPort = inputPortList[ ip ];
					input.get( ip ).deepCopy( getInputPort( inputPort, inspector.getOutputPorts() ) );
				}
			}
			cmdParser.close();

		} catch( CommandLineException | IOException e ) {
			throw new FaultException( "InputPortMetaDataFault", e );
		} catch( ParserException e ) {
			Value fault = Value.create();
			fault.getFirstChild( "message" ).setValue( e.getMessage() );
			fault.getFirstChild( "line" ).setValue( e.context().line() );
			fault.getFirstChild( "sourceName" ).setValue( e.context().sourceName() );
			throw new FaultException( "ParserException", fault );
		} catch( SemanticException e ) {
			Value fault = Value.create();
			List< SemanticException.SemanticError > errorList = e.getErrorList();
			for( int i = 0; i < errorList.size(); i++ ) {
				fault.getChildren( "error" ).get( i ).getFirstChild( "message" )
					.setValue( errorList.get( i ).getMessage() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "line" )
					.setValue( errorList.get( i ).context().line() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "sourceName" )
					.setValue( errorList.get( i ).context().sourceName() );
			}
			throw new FaultException( "SemanticException", fault );
		}

		return response;
	}

	@RequestResponse
	public Value getOutputPortMetaData( Value request ) throws FaultException {
		Value response = Value.create();
		try {
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );

			CommandLineParser cmdParser = new CommandLineParser( args, interpreter().getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterParameters().inputStream(),
				cmdParser.getInterpreterParameters().programFilepath().toURI(),
				cmdParser.getInterpreterParameters().charset(),
				cmdParser.getInterpreterParameters().includePaths(),
				cmdParser.getInterpreterParameters().jolieClassLoader(),
				cmdParser.getInterpreterParameters().constants(), true );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			OutputPortInfo[] outputPortList = inspector.getOutputPorts();
			ValueVector output = response.getChildren( "output" );

			if( outputPortList.length > 0 ) {
				for( int ip = 0; ip < outputPortList.length; ip++ ) {
					OutputPortInfo outputPortInfo = outputPortList[ ip ];
					output.get( ip ).deepCopy( getOutputPort( outputPortInfo ) );
				}
			}
			cmdParser.close();

		} catch( CommandLineException | IOException e ) {
			throw new FaultException( "OutputPortMetaDataFault", e );
		} catch( ParserException e ) {
			Value fault = Value.create();
			fault.getFirstChild( "message" ).setValue( e.getMessage() );
			fault.getFirstChild( "line" ).setValue( e.context().line() );
			fault.getFirstChild( "sourceName" ).setValue( e.context().sourceName() );
			throw new FaultException( "ParserException", fault );
		} catch( SemanticException e ) {
			Value fault = Value.create();
			List< SemanticException.SemanticError > errorList = e.getErrorList();
			for( int i = 0; i < errorList.size(); i++ ) {
				fault.getChildren( "error" ).get( i ).getFirstChild( "message" )
					.setValue( errorList.get( i ).getMessage() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "line" )
					.setValue( errorList.get( i ).context().line() );
				fault.getChildren( "error" ).get( i ).getFirstChild( "sourceName" )
					.setValue( errorList.get( i ).context().sourceName() );
			}
			throw new FaultException( "SemanticException", fault );
		}

		return response;
	}

	@RequestResponse
	public Value messageTypeCast( Value request )
		throws FaultException {
		Value message = request.getFirstChild( "message" );
		String messageTypeName = request.getFirstChild( "types" ).getFirstChild( "messageTypeName" ).strValue();
		ValueVector types = request.getFirstChild( "types" ).getChildren( "types" );
		Value response = Value.create();

		// get message type
		Value messageType = findType( types, messageTypeName );
		// casting root node
		castingType( messageType, message, types, response.getFirstChild( "message" ) );

		return response;
	}

	@RequestResponse
	public Value checkNativeType( Value request ) {
		Value response = Value.create();
		response.getFirstChild( "result" ).setValue( isNativeType( request.getFirstChild( "type_name" ).strValue() ) );
		return response;
	}

	@RequestResponse
	public Value getMetaData( Value request )
		throws FaultException {

		listOfGeneratedTypesInValues = new ArrayList<>();
		listOfGeneratedTypesInTypeDefinition = new ArrayList<>();
		List< InterfaceDefinition > interfaces = new ArrayList<>();
		Value response = Value.create();
		try {
			String[] args = getArgs( request.getFirstChild( "filename" ).strValue() );

			CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterParameters().inputStream(),
				cmdParser.getInterpreterParameters().programFilepath().toURI(),
				cmdParser.getInterpreterParameters().charset(),
				cmdParser.getInterpreterParameters().includePaths(),
				cmdParser.getInterpreterParameters().jolieClassLoader(),
				cmdParser.getInterpreterParameters().constants(), true );
			ProgramInspector inspector = ParsingUtils.createInspector( program );

			URI originalFile = program.context().source();

			cmdParser.close();

			// TODO: now the name of the service cannot be retrieved, to be considered during Jolie 2.0
			// refactoring
			response.getFirstChild( "service" ).getFirstChild( "name" ).setValue( "" );

			OutputPortInfo[] outputPortList = inspector.getOutputPorts();
			if( outputPortList.length > 0 ) {
				ValueVector output = response.getChildren( "output" );
				for( int op = 0; op < outputPortList.length; op++ ) {
					OutputPortInfo outputPort = outputPortList[ op ];
					output.get( op ).deepCopy( getPort( outputPort, interfaces ) );
					response.getFirstChild( "service" ).getChildren( "output" ).get( op ).setValue( outputPort.id() );
				}
			}

			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
			ValueVector input = response.getChildren( "input" );
			if( inputPortList.length > 0 ) {
				for( int ip = 0; ip < inputPortList.length; ip++ ) {
					InputPortInfo inputPort = inputPortList[ ip ];
					input.get( ip )
						.deepCopy( getInputPortInfoAndAddInterfacesToList( inputPort, outputPortList, interfaces ) );
					response.getFirstChild( "service" ).getChildren( "input" ).get( ip ).setValue( inputPort.id() );
				}
			}


			// adding interfaces
			for( int intf = 0; intf < interfaces.size(); intf++ ) {
				InterfaceDefinition interfaceDefinition = interfaces.get( intf );
				response.getChildren( "interfaces" ).get( intf )
					.deepCopy( getInterface( interfaceDefinition, null, null ) );
			}

			// adding types
			for( int tp = 0; tp < listOfGeneratedTypesInValues.size(); tp++ ) {
				Value typeDefinition = listOfGeneratedTypesInValues.get( tp );
				response.getChildren( "types" ).get( tp ).deepCopy( typeDefinition );
			}

			// adding embedded services
			EmbeddedServiceNode[] embeddedServices = inspector.getEmbeddedServices();
			for( int es = 0; es < embeddedServices.length; es++ ) {
				response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "type" )
					.setValue( embeddedServices[ es ].type().toString() );
				response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "servicepath" )
					.setValue( embeddedServices[ es ].servicePath() );
				if( embeddedServices[ es ].portId() != null ) {
					response.getChildren( "embeddedServices" ).get( es ).getFirstChild( "portId" )
						.setValue( embeddedServices[ es ].portId() );
				}
			}

			// adding communication dependencies
			Map< OLSyntaxNode, List< OLSyntaxNode > > communicationDependencies =
				inspector.getBehaviouralDependencies();
			if( communicationDependencies != null && !communicationDependencies.isEmpty() ) {
				final ValueVector comDepVect = response.getChildren( "communication_dependencies" );
				communicationDependencies.entrySet().stream()
					.forEach( p -> {
						Value v = Value.create();
						if( p.getKey() instanceof RequestResponseOperationStatement ) {
							v.getFirstChild( "input_operation" ).getFirstChild( "type" ).setValue( "RequestResponse" );
							v.getFirstChild( "input_operation" ).getFirstChild( "name" )
								.setValue( ((RequestResponseOperationStatement) p.getKey()).id() );
						} else if( p.getKey() instanceof OneWayOperationStatement ) {
							v.getFirstChild( "input_operation" ).getFirstChild( "type" ).setValue( "OneWay" );
							v.getFirstChild( "input_operation" ).getFirstChild( "name" )
								.setValue( ((OneWayOperationStatement) p.getKey()).id() );
						}
						if( p.getValue().size() > 0 ) {
							ValueVector dependencies = v.getChildren( "dependencies" );
							for( OLSyntaxNode o : p.getValue() ) {
								Value d = Value.create();
								if( o instanceof RequestResponseOperationStatement ) {
									d.getFirstChild( "name" ).setValue( ((RequestResponseOperationStatement) o).id() );
									d.getFirstChild( "type" ).setValue( "RequestResponse" );
								}
								if( o instanceof OneWayOperationStatement ) {
									d.getFirstChild( "name" ).setValue( ((OneWayOperationStatement) o).id() );
									d.getFirstChild( "type" ).setValue( "OneWay" );
								}
								if( o instanceof NotificationOperationStatement ) {
									d.getFirstChild( "name" ).setValue( ((NotificationOperationStatement) o).id() );
									d.getFirstChild( "type" ).setValue( "Notification" );
									d.getFirstChild( "port" )
										.setValue( ((NotificationOperationStatement) o).outputPortId() );
								}
								if( o instanceof SolicitResponseOperationStatement ) {
									d.getFirstChild( "name" ).setValue( ((SolicitResponseOperationStatement) o).id() );
									d.getFirstChild( "type" ).setValue( "SolicitResponse" );
									d.getFirstChild( "port" )
										.setValue( ((SolicitResponseOperationStatement) o).outputPortId() );
								}
								dependencies.add( d );
							}
						}
						comDepVect.add( v );
					} );
			}

		} catch( CommandLineException | IOException e ) {
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
				fault.getChildren( "error" ).get( i ).getFirstChild( "sourceName" )
					.setValue( error.context().sourceName() );
				i++;
			}
			throw new FaultException( "SemanticException", fault );
		}
		return response;
	}

	@RequestResponse
	public Value getNativeTypeFromString( Value request ) throws FaultException {
		if( isNativeType( request.getFirstChild( "type_name" ).strValue() ) ) {
			return getNativeType( request.getFirstChild( "type_name" ).strValue() );
		} else {
			throw new FaultException( "NativeTypeDoesNotExist" );
		}
	}

	@RequestResponse
	public Value getNativeTypeStringList( Value request ) {
		Value response = Value.create();
		for( int i = 0; i < nativeTypeList.size(); i++ ) {
			response.getChildren( "native_type" ).get( i ).setValue( nativeTypeList.get( i ) );
		}
		return response;
	}

}
