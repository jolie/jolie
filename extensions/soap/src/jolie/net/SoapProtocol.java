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

import com.ibm.wsdl.extensions.schema.SchemaImpl;
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
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
import javax.wsdl.Operation;
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
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.ports.Interface;
import jolie.net.protocols.AsyncCommProtocol;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implements the SOAP over HTTP protocol.
 *
 * @author Fabrizio Montesi
 *
 * 2006 - Fabrizio Montesi, Mauro Silvagni: first write. 2007 - Fabrizio Montesi: rewritten from scratch, exploiting new JOLIE capabilities.
 * 2008 - Fabrizio Montesi: initial support for schemas. 2008 - Claudio Guidi: initial support for WS-Addressing. 2010 - Fabrizio Montesi:
 * initial support for WSDL documents.
 *
 */
public class SoapProtocol extends AsyncCommProtocol
{

	private String inputId = null;
	private final Interpreter interpreter;
	private final MessageFactory messageFactory;
	private XSSchemaSet schemaSet = null;
	private final URI uri;
	private final boolean inInputPort;
	private Definition wsdlDefinition = null;
	private Port wsdlPort = null;
	private final TransformerFactory transformerFactory;
	private final Map< String, String> namespacePrefixMap = new HashMap< String, String>();
	private boolean received = false;
	private String encoding;

	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{
		if (inInputPort) {
			pipeline.addLast( new HttpServerCodec() );
			pipeline.addLast( new HttpContentCompressor() );
		} else {
			pipeline.addLast( new HttpClientCodec() );
			pipeline.addLast( new HttpContentDecompressor() );
		}
		pipeline.addLast( new HttpObjectAggregator( 65536 ) );
		pipeline.addLast( new SoapCommMessageCodec() );
	}

	@Override
	public boolean isThreadSafe()
	{
		return false;
	}

	private static class Parameters
	{
		private static final String WRAPPED = "wrapped";
		private static final String INHERITED_TYPE = "__soap_inherited_type";
		private static final String ADD_ATTRIBUTE = "add_attribute";
		private static final String ENVELOPE = "envelope";
		private static final String OPERATION = "operation";
		private static final String STYLE = "style";
	}

	/* 
     * it forced the insertion of namespaces within the soap message
     * 
     * 
     * type Attribute: void {
     *	.name: string
     *  .value: string
     * }
     * 
     * parameter add_attribute: void {
     *	.envelope: void {
     *      .attribute*: Attribute
     *	}
     *	.operation*: void {
     *		.operation_name: string
     *		.attribute: Attribute
     *	}
     * }
	 */

	public String name()
	{
		return "soap";
	}

