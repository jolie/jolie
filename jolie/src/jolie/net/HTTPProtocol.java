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
import javax.xml.soap.SOAPException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jolie.net.http.HTTPMessage;
import jolie.net.http.HTTPParser;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Operation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HTTPProtocol implements CommProtocol
{
	private URI uri;
	private String inputId = null;
	
	public HTTPProtocol( URI uri )
	{
		this.uri = uri;
	}
	
	public HTTPProtocol clone()
	{
		return new HTTPProtocol( uri );
	}
	
	private void valueToDocument(
			Value value,
			Element element,
			Document doc
			)
		throws SOAPException
	{
		Element currentElement;
		
		//if ( value.isDefined() )
			//element.appendChild( doc.createTextNode( value.strValue() ) );

		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			for( Value val : entry.getValue() ) {
				currentElement = doc.createElement( entry.getKey() );
				element.appendChild( currentElement );
				for( Entry< String, Value > attrEntry : val.attributes().entrySet() ) {
					currentElement.setAttribute(
							attrEntry.getKey(),
							attrEntry.getValue().strValue()
							);
				}
				currentElement.appendChild( doc.createTextNode( val.strValue() ) );
				valueToDocument( val, currentElement, doc );
			}
		}
	}
	
	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		try {
			String xmlString = new String();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();

			Element root = doc.createElement( "root" );
			xmlString += message.value().strValue();
			valueToDocument( message.value(), root, doc );
			
			NodeList list = root.getChildNodes();
			for( int i = 0; i < list.getLength(); i++ )
				doc.appendChild( list.item( i ) );

			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer transformer = tranFactory.newTransformer();

            Source src = new DOMSource( doc );
            ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
            Result dest = new StreamResult( tmpStream );
            transformer.transform( src, dest );
			
			xmlString += new String( tmpStream.toByteArray() );

			String messageString = new String();
			try {
				Operation operation = RequestResponseOperation.getById( message.inputId() );
				if ( operation != null ) {
					// We're responding to a request
					messageString += "HTTP/1.1 200 OK\n";
				} else {
					// We're sending a notification or a solicit
					String path = new String();
					if ( uri.getPath().length() < 1 || uri.getPath().charAt( 0 ) != '/' )
						path += "/";
					path += uri.getPath();
					if ( path.endsWith( "/" ) == false )
						path += "/";
					path += message.inputId();
					
					messageString += "POST " + path + " HTTP/1.1\n";
					messageString += "Host: " + uri.getHost() + '\n';
				}
			} catch( InvalidIdException iie ) {
				throw new IOException( iie );
			}

			messageString += "Content-Type: text/xml; charset=\"utf-8\"\n";
			messageString += "Content-Length: " + xmlString.length() + '\n';
			messageString += '\n' + xmlString + '\n';
			
			//System.out.println( "Sending: " + messageString );
			
			inputId = message.inputId();
			
			BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
			writer.write( messageString );
			writer.flush();
		} catch( SOAPException se ) {
			throw new IOException( se );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException te ) {
			throw new IOException( te );
		}
	}
	
	private void elementsToSubValues( Value value, NodeList list )
	{
		Node node;
		Value childValue;
		for( int i = 0; i < list.getLength(); i++ ) {
			node = list.item( i );
			switch( node.getNodeType() ) {
			case Node.ATTRIBUTE_NODE:
				value.getAttribute( node.getNodeName() ).setStrValue( node.getNodeValue() );
				break;
			case Node.ELEMENT_NODE:
				childValue = value.getNewChild( node.getLocalName() );
				elementsToSubValues( childValue, node.getChildNodes() ); 
				break;
			case Node.TEXT_NODE:
				value.setStrValue( node.getNodeValue() );
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		/*BufferedReader r = new BufferedReader( new InputStreamReader( istream ) );
			String p;
			System.out.println("---");
			while( (p=r.readLine()) != null ) 
				System.out.println(p+ "-");
			System.out.println("---");
		*/
			
		HTTPParser parser = new HTTPParser( istream );
		HTTPMessage message = parser.parse();
		CommMessage retVal = null;

		try {
			Value messageValue = Value.create();
			//String opId = null;
			if ( message.size() > 0 ) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware( true );
				DocumentBuilder builder = factory.newDocumentBuilder();

				//InputSource src = new InputSource( message.contentStream() );
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );

				// Debug incoming message

				/*
				BufferedReader r = new BufferedReader( message.contentStream() );
				String p;
				System.out.println("---");
				while( (p=r.readLine()) != null ) 
				System.out.println(p);
				System.out.println("---");
				*/
			
				Document doc = builder.parse( src );

				elementsToSubValues(
							messageValue,
							doc.getDocumentElement().getFirstChild().getChildNodes()
						);
			}
			
			

			if ( message.type() == HTTPMessage.Type.RESPONSE ) {
				retVal = new CommMessage( inputId, messageValue );
			} else if (
					message.type() == HTTPMessage.Type.POST ||
					message.type() == HTTPMessage.Type.GET ) {
				retVal = new CommMessage( message.requestPath(), messageValue );
			}
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
		
		return retVal;
	}
}