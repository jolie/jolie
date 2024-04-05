/*
 * Copyright (C) 2006-2012 by Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package jolie.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wsdl.extensions.schema.SchemaImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPHeaderImpl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;

import jakarta.xml.soap.Detail;
import jakarta.xml.soap.DetailEntry;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.monitoring.events.ProtocolMessageEvent;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.ports.Interface;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.net.soap.WSDLCache;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.tracer.ProtocolTraceAction;

/**
 * Implements the SOAP over HTTP protocol.
 *
 * @author Fabrizio Montesi
 *
 *         2006 - Fabrizio Montesi, Mauro Silvagni: first write. 2007 - Fabrizio Montesi: rewritten
 *         from scratch, exploiting new JOLIE capabilities. 2008 - Fabrizio Montesi: initial support
 *         for schemas. 2008 - Claudio Guidi: initial support for WS-Addressing. 2010 - Fabrizio
 *         Montesi: initial support for WSDL documents.
 *
 */
public class SoapProtocol extends SequentialCommProtocol implements HttpUtils.HttpProtocol {
	private String inputId = null;
	private final Interpreter interpreter;
	private final MessageFactory messageFactory;
	private XSSchemaSet schemaSet = null;
	private final URI uri;
	private final boolean inInputPort;
	private Definition wsdlDefinition = null;
	private Port wsdlPort = null;
	private final TransformerFactory transformerFactory;
	private final Map< String, String > namespacePrefixMap = new HashMap<>();
	private boolean received = false;
	private String encoding;
	private final WSDLCache wsdlCache;

	private static class Parameters {
		private static final String WRAPPED = "wrapped";
		private static final String INHERITED_TYPE = "__soap_inherited_type";
		private static final String ADD_ATTRIBUTE = "add_attribute";
		private static final String ENVELOPE = "envelope";
		private static final String OPERATION = "operation";
		private static final String STYLE = "style";
		private static final String HTTP_BASIC_AUTHENTICATION = "HttpBasicAuthentication";
	}

	/*
	 * it forced the insertion of namespaces within the soap message
	 *
	 *
	 * type Attribute: void { .name: string .value: string }
	 *
	 * parameter add_attribute: void { .envelope: void { .attribute*: Attribute } .operation*: void {
	 * .operation_name: string .attribute: Attribute } }
	 */
	@Override
	public String name() {
		return "soap";
	}

