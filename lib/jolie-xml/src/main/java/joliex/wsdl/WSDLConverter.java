/***************************************************************************
 *   Copyright (C) 2010-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.ibm.wsdl.extensions.schema.SchemaImpl;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;

import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.*;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.xml.xsd.XsdToJolieConverter;
import jolie.xml.xsd.XsdUtils;
import jolie.xml.xsd.impl.XsdToJolieConverterImpl;
import joliex.wsdl.impl.Interface;
import joliex.wsdl.impl.OutputPort;

/**
 *
 * @author Fabrizio Montesi
 */
public class WSDLConverter {
	private enum Style {
		DOCUMENT, HTTP, RPC
	}


	private final Writer writer;
	private final Definition definition;
	private int indentationLevel = 0;
	private final Map< String, OutputPort > outputPorts = new HashMap<>();
	private final Map< String, Interface > interfaces = new HashMap<>();
	private final List< TypeDefinition > typeDefinitions = new ArrayList<>();
	private final XSOMParser schemaParser;
	private final TransformerFactory transformerFactory;

	public WSDLConverter( Definition definition, Writer writer ) {
		this.writer = writer;
		this.definition = definition;
		transformerFactory = TransformerFactory.newInstance();
		schemaParser = new XSOMParser();
		schemaParser.setErrorHandler( new ErrorHandler() {
			@Override
			public void warning( SAXParseException exception )
				throws SAXException {
				throw new SAXException( exception );
			}

			@Override
			public void error( SAXParseException exception )
				throws SAXException {
				throw new SAXException( exception );
			}

			@Override
			public void fatalError( SAXParseException exception )
				throws SAXException {
				throw new SAXException( exception );
			}
		} );
	}

	private void indent() {
		indentationLevel++;
	}

	private void unindent() {
		indentationLevel--;
	}

	private void writeLine( String s )
		throws IOException {
		for( int i = 0; i < indentationLevel; i++ ) {
			writer.write( "\t" );
		}
		writer.write( s );
		writer.write( '\n' );
	}

