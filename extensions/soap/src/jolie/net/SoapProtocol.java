/***************************************************************************
 *   Copyright (C) 2006-2010 by Fabrizio Montesi <famontesi@gmail.com>     *
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


package jolie.net;

import com.ibm.wsdl.extensions.schema.SchemaImpl;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
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

import jolie.lang.Constants;
import jolie.Interpreter;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.net.soap.WSDLCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/** Implements the SOAP over HTTP protocol.
 * 
 * @author Fabrizio Montesi
 * 
 * 2006 - Fabrizio Montesi, Mauro Silvagni: first write.
 * 2007 - Fabrizio Montesi: rewritten from scratch, exploiting new JOLIE capabilities.
 * 2008 - Fabrizio Montesi: initial support for schemas.
 * 2008 - Claudio Guidi: initial support for WS-Addressing.
 * 2010 - Fabrizio Montesi: initial support for WSDL documents.
 * 
 */
public class SoapProtocol extends SequentialCommProtocol
{
	private String inputId = null;
	private final Interpreter interpreter;
	private final MessageFactory messageFactory;
	private XSSchemaSet schemaSet = null;
	private URI uri = null;
	private Definition wsdlDefinition = null;
	private Port wsdlPort = null;
	private final TransformerFactory transformerFactory;
	private Map< String, String > namespacePrefixMap = new HashMap< String, String > ();
	
	private boolean received = false;
	
	private final static String CRLF = new String( new char[] { 13, 10 } );

	public String name()
	{
		return "soap";
	}