	public SoapProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		Interpreter interpreter )
		throws SOAPException, WSDLException {
		super( configurationPath );
		this.uri = uri;
		this.inInputPort = inInputPort;
		this.transformerFactory = TransformerFactory.newInstance();
		this.interpreter = interpreter;
		this.messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_1_PROTOCOL );
		this.wsdlCache = new WSDLCache();
	}

	private void parseSchemaElement( Definition definition, Element element, XSOMParser schemaParser )
		throws IOException {
		try {
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
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
	private void parseWSDLTypes( XSOMParser schemaParser )
		throws IOException {
		Definition definition = getWSDLDefinition();
		if( definition != null ) {
			Types types = definition.getTypes();
			if( types != null ) {
				List< ExtensibilityElement > list = types.getExtensibilityElements();
				for( ExtensibilityElement element : list ) {
					if( element instanceof SchemaImpl ) {
						Element schemaElement = ((SchemaImpl) element).getElement();
						Map< String, String > namespaces = definition.getNamespaces();
						for( Entry< String, String > entry : namespaces.entrySet() ) {
							if( entry.getKey().equals( "xmlns" ) || entry.getKey().trim().isEmpty() ) {
								continue;
							}
							if( schemaElement.getAttribute( "xmlns:" + entry.getKey() ).isEmpty() ) {
								schemaElement.setAttribute( "xmlns:" + entry.getKey(), entry.getValue() );
							}
						}
						parseSchemaElement( definition, schemaElement, schemaParser );
					}
				}
			}
		}
	}

	private XSSchemaSet getSchemaSet()
		throws IOException, SAXException {
		if( schemaSet == null ) {
			XSOMParser schemaParser = new XSOMParser();
			ValueVector vec = getParameterVector( "schema" );
			if( vec.size() > 0 ) {
				for( Value v : vec ) {
					schemaParser.parse( new File( v.strValue() ) );
				}
			}
			parseWSDLTypes( schemaParser );
			schemaSet = schemaParser.getResult();
			String nsPrefix = "jolie";
			int i = 1;
			for( XSSchema schema : schemaSet.getSchemas() ) {
				if( !schema.getTargetNamespace().equals( XMLConstants.W3C_XML_SCHEMA_NS_URI ) ) {
					namespacePrefixMap.put( schema.getTargetNamespace(), nsPrefix + i++ );
				}
			}
		}

		return schemaSet;
	}

	private boolean convertAttributes() {
		boolean ret = false;
		if( hasParameter( "convertAttributes" ) ) {
			ret = checkBooleanParameter( "convertAttributes" );
		}
		return ret;
	}

	private void initNamespacePrefixes( SOAPElement element )
		throws SOAPException {
		for( Entry< String, String > entry : namespacePrefixMap.entrySet() ) {
			element.addNamespaceDeclaration( entry.getValue(), entry.getKey() );
		}
	}

	private void valueToSOAPElement(
		Value value,
		SOAPElement element,
		SOAPEnvelope soapEnvelope )
		throws SOAPException {
		String type = "any";
		if( value.isDefined() ) {
			if( value.isInt() ) {
				type = "int";
			} else if( value.isLong() ) {
				type = "long";
			} else if( value.isString() ) {
				type = "string";
			} else if( value.isDouble() ) {
				type = "double";
			} else if( value.isBool() ) {
				type = "boolean";
			}
			element.addAttribute( soapEnvelope.createName( "type", "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI ),
				"xsd:" + type );
			element.addTextNode( value.strValue() );
		} else if( !value.hasChildren() ) {
			element.addAttribute( soapEnvelope.createName( "nil", "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI ),
				"true" );
		}

		if( convertAttributes() ) {
			Map< String, ValueVector > attrs = getAttributesOrNull( value );
			if( attrs != null ) {
				for( Entry< String, ValueVector > attrEntry : attrs.entrySet() ) {
					element.addAttribute(
						soapEnvelope.createName( attrEntry.getKey() ),
						attrEntry.getValue().first().strValue() );
				}
			}
		}

		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					valueToSOAPElement(
						val,
						element.addChildElement( entry.getKey() ),
						soapEnvelope );
				}
			}
		}
	}

	private static Map< String, ValueVector > getAttributesOrNull( Value value ) {
		Map< String, ValueVector > ret = null;
		ValueVector vec = value.children().get( Constants.Predefined.ATTRIBUTES.token().content() );
		if( vec != null && vec.size() > 0 ) {
			ret = vec.first().children();
		}

		if( ret == null ) {
			ret = new HashMap<>();
		}

		return ret;
	}

	private static Value getAttributeOrNull( Value value, String attrName ) {
		Value ret = null;
		Map< String, ValueVector > attrs = getAttributesOrNull( value );
		if( attrs != null ) {
			ValueVector vec = attrs.get( attrName );
			if( vec != null && vec.size() > 0 ) {
				ret = vec.first();
			}
		}

		return ret;
	}

	private static Value getAttribute( Value value, String attrName ) {
		return value.getChildren( Constants.Predefined.ATTRIBUTES.token().content() ).first().getChildren( attrName )
			.first();
	}

	private String getPrefixOrNull( XSAttributeDecl decl ) {
		if( decl.getOwnerSchema().attributeFormDefault() ) {
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		}
		return null;
	}

	private String getPrefixOrNull( XSElementDecl decl ) {
		if( decl.getOwnerSchema().elementFormDefault() ) {
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		}
		return null;
	}

	private String getPrefix( XSElementDecl decl ) {
		return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
	}

	private String getPrefix( XSComplexType compType ) {
		return namespacePrefixMap.get( compType.getOwnerSchema().getTargetNamespace() );
	}

	private void termProcessing( Value value, SOAPElement element, SOAPEnvelope envelope, boolean first,
		XSTerm currTerm, int getMaxOccur,
		XSSchemaSet sSet, String messageNamespace )
		throws SOAPException {
		Value currValue = value.clone();
		if( currTerm.isElementDecl() ) {
			ValueVector vec;
			XSElementDecl currElementDecl = currTerm.asElementDecl();
			String name = currElementDecl.getName();
			String prefix = (first) ? getPrefix( currElementDecl ) : getPrefixOrNull( currElementDecl );
			SOAPElement childElement;
			if( (vec = currValue.children().get( name )) != null ) {
				int k = 0;
				while( vec.size() > 0 && (getMaxOccur > k || getMaxOccur == XSParticle.UNBOUNDED) ) {
					if( prefix == null ) {
						childElement = element.addChildElement( name );
					} else {
						childElement = element.addChildElement( name, prefix );
					}
					Value v = vec.remove( 0 );
					valueToTypedSOAP(
						v,
						currElementDecl,
						childElement,
						envelope,
						false,
						sSet,
						messageNamespace );
					k++;
				}
			}
		}

	}

	private void groupProcessing(
		Value value,
		XSElementDecl xsDecl,
		SOAPElement element,
		SOAPEnvelope envelope,
		boolean first,
		XSModelGroup modelGroup,
		XSSchemaSet sSet,
		String messageNamespace )
		throws SOAPException {

		XSParticle[] children = modelGroup.getChildren();
		XSTerm currTerm;
		for( XSParticle child : children ) {
			currTerm = child.getTerm();
			if( currTerm.isModelGroup() ) {
				groupProcessing( value, xsDecl, element, envelope, first, currTerm.asModelGroup(), sSet,
					messageNamespace );
			} else {
				termProcessing( value, element, envelope, first, currTerm, child.getMaxOccurs(), sSet,
					messageNamespace );
			}
		}
	}

	private void valueToTypedSOAP(
		Value value,
		XSElementDecl xsDecl,
		SOAPElement element,
		SOAPEnvelope envelope,
		boolean first, // Ugly fix! This should be removed as soon as another option arises.
		XSSchemaSet sSet,
		String messageNamespace )
		throws SOAPException {

		XSType currType = xsDecl.getType();

		if( currType.isSimpleType() ) {
			element.addTextNode( value.strValue() );

		} else if( currType.isComplexType() ) {
			XSType type = currType;
			if( currType.asComplexType().isAbstract() ) {
				// if the complex type is abstract search for the inherited type defined into the jolie value
				// under the node __soap_inherited_type
				if( value.hasChildren( Parameters.INHERITED_TYPE ) ) {
					String inheritedType = value.getFirstChild( Parameters.INHERITED_TYPE ).strValue();
					XSComplexType xsInheritedType = sSet.getComplexType( messageNamespace, inheritedType );
					if( xsInheritedType == null ) {
						System.out.println( "WARNING: Type " + inheritedType + " not found in the schema set" );
					} else {
						type = xsInheritedType;
						String nameType = "type";
						String prefixType = "xsi";
						QName attrName = envelope.createQName( nameType, prefixType );
						element.addAttribute( attrName, getPrefix( xsInheritedType ) + ":" + inheritedType );
					}
				}

			}
			String name;
			Value currValue;
			XSComplexType complexT = type.asComplexType();
			XSParticle particle;
			XSContentType contentT;

			// end new stuff
			// Iterate over attributes
			Collection< ? extends XSAttributeUse > attributeUses = complexT.getAttributeUses();
			for( XSAttributeUse attrUse : attributeUses ) {
				name = attrUse.getDecl().getName();
				if( (currValue = getAttributeOrNull( value, name )) != null ) {
					String prefix = getPrefixOrNull( attrUse.getDecl() );
					if( prefix == null ) {
						element.addAttribute( envelope.createName( name ), currValue.strValue() );
					} else {
						QName attrName = envelope.createQName( name, getPrefixOrNull( attrUse.getDecl() ) );
						element.addAttribute( attrName, currValue.strValue() );
					}
				}
			}

			// processing content (no base type parent )
			contentT = complexT.getContentType();
			if( contentT.asSimpleType() != null ) {
				element.addTextNode( value.strValue() );
			} else if( (particle = contentT.asParticle()) != null ) {
				XSTerm term = particle.getTerm();
				XSModelGroupDecl modelGroupDecl;
				XSModelGroup modelGroup = null;
				if( (modelGroupDecl = term.asModelGroupDecl()) != null ) {
					modelGroup = modelGroupDecl.getModelGroup();
				} else if( term.isModelGroup() ) {
					modelGroup = term.asModelGroup();
				}

				if( modelGroup != null ) {
					XSModelGroup.Compositor compositor = modelGroup.getCompositor();
					if( compositor.equals( XSModelGroup.SEQUENCE ) ) {
						groupProcessing( value, xsDecl, element, envelope, first, modelGroup, sSet, messageNamespace );
					}
				}
			}
		}
	}

	private Definition getWSDLDefinition()
		throws IOException {
		if( wsdlDefinition == null && hasParameter( "wsdl" ) ) {
			String wsdlUrl = getStringParameter( "wsdl" );
			try {
				wsdlDefinition = wsdlCache.get( wsdlUrl );
			} catch( WSDLException e ) {
				throw new IOException( e );
			}
		}
		return wsdlDefinition;
	}

	@SuppressWarnings( "unchecked" )
	private String getSoapActionForOperation( String operationName )
		throws IOException {
		String soapAction = null;
		Port port = getWSDLPort();
		if( port != null ) {
			BindingOperation bindingOperation = port.getBinding().getBindingOperation( operationName, null, null );
			for( ExtensibilityElement element : (List< ExtensibilityElement >) bindingOperation
				.getExtensibilityElements() ) {
				if( element instanceof SOAPOperation ) {
					soapAction = ((SOAPOperation) element).getSoapActionURI();
				}
			}
		}
		if( soapAction == null ) {
			soapAction = getStringParameter( "namespace" ) + "/" + operationName;
		}
		return soapAction;
	}

	@SuppressWarnings( "unchecked" )
	private Port getWSDLPort()
		throws IOException {
		Port port = wsdlPort;
		if( port == null && hasParameter( "wsdl" ) && getParameterFirstValue( "wsdl" ).hasChildren( "port" ) ) {
			String portName = getParameterFirstValue( "wsdl" ).getFirstChild( "port" ).strValue();
			Definition definition = getWSDLDefinition();
			if( definition != null ) {
				Map< QName, Service > services = definition.getServices();
				Iterator< Entry< QName, Service > > it = services.entrySet().iterator();
				while( port == null && it.hasNext() ) {
					port = it.next().getValue().getPort( portName );
				}
			}
			if( port != null ) {
				wsdlPort = port;
			}
		}
		return port;
	}

	@SuppressWarnings( "unchecked" )
	private String getOutputMessageRootElementName( String operationName )
		throws IOException {
		String elementName = operationName + ((received) ? "Response" : "");
		Port port = getWSDLPort();
		if( port != null ) {
			try {
				Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
				List< ExtensibilityElement > listExt;
				Message soapMessage;
				if( received ) {
					// We are sending a response
					soapMessage = operation.getOutput().getMessage();
					listExt = getWSDLPort().getBinding().getBindingOperation( operationName, null, null )
						.getBindingOutput().getExtensibilityElements();
				} else {
					// We are sending a request
					soapMessage = operation.getInput().getMessage();
					listExt = getWSDLPort().getBinding().getBindingOperation( operationName, null, null )
						.getBindingInput().getExtensibilityElements();
				}
				for( ExtensibilityElement element : listExt ) {
					if( element instanceof SOAPBodyImpl ) {
						SOAPBodyImpl sBodyImpl = (SOAPBodyImpl) element;
						if( sBodyImpl.getParts().size() > 0 ) {
							String partName = sBodyImpl.getParts().get( 0 ).toString();
							elementName = soapMessage.getPart( partName ).getElementName().getLocalPart();
						} else {
							Part part = ((Entry< String, Part >) soapMessage.getParts().entrySet().iterator().next())
								.getValue();
							elementName = part.getElementName().getLocalPart();
						}

					}
				}

			} catch( Exception e ) {
			}
		}
		return elementName;
	}

	@SuppressWarnings( "unchecked" )
	private String getOutputMessageNamespace( String operationName )
		throws IOException {
		String messageNamespace = "";
		Port port = getWSDLPort();
		if( port == null ) {
			if( hasParameter( "namespace" ) ) {
				messageNamespace = getStringParameter( "namespace" );
			}
		} else {
			Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
			List< ExtensibilityElement > listExt;
			Message soapMessage;
			if( received ) {
				// We are sending a response
				if( operation.getStyle().equals( OperationType.ONE_WAY ) ) {
					soapMessage = operation.getInput().getMessage();
					listExt = getWSDLPort().getBinding().getBindingOperation( operationName, null, null )
						.getBindingInput().getExtensibilityElements();
				} else {
					soapMessage = operation.getOutput().getMessage();
					listExt = getWSDLPort().getBinding().getBindingOperation( operationName, null, null )
						.getBindingOutput().getExtensibilityElements();
				}
			} else {
				// We are sending a request
				soapMessage = operation.getInput().getMessage();
				listExt = getWSDLPort().getBinding().getBindingOperation( operationName, null, null ).getBindingInput()
					.getExtensibilityElements();
			}
			for( ExtensibilityElement element : listExt ) {
				if( element instanceof SOAPBodyImpl ) {
					SOAPBodyImpl sBodyImpl = (SOAPBodyImpl) element;
					if( sBodyImpl.getParts() != null && sBodyImpl.getParts().size() > 0 ) {
						String partName = sBodyImpl.getParts().get( 0 ).toString();
						messageNamespace = soapMessage.getPart( partName ).getElementName().getNamespaceURI();
					} else {
						Part part =
							((Entry< String, Part >) soapMessage.getParts().entrySet().iterator().next()).getValue();
						messageNamespace = part.getElementName().getNamespaceURI();
					}
				}
			}
		}
		return messageNamespace;
	}

	@SuppressWarnings( "unchecked" )
	private String[] getParameterOrder( String operationName )
		throws IOException {
		List< String > parameters = null;
		Port port = getWSDLPort();
		if( port != null ) {
			Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
			if( operation != null ) {
				parameters = operation.getParameterOrdering();
			}
		}
		return (parameters == null) ? null : parameters.toArray( new String[ 0 ] );
	}

	@SuppressWarnings( "unchecked" )
	private void setOutputEncodingStyle( SOAPEnvelope soapEnvelope, String operationName )
		throws IOException, SOAPException {
		Port port = getWSDLPort();
		if( port != null ) {
			BindingOperation bindingOperation = port.getBinding().getBindingOperation( operationName, null, null );
			if( bindingOperation == null ) {
				return;
			}
			BindingOutput output = bindingOperation.getBindingOutput();
			if( output == null ) {
				return;
			}
			for( ExtensibilityElement element : (List< ExtensibilityElement >) output.getExtensibilityElements() ) {
				if( element instanceof javax.wsdl.extensions.soap.SOAPBody ) {
					List< String > list = ((javax.wsdl.extensions.soap.SOAPBody) element).getEncodingStyles();
					if( list != null && list.isEmpty() == false ) {
						soapEnvelope.setEncodingStyle( list.get( 0 ) );
						soapEnvelope.addNamespaceDeclaration( "enc", list.get( 0 ) );
					}
				}
			}
		}
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {

		final StringBuilder httpMessage = new StringBuilder();
		ByteArray content = null;

		try {
			inputId = message.operationName();
			String messageNamespace = getOutputMessageNamespace( message.operationName() );

			if( received ) {
				// We're responding to a request
				inputId += "Response";
			}

			SOAPMessage soapMessage = messageFactory.createMessage();
			soapMessage.setProperty( SOAPMessage.WRITE_XML_DECLARATION, "true" );
			soapMessage.setProperty( SOAPMessage.CHARACTER_SET_ENCODING, "utf-8" );
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			setOutputEncodingStyle( soapEnvelope, message.operationName() );
			SOAPBody soapBody = soapEnvelope.getBody();
			SOAPHeader soapHeader = soapEnvelope.getHeader();
			Value valueToSend = message.value();
			boolean basicAuthentication = false;
			String userpass = "";

			if( hasOperationSpecificParameter( message.operationName(), Parameters.HTTP_BASIC_AUTHENTICATION )
				|| hasParameter( Parameters.HTTP_BASIC_AUTHENTICATION ) ) {
				Value basicAuthValue = null;
				if( hasOperationSpecificParameter( message.operationName(), Parameters.HTTP_BASIC_AUTHENTICATION ) ) {
					basicAuthValue = getOperationSpecificParameterFirstValue( message.operationName(),
						Parameters.HTTP_BASIC_AUTHENTICATION );
				} else {
					basicAuthValue = getParameterFirstValue( Parameters.HTTP_BASIC_AUTHENTICATION );
				}
				userpass =
					basicAuthValue.getFirstChild( "userid" ).strValue() + ":" +
						basicAuthValue.getFirstChild( "password" ).strValue();
				Base64.Encoder encoder = Base64.getEncoder();
				userpass = encoder.encodeToString( userpass.getBytes() );
				basicAuthentication = true;
			}


			if( checkBooleanParameter( "wsAddressing" ) ) {
				// WS-Addressing namespace
				soapHeader.addNamespaceDeclaration( "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				// Message ID
				Name messageIdName =
					soapEnvelope.createName( "MessageID", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement messageIdElement = soapHeader.addHeaderElement( messageIdName );
				if( received ) {
					// TODO: remove this after we implement a mechanism for being sure message.id() is the one received
					// before.
					messageIdElement.setValue( "uuid:1" );
				} else {
					messageIdElement.setValue( "uuid:" + message.requestId() );
				}
				// Action element
				Name actionName =
					soapEnvelope.createName( "Action", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement actionElement = soapHeader.addHeaderElement( actionName );
				/*
				 * TODO: the action element could be specified within the parameter. Perhaps wsAddressing.action ?
				 * We could also allow for giving a prefix or a suffix to the operation name, like
				 * wsAddressing.action.prefix, wsAddressing.action.suffix
				 */
				actionElement.setValue( message.operationName() );
				// From element
				Name fromName =
					soapEnvelope.createName( "From", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement fromElement = soapHeader.addHeaderElement( fromName );
				Name addressName =
					soapEnvelope.createName( "Address", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPElement addressElement = fromElement.addChildElement( addressName );
				addressElement.setValue( "http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous" );
				// To element
				/*
				 * if ( operation == null ) { // we are sending a Notification or a Solicit Name toName =
				 * soapEnvelope.createName("To", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing");
				 * SOAPHeaderElement toElement=soapHeader.addHeaderElement(toName);
				 * toElement.setValue(getURI().getHost()); }
				 */
			}

			if( message.isFault() ) {
				FaultException f = message.fault();
				SOAPFault soapFault = soapBody.addFault();
				soapFault.setFaultCode( soapEnvelope.createQName( "Server", soapEnvelope.getPrefix() ) );
				soapFault.setFaultString( f.getMessage() );
				Detail detail = soapFault.addDetail();
				DetailEntry de =
					detail.addDetailEntry( soapEnvelope.createName( f.faultName(), null, messageNamespace ) );
				valueToSOAPElement( f.value(), de, soapEnvelope );
			} else {
				XSSchemaSet sSet = getSchemaSet();
				XSElementDecl elementDecl;
				String messageRootElementName = getOutputMessageRootElementName( message.operationName() );

				if( sSet == null
					|| (elementDecl = sSet.getElementDecl( messageNamespace, messageRootElementName )) == null ) {
					Name operationName;
					soapEnvelope.addNamespaceDeclaration( "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI );
					soapEnvelope.addNamespaceDeclaration( "xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI );
					if( messageNamespace.isEmpty() ) {
						operationName = soapEnvelope.createName( messageRootElementName );
					} else {
						soapEnvelope.addNamespaceDeclaration( "jolieMessage", messageNamespace );
						operationName =
							soapEnvelope.createName( messageRootElementName, "jolieMessage", messageNamespace );
					}

					/*
					 * if( hasParameter( "header") ) { if ( getParameterFirstValue( "header" ).hasChildren() ) { //
					 * Prepare SOAP Header getting data from parameter .header SOAPHeader soapHeader =
					 * soapEnvelope.getHeader();
					 * 
					 * } }
					 */
					SOAPBodyElement opBody = soapBody.addBodyElement( operationName );
					String[] parameters = getParameterOrder( message.operationName() );
					if( parameters == null ) {
						valueToSOAPElement( valueToSend, opBody, soapEnvelope );
					} else {
						for( String parameterName : parameters ) {
							valueToSOAPElement( valueToSend.getFirstChild( parameterName ),
								opBody.addChildElement( parameterName ), soapEnvelope );
						}
					}
				} else {
					initNamespacePrefixes( soapEnvelope );

					if( hasParameter( Parameters.ADD_ATTRIBUTE ) ) {
						Value add_parameter = getParameterFirstValue( Parameters.ADD_ATTRIBUTE );
						if( add_parameter.hasChildren( Parameters.ENVELOPE ) ) {
							// attributes must be added to the envelope
							ValueVector attributes =
								add_parameter.getFirstChild( Parameters.ENVELOPE ).getChildren( "attribute" );
							for( Value att : attributes ) {
								soapEnvelope.addNamespaceDeclaration( att.getFirstChild( "name" ).strValue(),
									att.getFirstChild( "value" ).strValue() );
							}
						}
					}
					boolean wrapped = true;
					Value vStyle = getParameterVector( Parameters.STYLE ).first();
					if( "document".equals( vStyle.strValue() ) ) {
						wrapped = vStyle.getFirstChild( Parameters.WRAPPED ).boolValue();
					}
					SOAPElement opBody = soapBody;
					if( wrapped ) {
						List< ExtensibilityElement > listExt;
						if( received ) {
							listExt =
								getWSDLPort().getBinding().getBindingOperation( message.operationName(), null, null )
									.getBindingOutput().getExtensibilityElements();
						} else {
							listExt =
								getWSDLPort().getBinding().getBindingOperation( message.operationName(), null, null )
									.getBindingInput().getExtensibilityElements();
						}

						for( ExtensibilityElement extElement : listExt ) {
							if( extElement instanceof SOAPHeaderImpl ) {
								SOAPHeaderImpl soapHeaderImpl = (SOAPHeaderImpl) extElement;
								if( valueToSend.getChildren( soapHeaderImpl.getPart() ).size() > 0 ) {
									Definition definition = getWSDLDefinition();

									Message wsdlMessage = definition.getMessage( soapHeaderImpl.getMessage() );
									XSElementDecl partElementDeclaration = sSet.getElementDecl(
										wsdlMessage.getPart( soapHeaderImpl.getPart() ).getElementName()
											.getNamespaceURI(),
										wsdlMessage.getPart( soapHeaderImpl.getPart() ).getElementName()
											.getLocalPart() );
									SOAPHeaderElement headerElement =
										soapHeader.addHeaderElement( soapEnvelope.createName(
											wsdlMessage.getPart( soapHeaderImpl.getPart() ).getElementName()
												.getLocalPart(),
											wsdlMessage.getPart( soapHeaderImpl.getPart() ).getElementName()
												.getPrefix(),
											wsdlMessage.getPart( soapHeaderImpl.getPart() ).getElementName()
												.getNamespaceURI() ) );
									valueToTypedSOAP(
										valueToSend.getFirstChild( soapHeaderImpl.getPart() ),
										partElementDeclaration,
										headerElement,
										soapEnvelope,
										!wrapped,
										sSet,
										partElementDeclaration.getTargetNamespace() );
									valueToSend.children().remove( soapHeaderImpl.getPart() );
								}
							}
						}
						opBody = soapBody.addBodyElement(
							soapEnvelope.createName( messageRootElementName,
								namespacePrefixMap.get( elementDecl.getOwnerSchema().getTargetNamespace() ), null ) );
						// adding forced attributes to operation
						if( hasParameter( Parameters.ADD_ATTRIBUTE ) ) {
							Value add_parameter = getParameterFirstValue( Parameters.ADD_ATTRIBUTE );
							if( add_parameter.hasChildren( Parameters.OPERATION ) ) {
								ValueVector operations = add_parameter.getChildren( Parameters.OPERATION );
								for( Value op : operations ) {
									if( op.getFirstChild( "operation_name" ).strValue()
										.equals( message.operationName() ) ) {
										// attributes must be added to the envelope
										Value attribute = op.getFirstChild( "attribute" );
										QName attrName;
										if( attribute.hasChildren( "prefix" ) ) {
											attrName = opBody.createQName( attribute.getFirstChild( "name" ).strValue(),
												attribute.getFirstChild( "prefix" ).strValue() );
										} else {
											attrName = opBody.createQName( attribute.getFirstChild( "name" ).strValue(),
												null );
										}
										opBody.addAttribute( attrName, attribute.getFirstChild( "value" ).strValue() );
									}
								}
							}
						}
					}

					// check if the body has been defined with more than one parts
					// Operation operation =
					// getWSDLPort().getBinding().getPortType().getOperation( message.operationName(), null, null );
					// Message wsdlMessage;
					List< ExtensibilityElement > listExt;
					if( received ) {
						// We are sending a response
						// wsdlMessage = operation.getOutput().getMessage();
						listExt = getWSDLPort().getBinding().getBindingOperation( message.operationName(), null, null )
							.getBindingOutput().getExtensibilityElements();
					} else {
						// We are sending a request
						// wsdlMessage = operation.getInput().getMessage();
						listExt = getWSDLPort().getBinding().getBindingOperation( message.operationName(), null, null )
							.getBindingInput().getExtensibilityElements();
					}
					boolean partsInBody = false;
					String partName = "";
					for( ExtensibilityElement element : listExt ) {
						if( element instanceof SOAPBodyImpl ) {
							SOAPBodyImpl sBodyImpl = (SOAPBodyImpl) element;
							if( sBodyImpl.getParts() != null && sBodyImpl.getParts().size() > 0 ) {
								partName = sBodyImpl.getParts().get( 0 ).toString();
								partsInBody = true;
							}
						}
					}

					if( !partsInBody ) {
						valueToTypedSOAP( valueToSend, elementDecl, opBody, soapEnvelope, !wrapped, sSet,
							messageNamespace );
					} else {
						// we support only body with one element as a root
						valueToTypedSOAP( valueToSend.getFirstChild( partName ), elementDecl, opBody, soapEnvelope,
							!wrapped, sSet, messageNamespace );
					}

				}
			}

			if( soapEnvelope.getHeader().hasChildNodes() == false ) {
				// Some service implementations do not like empty headers
				soapEnvelope.getHeader().detachNode();
			}

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			content = new ByteArray( tmpStream.toByteArray() );

			String soapAction = null;

			if( received ) {
				// We're responding to a request
				if( message.isFault() ) {
					httpMessage.append( "HTTP/1.1 500 Internal Server Error" ).append( HttpUtils.CRLF );
				} else {
					httpMessage.append( "HTTP/1.1 200 OK" ).append( HttpUtils.CRLF );
				}
				httpMessage.append( "Server: Jolie" ).append( HttpUtils.CRLF );
				received = false;
			} else {
				// We're sending a notification or a solicit
				String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
				if( path == null || path.length() == 0 ) {
					path = "*";
				}
				httpMessage.append( "POST " ).append( path ).append( " HTTP/1.1" ).append( HttpUtils.CRLF )
					.append( "Host: " ).append( uri.getHost() ).append( HttpUtils.CRLF );
				/* basic authentication: code replication from HttpProtocol. Refactoring is needed */
				if( basicAuthentication ) {
					httpMessage.append( "Authorization: Basic " ).append( userpass ).append( HttpUtils.CRLF );
				}


				/*
				 * soapAction = "SOAPAction: \"" + messageNamespace + "/" + message.operationName() + '\"' +
				 * HttpUtils.CRLF;
				 */
				soapAction =
					"SOAPAction: \"" + getSoapActionForOperation( message.operationName() ) + '\"' + HttpUtils.CRLF;

				if( checkBooleanParameter( "compression", true ) ) {
					String requestCompression = getStringParameter( "requestCompression" );
					if( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
						encoding = requestCompression;
						httpMessage.append( "Accept-Encoding: " ).append( encoding ).append( HttpUtils.CRLF );
					} else {
						httpMessage.append( "Accept-Encoding: gzip, deflate" ).append( HttpUtils.CRLF );
					}
				}
			}

			if( getParameterVector( "keepAlive" ).first().intValue() != 1 ) {
				if( received )
					channel().setToBeClosed( true ); // we may do this only in input (server) mode
				httpMessage.append( "Connection: close" ).append( HttpUtils.CRLF );
			}

			ByteArray plainTextContent = content;

			if( encoding != null && checkBooleanParameter( "compression", true ) ) {
				content = HttpUtils.encode( encoding, content, httpMessage );
			}

			// httpMessage.append("Content-Type: application/soap+xml; charset=utf-8" + HttpUtils.CRLF);
			httpMessage.append( "Content-Type: text/xml; charset=utf-8" ).append( HttpUtils.CRLF );
			httpMessage.append( "Content-Length: " ).append( content.size() ).append( HttpUtils.CRLF );
			if( soapAction != null ) {
				httpMessage.append( soapAction );
			}
			httpMessage.append( HttpUtils.CRLF );

			if( getParameterVector( "debug" ).first().intValue() > 0 ) {
				interpreter.logInfo(
					"[SOAP debug] Sending:\n" + httpMessage.toString() + plainTextContent.toString( "utf-8" ) );
			}

			interpreter.tracer().trace( () -> {
				try {
					final String traceMessage = httpMessage.toString() + plainTextContent.toString( "utf-8" );
					return new ProtocolTraceAction( ProtocolTraceAction.Type.SOAP, "SOAP MESSAGE SENT",
						message.operationName(), traceMessage, null );

				} catch( UnsupportedEncodingException e ) {
					return new ProtocolTraceAction( ProtocolTraceAction.Type.SOAP, "SOAP MESSAGE SENT",
						message.operationName(), e.getMessage(), null );

				}

			} );

			if( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance().fireMonitorEvent(
					new ProtocolMessageEvent(
						plainTextContent.toString( "utf-8" ),
						httpMessage.toString(),
						ExecutionThread.currentThread().getSessionId(),
						Long.toString( message.id() ),
						ProtocolMessageEvent.Protocol.SOAP ) );
			}

			inputId = message.operationName();
		} catch( Exception e ) {
			if( received ) {
				httpMessage.setLength( 0 );

				try {
					SOAPMessage soapMessage = messageFactory.createMessage();
					soapMessage.setProperty( SOAPMessage.WRITE_XML_DECLARATION, "true" );
					soapMessage.setProperty( SOAPMessage.CHARACTER_SET_ENCODING, "utf-8" );
					SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
					setOutputEncodingStyle( soapEnvelope, message.operationName() );
					SOAPBody soapBody = soapEnvelope.getBody();
					SOAPFault soapFault = soapBody.addFault();
					soapFault.setFaultCode( soapEnvelope.createQName( "Server", soapEnvelope.getPrefix() ) );
					soapFault.setFaultString( "Error found in SOAP/XML format" );
					ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
					soapMessage.writeTo( tmpStream );
					content = new ByteArray( tmpStream.toByteArray() );

					httpMessage.append( "HTTP/1.1 500 Internal Server Error" ).append( HttpUtils.CRLF )
						.append( "Server: Jolie" ).append( HttpUtils.CRLF )
						.append( "Connection: close" ).append( HttpUtils.CRLF )
						.append( "Content-Type: text/xml; charset=utf-8" ).append( HttpUtils.CRLF )
						.append( "Content-Length: " ).append( content.size() ).append( HttpUtils.CRLF )
						.append( HttpUtils.CRLF );

					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new ProtocolMessageEvent(
								content.toString( "utf-8" ),
								httpMessage.toString(),
								ExecutionThread.currentThread().getSessionId(),
								Long.toString( message.id() ),
								ProtocolMessageEvent.Protocol.SOAP ) );
					}


				} catch( SOAPException se ) {
					System.out.println( se.getMessage() );
				}


			} else {
				throw new IOException( e );
			}
		}

		ostream.write( httpMessage.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		if( content != null ) {
			ostream.write( content.getBytes() );
		}
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	private void xmlNodeToValue( Value value, Node node, boolean isRecRoot ) {
		String type = "xsd:string";
		Node currNode;
		boolean nil = false;

		// Set attributes
		NamedNodeMap attributes = node.getAttributes();
		if( attributes != null ) {
			for( int i = 0; i < attributes.getLength(); i++ ) {
				currNode = attributes.item( i );
				if( currNode.getNamespaceURI().equals( XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI ) ) {
					switch( currNode.getLocalName() ) {
					case "type":
						type = currNode.getNodeValue();
						break;
					case "nil":
						nil = "true".equals( currNode.getNodeValue() );
						break;
					default:
						if( convertAttributes() ) {
							getAttribute( value, currNode.getNodeName() ).setValue( currNode.getNodeValue() );
						}
						break;
					}
				} else if( convertAttributes() ) {
					getAttribute( value, currNode.getNodeName() ).setValue( currNode.getNodeValue() );
				}
			}
		}

		// Set children
		NodeList list = node.getChildNodes();
		Value childValue;

		StringBuilder tmpNodeValue = new StringBuilder();

		boolean foundSubElements = false;
		for( int i = 0; i < list.getLength(); i++ ) {
			currNode = list.item( i );
			switch( currNode.getNodeType() ) {
			case Node.ELEMENT_NODE:
				childValue = value.getNewChild( currNode.getLocalName() );
				xmlNodeToValue( childValue, currNode, false );
				foundSubElements = true;
				break;
			case Node.TEXT_NODE:
				tmpNodeValue.append( currNode.getNodeValue() );
				break;
			}
		}

		// the content of the root of a mixed element is not extracted
		if( !foundSubElements && !nil ) {
			if( !isRecRoot ) {
				value.setValue( tmpNodeValue.toString() );
			}
		}

		if( "xsd:int".equals( type ) ) {
			value.setValue( value.intValue() );
		} else if( "xsd:long".equals( type ) ) {
			value.setValue( value.longValue() );
		} else if( "xsd:double".equals( type ) ) {
			value.setValue( value.doubleValue() );
		} else if( "xsd:boolean".equals( type ) ) {
			value.setValue( value.boolValue() );
		}
	}

	private static Element getFirstElement( Node node ) {
		NodeList nodes = node.getChildNodes();
		for( int i = 0; i < nodes.getLength(); i++ ) {
			if( nodes.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				return (Element) nodes.item( i );
			}
		}
		return null;
	}

	/*
	 * private Schema getRecvMessageValidationSchema() throws IOException { List< Source > sources = new
	 * ArrayList< Source >(); Definition definition = getWSDLDefinition(); if ( definition != null ) {
	 * Types types = definition.getTypes(); if ( types != null ) { List< ExtensibilityElement > list =
	 * types.getExtensibilityElements(); for( ExtensibilityElement element : list ) { if ( element
	 * instanceof SchemaImpl ) { sources.add( new DOMSource( ((SchemaImpl)element).getElement() ) ); } }
	 * } } SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI
	 * ); try { return schemaFactory.newSchema( sources.toArray( new Source[sources.size()] ) ); }
	 * catch( SAXException e ) { throw new IOException( e ); } }
	 */
	@Override
	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException {
		HttpParser parser = new HttpParser( istream );
		HttpMessage message = parser.parse();
		String charset = HttpUtils.getResponseCharset( message );
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if( inInputPort && message.type() != HttpMessage.Type.POST ) {
			throw new UnsupportedMethodException( "Only HTTP method POST allowed!", Method.POST );
		}

		encoding = message.getProperty( "accept-encoding" );

		CommMessage retVal = null;
		String messageId = "";
		FaultException fault = null;
		Value value = Value.create();

		try {
			if( message.size() > 0 ) {
				if( checkBooleanParameter( "debug" ) ) {
					interpreter.logInfo( "[SOAP debug] Receiving:\n" + new String( message.content(), charset ) );
				}


				SOAPMessage soapMessage = messageFactory.createMessage();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				/*
				 * Schema messageSchema = getRecvMessageValidationSchema(); if ( messageSchema != null ) {
				 * factory.setIgnoringElementContentWhitespace( true ); factory.setSchema( messageSchema ); }
				 */
				factory.setNamespaceAware( true );
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				src.setEncoding( charset );
				Document doc = builder.parse( src );
				DOMSource dom = new DOMSource( doc );
				soapMessage.getSOAPPart().setContent( dom );

				/*
				 * if ( checkBooleanParameter( "debugAfter" ) ) { ByteArrayOutputStream tmpStream = new
				 * ByteArrayOutputStream(); soapMessage.writeTo( tmpStream ); interpreter.logInfo(
				 * "[SOAP debug] Receiving:\n" + tmpStream.toString() ); }
				 */
				SOAPFault soapFault = soapMessage.getSOAPBody().getFault();
				if( soapFault == null ) {
					Element soapValueElement = getFirstElement( soapMessage.getSOAPBody() );
					messageId = soapValueElement.getLocalName();

					if( !channel().parentPort().getInterface().containsOperation( messageId ) ) {
						String[] soapAction =
							message.getPropertyOrEmptyString( "soapaction" ).replaceAll( "\"", "" ).split( "/" );
						messageId = soapAction[ soapAction.length - 1 ];
						if( checkBooleanParameter( "debug" ) ) {
							interpreter.logInfo( "Operation from SoapAction:" + messageId );
						}
					}

					// explanation: https://github.com/jolie/jolie/issues/5
					xmlNodeToValue( value, soapValueElement, checkBooleanParameter( "dropRootValue", false ) );

					ValueVector schemaPaths = getParameterVector( "schema" );
					if( schemaPaths.size() > 0 ) {
						List< Source > sources = new LinkedList<>();
						Value schemaPath;
						for( int i = 0; i < schemaPaths.size(); i++ ) {
							schemaPath = schemaPaths.get( i );
							if( schemaPath.getChildren( "validate" ).first().intValue() > 0 ) {
								sources.add( new StreamSource( new File( schemaPaths.get( i ).strValue() ) ) );
							}
						}

						if( !sources.isEmpty() ) {
							Schema schema = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
								.newSchema( sources.toArray( new Source[ 0 ] ) );
							schema.newValidator()
								.validate( new DOMSource( soapMessage.getSOAPBody().getFirstChild() ) );
						}
					}
				} else {
					String faultName = "UnknownFault";
					Value faultValue = Value.create();
					Detail d = soapFault.getDetail();
					if( d != null ) {
						Node n = d.getFirstChild();
						if( n != null ) {
							faultName = n.getLocalName();
							xmlNodeToValue( faultValue, n, true );
						} else {
							faultValue.setValue( soapFault.getFaultString() );
						}
					}
					fault = new FaultException( faultName, faultValue );
				}
			}

			String resourcePath = recv_getResourcePath( message );
			if( message.isResponse() ) {
				if( fault != null && message.statusCode() == 500 ) {
					fault = new FaultException( "InternalServerError", "" );
				}
				retVal = new CommMessage( CommMessage.GENERIC_REQUEST_ID, inputId, resourcePath, value, fault );
			} else {
				if( messageId.isEmpty() ) {
					throw new IOException( "Received SOAP Message without a specified operation" );
				}
				retVal = new CommMessage( CommMessage.GENERIC_REQUEST_ID, messageId, resourcePath, value, fault );
			}

			final String mId = messageId;
			interpreter.tracer().trace( () -> {
				final StringBuilder traceMessage = new StringBuilder();
				try {
					traceMessage.append( getHeadersFromHttpMessage( message ) ).append( "\n" )
						.append( new String( message.content(), charset ) );
					return new ProtocolTraceAction( ProtocolTraceAction.Type.SOAP, "SOAP MESSAGE RECEIVED", mId,
						traceMessage.toString(), null );
				} catch( UnsupportedEncodingException e ) {
					return new ProtocolTraceAction( ProtocolTraceAction.Type.SOAP, "SOAP MESSAGE RECEIVED", mId,
						e.getMessage(), null );
				}

			} );

			if( Interpreter.getInstance().isMonitoring() ) {
				final StringBuilder headerMonitor = new StringBuilder();

				headerMonitor.append( getHeadersFromHttpMessage( message ) ).append( "\n" );

				Interpreter.getInstance().fireMonitorEvent(
					new ProtocolMessageEvent(
						new String( message.content(), charset ),
						headerMonitor.toString(),
						"",
						Long.toString( retVal.id() ),
						ProtocolMessageEvent.Protocol.SOAP ) );
			}

		} catch( SOAPException | ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			// TODO support resourcePath
			retVal = new CommMessage( CommMessage.GENERIC_REQUEST_ID, messageId, "/", value,
				new FaultException( "TypeMismatch", e ) );
		}

		received = true;

		if( retVal != null && "/".equals( retVal.resourcePath() ) && channel().parentPort() != null
			&& channel().parentPort().getInterface().containsOperation( retVal.operationName() ) ) {
			try {
				// The message is for this service
				Interface iface = channel().parentPort().getInterface();
				OneWayTypeDescription oneWayTypeDescription = iface.oneWayOperations().get( retVal.operationName() );
				if( oneWayTypeDescription != null ) {
					// We are receiving a One-Way message
					if( message.isResponse() == false ) {
						oneWayTypeDescription.requestType().cast( retVal.value() );
					}
				} else {
					RequestResponseTypeDescription rrTypeDescription =
						iface.requestResponseOperations().get( retVal.operationName() );
					if( retVal.isFault() ) {
						Type faultType = rrTypeDescription.faults().get( retVal.fault().faultName() );
						if( faultType != null ) {
							faultType.cast( retVal.value() );
						}
					} else if( message.isResponse() ) {
						rrTypeDescription.responseType().cast( retVal.value() );
					} else {
						rrTypeDescription.requestType().cast( retVal.value() );
					}
				}
			} catch( TypeCastingException e ) {
				// TODO: do something here?
			}
		}

		return retVal;
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException {
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}

	private String recv_getResourcePath( HttpMessage message ) {
		String ret = "/";
		if( checkBooleanParameter( "interpretResource" ) ) {
			ret = message.requestPath();
		}
		return ret;
	}

	private String getHeadersFromHttpMessage( HttpMessage message ) {
		StringBuilder headers = new StringBuilder();
		headers.append( "HTTP Code: " ).append( message.statusCode() ).append( "\n" ).append( "Resource: " )
			.append( message.requestPath() ).append( "\n" );
		for( Entry< String, String > entry : message.properties() ) {
			headers.append( '\t' ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( '\n' );
		}
		for( HttpMessage.Cookie cookie : message.setCookies() ) {
			headers.append( "\tset-cookie: " ).append( cookie.toString() ).append( '\n' );
		}
		for( Entry< String, String > entry : message.cookies().entrySet() ) {
			headers.append( "\tcookie: " ).append( entry.getKey() ).append( '=' ).append( entry.getValue() )
				.append( '\n' );
		}

		return headers.toString();
	}
}