	private void parseSchemaElement( Element element )
		throws IOException {
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
		} catch( SAXException | TransformerException e ) {
			throw new IOException( e );
		}
	}

	@SuppressWarnings( "unchecked" )
	public void convert()
		throws IOException {
		convertTypes();
		Map< QName, Service > services = definition.getServices();
		for( Entry< QName, Service > service : services.entrySet() ) {
			convertService( service.getValue() );
		}
		writeData();
	}

	@SuppressWarnings( "unchecked" )
	private void convertTypes()
		throws IOException {
		Types types = definition.getTypes();
		if( types != null ) {
			List< ExtensibilityElement > list = types.getExtensibilityElements();
			for( ExtensibilityElement element : list ) {
				if( element instanceof SchemaImpl ) {
					Element schemaElement = ((SchemaImpl) element).getElement();
					// We need to inject the namespaces declared in parent nodes into the schema element
					Map< String, String > namespaces = definition.getNamespaces();
					for( Entry< String, String > entry : namespaces.entrySet() ) {
						if( schemaElement.getAttribute( "xmlns:" + entry.getKey() ).isEmpty() ) {
							schemaElement.setAttribute( "xmlns:" + entry.getKey(), entry.getValue() );
						}
					}
					parseSchemaElement( schemaElement );
				}
			}

			try {
				XSSchemaSet schemaSet = schemaParser.getResult();
				if( schemaSet == null ) {
					throw new IOException( "An error occurred while parsing the WSDL types section" );
				}
				XsdToJolieConverter schemaConverter = new XsdToJolieConverterImpl( schemaSet, false, null );
				typeDefinitions.addAll( schemaConverter.convert() );
			} catch( SAXException | XsdToJolieConverter.ConversionException e ) {
				throw new IOException( e );
			}
		}
	}

	private String getCardinalityString( TypeDefinition type ) {
		if( type.cardinality().equals( Constants.RANGE_ONE_TO_ONE ) ) {
			return "";
		} else if( type.cardinality().min() == 0 && type.cardinality().max() == 1 ) {
			return "?";
		} else if( type.cardinality().min() == 0 && type.cardinality().max() == Integer.MAX_VALUE ) {
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
		throws IOException {
		StringBuilder builder = new StringBuilder();
		if( subType ) {
			builder.append( '.' );
		} else {
			builder.append( "type " );
		}
		builder.append( type.name() )
			.append( getCardinalityString( type ) )
			.append( ':' );
		if( type instanceof TypeDefinitionLink ) {
			TypeDefinitionLink link = (TypeDefinitionLink) type;
			builder.append( link.linkedTypeName() );
			if( subType == false ) {
				builder.append( '\n' );
			}
			writeLine( builder.toString() );
		} else if( type instanceof TypeInlineDefinition ) {
			TypeInlineDefinition def = (TypeInlineDefinition) type;
			if( def.untypedSubTypes() ) {
				builder.append( "undefined" );
				writeLine( builder.toString() );
				writeLine( "" );
			} else {
				builder.append( nativeTypeToString( def.basicType().nativeType() ) );
				if( def.hasSubTypes() ) {
					builder.append( " {" );
				}
				writeLine( builder.toString() );
				if( def.hasSubTypes() ) {
					indent();
					for( Entry< String, TypeDefinition > entry : def.subTypes() ) {
						writeType( entry.getValue(), true );
					}
					unindent();
					writeLine( "}" );
				}
				if( subType == false ) {
					writeLine( "" );
				}
			}
		} else if( type instanceof TypeChoiceDefinition ) {
			TypeChoiceDefinition choice = (TypeChoiceDefinition) type;
			if( choice.left() instanceof TypeInlineDefinition
				&& !((TypeInlineDefinition) choice.left()).hasSubTypes()
				&& choice.right() instanceof TypeInlineDefinition
				&& !((TypeInlineDefinition) choice.right()).hasSubTypes() ) {
				builder.append( ((TypeInlineDefinition) choice.left()).basicType().nativeType().id() );
				builder.append( " | " );
				builder.append( ((TypeInlineDefinition) choice.right()).basicType().nativeType().id() );
				writeLine( builder.toString() );
			} else {
				writeLine( builder.toString() );
				writeType( choice.left(), true );
				writeLine( " | " );
				writeType( choice.right(), true );
			}
		}
	}

	private static String nativeTypeToString( NativeType nativeType ) {
		return (nativeType == null) ? "" : nativeType.id();
	}

	private void writeData()
		throws IOException {
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
		throws IOException {
		writeLine( "outputPort " + port.name() + " {" );
		writeLine( "Location: \"" + port.location() + "\"" );
		writeLine( "Protocol: " + port.protocol() );
		writeLine( "Interfaces: " + port.interfaceName() );
		writeLine( "}" );
	}

	private String owOperationToString( joliex.wsdl.impl.Operation operation ) {
		StringBuilder builder = new StringBuilder();
		builder.append( operation.name() ).append( '(' );
		if( operation.requestTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.requestTypeName() );
		}
		builder.append( ')' );
		return builder.toString();
	}

	private String rrOperationToString( joliex.wsdl.impl.Operation operation ) {
		StringBuilder builder = new StringBuilder();
		builder.append( operation.name() ).append( '(' );
		if( operation.requestTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.requestTypeName() );
		}
		builder.append( ')' ).append( '(' );
		if( operation.responseTypeName() == null ) {
			builder.append( "void" );
		} else {
			builder.append( operation.responseTypeName() );
		}
		builder.append( ')' );
		if( operation.faults().isEmpty() == false ) {
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
		throws IOException {
		writeLine( "interface " + iface.name() + " {" );
		if( iface.oneWayOperations().isEmpty() == false ) {
			writeLine( "OneWay:" );
			indent();
			int i;
			for( i = 0; i < iface.oneWayOperations().size() - 1; i++ ) {
				writeLine( owOperationToString( iface.oneWayOperations().get( i ) ) + "," );
			}
			writeLine( owOperationToString( iface.oneWayOperations().get( i ) ) );
			unindent();
		}
		if( iface.requestResponseOperations().isEmpty() == false ) {
			writeLine( "RequestResponse:" );
			indent();
			int i;
			for( i = 0; i < iface.requestResponseOperations().size() - 1; i++ ) {
				/*
				 * if ( iface.requestResponseOperations().get( i ).comment().isEmpty() == false ) { writeLine( "// "
				 * + iface.requestResponseOperations().get( i ).comment() ); }
				 */
				writeLine( rrOperationToString( iface.requestResponseOperations().get( i ) ) + "," );
			}
			writeLine( rrOperationToString( iface.requestResponseOperations().get( i ) ) );
			unindent();
		}
		writeLine( "}" );
	}

	@SuppressWarnings( "unchecked" )
	private void convertService( Service service )
		throws IOException {
		// String comment = service.getDocumentationElement().getNodeValue();
		for( Entry< String, Port > entry : (Set< Entry< String, Port > >) service.getPorts().entrySet() ) {
			convertPort( entry.getValue() );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void convertPort( Port port )
		throws IOException {
		String comment = "";
		String name = port.getName();
		String protocol = "soap";
		String location = "socket://localhost:80/";
		if( port.getDocumentationElement() != null ) {
			comment = port.getDocumentationElement().getNodeValue();
		}
		List< ExtensibilityElement > extElements = port.getExtensibilityElements();
		for( ExtensibilityElement element : extElements ) {
			if( element instanceof SOAPAddress ) {
				location = ((SOAPAddress) element).getLocationURI();
				StringBuilder builder = new StringBuilder();
				builder.append( "soap {\n" )
					.append( "\t.wsdl = \"" )
					.append( definition.getDocumentBaseURI() )
					.append( "\";\n" )
					.append( "\t.wsdl.port = \"" )
					.append( port.getName() )
					.append( "\"\n}" );
				protocol = builder.toString();

			} else if( element instanceof HTTPAddress ) {
				location = ((HTTPAddress) element).getLocationURI();
				protocol = "http";
			}
		}
		try {
			URI uri = new URI( location );
			uri = new URI(
				"socket",
				uri.getUserInfo(),
				uri.getHost(),
				(uri.getPort() < 1) ? 80 : uri.getPort(),
				uri.getPath(),
				uri.getQuery(),
				uri.getFragment() );
			location = uri.toString();
		} catch( URISyntaxException e ) {
			e.printStackTrace();
		}
		Binding binding = port.getBinding();
		PortType portType = binding.getPortType();
		convertPortType( portType, binding );
		outputPorts.put( name, new OutputPort(
			name, location, protocol, portType.getQName().getLocalPart(), comment ) );
	}

	@SuppressWarnings( "unchecked" )
	private void convertPortType( PortType portType, Binding binding )
		throws IOException {
		String comment = "";
		if( portType.getDocumentationElement() != null ) {
			comment = portType.getDocumentationElement().getNodeValue();
		}

		Style style = Style.DOCUMENT;
		for( ExtensibilityElement element : (List< ExtensibilityElement >) binding.getExtensibilityElements() ) {
			if( element instanceof SOAPBinding ) {
				if( "rpc".equals( ((SOAPBinding) element).getStyle() ) ) {
					style = Style.RPC;
				}
			} else if( element instanceof HTTPBinding ) {
				style = Style.HTTP;
			}
		}
		Interface iface = new Interface( portType.getQName().getLocalPart(), comment );
		List< Operation > operations = portType.getOperations();
		for( Operation operation : operations ) {
			if( operation.getOutput() == null ) {
				iface.addOneWayOperation( convertOperation( operation, style ) );
			} else {
				iface.addRequestResponseOperation( convertOperation( operation, style ) );
			}
		}
		interfaces.put( iface.name(), iface );
	}

	@SuppressWarnings( "unchecked" )
	private String convertOperationMessage( Message message, String operationName, Style style )
		throws IOException {
		String typeName = null; // "void" datatype per default
		Map< String, Part > parts = message.getParts();
		if( parts.size() > 1 || style == Style.RPC ) {
			typeName = message.getQName().getLocalPart();
			TypeInlineDefinition requestType = new TypeInlineDefinition( ParsingContext.DEFAULT, typeName,
				BasicTypeDefinition.of( NativeType.VOID ), jolie.lang.Constants.RANGE_ONE_TO_ONE );
			for( Entry< String, Part > entry : parts.entrySet() ) {
				Part part = entry.getValue();
				if( part.getElementName() == null ) {
					if( part.getTypeName() == null ) {
						throw new IOException( "Could not parse message part " + entry.getKey() + " for operation "
							+ operationName + "." );
					}
					TypeDefinitionLink link = new TypeDefinitionLink(
						ParsingContext.DEFAULT,
						part.getName(),
						jolie.lang.Constants.RANGE_ONE_TO_ONE,
						XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id() );
					requestType.putSubType( link );
				} else {
					TypeDefinitionLink link = new TypeDefinitionLink(
						ParsingContext.DEFAULT,
						part.getName(),
						jolie.lang.Constants.RANGE_ONE_TO_ONE,
						part.getElementName().getLocalPart() );
					requestType.putSubType( link );
				}
			}
			typeDefinitions.add( requestType );
		} else {
			for( Entry< String, Part > entry : parts.entrySet() ) {
				Part part = entry.getValue();
				if( part.getElementName() == null ) {
					if( part.getTypeName() == null ) {
						throw new IOException( "Could not parse message part " + entry.getKey() + " for operation "
							+ operationName + "." );
					}
					typeName = XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id();
				} else {
					typeName = part.getElementName().getLocalPart();
					NativeType nativeType = XsdUtils.xsdToNativeType( typeName );
					if( nativeType != null ) {
						typeName = nativeType.id();
					}
				}
			}
		}

		return typeName;
	}

	@SuppressWarnings( "unchecked" )
	private joliex.wsdl.impl.Operation convertOperation( Operation operation, Style style )
		throws IOException {
		String comment = "";
		if( operation.getDocumentationElement() != null ) {
			operation.getDocumentationElement().getNodeValue();
		}
		String responseTypeName = null;

		String requestTypeName =
			convertOperationMessage( operation.getInput().getMessage(), operation.getName(), style );
		if( operation.getOutput() != null ) {
			responseTypeName =
				convertOperationMessage( operation.getOutput().getMessage(), operation.getName(), style );
		}

		Map< String, Part > parts;
		List< Pair< String, String > > faultList = new ArrayList<>();
		Map< String, Fault > faults = operation.getFaults();
		for( Entry< String, Fault > entry : faults.entrySet() ) {
			String faultName = entry.getKey();
			String faultTypeName = null;
			parts = entry.getValue().getMessage().getParts();
			if( parts.size() > 1 ) {
				String typeName = faultName = faultTypeName;
				TypeInlineDefinition faultType = new TypeInlineDefinition( ParsingContext.DEFAULT, typeName,
					BasicTypeDefinition.of( NativeType.VOID ), jolie.lang.Constants.RANGE_ONE_TO_ONE );
				for( Entry< String, Part > partEntry : parts.entrySet() ) {
					Part part = partEntry.getValue();
					if( part.getElementName() == null ) {
						if( part.getTypeName() == null ) {
							throw new IOException( "Could not parse message part " + entry.getKey() + " for operation "
								+ operation.getName() + "." );
						}
						TypeDefinitionLink link = new TypeDefinitionLink(
							ParsingContext.DEFAULT,
							part.getName(),
							jolie.lang.Constants.RANGE_ONE_TO_ONE,
							XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id() );
						faultType.putSubType( link );
					} else {
						TypeDefinitionLink link = new TypeDefinitionLink(
							ParsingContext.DEFAULT,
							part.getName(),
							jolie.lang.Constants.RANGE_ONE_TO_ONE,
							part.getElementName().getLocalPart() );
						faultType.putSubType( link );
					}
				}
				typeDefinitions.add( faultType );
			} else {
				for( Entry< String, Part > e : parts.entrySet() ) {
					Part part = e.getValue();
					if( part.getElementName() == null ) {
						if( part.getTypeName() == null ) {
							throw new IOException( "Could not parse message part " + e.getKey() + " for operation "
								+ operation.getName() + ", fault " + entry.getKey() + "." );
						}
						faultTypeName = XsdUtils.xsdToNativeType( part.getTypeName().getLocalPart() ).id();
					} else {
						faultTypeName = part.getElementName().getLocalPart();
						NativeType nativeType = XsdUtils.xsdToNativeType( faultTypeName );
						if( nativeType != null ) {
							faultTypeName = nativeType.id();
						}
					}
				}
			}
			faultList.add( new Pair<>( faultName, faultTypeName ) );
		}

		return new joliex.wsdl.impl.Operation(
			operation.getName(),
			requestTypeName,
			responseTypeName,
			faultList,
			comment );
	}
}
