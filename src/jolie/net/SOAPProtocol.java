/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie.net;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;

import jolie.TempVariable;
import jolie.Variable;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SOAPProtocol implements CommProtocol
{
	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		String soapString = new String();
		String messageString = new String();
		messageString += "POST /Jolie HTTP/1.1\n";
		messageString += "Host: Jolie\n";
		messageString += "Content-type: application/soap+xml; charset=utf-8\n";
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPBody soapBody = soapEnvelope.getBody();
			
			Name operationName = soapEnvelope.createName( "operation" );
			soapBody.addBodyElement( operationName ).addTextNode( message.inputId() );
			
			for( Variable var : message ) {
				Name varName = soapEnvelope.createName( var.id() );
				soapBody.addBodyElement( varName ).addTextNode( var.strValue() );
			}

			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			soapMessage.writeTo( tmpStream );
			soapString = new String( tmpStream.toByteArray() );
		} catch( SOAPException se ) {
			throw new IOException( se );
		}
		
		messageString += "Content-Length: " + soapString.length() + '\n';
		messageString += soapString;
		
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
		writer.write( messageString );
		writer.flush();
	}
	
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		int length;
		HTTPScanner.Token token;
		HTTPScanner scanner = new HTTPScanner( istream, "network" );
		
		token = scanner.getToken();
		while( token.type() != HTTPScanner.TokenType.ERROR &&
				token.type() != HTTPScanner.TokenType.EOF && 
				token.type() != HTTPScanner.TokenType.CONTENTLENGTH )
			token = scanner.getToken();
		
		if ( token.type() != HTTPScanner.TokenType.CONTENTLENGTH )
			throw new IOException( "Malformed SOAP packet (element content-length)." );
		
		token = scanner.getToken();
		if ( token.type() != HTTPScanner.TokenType.COLON )
			throw new IOException( "Malformed SOAP packet (element content-length)." );
		token = scanner.getToken();
		if ( token.type() != HTTPScanner.TokenType.INT )
			throw new IOException( "Malformed SOAP packet (element content-length)." );
		
		length = Integer.parseInt( token.content() );
		
		byte buffer[] = new byte[ length ];
		istream.read( buffer );
		
		CommMessage message = null;
		try {
			MessageFactory messageFactory =
				MessageFactory.newInstance(); //SOAPConstants.DYNAMIC_SOAP_PROTOCOL );
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
			message = new CommMessage( elem.getValue() );
			
			String str;
			TempVariable tempVar;
			while( it.hasNext() ) {
				elem = (SOAPBodyElement) it.next();
				str = elem.getValue();
				if ( str == null ) // Crash fix
					str = new String();
				
				try {
					tempVar = new TempVariable( Integer.parseInt( str ) );
				} catch( NumberFormatException e ) {
					tempVar = new TempVariable( str );
				}
				message.addValue( tempVar );
			}
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}

		return message;
	}
}