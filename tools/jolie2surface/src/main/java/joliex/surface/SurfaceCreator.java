/***************************************************************************
 *   Copyright (C) 2011 by Claudio Guidi <cguidi@italianasoftware.com>     *
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
package joliex.surface;

import java.util.ArrayList;
import java.util.Map.Entry;

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
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.Interfaces;
import jolie.lang.parse.util.ProgramInspector;
import jolie.util.Range;

/**
 *
 * @author Claudio Guidi
 *
 *         Modified by Francesco Bullini, 05/07/2012
 */
public class SurfaceCreator {
	private final ProgramInspector inspector;
	private ArrayList< RequestResponseOperationDeclaration > rrVector;
	private ArrayList< OneWayOperationDeclaration > owVector;
	private ArrayList< String > typesVector;
	private ArrayList< TypeDefinition > auxTypesVector;
	private final static int MAX_CARD = Integer.MAX_VALUE;


	public SurfaceCreator( ProgramInspector inspector ) {
		this.inspector = inspector;
	}

	public void ConvertDocument( String inputPortToCreate, boolean noOutputPort, boolean noLocation,
		boolean noProtocol ) {

		ArrayList< InterfaceDefinition > interface_vector = new ArrayList<>();
		rrVector = new ArrayList<>();
		owVector = new ArrayList<>();
		typesVector = new ArrayList<>();
		auxTypesVector = new ArrayList<>();

		// find inputPort

		InputPortInfo[] inputPortList = inspector.getInputPorts();

		InputPortInfo inputPort = null;
		for( InputPortInfo iP : inputPortList ) {
			if( iP.id().equals( inputPortToCreate ) ) {
				inputPort = iP;
			}
		}
		if( inputPort == null ) {
			throw new IllegalArgumentException( "Error! inputPort not found!" );
		}

		// extracts the list of all the interfaces to be parsed
		// extracts interfaces declared into Interfaces
		interface_vector.addAll( inputPort.getInterfaceList() );
		OutputPortInfo[] outputPortList = inspector.getOutputPorts();
		// extracts interfaces from aggregated outputPorts
		for( int x = 0; x < inputPort.aggregationList().length; x++ ) {
			int i = 0;
			while( !inputPort.aggregationList()[ x ].outputPortList()[ 0 ].equals( outputPortList[ i ].id() ) ) {
				i++;

			}
			for( InterfaceDefinition interfaceDefinition : outputPortList[ i ].getInterfaceList() ) {
				interface_vector.add( Interfaces.extend( interfaceDefinition,
					inputPort.aggregationList()[ x ].interfaceExtender(), inputPort.id() ) );
			}
		}

		// for each interface extract the list of all the available operations and types
		for( InterfaceDefinition interfaceDefinition : interface_vector ) {
			addOperation( interfaceDefinition );
		}

		// create oputput
		createOutput( inputPort, noOutputPort, noLocation, noProtocol );

	}

	private void addOperation( InterfaceDefinition interfaceDefinition ) {
		for( OperationDeclaration op : interfaceDefinition.operationsMap().values() ) {
			if( op instanceof RequestResponseOperationDeclaration ) {
				rrVector.add( (RequestResponseOperationDeclaration) op );
			} else {
				owVector.add( (OneWayOperationDeclaration) op );
			}
		}
	}

	private String getOWString( OneWayOperationDeclaration ow ) {
		return ow.id() + "( " + ow.requestType().name() + " )";
	}

	private String getRRString( RequestResponseOperationDeclaration rr ) {
		String ret = rr.id() + "( " + rr.requestType().name() + " )( " + rr.responseType().name() + " )";
		if( rr.faults().size() > 0 ) {
			ret = ret + " throws ";
			boolean flag = false;
			for( Entry< String, TypeDefinition > fault : rr.faults().entrySet() ) {
				if( flag == false ) {
					flag = true;
				} else {
					ret = ret + " ";
				}
				ret = ret + fault.getKey();
				if( fault.getValue() != null ) {
					ret = ret + "( " + fault.getValue().name() + " )";
				}
			}
		}
		return ret;
	}

	private String getMax( int max ) {
		if( max == MAX_CARD ) {
			return "*";
		} else {
			return Integer.toString( max );
		}
	}

	private String getCardinality( Range card ) {
		return (card.min() == 1 && card.max() == 1) ? "" : ("[" + card.min() + "," + getMax( card.max() ) + "]");
	}

	private boolean choice;

	private String getSubType( TypeDefinition type, int indent ) {
		String ret = "";

		if( choice ) {
			choice = false;
		} else {
			for( int y = 0; y < indent; y++ ) {
				ret = ret + "\t";
			}

			ret = ret + "." + type.name() + getCardinality( type.cardinality() ) + ":";
		}

		if( type instanceof TypeDefinitionLink ) {
			ret = ret + ((TypeDefinitionLink) type).linkedTypeName();
			if( !auxTypesVector.contains( ((TypeDefinitionLink) type).linkedType() ) ) {
				auxTypesVector.add( ((TypeDefinitionLink) type).linkedType() );
			}

		} else if( type instanceof TypeInlineDefinition ) {
			TypeInlineDefinition def = (TypeInlineDefinition) type;
			ret = ret + def.basicType().nativeType().id();
			if( def.hasSubTypes() ) {
				ret = ret + " {\n";
				for( Entry< String, TypeDefinition > entry : def.subTypes() ) {
					ret = ret + getSubType( entry.getValue(), indent + 1 ) + "\n";
				}
				for( int y = 0; y < indent; y++ ) {
					ret = ret + "\t";
				}
				ret = ret + "}";
			} else if( ((TypeInlineDefinition) type).untypedSubTypes() ) {
				ret = ret + " { ? }";
			}
		} else if( type instanceof TypeChoiceDefinition ) {
			choice = true;
			ret += getSubType( ((TypeChoiceDefinition) type).left(), indent ) + " | ";
			choice = true;
			ret += getSubType( ((TypeChoiceDefinition) type).right(), indent );

		}

		return ret;
	}