	public SoapProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		Interpreter interpreter )
		throws SOAPException, TransformerConfigurationException
	{
		super( configurationPath );
		this.uri = uri;
		this.inInputPort = inInputPort;
		this.transformerFactory = TransformerFactory.newInstance();
		this.interpreter = interpreter;
		this.messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_1_PROTOCOL );
	}
	
	public class SoapCommMessageCodec extends MessageToMessageCodec<FullHttpMessage, StatefulMessage>
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, StatefulMessage message, List<Object> out ) throws Exception
		{
			System.out.println( "Sending: " + message.toString() );
			FullHttpMessage msg = buildSoapMessage(message );
			out.add( msg );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, FullHttpMessage msg, List<Object> out ) throws Exception
		{
			if ( msg instanceof FullHttpRequest ) {
				FullHttpRequest request = (FullHttpRequest) msg;
				System.out.println( "HTTP request ! (" + request.uri() + ")" );
			} else if ( msg instanceof FullHttpResponse ) {
				FullHttpResponse response = (FullHttpResponse) msg;
				System.out.println( "HTTP response !" );
			}
			StatefulMessage message = recv_internal( msg );
			System.out.println( "Decoded Soap message for operation: " + message.message().operationName() );
			out.add( message );
		}

	}

	private void parseSchemaElement( Definition definition, Element element, XSOMParser schemaParser )
		throws IOException
	{
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
		} catch( TransformerConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			throw new IOException( e );
		}
	}

	private void parseWSDLTypes( StatefulContext ctx, XSOMParser schemaParser )
		throws IOException
	{
		Definition definition = getWSDLDefinition( ctx );
		if ( definition != null ) {
			Types types = definition.getTypes();
			if ( types != null ) {
				List<ExtensibilityElement> list = types.getExtensibilityElements();
				for( ExtensibilityElement element : list ) {
					if ( element instanceof SchemaImpl ) {
						Element schemaElement = ((SchemaImpl) element).getElement();
						Map<String, String> namespaces = definition.getNamespaces();
						for( Entry<String, String> entry : namespaces.entrySet() ) {
							if ( entry.getKey().equals( "xmlns" ) || entry.getKey().trim().isEmpty() ) {
								continue;
							}
							if ( schemaElement.getAttribute( "xmlns:" + entry.getKey() ).isEmpty() ) {
								schemaElement.setAttribute( "xmlns:" + entry.getKey(), entry.getValue() );
							}
						}
						parseSchemaElement( definition, schemaElement, schemaParser );
					}
				}
			}
		}
	}

	private XSSchemaSet getSchemaSet( StatefulContext ctx )
		throws IOException, SAXException
	{
		if ( schemaSet == null ) {
			XSOMParser schemaParser = new XSOMParser();
			ValueVector vec = getParameterVector( ctx, "schema" );
			if ( vec.size() > 0 ) {
				for( Value v : vec ) {
					schemaParser.parse( new File( v.strValue() ) );
				}
			}
			parseWSDLTypes( ctx, schemaParser );
			schemaSet = schemaParser.getResult();
			String nsPrefix = "jolie";
			int i = 1;
			for( XSSchema schema : schemaSet.getSchemas() ) {
				if ( !schema.getTargetNamespace().equals( XMLConstants.W3C_XML_SCHEMA_NS_URI ) ) {
					namespacePrefixMap.put( schema.getTargetNamespace(), nsPrefix + i++ );
				}
			}
		}

		return schemaSet;
	}

	private boolean convertAttributes( StatefulContext ctx )
	{
		boolean ret = false;
		if ( hasParameter( ctx, "convertAttributes" ) ) {
			ret = checkBooleanParameter( ctx, "convertAttributes" );
		}
		return ret;
	}

	private void initNamespacePrefixes( SOAPElement element )
		throws SOAPException
	{
		for( Entry<String, String> entry : namespacePrefixMap.entrySet() ) {
			element.addNamespaceDeclaration( entry.getValue(), entry.getKey() );
		}
	}

	private void valueToSOAPElement(
		StatefulContext ctx, 
		Value value,
		SOAPElement element,
		SOAPEnvelope soapEnvelope )
		throws SOAPException
	{
		String type = "any";
		if ( value.isDefined() ) {
			if ( value.isInt() ) {
				type = "int";
			} else if ( value.isLong() ) {
				type = "long";
			} else if ( value.isString() ) {
				type = "string";
			} else if ( value.isDouble() ) {
				type = "double";
			} else if ( value.isBool() ) {
				type = "boolean";
			}
			element.addAttribute( soapEnvelope.createName( "type", "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI ), "xsd:" + type );
			element.addTextNode( value.strValue() );
		}

		if ( convertAttributes( ctx ) ) {
			Map<String, ValueVector> attrs = getAttributesOrNull( value );
			if ( attrs != null ) {
				for( Entry<String, ValueVector> attrEntry : attrs.entrySet() ) {
					element.addAttribute(
						soapEnvelope.createName( attrEntry.getKey() ),
						attrEntry.getValue().first().strValue() );
				}
			}
		}

		for( Entry<String, ValueVector> entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					valueToSOAPElement(
						ctx,
						val,
						element.addChildElement( entry.getKey() ),
						soapEnvelope );
				}
			}
		}
	}

	private static Map<String, ValueVector> getAttributesOrNull( Value value )
	{
		Map<String, ValueVector> ret = null;
		ValueVector vec = value.children().get( Constants.Predefined.ATTRIBUTES.token().content() );
		if ( vec != null && vec.size() > 0 ) {
			ret = vec.first().children();
		}

		if ( ret == null ) {
			ret = new HashMap<String, ValueVector>();
		}

		return ret;
	}

	private static Value getAttributeOrNull( Value value, String attrName )
	{
		Value ret = null;
		Map<String, ValueVector> attrs = getAttributesOrNull( value );
		if ( attrs != null ) {
			ValueVector vec = attrs.get( attrName );
			if ( vec != null && vec.size() > 0 ) {
				ret = vec.first();
			}
		}

		return ret;
	}

	private static Value getAttribute( Value value, String attrName )
	{
		return value.getChildren( Constants.Predefined.ATTRIBUTES.token().content() ).first().getChildren( attrName ).first();
	}

	private String getPrefixOrNull( XSAttributeDecl decl )
	{
		if ( decl.getOwnerSchema().attributeFormDefault() ) {
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		}
		return null;
	}

	private String getPrefixOrNull( XSElementDecl decl )
	{
		if ( decl.getOwnerSchema().elementFormDefault() ) {
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		}
		return null;
	}

	private String getPrefix( XSElementDecl decl )
	{
		return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
	}

	private String getPrefix( XSComplexType compType )
	{
		return namespacePrefixMap.get( compType.getOwnerSchema().getTargetNamespace() );
	}

	private void termProcessing( Value value, SOAPElement element, SOAPEnvelope envelope, boolean first,
		XSTerm currTerm, int getMaxOccur,
		XSSchemaSet sSet, String messageNamespace )
		throws SOAPException
	{

		if ( currTerm.isElementDecl() ) {
			ValueVector vec;
			XSElementDecl currElementDecl = currTerm.asElementDecl();
			String name = currElementDecl.getName();
			String prefix = (first) ? getPrefix( currElementDecl ) : getPrefixOrNull( currElementDecl );
			SOAPElement childElement;
			if ( (vec = value.children().get( name )) != null ) {
				int k = 0;
				while( vec.size() > 0 && (getMaxOccur > k || getMaxOccur == XSParticle.UNBOUNDED) ) {
					if ( prefix == null ) {
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
		throws SOAPException
	{

		XSParticle[] children = modelGroup.getChildren();
		XSTerm currTerm;
		for( XSParticle child : children ) {
			currTerm = child.getTerm();
			if ( currTerm.isModelGroup() ) {
				groupProcessing( value, xsDecl, element, envelope, first, currTerm.asModelGroup(), sSet, messageNamespace );
			} else {
				termProcessing( value, element, envelope, first, currTerm, child.getMaxOccurs(), sSet, messageNamespace );
			}
		}
	}

	private void valueToTypedSOAP(
		Value value,
		XSElementDecl xsDecl,
		SOAPElement element,
		SOAPEnvelope envelope,
		boolean first,// Ugly fix! This should be removed as soon as another option arises.
		XSSchemaSet sSet,
		String messageNamespace )
		throws SOAPException
	{

		XSType currType = xsDecl.getType();

		if ( currType.isSimpleType() ) {
			element.addTextNode( value.strValue() );

		} else if ( currType.isComplexType() ) {
			XSType type = currType;
			if ( currType.asComplexType().isAbstract() ) {
				// if the complex type is abstract search for the inherited type defined into the jolie value
				// under the node __soap_inherited_type
				if ( value.hasChildren( Parameters.INHERITED_TYPE ) ) {
					String inheritedType = value.getFirstChild( Parameters.INHERITED_TYPE ).strValue();
					XSComplexType xsInheritedType = sSet.getComplexType( messageNamespace, inheritedType );
					if ( xsInheritedType == null ) {
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

			//end new stuff
			// Iterate over attributes
			Collection<? extends XSAttributeUse> attributeUses = complexT.getAttributeUses();
			for( XSAttributeUse attrUse : attributeUses ) {
				name = attrUse.getDecl().getName();
				if ( (currValue = getAttributeOrNull( value, name )) != null ) {
					QName attrName = envelope.createQName( name, getPrefixOrNull( attrUse.getDecl() ) );
					element.addAttribute( attrName, currValue.strValue() );
				}
			}

			// processing content (no base type parent )
			contentT = complexT.getContentType();
			if ( contentT.asSimpleType() != null ) {
				element.addTextNode( value.strValue() );
			} else if ( (particle = contentT.asParticle()) != null ) {
				XSTerm term = particle.getTerm();
				XSModelGroupDecl modelGroupDecl;
				XSModelGroup modelGroup = null;
				if ( (modelGroupDecl = term.asModelGroupDecl()) != null ) {
					modelGroup = modelGroupDecl.getModelGroup();
				} else if ( term.isModelGroup() ) {
					modelGroup = term.asModelGroup();
				}

				if ( modelGroup != null ) {
					XSModelGroup.Compositor compositor = modelGroup.getCompositor();
					if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
						groupProcessing( value, xsDecl, element, envelope, first, modelGroup, sSet, messageNamespace );
					}
				}
			}
		}
	}

	private Definition getWSDLDefinition( StatefulContext ctx )
		throws IOException
	{
		if ( wsdlDefinition == null && hasParameter( ctx, "wsdl" ) ) {
			String wsdlUrl = getStringParameter( ctx, "wsdl" );
			try {
				wsdlDefinition = WSDLCache.getInstance().get( wsdlUrl );
			} catch( WSDLException e ) {
				throw new IOException( e );
			}
		}
		return wsdlDefinition;
	}

	private String getSoapActionForOperation( StatefulContext ctx, String operationName )
		throws IOException
	{
		String soapAction = null;
		Port port = getWSDLPort( ctx );
		if ( port != null ) {
			BindingOperation bindingOperation = port.getBinding().getBindingOperation( operationName, null, null );
			for( ExtensibilityElement element : (List<ExtensibilityElement>) bindingOperation.getExtensibilityElements() ) {
				if ( element instanceof SOAPOperation ) {
					soapAction = ((SOAPOperation) element).getSoapActionURI();
				}
			}
		}
		if ( soapAction == null ) {
			soapAction = getStringParameter( ctx, "namespace" ) + "/" + operationName;
		}
		return soapAction;
	}

	private Port getWSDLPort( StatefulContext ctx )
		throws IOException
	{
		Port port = wsdlPort;
		if ( port == null && hasParameter( ctx, "wsdl" ) && getParameterFirstValue( ctx, "wsdl" ).hasChildren( "port" ) ) {
			String portName = getParameterFirstValue( ctx, "wsdl" ).getFirstChild( "port" ).strValue();
			Definition definition = getWSDLDefinition( ctx );
			if ( definition != null ) {
				Map<QName, Service> services = definition.getServices();
				Iterator<Entry<QName, Service>> it = services.entrySet().iterator();
				while( port == null && it.hasNext() ) {
					port = it.next().getValue().getPort( portName );
				}
			}
			if ( port != null ) {
				wsdlPort = port;
			}
		}
		return port;
	}

	private String getOutputMessageRootElementName( StatefulContext ctx, String operationName )
		throws IOException
	{
		String elementName = operationName + ((received) ? "Response" : "");
		Port port = getWSDLPort( ctx );
		if ( port != null ) {
			try {
				Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
				Part part;
				if ( received ) {
					// We are sending a response
					part = ((Entry<String, Part>) operation.getOutput().getMessage().getParts().entrySet().iterator().next()).getValue();
				} else {
					// We are sending a request
					part = ((Entry<String, Part>) operation.getInput().getMessage().getParts().entrySet().iterator().next()).getValue();
				}
				elementName = part.getElementName().getLocalPart();
			} catch( Exception e ) {
			}
		}
		return elementName;
	}

	private String getOutputMessageNamespace( StatefulContext ctx, String operationName )
		throws IOException
	{
		String messageNamespace = "";
		Port port = getWSDLPort( ctx );
		if ( port == null ) {
			if ( hasParameter( ctx, "namespace" ) ) {
				messageNamespace = getStringParameter( ctx, "namespace" );
			}
		} else {
			Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
			if ( operation != null ) {
				Map<String, Part> parts = operation.getOutput().getMessage().getParts();
				if ( parts.size() > 0 ) {
					Part part = parts.entrySet().iterator().next().getValue();
					if ( part.getElementName() == null ) {
						messageNamespace = operation.getOutput().getMessage().getQName().getNamespaceURI();
					} else {
						messageNamespace = part.getElementName().getNamespaceURI();
					}
				}
			}
		}
		return messageNamespace;
	}

	private String[] getParameterOrder( StatefulContext ctx, String operationName )
		throws IOException
	{
		List<String> parameters = null;
		Port port = getWSDLPort( ctx );
		if ( port != null ) {
			Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
			if ( operation != null ) {
				parameters = operation.getParameterOrdering();
			}
		}
		return (parameters == null) ? null : parameters.toArray( new String[ 0 ] );
	}

	private void setOutputEncodingStyle( StatefulContext ctx, SOAPEnvelope soapEnvelope, String operationName )
		throws IOException, SOAPException
	{
		Port port = getWSDLPort( ctx );
		if ( port != null ) {
			BindingOperation bindingOperation = port.getBinding().getBindingOperation( operationName, null, null );
			if ( bindingOperation == null ) {
				return;
			}
			BindingOutput output = bindingOperation.getBindingOutput();
			if ( output == null ) {
				return;
			}
			for( ExtensibilityElement element : (List<ExtensibilityElement>) output.getExtensibilityElements() ) {
				if ( element instanceof javax.wsdl.extensions.soap.SOAPBody ) {
					List<String> list = ((javax.wsdl.extensions.soap.SOAPBody) element).getEncodingStyles();
					if ( list != null && list.isEmpty() == false ) {
						soapEnvelope.setEncodingStyle( list.get( 0 ) );
						soapEnvelope.addNamespaceDeclaration( "enc", list.get( 0 ) );
					}
				}
			}
		}
	}

	public FullHttpMessage buildSoapMessage( StatefulMessage msg )
		throws IOException
	{
		CommMessage message = msg.message();
		StatefulContext ctx = msg.context();
		try {
			inputId = message.operationName();
			String messageNamespace = getOutputMessageNamespace( ctx, message.operationName() );

			if ( received ) {
				// We're responding to a request
				inputId += "Response";
			}

			SOAPMessage soapMessage = messageFactory.createMessage();
			soapMessage.setProperty( SOAPMessage.WRITE_XML_DECLARATION, "true" );
			soapMessage.setProperty( SOAPMessage.CHARACTER_SET_ENCODING, "utf-8" );
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			setOutputEncodingStyle( ctx, soapEnvelope, message.operationName() );
			SOAPBody soapBody = soapEnvelope.getBody();

			if ( checkBooleanParameter( ctx, "wsAddressing" ) ) {
				SOAPHeader soapHeader = soapEnvelope.getHeader();
				// WS-Addressing namespace
				soapHeader.addNamespaceDeclaration( "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				// Message ID
				Name messageIdName = soapEnvelope.createName( "MessageID", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement messageIdElement = soapHeader.addHeaderElement( messageIdName );
				if ( received ) {
					// TODO: remove this after we implement a mechanism for being sure message.id() is the one received before.
					messageIdElement.setValue( "uuid:1" );
				} else {
					messageIdElement.setValue( "uuid:" + message.id() );
				}
				// Action element
				Name actionName = soapEnvelope.createName( "Action", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement actionElement = soapHeader.addHeaderElement( actionName );
				/*
                 * TODO: the action element could be specified within the
                 * parameter. Perhaps wsAddressing.action ? We could also allow
                 * for giving a prefix or a suffix to the operation name, like
                 * wsAddressing.action.prefix, wsAddressing.action.suffix
				 */
				actionElement.setValue( message.operationName() );
				// From element
				Name fromName = soapEnvelope.createName( "From", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement fromElement = soapHeader.addHeaderElement( fromName );
				Name addressName = soapEnvelope.createName( "Address", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPElement addressElement = fromElement.addChildElement( addressName );
				addressElement.setValue( "http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous" );
				// To element
				/*
                 * if ( operation == null ) { // we are sending a Notification
                 * or a Solicit Name toName = soapEnvelope.createName("To",
                 * "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing");
                 * SOAPHeaderElement
                 * toElement=soapHeader.addHeaderElement(toName);
                 * toElement.setValue(getURI().getHost()); }
				 */
			}

			if ( message.isFault() ) {
				FaultException f = message.fault();
				SOAPFault soapFault = soapBody.addFault();
				soapFault.setFaultCode( soapEnvelope.createQName( "Server", soapEnvelope.getPrefix() ) );
				soapFault.setFaultString( f.getMessage() );
				Detail detail = soapFault.addDetail();
				DetailEntry de = detail.addDetailEntry( soapEnvelope.createName( f.faultName(), null, messageNamespace ) );
				valueToSOAPElement( ctx, f.value(), de, soapEnvelope );
			} else {
				XSSchemaSet sSet = getSchemaSet( ctx );
				XSElementDecl elementDecl;
				String messageRootElementName = getOutputMessageRootElementName( ctx, message.operationName() );
				if ( sSet == null
					|| (elementDecl = sSet.getElementDecl( messageNamespace, messageRootElementName )) == null ) {
					Name operationName;
					soapEnvelope.addNamespaceDeclaration( "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI );
					soapEnvelope.addNamespaceDeclaration( "xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI );
					if ( messageNamespace.isEmpty() ) {
						operationName = soapEnvelope.createName( messageRootElementName );
					} else {
						soapEnvelope.addNamespaceDeclaration( "jolieMessage", messageNamespace );
						operationName = soapEnvelope.createName( messageRootElementName, "jolieMessage", messageNamespace );
					}

					SOAPBodyElement opBody = soapBody.addBodyElement( operationName );
					String[] parameters = getParameterOrder( ctx, message.operationName() );
					if ( parameters == null ) {
						valueToSOAPElement( ctx, message.value(), opBody, soapEnvelope );
					} else {
						for( String parameterName : parameters ) {
							valueToSOAPElement( ctx, message.value().getFirstChild( parameterName ), opBody.addChildElement( parameterName ), soapEnvelope );
						}
					}
				} else {
					initNamespacePrefixes( soapEnvelope );

					if ( hasParameter( ctx, Parameters.ADD_ATTRIBUTE ) ) {
						Value add_parameter = getParameterFirstValue( ctx, Parameters.ADD_ATTRIBUTE );
						if ( add_parameter.hasChildren( Parameters.ENVELOPE ) ) {
							// attributes must be added to the envelope
							ValueVector attributes = add_parameter.getFirstChild( Parameters.ENVELOPE ).getChildren( "attribute" );
							for( Value att : attributes ) {
								soapEnvelope.addNamespaceDeclaration( att.getFirstChild( "name" ).strValue(), att.getFirstChild( "value" ).strValue() );
							}
						}
					}
					boolean wrapped = true;
					Value vStyle = getParameterVector( ctx, Parameters.STYLE ).first();
					if ( "document".equals( vStyle.strValue() ) ) {
						wrapped = vStyle.getFirstChild( Parameters.WRAPPED ).boolValue();
					}
					SOAPElement opBody = soapBody;
					if ( wrapped ) {
						opBody = soapBody.addBodyElement(
							soapEnvelope.createName( messageRootElementName, namespacePrefixMap.get( elementDecl.getOwnerSchema().getTargetNamespace() ), null ) );
						// adding forced attributes to operation 
						if ( hasParameter( ctx, Parameters.ADD_ATTRIBUTE ) ) {
							Value add_parameter = getParameterFirstValue( ctx, Parameters.ADD_ATTRIBUTE );
							if ( add_parameter.hasChildren( Parameters.OPERATION ) ) {
								ValueVector operations = add_parameter.getChildren( Parameters.OPERATION );
								for( Value op : operations ) {
									if ( op.getFirstChild( "operation_name" ).strValue().equals( message.operationName() ) ) {
										// attributes must be added to the envelope
										Value attribute = op.getFirstChild( "attribute" );
										QName attrName;
										if ( attribute.hasChildren( "prefix" ) ) {
											attrName = opBody.createQName( attribute.getFirstChild( "name" ).strValue(), attribute.getFirstChild( "prefix" ).strValue() );
										} else {
											attrName = opBody.createQName( attribute.getFirstChild( "name" ).strValue(), null );
										}
										opBody.addAttribute( attrName, attribute.getFirstChild( "value" ).strValue() );
									}
								}

							}
						}
					}
					valueToTypedSOAP( message.value(), elementDecl, opBody, soapEnvelope, !wrapped, sSet, messageNamespace );
				}
			}

			if ( soapEnvelope.getHeader().hasChildNodes() == false ) {
				// Some service implementations do not like empty headers
				soapEnvelope.getHeader().detachNode();
			}

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			ByteArray content = new ByteArray( tmpStream.toByteArray() );

			//StringBuilder httpMessage = new StringBuilder();
			FullHttpMessage httpMessage;
			
			String soapAction = null;

			if ( received ) {
				// We're responding to a request
				if ( message.isFault() ) {
					httpMessage = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
				} else {
					httpMessage = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				}
				httpMessage.headers().add( HttpHeaderNames.SERVER, "Jolie");
				received = false;
			} else {
				// We're sending a notification or a solicit
				String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
				if ( path == null || path.length() == 0 ) {
					path = "*";
				}
				httpMessage = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path);
				httpMessage.headers().add( HttpHeaderNames.HOST, uri.getHost() );
				/*
                 * soapAction = "SOAPAction: \"" + messageNamespace + "/" +
                 * message.operationName() + '\"' + HttpUtils.CRLF;
				 */
				soapAction = "\"" + getSoapActionForOperation( ctx, message.operationName() ) + '\"';

				if ( checkBooleanParameter( ctx, "compression", true ) ) {
					String requestCompression = getStringParameter( ctx, "requestCompression" );
					if ( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
						encoding = requestCompression;
						httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, encoding );
					} else {
						httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate" );
					}
				}
			}

			if ( getParameterVector( ctx, "keepAlive" ).first().intValue() != 1 ) {
				channel().setToBeClosed( true );
				httpMessage.headers().add( HttpHeaderNames.CONNECTION, "close" );
			}

			//if ( encoding != null && checkBooleanParameter( ctx, "compression", true ) ) {
			//	content = HttpUtils.encode( encoding, content, httpMessage );
			//}

			//httpMessage.append("Content-Type: application/soap+xml; charset=utf-8" + HttpUtils.CRLF);
			httpMessage.headers().add( HttpHeaderNames.CONTENT_TYPE, "text/xml; charset=utf-8" );
			httpMessage.headers().add( HttpHeaderNames.CONTENT_LENGTH, content.size() );
			
			if ( soapAction != null ) {
				httpMessage.headers().add( "SOAPAction", soapAction );
			}

			if ( getParameterVector( ctx, "debug" ).first().intValue() > 0 ) {
				interpreter.logInfo( "[SOAP debug] Sending:\n" + httpMessage.toString() + content.toString( "utf-8" ) );
			}

			inputId = message.operationName();
			
			httpMessage.content().writeBytes( content.getBytes() );
			return httpMessage;
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
	}

	private void xmlNodeToValue( StatefulContext ctx, Value value, Node node, boolean isRecRoot )
	{
		String type = "xsd:string";
		Node currNode;

		// Set attributes
		NamedNodeMap attributes = node.getAttributes();
		if ( attributes != null ) {
			for( int i = 0; i < attributes.getLength(); i++ ) {
				currNode = attributes.item( i );
				if ( "type".equals( currNode.getNodeName() ) == false && convertAttributes( ctx ) ) {
					getAttribute( value, currNode.getNodeName() ).setValue( currNode.getNodeValue() );
				} else {
					type = currNode.getNodeValue();
				}
			}
		}

		// Set children
		NodeList list = node.getChildNodes();
		Value childValue;
		for( int i = 0; i < list.getLength(); i++ ) {
			currNode = list.item( i );
			switch( currNode.getNodeType() ) {
				case Node.ELEMENT_NODE:
					childValue = value.getNewChild( currNode.getLocalName() );
					xmlNodeToValue( ctx, childValue, currNode, false );
					break;
				case Node.TEXT_NODE:
					if ( !isRecRoot ) {
						value.setValue( currNode.getNodeValue() );
					}
					break;
			}
		}

		if ( "xsd:int".equals( type ) ) {
			value.setValue( value.intValue() );
		} else if ( "xsd:long".equals( type ) ) {
			value.setValue( value.longValue() );
		} else if ( "xsd:double".equals( type ) ) {
			value.setValue( value.doubleValue() );
		} else if ( "xsd:boolean".equals( type ) ) {
			value.setValue( value.boolValue() );
		}
	}

	private static Element getFirstElement( Node node )
	{
		NodeList nodes = node.getChildNodes();
		for( int i = 0; i < nodes.getLength(); i++ ) {
			if ( nodes.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				return (Element) nodes.item( i );
			}
		}
		return null;
	}

	/*
     * private Schema getRecvMessageValidationSchema() throws IOException {
     * List< Source > sources = new ArrayList< Source >(); Definition definition
     * = getWSDLDefinition(); if ( definition != null ) { Types types =
     * definition.getTypes(); if ( types != null ) { List< ExtensibilityElement
     * > list = types.getExtensibilityElements(); for( ExtensibilityElement
     * element : list ) { if ( element instanceof SchemaImpl ) { sources.add(
     * new DOMSource( ((SchemaImpl)element).getElement() ) ); } } } }
     * SchemaFactory schemaFactory = SchemaFactory.newInstance(
     * XMLConstants.W3C_XML_SCHEMA_NS_URI ); try { return
     * schemaFactory.newSchema( sources.toArray( new Source[sources.size()] ) );
     * } catch( SAXException e ) { throw new IOException( e ); } }
	 */
	public StatefulMessage recv_internal( FullHttpMessage message )
		throws IOException
	{
		String charset = HttpUtils.getCharset( null, message );
		String str = message.content().toString( Charset.forName( charset ) );
		
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if ( inInputPort && ((FullHttpRequest)message).method() != HttpMethod.POST ) {
			throw new UnsupportedMethodException( "Only HTTP method POST allowed!", Method.POST );
		}

		encoding = message.headers().get( "accept-encoding" );

		CommMessage retVal = null;
		String messageId = message.headers().get( "soapaction", "" );
		FaultException fault = null;
		Value value = Value.create();

		StatefulContext ctx = channel().getContextFor( CommMessage.GENERIC_ID );
		
		try {
			if ( message.content().readableBytes() > 0 ) {
				if ( checkBooleanParameter( ctx, "debug" ) ) {
					interpreter.logInfo( "[SOAP debug] Receiving:\n" + message.content().toString( Charset.forName( charset ) ) );
				}

				SOAPMessage soapMessage = messageFactory.createMessage();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				/*
                 * Schema messageSchema = getRecvMessageValidationSchema(); if (
                 * messageSchema != null ) {
                 * factory.setIgnoringElementContentWhitespace( true );
                 * factory.setSchema( messageSchema ); }
				 */
				factory.setNamespaceAware( true );
				DocumentBuilder builder = factory.newDocumentBuilder();
				byte[] content = new byte[message.content().readableBytes()];
				message.content().readBytes( content, 0, content.length);
				InputSource src = new InputSource( new ByteArrayInputStream( content ) );
				src.setEncoding( charset );
				Document doc = builder.parse( src );
				DOMSource dom = new DOMSource( doc );
				soapMessage.getSOAPPart().setContent( dom );

				/*
                 * if ( checkBooleanParameter( "debugAfter" ) ) {
                 * ByteArrayOutputStream tmpStream = new
                 * ByteArrayOutputStream(); soapMessage.writeTo( tmpStream );
                 * interpreter.logInfo( "[SOAP debug] Receiving:\n" +
                 * tmpStream.toString() ); }
				 */
				SOAPFault soapFault = soapMessage.getSOAPBody().getFault();
				if ( soapFault == null ) {
					Element soapValueElement = getFirstElement( soapMessage.getSOAPBody() );
					messageId = soapValueElement.getLocalName();
					// explanation: https://github.com/jolie/jolie/issues/5
					xmlNodeToValue( ctx, value, soapValueElement, checkBooleanParameter( ctx, "dropRootValue", false ) );

					ValueVector schemaPaths = getParameterVector( ctx, "schema" );
					if ( schemaPaths.size() > 0 ) {
						List<Source> sources = new LinkedList<Source>();
						Value schemaPath;
						for( int i = 0; i < schemaPaths.size(); i++ ) {
							schemaPath = schemaPaths.get( i );
							if ( schemaPath.getChildren( "validate" ).first().intValue() > 0 ) {
								sources.add( new StreamSource( new File( schemaPaths.get( i ).strValue() ) ) );
							}
						}

						if ( !sources.isEmpty() ) {
							Schema schema
								= SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ).newSchema( sources.toArray( new Source[ 0 ] ) );
							schema.newValidator().validate( new DOMSource( soapMessage.getSOAPBody().getFirstChild() ) );
						}
					}
				} else {
					String faultName = "UnknownFault";
					Value faultValue = Value.create();
					Detail d = soapFault.getDetail();
					if ( d != null ) {
						Node n = d.getFirstChild();
						if ( n != null ) {
							faultName = n.getLocalName();
							xmlNodeToValue( ctx, faultValue, n, true );
						} else {
							faultValue.setValue( soapFault.getFaultString() );
						}
					}
					fault = new FaultException( faultName, faultValue );
				}
			}

			String resourcePath = recv_getResourcePath( ctx, message );
			if ( message instanceof FullHttpResponse ) {
				if ( fault != null && ((FullHttpResponse)message).status() == HttpResponseStatus.INTERNAL_SERVER_ERROR ) {
					fault = new FaultException( "InternalServerError", "" );
				}
				retVal = new CommMessage( CommMessage.GENERIC_ID, inputId, resourcePath, value, fault );
			} else /* if ( !message.isError() )*/ { // TODO It appears that a message of type ERROR cannot be returned from the old parser.
				if ( messageId.isEmpty() ) {
					throw new IOException( "Received SOAP Message without a specified operation" );
				}
				retVal = new CommMessage( CommMessage.GENERIC_ID, messageId, resourcePath, value, fault );
			}
		} catch( SOAPException e ) {
			throw new IOException( e );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			//TODO support resourcePath
			retVal = new CommMessage( CommMessage.GENERIC_ID, messageId, "/", value, new FaultException( "TypeMismatch", e ) );
		}

		received = true;

		if ( "/".equals( retVal.resourcePath() ) && channel().parentPort() != null
			&& channel().parentPort().getInterface().containsOperation( retVal.operationName() ) ) {
			try {
				// The message is for this service
				Interface iface = channel().parentPort().getInterface();
				OneWayTypeDescription oneWayTypeDescription = iface.oneWayOperations().get( retVal.operationName() );
				if ( oneWayTypeDescription != null ) {
					// We are receiving a One-Way message
					if ( !(message instanceof FullHttpResponse) ) {
						oneWayTypeDescription.requestType().cast( retVal.value() );
					}
				} else {
					RequestResponseTypeDescription rrTypeDescription = iface.requestResponseOperations().get( retVal.operationName() );
					if ( retVal.isFault() ) {
						Type faultType = rrTypeDescription.faults().get( retVal.fault().faultName() );
						if ( faultType != null ) {
							faultType.cast( retVal.value() );
						}
					} else if ( message instanceof FullHttpResponse ) {
						rrTypeDescription.responseType().cast( retVal.value() );
					} else {
						rrTypeDescription.requestType().cast( retVal.value() );
					}
				}
			} catch( TypeCastingException e ) {
				// TODO: do something here?
			}
		}

		return new StatefulMessage( retVal, ctx );
	}

	private String recv_getResourcePath(StatefulContext ctx, FullHttpMessage message )
	{
		String ret = "/";
		if ( checkBooleanParameter(ctx, "interpretResource" ) ) {
			if (message instanceof FullHttpRequest)
				return ((FullHttpRequest)message).uri();
			return null;
		}
		return ret;
	}
}
