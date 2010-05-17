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

import com.ibm.wsdl.extensions.schema.SchemaImpl;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.util.Pair;
import jolie.xml.xsd.XsdToJolieConverter;
import jolie.xml.xsd.XsdUtils;
import jolie.xml.xsd.impl.XsdToJolieConverterImpl;
import joliex.wsdl.impl.Interface;
import joliex.wsdl.impl.OutputPort;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
	private List< TypeDefinition > typeDefinitions = new ArrayList< TypeDefinition >( 0 );
	private final XSOMParser schemaParser;
	private final TransformerFactory transformerFactory;

	public WSDLConverter( Definition definition, Writer writer )
	{
		this.writer = writer;
		this.definition = definition;
		transformerFactory = TransformerFactory.newInstance();
		schemaParser = new XSOMParser();
		schemaParser.setErrorHandler( new ErrorHandler() {
			public void warning( SAXParseException exception )
				throws SAXException
			{
				throw new SAXException( exception );
			}

			public void error( SAXParseException exception )
				throws SAXException
			{
				throw new SAXException( exception );
			}

			public void fatalError( SAXParseException exception )
				throws SAXException
			{
				throw new SAXException( exception );
			}
		} );
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
		writer.write( '\n' );
	}

	private void parseSchemaElement( Element element )
		throws IOException
	{
		try {
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty( "indent", "yes" );
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult( sw );
			DOMSource source = new DOMSource( element );
			transformer.transform( source, result );
			InputSource schemaSource = new InputSource( new StringReader( sw.toString() ) );
			schemaSource.setSystemId( definition.getDocumentBaseURI() );
			schemaParser.parse( schemaSource );
		} catch( TransformerConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			throw new IOException( e );
		}
	}

	public void convert()
		throws IOException
	{
		convertTypes();
		Map< QName, Service > services = definition.getServices();
		for( Entry< QName, Service > service : services.entrySet() ) {
			convertService( service.getValue() );
		}
		writeData();
	}

	private void convertTypes()
		throws IOException
	{
		Types types = definition.getTypes();
		if ( types != null ) {
			List< ExtensibilityElement > list = types.getExtensibilityElements();
			for( ExtensibilityElement element : list ) {
				if ( element instanceof SchemaImpl ) {
					Element schemaElement = ((SchemaImpl)element).getElement();
					// We need to inject the namespaces declared in parent nodes into the schema element
					Map< String, String > namespaces = definition.getNamespaces();
					for( Entry< String, String > entry : namespaces.entrySet() ) {
						if ( schemaElement.getAttribute( "xmlns:" + entry.getKey() ).isEmpty() ) {
							schemaElement.setAttribute( "xmlns:" + entry.getKey(), entry.getValue() );
						}
					}
					parseSchemaElement( schemaElement );
				}
			}

			try {
				XSSchemaSet schemaSet = schemaParser.getResult();
				if ( schemaSet == null ) {
					throw new IOException( "An error occurred while parsing the WSDL types section" ) ;
				}
				XsdToJolieConverter schemaConverter = new XsdToJolieConverterImpl( schemaSet, false, null );
				typeDefinitions = schemaConverter.convert();
			} catch( SAXException e ) {
				throw new IOException( e );
			} catch( XsdToJolieConverter.ConversionException e ) {
				throw new IOException( e );
			}
		}
	}

	private String getCardinalityString( TypeDefinition type )
	{
		if ( type.cardinality().equals( Constants.RANGE_ONE_TO_ONE ) ) {
			return "";
		} else if ( type.cardinality().min() == 0 && type.cardinality().max() == 1 ) {
			return "?";
		} else if ( type.cardinality().min() == 0 && type.cardinality().max() == Integer.MAX_VALUE ) {
			return "*";
		} else {
			return new StringBuilder()
				.append( '[' )
				.append( type.cardinality().min() )
				.append( ',' )
				.append( type.cardinality().max() )
				.append( ']' )
				.toString();
		}
	}

	private void writeType( TypeDefinition type, boolean subType )
		throws IOException
	{
		StringBuilder builder = new StringBuilder();
		if ( subType ) {
			builder.append( '.' );
		} else {
			builder.append( "type " );
		}
		builder.append( type.id() )
			.append( getCardinalityString( type ) )
			.append( ':' );
		if ( type instanceof TypeDefinitionLink ) {
			TypeDefinitionLink link = (TypeDefinitionLink)type;
			builder.append( link.linkedType().id() );
			if ( subType == false ) {
				builder.append( '\n' );
			}
			writeLine( builder.toString() );
		} else if ( type.untypedSubTypes() ) {
			builder.append( "undefined" );
			writeLine( builder.toString() );
		} else {
			builder.append( nativeTypeToString( type.nativeType() ) );
			if ( type.hasSubTypes() ) {
				builder.append( " {" );
			}
			writeLine( builder.toString() );
			if ( type.hasSubTypes() ) {
				indent();
				for( Entry< String, TypeDefinition > entry : type.subTypes() ) {
					writeType( entry.getValue(), true );
				}
				unindent();
			}

			if ( type.hasSubTypes() ) {
				writeLine( "}" );
			}
			if ( subType == false ) {
				writeLine( "" );
			}
		}		
	}

	private static String nativeTypeToString( NativeType nativeType )
	{
		return (nativeType == null) ? "" : nativeType.id();
    }

	private void writeData()
		throws IOException
	{
		for( TypeDefinition typeDefinition : typeDefinitions ) {
			writeType( typeDefinition, false );
		}
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

	private String owOperationToString( joliex.wsdl.impl.Operation operation )
	{
		StringBuilder builder = new StringBuilder();
		builder.append( operation.name() );
		builder.append( '(' );
		if ( operation.requestTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.requestTypeName() );
		}
		builder.append( ')' );
		return builder.toString();
	}

	private String rrOperationToString( joliex.wsdl.impl.Operation operation )
	{
		StringBuilder builder = new StringBuilder();
		builder.append( operation.name() );
		builder.append( '(' );
		if ( operation.requestTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.requestTypeName() );
		}
		builder.append( ')' );
		builder.append( '(' );
		if ( operation.responseTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.responseTypeName() );
		}
		builder.append( ')' );
		if ( operation.faults().isEmpty() == false ) {
			builder.append( " throws " );
			List< Pair< String, String > > faults = operation.faults();
			int i = 0;
			for( i = 0; i < faults.size() - 1; i++ ) {
				builder.append( faults.get( i ).key() )
					.append( '(' )
					.append( faults.get( i ).value() )
					.append( ')' )
					.append( ' ' );
			}
			builder.append( faults.get( i ).key() )
					.append( '(' )
					.append( faults.get( i ).value() )
					.append( ')' );
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
				writeLine( owOperationToString( iface.oneWayOperations().get( i ) ) + "," );
			}
			writeLine( owOperationToString( iface.oneWayOperations().get( i ) ) );
			unindent();
		}
		if ( iface.requestResponseOperations().isEmpty() == false ) {
			writeLine( "RequestResponse:" );
			indent();
			int i;
			for( i = 0; i < iface.requestResponseOperations().size() - 1; i++ ) {
				/*if ( iface.requestResponseOperations().get( i ).comment().isEmpty() == false ) {
					writeLine( "// " + iface.requestResponseOperations().get( i ).comment() );
				}*/
				writeLine( rrOperationToString( iface.requestResponseOperations().get( i ) ) + "," );
			}
			writeLine( rrOperationToString( iface.requestResponseOperations().get( i ) ) );
			unindent();
		}
		writeLine( "}" );
	}

	private void convertService( Service service )
		throws IOException
	{
		//String comment = service.getDocumentationElement().getNodeValue();
		for( Entry< String, Port > entry : (Set< Entry<String, Port> >)service.getPorts().entrySet() ) {
			convertPort( entry.getValue() );
		}
	}

	private void convertPort( Port port )
		throws IOException
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
					.append( "\";\n" )
					.append( "\t.wsdl.port = \"" )
					.append( port.getName() )
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
		throws IOException
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
		throws IOException
	{
		String requestTypeName = null;
		String responseTypeName = null;
		String comment = "";
		if ( operation.getDocumentationElement() != null ) {
			operation.getDocumentationElement().getNodeValue();
		}

		Map< String, Part > parts = operation.getInput().getMessage().getParts();
		if ( parts.size() > 1 ) {
			throw new IOException( "Operation " + operation.getName() + " specifies more than one input message part. Only \"document\" style interfaces are supported." );
		} else {
			for( Entry< String, Part > entry : parts.entrySet() ) {
				Part part = entry.getValue();
				if ( part.getElementName() == null ) {
					if ( part.getTypeName() == null ) {
						throw new IOException( "Could not parse message part " + entry.getKey() + " for operation " + operation.getName() + "." );
					}
					requestTypeName = XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id();
				} else {
					requestTypeName = part.getElementName().getLocalPart();
				}
			}
		}

		parts = operation.getOutput().getMessage().getParts();
		if ( parts.size() > 1 ) {
			throw new IOException( "Operation " + operation.getName() + " specifies more than one output message part. Only \"document\" style interfaces are supported." );
		} else {
			for( Entry< String, Part > entry : parts.entrySet() ) {
				Part part = entry.getValue();
				if ( part.getElementName() == null ) {
					if ( part.getTypeName() == null ) {
						throw new IOException( "Could not parse message part " + entry.getKey() + " for operation " + operation.getName() + "." );
					}
					responseTypeName = XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id();
				} else {
					responseTypeName = part.getElementName().getLocalPart();
				}
			}
		}

		List< Pair< String, String > > faultList = new ArrayList< Pair< String, String > >();
		Map< String, Fault > faults = operation.getFaults();
		for( Entry< String, Fault > entry : faults.entrySet() ) {
			String faultName = entry.getKey();
			String faultTypeName = null;
			parts = entry.getValue().getMessage().getParts();
			if ( parts.size() > 1 ) {
				throw new IOException( "Operation " + operation.getName() + " specifies more than one message part for fault " + entry.getKey() + ". Only \"document\" style interfaces are supported." );
			} else {
				for( Entry< String, Part > e : parts.entrySet() ) {
					Part part = e.getValue();
					if ( part.getElementName() == null ) {
						if ( part.getTypeName() == null ) {
							throw new IOException( "Could not parse message part " + e.getKey() + " for operation " + operation.getName() + ", fault " + entry.getKey() + "." );
						}
						faultTypeName = XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id();
					} else {
						faultTypeName = part.getElementName().getLocalPart();
					}
				}
			}
			faultList.add( new Pair< String, String >( faultName, faultTypeName ) );
		}

		return new joliex.wsdl.impl.Operation(
			operation.getName(),
			requestTypeName,
			responseTypeName,
			faultList,
			comment
		);
	}
}