	private String getType( TypeDefinition type ) {
		String ret = "";
		if( !typesVector.contains( type.name() ) && !NativeType.isNativeTypeKeyword( type.name() )
			&& !type.name().equals( "undefined" ) ) {

			System.out.print( "type " + type.name() + ":" );
			checkType( type );
			System.out.println();
			typesVector.add( type.name() );
		}

		return ret;
	}

	private void checkType( TypeDefinition type ) {
		if( type instanceof TypeDefinitionLink ) {
			System.out.print( ((TypeDefinitionLink) type).linkedTypeName() );
			if( !auxTypesVector.contains( ((TypeDefinitionLink) type).linkedType() ) ) {
				auxTypesVector.add( ((TypeDefinitionLink) type).linkedType() );
			}
		} else if( type instanceof TypeInlineDefinition ) {
			TypeInlineDefinition def = (TypeInlineDefinition) type;
			System.out.print( def.basicType().nativeType().id() );
			if( def.hasSubTypes() ) {
				System.out.print( " {\n" );
				for( Entry< String, TypeDefinition > entry : def.subTypes() ) {
					System.out.print( getSubType( entry.getValue(), 1 ) + "\n" );
				}

				System.out.print( "}" );
			} else {

				if( ((TypeInlineDefinition) type).untypedSubTypes() ) {
					System.out.print( " { ? }" );
				}

			}
		} else if( type instanceof TypeChoiceDefinition ) {
			checkType( ((TypeChoiceDefinition) type).left() );
			System.out.print( " | " );
			checkType( ((TypeChoiceDefinition) type).right() );
		}

	}

	private void printType( String type ) {
		if( !type.isEmpty() ) {
			System.out.println( type );
		}

	}

	private void createOutput( InputPortInfo inputPort, boolean noOutputPort, boolean noLocation, boolean noProtocol ) {
		// types creation
		if( !owVector.isEmpty() ) {
			for( OneWayOperationDeclaration opDecl : owVector ) {
				// System.out.println("// types for operation " + ow_vector.get(x).id() );
				printType( getType( opDecl.requestType() ) );
			}
			System.out.println();
		}

		if( !rrVector.isEmpty() ) {
			for( RequestResponseOperationDeclaration rrDecl : rrVector ) {
				// System.out.println("// types for operation " + rr_vector.get(x).id() );
				printType( getType( rrDecl.requestType() ) );
				printType( getType( rrDecl.responseType() ) );
				for( Entry< String, TypeDefinition > fault : rrDecl.faults().entrySet() ) {
					if( !fault.getValue().name().equals( "undefined" ) ) {
						System.out.println( getType( fault.getValue() ) );
					}
				}
			}
			System.out.println();
		}

		// add auxiliary types
		while( !auxTypesVector.isEmpty() ) {
			ArrayList< TypeDefinition > aux_types_temp_vector = new ArrayList<>();
			aux_types_temp_vector.addAll( auxTypesVector );
			auxTypesVector.clear();
			for( TypeDefinition typeDefinition : aux_types_temp_vector ) {
				printType( getType( typeDefinition ) );
			}
		}

		System.out.println();

		// interface creation
		System.out.println( "interface " + inputPort.id() + "Surface {" );
		// oneway declaration
		if( !owVector.isEmpty() ) {
			System.out.println( "OneWay:" );
			for( int x = 0; x < owVector.size(); x++ ) {
				if( x != 0 ) {
					System.out.println( "," );
				}
				System.out.print( "\t" + getOWString( owVector.get( x ) ) );
			}
			System.out.println();
		}
		// request response declaration
		if( !rrVector.isEmpty() ) {
			System.out.println( "RequestResponse:" );
			for( int x = 0; x < rrVector.size(); x++ ) {
				if( x != 0 ) {
					System.out.println( "," );
				}
				System.out.print( "\t" + getRRString( rrVector.get( x ) ) );
			}
			System.out.println();
		}
		System.out.println( "}" );
		System.out.println();

		// outputPort definition
		if( !noOutputPort ) {
			System.out.println( "outputPort " + inputPort.id() + " {" );
			if( !noLocation )
				System.out.println( "\tLocation: \"" + inputPort.location() + "\"" );
			if( !noProtocol && !inputPort.protocolId().isEmpty() )
				System.out.println( "\tProtocol: " + inputPort.protocolId() );
			System.out.println( "\tInterfaces: " + inputPort.id() + "Surface" );
			System.out.println( "}" );
		}
	}
}
