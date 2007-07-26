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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

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

import jolie.Constants;
import jolie.deploy.OperationWSDLInfo;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Operation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.TempVariable;
import jolie.runtime.Variable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class HTTPSOAPException extends Exception
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
}

/** Implements the SOAP/HTTP protocol.
 * 
 * @author Fabrizio Montesi
 * @author Mauro Silvagni
 * @todo Implement the message WSDL namespace information.
 * @todo Various checks should be made from the deploy parser.
 * 
 */
public class SOAPProtocol implements CommProtocol
{
	private URI uri;
	private OperationWSDLInfo wsdlInfo;
	
	public SOAPProtocol clone()
	{
		SOAPProtocol retval = new SOAPProtocol();
		retval.uri = uri;
		retval.wsdlInfo = wsdlInfo;
		return retval;
	}

	public SOAPProtocol()
	{
		uri = null;
		wsdlInfo = null;
	}
	
	public SOAPProtocol( URI uri, OperationWSDLInfo wsdlInfo )
	{
		this.uri = uri;
		this.wsdlInfo = wsdlInfo;
	}
	
	public SOAPProtocol( URI uri )
	{
		this.uri = uri;
	}
	
	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		if ( wsdlInfo == null )
			throw new IOException( "Error: WSDL information missing in SOAP protocol" );

		HTTPSOAPException httpException = null;
		String soapString = null;
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL );
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();

			Vector< String > varNames = wsdlInfo.outVarNames();
			if ( varNames == null )
				throw new HTTPSOAPException( 500, "Error: output vars information missing in SOAP protocol" );

			Name operationName = soapEnvelope.createName( message.inputId(), "m", "jolieSOAP" );
			SOAPBodyElement opBody = soapBody.addBodyElement( operationName );

			SOAPElement varElement;
			int i = 0;
			
			for( Variable var : message ) {
				varElement = opBody.addChildElement( varNames.elementAt( i++ ), operationName.getPrefix() );
				varElement.addTextNode( var.strValue() );
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
	}
	
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		HTTPScanner.Token token;
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

			Vector< String > varNames = operation.wsdlInfo().inVarNames();
			if ( varNames == null )
				throw new IOException( "Error: missing WSDL input variable names in operation " + operation.id() );
			
			int namesSize = varNames.size();
			
			if ( nodeList.getLength() != namesSize )
				throw new IOException( "Received malformed SOAP Packet: wrong variables number" );
			
			Node currNode;
			String nodeName;
			int j;
			List< Variable > list = new Vector< Variable >();
			for( int k = 0; k < namesSize; k++ )
				list.add( new TempVariable() );
			TempVariable tempVar;
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
					tempVar = new TempVariable( "" );
				else {
					try {
						tempVar = new TempVariable( Integer.parseInt( currNode.getFirstChild().getNodeValue() ) );
					} catch( NumberFormatException e ) {
						tempVar = new TempVariable( currNode.getFirstChild().getNodeValue() );
					}
				}
				list.set( j, tempVar );
			}
			message.addAllValues( list );
			
			/**
			 * This is needed to maintain information useful in a RequestResponse.
			 */
			this.wsdlInfo = operation.wsdlInfo();
			this.uri = new URI( "localhost/response" );
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
		
		return message;
	}
}