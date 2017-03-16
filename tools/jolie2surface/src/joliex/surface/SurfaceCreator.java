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

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
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
 * Modified by Francesco Bullini, 05/07/2012
 */
public class SurfaceCreator
{
	private ProgramInspector inspector;
	private URI originalFile;
	private ArrayList<RequestResponseOperationDeclaration> rr_vector;
	private ArrayList<OneWayOperationDeclaration> ow_vector;
	private ArrayList<String> types_vector;
	private ArrayList<TypeDefinition> aux_types_vector;
	private int MAX_CARD = 2147483647;

	
	public SurfaceCreator( ProgramInspector inspector, URI originalFile )
	{

		this.inspector = inspector;
		this.originalFile = originalFile;

	}

	public void ConvertDocument( String inputPortToCreate, boolean noOutputPort, boolean noLocation, boolean noProtocol )
		throws Exception
	{

		ArrayList<InterfaceDefinition> interface_vector = new ArrayList<InterfaceDefinition>();
		rr_vector = new ArrayList<RequestResponseOperationDeclaration>();
		ow_vector = new ArrayList<OneWayOperationDeclaration>();
		types_vector = new ArrayList<String>();
		aux_types_vector = new ArrayList<TypeDefinition>();
             
		// find inputPort
               
		InputPortInfo[] inputPortList = inspector.getInputPorts( );
            
		InputPortInfo inputPort = null;
		for( InputPortInfo iP : inputPortList ) {
			if ( iP.id().equals( inputPortToCreate ) ) {
				inputPort = iP;
			}
		}
		if ( inputPort == null ) {
			throw (new Exception( "Error! inputPort not found!" ));
		}

		// extracts the list of all the interfaces to be parsed
		// extracts interfaces declared into Interfaces
		for( InterfaceDefinition interfaceDefinition : inputPort.getInterfaceList() ) {
			interface_vector.add( interfaceDefinition );
		}
		OutputPortInfo[] outputPortList = inspector.getOutputPorts( );
		// extracts interfaces from aggregated outputPorts
		for( int x = 0; x < inputPort.aggregationList().length; x++ ) {
			int i = 0;
			while( !inputPort.aggregationList()[x].outputPortList()[0].equals( outputPortList[i].id() ) ) {
                                i++;
                                
			}
			for( InterfaceDefinition interfaceDefinition : outputPortList[i].getInterfaceList() ) {
				interface_vector.add( Interfaces.extend( interfaceDefinition, inputPort.aggregationList()[x].interfaceExtender(), inputPort.id() ) );
			}
		}

		//  for each interface extract the list of all the available operations and types
		for( InterfaceDefinition interfaceDefinition : interface_vector ) {
			addOperation( interfaceDefinition );
		}

		// create oputput
		createOutput( inputPort, noOutputPort, noLocation, noProtocol );

	}

	private void addOperation( InterfaceDefinition interfaceDefinition )
	{
		for( OperationDeclaration op : interfaceDefinition.operationsMap().values() ) {
			if ( op instanceof RequestResponseOperationDeclaration ) {
				rr_vector.add( (RequestResponseOperationDeclaration) op );
			} else {
				ow_vector.add( (OneWayOperationDeclaration) op );
			}
		}
	}

	private String getOWString( OneWayOperationDeclaration ow )
	{
		String ret = ow.id() + "( " + ow.requestType().id() + " )";
		return ret;
	}

	private String getRRString( RequestResponseOperationDeclaration rr )
	{
		String ret = rr.id() + "( " + rr.requestType().id() + " )( " + rr.responseType().id() + " )";
		if ( rr.faults().size() > 0 ) {
			ret = ret + " throws ";
			boolean flag = false;
			for( Entry<String, TypeDefinition> fault : rr.faults().entrySet() ) {
				if ( flag == false ) {
					flag = true;
				} else {
					ret = ret + " ";
				}
				ret = ret + fault.getKey();
				if ( fault.getValue() != null ) {
					ret = ret + "( " + fault.getValue().id() + " )";
				}
			}
		}
		return ret;
	}

	private String getMax( int max )
	{
		if ( max == MAX_CARD ) {
			return "*";
		} else {
			return new Integer( max ).toString();
		}
	}

	private String getCardinality( Range card )
	{
		return ( card.min() == 1 && card.max() == 1 ) ? "" : ("[" + card.min() + "," + getMax(card.max()) + "]");
	}

	private boolean choice;
	private String getSubType( TypeDefinition type, int indent )
	{
		String ret = "";

		if ( choice ) {
			choice = false;
		} else {
			for ( int y = 0; y < indent; y++ ) {
				ret = ret + "\t";
			}

			ret = ret + "." + type.id() + getCardinality( type.cardinality() ) + ":";
		}

		if ( type instanceof TypeDefinitionLink ) {
			ret = ret + ( (TypeDefinitionLink) type ).linkedTypeName();
			if ( !aux_types_vector.contains( ((TypeDefinitionLink) type ).linkedType() ) ) {
				aux_types_vector.add( ((TypeDefinitionLink) type ).linkedType() );
			}

		} else if ( type instanceof TypeInlineDefinition ) {
			TypeInlineDefinition def = (TypeInlineDefinition)type;
			ret = ret + def.nativeType().id();
			if ( def.hasSubTypes() ) {
				ret = ret + "{ \n";
				for( Entry<String, TypeDefinition> entry : def.subTypes() ) {
					ret = ret + getSubType(entry.getValue(), indent + 1) + "\n";
				}
				for( int y = 0; y < indent; y++ ) {
					ret = ret + "\t";
				}
				ret = ret + "}";
			} else if ( ( (TypeInlineDefinition) type ).untypedSubTypes() ){
				ret = ret+ "{ ? }";
			}
		} else if ( type instanceof TypeChoiceDefinition ){
			choice = true;
			ret += getSubType( ( (TypeChoiceDefinition) type).left(), indent ) + " | ";
			choice = true;
			ret += getSubType( ( (TypeChoiceDefinition) type ).right(), indent );

		}

		return ret;
	}

