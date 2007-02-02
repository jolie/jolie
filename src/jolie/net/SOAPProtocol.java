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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
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

import jolie.InvalidIdException;
import jolie.Location;
import jolie.Operation;
import jolie.TempVariable;
import jolie.Variable;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SOAPProtocol implements CommProtocol
{
	private URL url;
	private Operation op;

	public SOAPProtocol()
	{
		url = null;
		op = null;
	}
	
	public SOAPProtocol( Location location, Operation op )
		throws MalformedURLException
	{
		url = new URL( location.value() );
		this.op = op;
	}

	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		if ( op == null )
			return;
		String soapString = new String();
		String messageString = new String();
		messageString += "POST " + url.getPath() + " HTTP/1.1\n";
		messageString += "Host: " + url.getHost() + '\n';
		messageString += "Content-type: application/soap+xml; charset=\"utf-8\"\n";
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL );
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();

			Name operationName = soapEnvelope.createName( message.inputId(), "m", "http://jolie.sf.net/wsdlfile.xml" );
			SOAPBodyElement opBody = soapBody.addBodyElement( operationName );

			SOAPElement varElement;
			int i = 0;
			//Operation op = Operation.getById( message.inputId() );
			Vector< String > varNames = op.wsdlInfo().outVarNames();
			for( Variable var : message ) {
				varElement = opBody.addChildElement( varNames.elementAt( i++ ) );
				varElement.addTextNode( var.strValue() );
			}

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			soapString = new String( tmpStream.toByteArray() );
		} catch( SOAPException se ) {
			throw new IOException( se );
		}/* catch( InvalidIdException ie ) {
			throw new IOException( ie );
		}*/
		
		messageString += "Content-Length: " + soapString.length() + '\n';
		messageString += soapString;
		
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
		writer.write( messageString );
		writer.flush();
	}
	
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		HTTPScanner.Token token;
		HTTPScanner scanner = new HTTPScanner( istream, "network" );
		
		token = scanner.getToken();
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

			Iterator it = soapMessage.getSOAPBody().getChildElements();
			SOAPBodyElement elem = (SOAPBodyElement) it.next();
			message = new CommMessage( elem.getLocalName() );

			String value;
			String varName;
			int i;
			Vector< String > varNames = Operation.getById( elem.getLocalName() ).wsdlInfo().inVarNames();
			TempVariable tempVar;
			List< Variable > list = new LinkedList< Variable >();
			while( it.hasNext() ) {
				elem = (SOAPBodyElement) it.next();
				varName = elem.getLocalName();
				value = elem.getValue();
				if ( value == null ) // Crash fix
					value = new String();
				
				for( i = 0; !varNames.get( i ).equals( varName ); i++ );
				
				try {
					tempVar = new TempVariable( Integer.parseInt( value ) );
				} catch( NumberFormatException e ) {
					tempVar = new TempVariable( value );
				}
				list.set( i, tempVar );
			}
			message.addAllValues( list );
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		} catch( InvalidIdException iie ) {
			throw new IOException( "Received invalid operation name: " + iie.getMessage() );
		}

		return message;
	}
}