/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *   Copyright (C) by Mauro Silvagni                                       *
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;

import jolie.net.http.HTTPMessage;
import jolie.net.http.HTTPParser;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Operation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*class HTTPSOAPException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	private int httpCode;

	public HTTPSOAPException( int httpCode, String message )
	{
		super( message );
		this.httpCode = httpCode;
	}

	public int httpCode()
	{
		return httpCode;
	}

	public String createSOAPFaultMessage()
		throws SOAPException, IOException
	{
		MessageFactory messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL );
		SOAPMessage soapMessage = messageFactory.createMessage();
		SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
		SOAPBody soapBody = soapEnvelope.getBody();
		soapBody.addFault( SOAPConstants.SOAP_SENDER_FAULT, getMessage() );
		ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
		soapMessage.writeTo( tmpStream );
		return new String( tmpStream.toByteArray() );
	}
}*/

/** Implements the SOAP over HTTP protocol.
 * 
 * @author Fabrizio Montesi
 * @todo Implement the message WSDL namespace information.
 * @todo Various checks should be made from the deploy parser.
 * 
 * 2006 - Initially written by Fabrizio Montesi and Mauro Silvagni
 * 2007 - Totally re-built by Fabrizio Montesi, exploiting new JOLIE capabilities
 * 
 */
public class SOAPProtocol implements CommProtocol
{
	private URI uri;
	private String messageNamespace;
	private String inputId = null;
	
	public SOAPProtocol clone()
	{
		return new SOAPProtocol( uri, messageNamespace );
	}

	public SOAPProtocol( URI uri, String messageNamespace )
	{
		this.uri = uri;
		this.messageNamespace = messageNamespace;
	}
	
