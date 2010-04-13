/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.wsdl;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import joliex.wsdl.impl.Interface;
import joliex.wsdl.impl.OutputPort;

/**
 *
 * @author Fabrizio Montesi
 */
public class WSDLConverter
{
	private final Writer writer;
	private final Definition definition;
	private int indentationLevel = 0;
	private Map< String, OutputPort > outputPorts = new HashMap< String, OutputPort >();
	private Map< String, Interface > interfaces = new HashMap< String, Interface >();

	public WSDLConverter( Definition definition, Writer writer )
	{
		this.writer = writer;
		this.definition = definition;
	}

	private void indent()
	{
		indentationLevel++;
	}

	private void unindent()
	{
		indentationLevel--;
	}

	private void writeLine( String s )
		throws IOException
	{
		for( int i = 0; i < indentationLevel; i++ ) {
			writer.write( "\t" );
		}
		writer.write( s );
		writer.write( "\n" );
	}

	public void convert()
		throws IOException
	{
		/*Types types = definition.getTypes();
		List< ExtensibilityElement > list = types.getExtensibilityElements();
		for( ExtensibilityElement element : list ) {
			if ( element instanceof SchemaImpl ) {
				Element schemaElement = ((SchemaImpl)element).getElement();
				
			}
		}*/
		Map< QName, Service > services = definition.getServices();
		for( Entry< QName, Service > service : services.entrySet() ) {
			convertService( service.getValue() );
		}
		writeData();
	}

	private void writeData()
		throws IOException
	{
		for( Entry< String, Interface > entry : interfaces.entrySet() ) {
			writeInterface( entry.getValue() );
			writeLine( "" );
		}
		for( Entry< String, OutputPort > entry : outputPorts.entrySet() ) {
			writeOutputPort( entry.getValue() );
			writeLine( "" );
		}
		writer.flush();
	}

	private void writeOutputPort( OutputPort port )
		throws IOException
	{
		writeLine( "outputPort " + port.name() + " {" );
		writeLine( "Location: \"" + port.location() + "\"" );
		writeLine( "Protocol: " + port.protocol() );
		writeLine( "Interfaces: " + port.interfaceName() );
		writeLine( "}" );
	}

	private String operationToString( joliex.wsdl.impl.Operation operation )
	{
		StringBuilder builder = new StringBuilder();
		builder.append( operation.name() );
		if ( operation.faults().isEmpty() == false ) {
			builder.append( " throws " );
			List< String > faults = operation.faults();
			int i = 0;
			for( i = 0; i < faults.size() - 1; i++ ) {
				builder.append( faults.get( i ) + " " );
			}
			builder.append( faults.get( i ) );
		}
		return builder.toString();
	}

	private void writeInterface( Interface iface )
		throws IOException
	{
		writeLine( "interface " + iface.name() + " {" );
		if ( iface.oneWayOperations().isEmpty() == false ) {
			writeLine( "OneWay:" );
			indent();
			int i;
			for( i = 0; i < iface.oneWayOperations().size() - 1; i++ ) {
				writeLine( operationToString( iface.oneWayOperations().get( i ) ) + "," );
			}
			writeLine( operationToString( iface.oneWayOperations().get( i ) ) );
			unindent();
		}
		if ( iface.requestResponseOperations().isEmpty() == false ) {
			writeLine( "RequestResponse:" );
			indent();
			int i;
			for( i = 0; i < iface.requestResponseOperations().size() - 1; i++ ) {
				writeLine( operationToString( iface.requestResponseOperations().get( i ) ) + "," );
			}
			writeLine( operationToString( iface.requestResponseOperations().get( i ) ) );
			unindent();
		}
		writeLine( "}" );
	}

	private void convertService( Service service )
	{
		//String comment = service.getDocumentationElement().getNodeValue();
		for( Entry< String, Port > entry : (Set< Entry<String, Port> >)service.getPorts().entrySet() ) {
			convertPort( entry.getValue() );
		}
	}

	private void convertPort( Port port )
	{
		String comment = "";
		String name = port.getName();
		String protocol = "soap";
		String location = "socket://localhost:80/";
		if ( port.getDocumentationElement() != null ) {
			comment = port.getDocumentationElement().getNodeValue();
		}
		List< ExtensibilityElement > extElements = port.getExtensibilityElements();
		for( ExtensibilityElement element : extElements ) {
			if ( element instanceof SOAPAddress ) {
				location = ((SOAPAddress)element).getLocationURI().toString();
				StringBuilder builder = new StringBuilder();
				builder.append( "soap {\n" )
					.append( "\t.wsdl = \"" )
					.append( definition.getDocumentBaseURI() )
					.append( "\"\n}");
				protocol = builder.toString();

			} else if ( element instanceof HTTPAddress ) {
				location = ((HTTPAddress)element).getLocationURI().toString();
				protocol = "http";
			}
		}
		try {
			URI uri = new URI( location );
			uri = new URI(
				"socket",
				uri.getUserInfo(),
				uri.getHost(),
				( uri.getPort() < 1 ) ? 80 : uri.getPort(),
				uri.getPath(),
				uri.getQuery(),
				uri.getFragment()
			);
			location = uri.toString();
		} catch( URISyntaxException e ) {
			e.printStackTrace();
		}
		Binding binding = port.getBinding();
		PortType portType = binding.getPortType();
		convertPortType( portType );
		outputPorts.put( name, new OutputPort(
			name, location, protocol, portType.getQName().getLocalPart(), comment
		) );
	}

	private void convertPortType( PortType portType )
	{
		String comment = "";
		if ( portType.getDocumentationElement() != null ) {
			comment = portType.getDocumentationElement().getNodeValue();
		}
		Interface iface = new Interface( portType.getQName().getLocalPart(), comment );
		List< Operation > operations = portType.getOperations();
		for( Operation operation : operations ) {
			if ( operation.getOutput() == null ) {
				iface.addOneWayOperation( convertOperation( operation ) );
			} else {
				iface.addRequestResponseOperation( convertOperation( operation ) );
			}
		}
		interfaces.put( iface.name(), iface );
	}

	private joliex.wsdl.impl.Operation convertOperation( Operation operation )
	{
		String comment = "";
		if ( operation.getDocumentationElement() != null ) {
			operation.getDocumentationElement().getNodeValue();
		}

		List< String > faultList = new LinkedList< String >();
		Map< String, Fault > faults = operation.getFaults();
		for( Entry< String, Fault > entry : faults.entrySet() ) {
			faultList.add( entry.getKey() );
		}

		return new joliex.wsdl.impl.Operation(
			operation.getName(),
			faultList,
			comment
		);
	}
}