	private String getType( TypeDefinition type )
	{
		String ret = "";                            
		if ( !types_vector.contains( type.id() ) && !NativeType.isNativeTypeKeyword( type.id() )  && !type.id().equals( "undefined" ) ) {

			System.out.print( "type " + type.id() + ":" );
			checkType(type);
			System.out.println( "" );
			types_vector.add( type.id() );
		}

		return ret;
	}

	private void checkType( TypeDefinition type ){
		if ( type instanceof TypeDefinitionLink ) {
			System.out.print( ( (TypeDefinitionLink) type ).linkedTypeName() );
			if ( !aux_types_vector.contains( ((TypeDefinitionLink) type).linkedType() ) ) {
				aux_types_vector.add( ((TypeDefinitionLink) type).linkedType() );
			}
		} else  if ( type instanceof TypeInlineDefinition ){
			TypeInlineDefinition def = (TypeInlineDefinition)type;
			System.out.print( def.nativeType().id() );
			if ( def.hasSubTypes() ) {
				System.out.print( "{\n" );
				for( Entry<String, TypeDefinition> entry : def.subTypes() ) {
					System.out.print( getSubType( entry.getValue(), 1) + "\n" );
				}

				System.out.print("}");
			} else {

				if ( ( (TypeInlineDefinition) type ).untypedSubTypes() ) {
					System.out.print( " { ? }" );}

			}
		} else if ( type instanceof TypeChoiceDefinition ){
			checkType( ( (TypeChoiceDefinition) type ).left());
			System.out.print( " | " );
			checkType( ( (TypeChoiceDefinition) type ).right());
		}

	}

	private void printType( String type )
	{
		if ( !type.equals( "" ) ) {
			System.out.println( type );
		}

	}

	private void createOutput( InputPortInfo inputPort, boolean noOutputPort, boolean noLocation, boolean noProtocol )
	{
		// types creation
		if ( ow_vector.size() > 0 ) {
			for( int x = 0; x < ow_vector.size(); x++ ) {
				//System.out.println("// types for operation " + ow_vector.get(x).id() );
				printType( getType( ow_vector.get( x ).requestType() ) );
			}
			System.out.println();
		}

		if ( rr_vector.size() > 0 ) {
			for( int x = 0; x < rr_vector.size(); x++ ) {
				//System.out.println("// types for operation " + rr_vector.get(x).id() );
				printType( getType( rr_vector.get( x ).requestType() ) );
				printType( getType( rr_vector.get( x ).responseType() ) );
				for( Entry<String, TypeDefinition> fault : rr_vector.get( x ).faults().entrySet() ) {
					if ( !fault.getValue().id().equals( "undefined" ) ) {
						System.out.println( getType( fault.getValue() ) );
					}
				}
			}
			System.out.println();
		}

		// add auxiliary types
		while( !aux_types_vector.isEmpty() ) {
			ArrayList<TypeDefinition> aux_types_temp_vector = new ArrayList<TypeDefinition>();
			aux_types_temp_vector.addAll( aux_types_vector );
			aux_types_vector.clear();
			Iterator it = aux_types_temp_vector.iterator();
			while( it.hasNext() ) {
				printType( getType( (TypeDefinition) it.next() ) );
			}
		}

		System.out.println();

		// interface creation
		System.out.println( "interface " + inputPort.id() + "Surface {" );
		// oneway declaration
		if ( ow_vector.size() > 0 ) {
			System.out.println( "OneWay:" );
			for( int x = 0; x < ow_vector.size(); x++ ) {
				if ( x != 0 ) {
					System.out.println( "," );
				}
				System.out.print( "\t" + getOWString( ow_vector.get( x ) ) );
			}
			System.out.println();
		}
		// request response declaration
		if ( rr_vector.size() > 0 ) {
			System.out.println( "RequestResponse:" );
			for( int x = 0; x < rr_vector.size(); x++ ) {
				if ( x != 0 ) {
					System.out.println( "," );
				}
				System.out.print( "\t" + getRRString( rr_vector.get( x ) ) );
			}
			System.out.println();
		}
		System.out.println( "}" );
		System.out.println();

		// outputPort definition
		if ( !noOutputPort ) {
			System.out.println( "outputPort " + inputPort.id() + "{" );
			if ( !noLocation ) System.out.println( "\tLocation:\"" + inputPort.location() + "\"" );
			if ( !noProtocol ) System.out.println( "\tProtocol:" + inputPort.protocolId() );
			System.out.println( "\tInterfaces:" + inputPort.id() + "Surface" );
			System.out.println( "}" );
		}
	}
}