	private void valueToSOAPBody(
			Value value,
			SOAPElement element,
			SOAPEnvelope soapEnvelope
			)
		throws SOAPException
	{
		String type = null;
		SOAPElement currentElement;
		if ( value.isDefined() ) {
			if ( value.isInt() )
				type = "int";
			else
				type = "string";
			element.addAttribute( soapEnvelope.createName( "type" ), "xsd:" + type );
			element.addTextNode( value.strValue() );
		}

		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			for( Value val : entry.getValue() ) {
				currentElement = element.addChildElement( entry.getKey() );
				for( Entry< String, Value > attrEntry : val.attributes().entrySet() ) {
					currentElement.addAttribute(
							soapEnvelope.createName( attrEntry.getKey() ),
							attrEntry.getValue().strValue()
							);
				}
				valueToSOAPBody( val, currentElement, soapEnvelope );
			}
		}
	}
	
	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		try {
			MessageFactory messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_1_PROTOCOL );
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();
			Name operationName = soapEnvelope.createName( message.inputId(), "", messageNamespace );
			SOAPBodyElement opBody = soapBody.addBodyElement( operationName );
			
			valueToSOAPBody( message.value(), opBody, soapEnvelope );

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			String soapString = "\n<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
								new String( tmpStream.toByteArray() );

			String messageString = new String();
			String soapAction = null;
			Operation operation = null;
			try {
				operation = RequestResponseOperation.getById( message.inputId() );
			} catch( InvalidIdException iie ) {}
			if ( operation != null ) {
				// We're responding to a request
				messageString += "HTTP/1.1 200 OK\n";
			} else {
				// We're sending a notification or a solicit
				/*String path = new String();
				if ( uri.getPath().length() < 1 || uri.getPath().charAt( 0 ) != '/' )
					path += "/";
				path += uri.getPath();
				if ( path.endsWith( "/" ) == false )
					path += "/";
				path += message.inputId();
				System.out.println( path );
				*/
				String path = uri.getPath();
				if ( path.length() == 0 )
					path = "*";
				messageString += "POST " + path + " HTTP/1.1\n";
				//messageString += "POST " + message.inputId() + " HTTP/1.1\n";
				messageString += "Host: " + uri.getHost() + '\n';
				soapAction =
					"SOAPAction: \"" + messageNamespace + "/" + message.inputId() + "\"\n";
			}
			
			//messageString += "Content-Type: application/soap+xml; charset=\"utf-8\"\n";
			messageString += "Content-Type: text/xml; charset=\"utf-8\"\n";
			messageString += "Content-Length: " + soapString.length() + '\n';
			if ( soapAction != null )
				messageString += soapAction;
			messageString += soapString + '\n';
			
			//System.out.println( "Sending: " + messageString );
			
			inputId = message.inputId();
			
			BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
			writer.write( messageString );
			writer.flush();
		} catch( SOAPException se ) {
			throw new IOException( se );
		}
		
		
		
		/*if ( deployInfo == null )
			throw new IOException( "Error: WSDL information missing in SOAP protocol" );

		HTTPSOAPException httpException = null;
		String soapString = null;
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL );
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();

			Vector< String > varNames = deployInfo.outVarNames();
			if ( varNames == null )
				throw new HTTPSOAPException( 500, "Error: output vars information missing in SOAP protocol" );

			Name operationName = soapEnvelope.createName( message.inputId(), "m", "jolieSOAP" );
			SOAPBodyElement opBody = soapBody.addBodyElement( operationName );

			SOAPElement varElement;
			int i = 0;
			
			for( Value val : message ) {
				varElement = opBody.addChildElement( varNames.elementAt( i++ ), operationName.getPrefix() );
				varElement.addTextNode( val.strValue() );
			}

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			soapString = new String( tmpStream.toByteArray() );
		} catch( HTTPSOAPException hse ) {
			httpException = hse;
		} catch( SOAPException se ) {
			throw new IOException( se );
		}
		
		String messageString = new String();
		
		try {
			Operation operation = Operation.getByWSDLBoundName( message.inputId() );
			if ( operation instanceof RequestResponseOperation ) {
				int httpCode;
				if ( httpException == null )
					httpCode = 200;
				else {
					httpCode = httpException.httpCode();
					try {
						soapString = httpException.createSOAPFaultMessage();
					} catch( SOAPException se ) {
						throw new IOException( se );
					}
				}

				messageString += "HTTP/1.1 " + httpCode + ' ';
				
				switch( httpCode ) {
					case 500:
						messageString += "Internal Server Error";
						break;
					case 200:
					default:
						messageString += "OK";
						break;
				}
				messageString += '\n';
			} else {
				if ( httpException != null )
					throw new IOException( httpException );
				if ( uri == null )
					throw new IOException( "Error: URI information missing in SOAP protocol" );
				messageString += "POST " + uri.getPath() + " HTTP/1.1\n";
				messageString += "Host: " + uri.getHost() + '\n';
			}
			messageString += "Content-type: application/soap+xml; charset=\"utf-8\"\n";
		} catch ( InvalidIdException iie ) {
			throw new IOException( iie );
		}
		
		messageString += "Content-Length: " + soapString.length() + '\n';
		messageString += soapString;
		
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
		writer.write( messageString );
		writer.flush();
		//System.out.println( messageString );
		if ( httpException != null )
			throw new IOException( httpException );
		*/
	}
	
	private void xmlNodeToValue( Value value, Node node )
	{
		Node currNode;
		
		// Set attributes
		NamedNodeMap attributes = node.getAttributes();
		if ( attributes != null ) {
			for( int i = 0; i < attributes.getLength(); i++ ) {
				currNode = attributes.item( i );
				value.getAttribute( currNode.getNodeName() ).setStrValue( currNode.getNodeValue() );
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
				value.setStrValue( currNode.getNodeValue() );
				break;
			}
		}
		
		Value attr;
		if ( (attr=value.attributes().get( "type" )) != null ) {
			String type = attr.strValue();
			if ( "xsd:int".equals( type ) )
				value.setIntValue( value.intValue() );
			else if ( "xsd:double".equals( type ) )
				value.setDoubleValue( value.doubleValue() );
			else if ( "xsd:string".equals( type ) )
				value.setStrValue( value.strValue() );
		}
	}
	
	@SuppressWarnings("unchecked")
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		HTTPParser parser = new HTTPParser( istream );
		HTTPMessage message = parser.parse();
		CommMessage retVal = null;
		
		try {
			MessageFactory messageFactory =
				MessageFactory.newInstance( SOAPConstants.SOAP_1_1_PROTOCOL );
			SOAPMessage soapMessage =
				messageFactory.createMessage();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
			
			
			// Debug incoming message
			
			/*BufferedReader r = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( message.content() ) ) );
			String p;
			System.out.println("---");
			while( (p=r.readLine()) != null ) 
				System.out.println(p);
			System.out.println("---");
			*/
			
			Document doc = builder.parse( src );
			DOMSource dom = new DOMSource( doc );

			soapMessage.getSOAPPart().setContent( dom );
			
			Value value = Value.create();
			xmlNodeToValue(
					value, soapMessage.getSOAPBody().getFirstChild()
					);
			
			if ( message.type() == HTTPMessage.Type.RESPONSE ) { 
				retVal = new CommMessage( inputId, value );
			} else if (
					message.type() == HTTPMessage.Type.POST ||
					message.type() == HTTPMessage.Type.GET
					) {
				// @todo -- Beware, this does not handle a querystring or nested paths! 
				//retVal = new CommMessage( message.requestPath(), value );
				retVal = new CommMessage( soapMessage.getSOAPBody().getFirstChild().getLocalName(), value );
			}
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
		
		return retVal;
		
		/*HTTPScanner.Token token;
		HTTPScanner scanner = new HTTPScanner( istream, "network" );
		int httpCode = 0;
		
		token = scanner.getToken();
		if ( !token.isA( HTTPScanner.TokenType.POST ) ) {
			if ( token.isA( HTTPScanner.TokenType.HTTP ) ) {
				// Revise these checks.. for example, 1.1 and not int/int
				token = scanner.getToken();
				if ( token.type() != HTTPScanner.TokenType.DIVIDE )
					throw new IOException( "Malformed HTTP SOAP packet received." );
				token = scanner.getToken();
				if ( token.type() != HTTPScanner.TokenType.INT )
					throw new IOException( "Malformed HTTP SOAP packet received." );
				token = scanner.getToken();
				if ( token.type() != HTTPScanner.TokenType.DOT )
					throw new IOException( "Malformed HTTP SOAP packet received." );
				token = scanner.getToken();
				if ( token.type() != HTTPScanner.TokenType.INT )
					throw new IOException( "Malformed HTTP SOAP packet received." );
				token = scanner.getToken();
				if ( token.type() != HTTPScanner.TokenType.INT )
					throw new IOException( "Malformed HTTP SOAP packet received." );
				httpCode = Integer.parseInt( token.content() );
			} else
				throw new IOException( "Malformed HTTP SOAP packet received." );
		}
		while( !token.isA( HTTPScanner.TokenType.ERROR ) &&
				!token.isA( HTTPScanner.TokenType.EOF ) && 
				!token.isA( HTTPScanner.TokenType.CONTENTLENGTH ) )
			token = scanner.getToken();
		
		if ( !token.isA( HTTPScanner.TokenType.CONTENTLENGTH ) )
			throw new IOException( "Malformed SOAP packet (element content-length)." );
		
		token = scanner.getToken();
		if ( !token.isA( HTTPScanner.TokenType.COLON ) )
			throw new IOException( "Malformed SOAP packet (element content-length)." );
		token = scanner.getToken();
		if ( !token.isA( HTTPScanner.TokenType.INT ) )
			throw new IOException( "Malformed SOAP packet (element content-length)." );

		int length = Integer.parseInt( token.content() );

		byte buffer[] = new byte[ length ];
		istream.read( buffer );
		
		CommMessage message = null;
		try {
			MessageFactory messageFactory =
				MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL ); //SOAPConstants.DYNAMIC_SOAP_PROTOCOL );
			SOAPMessage soapMessage =
				messageFactory.createMessage();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputSource src = new InputSource( new ByteArrayInputStream( buffer ) );

			Document doc = builder.parse( src );
			DOMSource dom = new DOMSource( doc );

			soapMessage.getSOAPPart().setContent( dom );

			if ( httpCode != 0 && httpCode != 200 ) {
				String exMesg =	"\n--> Received an HTTP error packet\n" +
									"\tHTTP Code: " + httpCode + '\n';
				try {
					exMesg += 
						"\tSOAP Fault Message: "
						+ soapMessage.getSOAPBody().getFault().getFaultString() + '\n';
				} catch ( NullPointerException npe ) {}
				throw new IOException( exMesg );
			}

			Node opNode = soapMessage.getSOAPBody().getFirstChild();
			Operation operation = Operation.getByWSDLBoundName( opNode.getLocalName() );
		
			message = new CommMessage( operation.id() );

			NodeList nodeList = opNode.getChildNodes();

			Vector< String > varNames = operation.deployInfo().inVarNames();
			if ( varNames == null )
				throw new IOException( "Error: missing WSDL input variable names in operation " + operation.id() );
			
			int namesSize = varNames.size();
			
			if ( nodeList.getLength() != namesSize )
				throw new IOException( "Received malformed SOAP Packet: wrong variables number" );
			
			Node currNode;
			String nodeName;
			int j;
			List< Value > list = new Vector< Value >();
			for( int k = 0; k < namesSize; k++ )
				list.add( Value.createValue() );
			Value tempVar;
			for( int i = 0; i < nodeList.getLength(); i++ ) {
				currNode = nodeList.item( i );
				nodeName = currNode.getLocalName();
				j = 0;
				for( String str : varNames ) {
					if ( str.equals( nodeName ) )
						break;
					else
						j++;
				}

				if ( j >= namesSize )
					throw new IOException( "Received malformed SOAP packet: corresponding variable name not found: " + nodeName );
				if ( currNode.getFirstChild() == null )
					tempVar = Value.createValue( "" );
				else {
					try {
						tempVar = Value.createValue( Integer.parseInt( currNode.getFirstChild().getNodeValue() ) );
					} catch( NumberFormatException e ) {
						tempVar = Value.createValue( currNode.getFirstChild().getNodeValue() );
					}
				}
				list.set( j, tempVar );
			}
			message.addAllValues( list );
			*/
			/**
			 * This is needed to maintain information useful in a RequestResponse.
			 */
			//this.deployInfo = operation.deployInfo();
			/*this.uri = new URI( "localhost/response" );
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		} catch( InvalidIdException iie ) {
			throw new IOException( "Received invalid operation name: " + iie.getMessage() );
		} catch( URISyntaxException ue ) {
			throw new IOException( ue );
		}
		
		return message;*/
	}
}