	public SoapProtocol( VariablePath configurationPath, URI uri, Interpreter interpreter )
		throws SOAPException
	{
		super( configurationPath );
		this.uri = uri;
		this.transformerFactory = TransformerFactory.newInstance();
		this.interpreter = interpreter;
		this.messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_1_PROTOCOL );
	}

	private void parseSchemaElement( Definition definition, Element element, XSOMParser schemaParser )
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

	private void parseWSDLTypes( XSOMParser schemaParser )
		throws IOException
	{
		Definition definition = getWSDLDefinition();
		if ( definition != null ) {
			Types types = definition.getTypes();
			if ( types != null ) {
				List< ExtensibilityElement > list = types.getExtensibilityElements();
				for( ExtensibilityElement element : list ) {
					if ( element instanceof SchemaImpl ) {
						Element schemaElement = ((SchemaImpl)element).getElement();
						Map< String, String > namespaces = definition.getNamespaces();
						for( Entry< String, String > entry : namespaces.entrySet() ) {
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

	private XSSchemaSet getSchemaSet()
		throws IOException, SAXException
	{
		if ( schemaSet == null ) {
			XSOMParser schemaParser = new XSOMParser();
			ValueVector vec = getParameterVector( "schema" );
			if ( vec.size() > 0 ) {
				for( Value v : vec ) {
					schemaParser.parse( new File( v.strValue() ) );
				}				
			}
			parseWSDLTypes( schemaParser );
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

	private boolean convertAttributes()
	{
		boolean ret = false;
		if ( hasParameter( "convertAttributes" ) ) {
			ret = checkBooleanParameter( "convertAttributes" );
		}
		return ret;
	}
	
	private void initNamespacePrefixes( SOAPElement element )
		throws SOAPException
	{
		for( Entry< String, String > entry : namespacePrefixMap.entrySet() ) {
			element.addNamespaceDeclaration( entry.getValue(), entry.getKey() );
		}
	}
	
	private void valueToSOAPElement(
			Value value,
			SOAPElement element,
			SOAPEnvelope soapEnvelope
			)
		throws SOAPException
	{
		String type = "any";
		if ( value.isDefined() ) {
			if ( value.isInt() ) {
				type = "int";
			} else if ( value.isString() ) {
				type = "string";
			} else if ( value.isDouble() ) {
				type = "double";
			}
			element.addAttribute( soapEnvelope.createName( "type" ), "xsd:" + type );
			element.addTextNode( value.strValue() );
		}

		if ( convertAttributes() ) {
			Map< String, ValueVector > attrs = getAttributesOrNull( value );
			if ( attrs != null ) {
				for( Entry< String, ValueVector > attrEntry : attrs.entrySet() ) {
					element.addAttribute(
						soapEnvelope.createName( attrEntry.getKey() ),
						attrEntry.getValue().first().strValue()
					);
				}
			}
		}

		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					valueToSOAPElement(
								val,
								element.addChildElement( entry.getKey() ),
								soapEnvelope
							);
				}
			}
		}
	}
	
	private static Map< String, ValueVector > getAttributesOrNull( Value value )
	{
		Map< String, ValueVector > ret = null;
		ValueVector vec = value.children().get( Constants.Predefined.ATTRIBUTES.token().content() );
		if ( vec != null && vec.size() > 0 )
			ret = vec.first().children();
		
		if ( ret == null )
			ret = new HashMap< String, ValueVector >();
		
		return ret;
	}
	
	private static Value getAttributeOrNull( Value value, String attrName )
	{
		Value ret = null;
		Map< String, ValueVector > attrs = getAttributesOrNull( value );
		if ( attrs != null ) {
			ValueVector vec = attrs.get( attrName );
			if ( vec != null && vec.size() > 0 )
				ret = vec.first();
		}
		
		return ret;
	}
	
	private static Value getAttribute( Value value, String attrName )
	{
		return value.getChildren( Constants.Predefined.ATTRIBUTES.token().content() ).first()
					.getChildren( attrName ).first();
	}
	
	private String getPrefixOrNull( XSAttributeDecl decl )
	{
		if ( decl.getOwnerSchema().attributeFormDefault() )
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		return null;
	}
	
	private String getPrefixOrNull( XSElementDecl decl )
	{
		if ( decl.getOwnerSchema().elementFormDefault() )
			return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
		return null;
	}
	
	private String getPrefix( XSElementDecl decl )
	{
		return namespacePrefixMap.get( decl.getOwnerSchema().getTargetNamespace() );
	}
	
	private void valueToTypedSOAP(
			Value value,
			XSElementDecl xsDecl,
			SOAPElement element,
			SOAPEnvelope envelope,
			boolean first // Ugly fix! This should be removed as soon as another option arises.
			)
		throws SOAPException
	{
		XSType type = xsDecl.getType();
		
		if ( type.isSimpleType() ) {
			element.addTextNode( value.strValue() );
		} else if ( type.isComplexType() ) {
			String name;
			Value currValue;
			XSComplexType complexT = type.asComplexType();

			// Iterate over attributes
			Collection< ? extends XSAttributeUse > attributeUses = complexT.getAttributeUses();
			for( XSAttributeUse attrUse : attributeUses ) {
				name = attrUse.getDecl().getName();
				if ( (currValue=getAttributeOrNull( value, name )) != null ) {
					QName attrName = envelope.createQName( name, getPrefixOrNull( attrUse.getDecl() ) );
					element.addAttribute( attrName, currValue.strValue() );
				}
			}
			
			
			XSParticle particle;
			XSContentType contentT;
			contentT = complexT.getContentType();
			if ( contentT.asSimpleType() != null ) {
				element.addTextNode( value.strValue() );
			} else if ( (particle=contentT.asParticle()) != null ) {
				XSTerm term = particle.getTerm();
//				XSElementDecl elementDecl;
				XSModelGroupDecl modelGroupDecl;
				XSModelGroup modelGroup = null;
				//int size = value.children().size();
				//if ( particle.getMinOccurs()
				// It's a simple element, repeated some times
				/*if ( (elementDecl=term.asElementDecl()) != null ) {

				} else */if ( (modelGroupDecl=term.asModelGroupDecl()) != null ) {
					modelGroup = modelGroupDecl.getModelGroup();
				} else if ( term.isModelGroup() )
					modelGroup = term.asModelGroup();
				
				if ( modelGroup != null ) {
					XSModelGroup.Compositor compositor = modelGroup.getCompositor();
					if ( compositor.equals( XSModelGroup.SEQUENCE ) ) {
						XSParticle[] children = modelGroup.getChildren();
						XSTerm currTerm;
						XSElementDecl currElementDecl;
						Value v;
						ValueVector vec;
						String prefix;
						for( int i = 0; i < children.length; i++ ) {
							currTerm = children[i].getTerm();
							if ( currTerm.isElementDecl() ) {
								currElementDecl = currTerm.asElementDecl();
								name = currElementDecl.getName();
								prefix = ( first ) ? getPrefix( currElementDecl ) : getPrefixOrNull( currElementDecl );
								SOAPElement childElement = null;
								if ( (vec=value.children().get( name )) != null ) {
									int k = 0;
									while( vec.size() > 0 && (children[i].getMaxOccurs() > k || children[i].getMaxOccurs() == XSParticle.UNBOUNDED ) ) {
										if ( prefix == null ) {
											childElement = element.addChildElement( name );
										} else {
											childElement = element.addChildElement( name, prefix );
										}
										v = vec.remove( 0 );
										valueToTypedSOAP(
											v,
											currElementDecl,
											childElement,
											envelope,
											false );
										k++;
									}
								} else if ( children[i].getMinOccurs() > 0 ) {
									// TODO improve this error message.
									throw new SOAPException( "Invalid variable structure: expected " + name );
								}
							}
						}
					}
				}
			}
		}
	}

	private Definition getWSDLDefinition()
		throws IOException
	{
		if ( wsdlDefinition == null && hasParameter( "wsdl" ) ) {
			String wsdlUrl = getStringParameter( "wsdl" );
			try {
				wsdlDefinition = WSDLCache.getInstance().get( wsdlUrl );
			} catch( WSDLException e ) {
				throw new IOException( e );
			}
		}
		return wsdlDefinition;
	}

	private String getSoapActionForOperation( String operationName )
		throws IOException
	{
		String soapAction = null;
		Port port = getWSDLPort();
		if ( port != null ) {
			BindingOperation bindingOperation = port.getBinding().getBindingOperation( operationName, null, null );
			for( ExtensibilityElement element : (List< ExtensibilityElement >)bindingOperation.getExtensibilityElements() ) {
				if ( element instanceof SOAPOperation ) {
					soapAction = ((SOAPOperation)element).getSoapActionURI();
				}
			}
		}
		if ( soapAction == null ) {
			soapAction = getStringParameter( "namespace" ) + "/" + operationName;
		}
		return soapAction;
	}

	private Port getWSDLPort()
		throws IOException
	{
		Port port = wsdlPort;
		if ( port == null && hasParameter( "wsdl" ) && getParameterFirstValue( "wsdl" ).hasChildren( "port" ) ) {
			String portName = getParameterFirstValue( "wsdl" ).getFirstChild( "port" ).strValue();
			Definition definition = getWSDLDefinition();
			if ( definition != null ) {
				Map< QName, Service > services = definition.getServices();
				Iterator< Entry< QName, Service > > it = services.entrySet().iterator();
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

	private String getOutputMessageRootElementName( String operationName )
		throws IOException
	{
		String elementName = operationName;
		Port port = getWSDLPort();
		if ( port != null ) {
			try {
				Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
				Part part = ((Entry<String,Part>) operation.getInput().getMessage().getParts().entrySet().iterator().next()).getValue();
				elementName = part.getElementName().getLocalPart();
				
			} catch( Exception e ) {}
		}
		return elementName;
	}

	private String getOutputMessageNamespace( String operationName )
		throws IOException
	{
		String messageNamespace = "";
		Port port = getWSDLPort();
		if ( port == null ) {
			if ( hasParameter( "namespace" ) ) {
				messageNamespace = getStringParameter( "namespace" );
			}
		} else {
			Operation operation = port.getBinding().getPortType().getOperation( operationName, null, null );
			if ( operation != null ) {
				Map< String, Part > parts = operation.getOutput().getMessage().getParts();
				if ( parts.size() == 1 ) {
					Part part = parts.entrySet().iterator().next().getValue();
					messageNamespace = part.getElementName().getNamespaceURI();
				}
			}
		}
		return messageNamespace;
	}
	
	
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{		
		try {
			inputId = message.operationName();
			String messageNamespace = getOutputMessageNamespace( message.operationName() );

			if ( received ) {
				// We're responding to a request
				inputId += "Response";
			}

			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();

			if ( checkBooleanParameter( "wsAddressing" ) ) {
				SOAPHeader soapHeader = soapEnvelope.getHeader();
				// WS-Addressing namespace
				soapHeader.addNamespaceDeclaration( "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				// Message ID
				Name messageIdName = soapEnvelope.createName( "MessageID", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement messageIdElement = soapHeader.addHeaderElement(messageIdName);
				if ( received ) {
					// TODO: remove this after we implement a mechanism for being sure message.id() is the one received before.
					messageIdElement.setValue( "uuid:1" );
				} else {
					messageIdElement.setValue( "uuid:" + message.id() );
				}
				// Action element
				Name actionName = soapEnvelope.createName( "Action", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement actionElement = soapHeader.addHeaderElement( actionName );
				/* TODO: the action element could be specified within the parameter.
				 * Perhaps wsAddressing.action ?
				 * We could also allow for giving a prefix or a suffix to the operation name,
				 * like wsAddressing.action.prefix, wsAddressing.action.suffix
				 */
				actionElement.setValue( message.operationName() );
				// From element
				Name fromName = soapEnvelope.createName( "From", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPHeaderElement fromElement = soapHeader.addHeaderElement( fromName );
				Name addressName = soapEnvelope.createName( "Address", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
				SOAPElement addressElement = fromElement.addChildElement( addressName );
				addressElement.setValue( "http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous" );
				// To element
				/*if ( operation == null ) {
					// we are sending a Notification or a Solicit
					Name toName = soapEnvelope.createName("To", "wsa", "http://schemas.xmlsoap.org/ws/2004/03/addressing");
					SOAPHeaderElement toElement=soapHeader.addHeaderElement(toName);
					toElement.setValue(getURI().getHost());
				}*/
			}

			if ( message.isFault() ) {
				FaultException f = message.fault();
				SOAPFault soapFault = soapBody.addFault();
				soapFault.setFaultCode( soapEnvelope.createQName( "Server", soapEnvelope.getPrefix() ) );
				soapFault.setFaultString( f.getMessage() );
				Detail detail = soapFault.addDetail();
				DetailEntry de = detail.addDetailEntry( soapEnvelope.createName( f.faultName(), null, messageNamespace ) );
				valueToSOAPElement( f.value(), de, soapEnvelope );
			} else {
				XSSchemaSet sSet = getSchemaSet();
				XSElementDecl elementDecl;
				String messageRootElementName = getOutputMessageRootElementName( message.operationName() );
				if ( sSet == null ||
						(elementDecl=sSet.getElementDecl( messageNamespace, messageRootElementName )) == null
					) {
					Name operationName = soapEnvelope.createName( messageRootElementName );
					SOAPBodyElement opBody = soapBody.addBodyElement( operationName );
					valueToSOAPElement( message.value(), opBody, soapEnvelope );
				} else {
					initNamespacePrefixes( soapEnvelope );					
					boolean wrapped = true;
					Value vStyle = getParameterVector( "style" ).first();
					if ( "document".equals( vStyle.strValue() ) ) {
						wrapped = ( vStyle.getChildren( "wrapped" ).first().intValue() > 0 );
					}
					SOAPElement opBody = soapBody;
					if ( wrapped ) {
						opBody = soapBody.addBodyElement(
							soapEnvelope.createName( messageRootElementName, namespacePrefixMap.get( elementDecl.getOwnerSchema().getTargetNamespace() ), null )
						);
					}
					valueToTypedSOAP( message.value(), elementDecl, opBody, soapEnvelope, !wrapped );
				}
			}
			
			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			
			String soapString = CRLF + "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
								new String( tmpStream.toByteArray() );

			String messageString = "";
			String soapAction = null;
			
			if ( received ) {
				// We're responding to a request
				messageString += "HTTP/1.1 200 OK" + CRLF;
				received = false;
			} else {
				// We're sending a notification or a solicit
				String path = uri.getPath(); // TODO: fix this to consider resourcePaths
				if ( path == null || path.length() == 0 ) {
					path = "*";
				}
				messageString += "POST " + path + " HTTP/1.1" + CRLF;
				messageString += "Host: " + uri.getHost() + CRLF;
				/*soapAction =
					"SOAPAction: \"" + messageNamespace + "/" + message.operationName() + '\"' + CRLF;*/
				soapAction = "SOAPAction: \"" + getSoapActionForOperation( message.operationName() ) + '\"' + CRLF;
			}
			
			if ( getParameterVector( "keepAlive" ).first().intValue() != 1 ) {
				channel().setToBeClosed( true );
				messageString += "Connection: close" + CRLF;
			}
			
			//messageString += "Content-Type: application/soap+xml; charset=\"utf-8\"\n";
			messageString += "Content-Type: text/xml; charset=\"utf-8\"" + CRLF;
			messageString += "Content-Length: " + soapString.length() + CRLF;
			if ( soapAction != null ) {
				messageString += soapAction;
			}
			messageString += soapString + CRLF;
			
			if ( getParameterVector( "debug" ).first().intValue() > 0 ) {
				interpreter.logInfo( "[SOAP debug] Sending:\n" + tmpStream.toString() );
			}

			inputId = message.operationName();
			
			Writer writer = new OutputStreamWriter( ostream );
			writer.write( messageString );
			writer.flush();
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
	}
	
	private void xmlNodeToValue( Value value, Node node )
	{
		String type = "xsd:string";
		Node currNode;

		// Set attributes
		NamedNodeMap attributes = node.getAttributes();
		if ( attributes != null ) {
			for( int i = 0; i < attributes.getLength(); i++ ) {
				currNode = attributes.item( i );
				if ( "type".equals( currNode.getNodeName() ) == false && convertAttributes() ) {
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
				xmlNodeToValue( childValue, currNode );
				break;
			case Node.TEXT_NODE:
				value.setValue( currNode.getNodeValue() );
				break;
			}
		}

		if ( "xsd:int".equals( type ) ) {
			value.setValue( value.intValue() );
		} else if ( "xsd:double".equals( type ) ) {
			value.setValue( value.doubleValue() );
		}
	}

	private static Element getFirstElement( Node node )
	{
		NodeList nodes = node.getChildNodes();
		for( int i = 0; i < nodes.getLength(); i++ ) {
			if ( nodes.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				return (Element)nodes.item( i );
			}
		}
		return null;
	}

	/*private Schema getRecvMessageValidationSchema()
		throws IOException
	{
		List< Source > sources = new ArrayList< Source >();
		Definition definition = getWSDLDefinition();
		if ( definition != null ) {
			Types types = definition.getTypes();
			if ( types != null ) {
				List< ExtensibilityElement > list = types.getExtensibilityElements();
				for( ExtensibilityElement element : list ) {
					if ( element instanceof SchemaImpl ) {
						sources.add( new DOMSource( ((SchemaImpl)element).getElement() ) );
					}
				}
			}
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
		try {
			return schemaFactory.newSchema( sources.toArray( new Source[sources.size()] ) );
		} catch( SAXException e ) {
			throw new IOException( e );
		}
	}*/

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		HttpParser parser = new HttpParser( istream );
		HttpMessage message = parser.parse();
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		CommMessage retVal = null;
		String messageId = message.getPropertyOrEmptyString( "soapaction" );
		FaultException fault = null;
		Value value = Value.create();
		
		try {
			if ( message.content() != null && message.content().length > 0 ) {
				SOAPMessage soapMessage = messageFactory.createMessage();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				/*Schema messageSchema = getRecvMessageValidationSchema();
				if ( messageSchema != null ) {
					factory.setIgnoringElementContentWhitespace( true );
					factory.setSchema( messageSchema );
				}*/
				factory.setNamespaceAware( true );
				DocumentBuilder builder = factory.newDocumentBuilder();
	
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				Document doc = builder.parse( src );
				DOMSource dom = new DOMSource( doc );
				soapMessage.getSOAPPart().setContent( dom );
				
				if ( getParameterVector( "debug" ).first().intValue() > 0 ) {
					ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
					soapMessage.writeTo( tmpStream );
					interpreter.logInfo( "[SOAP debug] Receiving:\n" + tmpStream.toString() );
				}
				
				SOAPFault soapFault = soapMessage.getSOAPBody().getFault(); 
				if ( soapFault == null ) {
					Element soapValueElement = getFirstElement( soapMessage.getSOAPBody() );
					messageId = soapValueElement.getLocalName();
					xmlNodeToValue( value, soapValueElement );
					
					ValueVector schemaPaths = getParameterVector( "schema" );
					if ( schemaPaths.size() > 0 ) {
						List< Source > sources = new LinkedList< Source >();
						Value schemaPath;
						for( int i = 0; i < schemaPaths.size(); i++ ) {
							schemaPath = schemaPaths.get( i );
							if ( schemaPath.getChildren( "validate" ).first().intValue() > 0 )
								sources.add( new StreamSource( new File( schemaPaths.get( i ).strValue() ) ) );
						}
						
						if ( !sources.isEmpty() ) {
							Schema schema =
								SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ) 
									.newSchema( sources.toArray( new Source[0] ) );
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
							xmlNodeToValue(
									faultValue, n
							);
						} else {
							faultValue.setValue( soapFault.getFaultString() );
						}
					}
					fault = new FaultException( faultName, faultValue );
				}
			}

			String resourcePath = recv_getResourcePath( message );
			if ( message.isResponse() ) { 
				if ( fault != null && message.httpCode() == 500 ) {
					fault = new FaultException( "InternalServerError", "" );
				}
				retVal = new CommMessage( CommMessage.GENERIC_ID, inputId, resourcePath, value, fault );
			} else if ( !message.isError() ) {
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
		
		return retVal;
	}

	private String recv_getResourcePath( HttpMessage message )
	{
		String ret = "/";
		if ( checkBooleanParameter( "interpretResource" ) ) {
			ret = message.requestPath();
		}
		return ret;
	}
}