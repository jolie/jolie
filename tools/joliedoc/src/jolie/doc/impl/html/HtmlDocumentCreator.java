/***************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>  *
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

package jolie.doc.impl.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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

/**
 *
 * @author balint maschio, claudio guidi
 */
public class HtmlDocumentCreator
{
	private Writer writer;
	private ProgramInspector inspector;
	private URI directorySourceFile;
	private String directorySOA;
	private JolieDocWriter jolieDocWriter;
	private ArrayList<String> types;

	public HtmlDocumentCreator( ProgramInspector inspector, URI directorySourceFile )
	{
		this.inspector = inspector;
		this.directorySourceFile = directorySourceFile;
	}

	public void ConvertDocument( boolean outputPortConversionEnabled, String inputPortName )
		throws IOException
	{
		int filenameIndex = directorySourceFile.getRawSchemeSpecificPart().lastIndexOf( "/" ) + 1;
		directorySOA = new File( directorySourceFile ).toString().substring( 0, filenameIndex );
//		directorySOA = directorySourceFile.getSchemeSpecificPart().substring( 0, filenameIndex );

		// scanning inputPorts. For each inputPort will be generated an html file
		InputPortInfo[] inputPorts = inspector.getInputPorts( directorySourceFile );

		for( InputPortInfo inputPort : inputPorts ) {

			if ( inputPortName.isEmpty() || inputPortName.equals( inputPort.id() ) ) {
				types = new ArrayList<>();
				writer = new BufferedWriter( new FileWriter( inputPort.id() + ".html" ) );
				jolieDocWriter = new JolieDocWriter( writer, directorySourceFile.getRawSchemeSpecificPart().substring( filenameIndex ) );
				jolieDocWriter.addPort( inputPort );
				List<InterfaceDefinition> interfacesList = inputPort.getInterfaceList();
				for( InterfaceDefinition interfaceDefintion : interfacesList ) {
					jolieDocWriter.addInterface( interfaceDefintion );
					addOperations( interfaceDefintion );
				}

				// scanning aggregation
				OutputPortInfo[] outputPortList = inspector.getOutputPorts( directorySourceFile );
				// extracts interfaces from aggregated outputPorts
				for( int x = 0; x < inputPort.aggregationList().length; x++ ) {
					int i = 0;
					while( !inputPort.aggregationList()[ x ].outputPortList()[ 0 ].equals( outputPortList[ i ].id() ) ) {
						i++;
					}
					for( InterfaceDefinition interfaceDefinition : outputPortList[ i ].getInterfaceList() ) {
						jolieDocWriter.addInterface( Interfaces.extend( interfaceDefinition, inputPort.aggregationList()[ x ].interfaceExtender(), inputPort.id() ) );
						addOperations( Interfaces.extend( interfaceDefinition, inputPort.aggregationList()[ x ].interfaceExtender(), inputPort.id() ) );
					}
				}
				jolieDocWriter.write();
				System.out.println( "Generated joliedoc " + inputPort.id() + ".html" );
			}

		}
		if ( outputPortConversionEnabled ) {
			//System.out.println( "JolieDoc: no inputPort found, generated joliedocs for outputPorts." );
			OutputPortInfo[] outputPortList = inspector.getOutputPorts( directorySourceFile );
			for( OutputPortInfo outputPort : outputPortList ) {
				writer = new BufferedWriter( new FileWriter( outputPort.id() + ".html" ) );
				types = new ArrayList<>();
				jolieDocWriter = new JolieDocWriter( writer, directorySourceFile.getRawSchemeSpecificPart().substring( filenameIndex ) );
				jolieDocWriter.addPort( outputPort );
				List<InterfaceDefinition> interfacesList = outputPort.getInterfaceList();
				for( InterfaceDefinition interfaceDefintion : interfacesList ) {
					jolieDocWriter.addInterface( interfaceDefintion );
					addOperations( interfaceDefintion );
				}
				jolieDocWriter.write();
				System.out.println( "Generated joliedoc " + outputPort.id() + ".html" );
			}
		}
	}

	private void addOperations( InterfaceDefinition interfaceDefinition )
		throws IOException
	{
		for( Entry<String, OperationDeclaration> operation : interfaceDefinition.operationsMap().entrySet() ) {
			OperationDeclaration operationDeclaration = operation.getValue();
			if ( operationDeclaration instanceof RequestResponseOperationDeclaration ) {
				TypeDefinition requestType = ((RequestResponseOperationDeclaration) operationDeclaration).requestType();
				TypeDefinition responseType = ((RequestResponseOperationDeclaration) operationDeclaration).responseType();
				if ( !types.contains( requestType.id() ) ) {
					jolieDocWriter.addType( requestType );
					types.add( requestType.id() );
				}
				if ( !types.contains( responseType.id() ) ) {
					jolieDocWriter.addType( responseType );
					types.add( responseType.id() );
				}
				addSubTypes( requestType );
				addSubTypes( responseType );

				for( Entry< String, TypeDefinition> fault : ((RequestResponseOperationDeclaration) operationDeclaration).faults().entrySet() ) {
					if ( !fault.getValue().id().equals( "undefined" ) ) {
						if ( !types.contains( fault.getValue().id() ) ) {
							jolieDocWriter.addType( fault.getValue() );
							types.add( fault.getValue().id() );
						}
						addSubTypes( fault.getValue() );
					}
				}
			} else {
				TypeDefinition requestType = ((OneWayOperationDeclaration) operationDeclaration).requestType();
				jolieDocWriter.addType( requestType );
				addSubTypes( requestType );
			}
		}
	}

	private void addSubTypes( TypeDefinition type )
		throws IOException
	{
		if (type instanceof TypeChoiceDefinition){
			addSubTypes(((TypeChoiceDefinition) type).left());
			addSubTypes(((TypeChoiceDefinition) type).right());
		} else if (type instanceof TypeDefinitionLink){
			addSubTypes(((TypeDefinitionLink) type).linkedType());
		} else if ( ( (TypeInlineDefinition)type).hasSubTypes() ) {
			for( Entry<String, TypeDefinition> entry : ((TypeInlineDefinition) type).subTypes() ) {
				if ( entry.getValue() instanceof TypeDefinitionLink ) {
					if ( !types.contains( ((TypeDefinitionLink) entry.getValue()).linkedType().id() ) ) {
						types.add( ((TypeDefinitionLink) entry.getValue()).linkedType().id() );
						jolieDocWriter.addLinkedType( (TypeDefinitionLink) entry.getValue() );
						addSubTypes( ((TypeDefinitionLink) entry.getValue()).linkedType() );
					}
				} else if ( entry.getValue() instanceof TypeChoiceDefinition ) {
					addSubTypes(((TypeChoiceDefinition) entry.getValue()).left());
					addSubTypes(((TypeChoiceDefinition) entry.getValue()).right());
				} else if ( entry.getValue() instanceof TypeInlineDefinition ) {
					if ( ((TypeInlineDefinition) entry.getValue()).hasSubTypes() ) {
						addSubTypes( entry.getValue() );
					}
				}
			}
		}
	}
}
