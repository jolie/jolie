/***************************************************************************
 *   Copyright (C) 2009 by Claudio Guidi                                   *
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Map.Entry;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import jolie.Interpreter;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.protocols.SequentialCommProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.io.ByteArrayOutputStream;
import org.w3c.dom.NodeList;

/** Implements the XML-RPC over HTTP protocol.
 * 
 * @author Claudio Guidi
 * 2009 - Fabrizio Montesi: optimizations and refactoring to use the Element-based API
 * 
 */

// TO DO: faults

/* NOTE about this module:
 * The XML-RPC specification reference considered is http://www.xmlrpc.com/spec
 * I made different assumptions for input and output messages.
 * Output:
 * aliases for operation names are allowed because Jolie does not support operation names with dots (es. mickey.mouse)
 * aliases are expressed as protocol parameters as aliases.opname = "aliasName".
 * In output the request message must contain a subelement param (which could be an array).
 * Each element of teh subelement param is translated into a tag <param>.
 * in order to express an array within an element of param, you need to use the keyword array:
 * request.param.array[0]=....; request.param.array[1]=...
 * will be translated into <param><value><array><data><value>...</value><value>...</value>..</data></array></value></param>
 * Since boolean are not managed by Jolie, it is necessary to represent it with subelement boolean
 *
 * Output Faults:
 * At the present Jolie always generates zero code faults.
 *
 * Input:
 * All the array in an input XMLRPC message will be translated into Jolie by means of arrays of the keyword array.
 * 
 */
public class XmlRpcProtocol extends SequentialCommProtocol
{
	private String inputId = null;
	final private Transformer transformer;
	final private Interpreter interpreter;
	final private DocumentBuilderFactory docBuilderFactory;
	final private DocumentBuilder docBuilder;
	final private URI uri;
	private boolean received = false;
	final private static String CRLF = new String( new char[]{13, 10} );

	public String name()
	{
		return "xmlrpc";
	}

	public XmlRpcProtocol(
		VariablePath configurationPath,
		URI uri,
		Transformer transformer,
		DocumentBuilderFactory docBuilderFactory,
		DocumentBuilder docBuilder,
		Interpreter interpreter )
	{
		super( configurationPath );
		this.uri = uri;
		this.transformer = transformer;
		this.interpreter = interpreter;
		this.docBuilderFactory = docBuilderFactory;
		this.docBuilder = docBuilder;
	}

	private static Element getFirstElement( Element element, String name )
		throws IOException
	{
		NodeList children = element.getElementsByTagName( name );
		if ( children.getLength() < 1 ) {
			throw new IOException( "Could not find element " + name );
		}

		return (Element)children.item( 0 );
	}

	private void navigateValue( Value value, Element element )
		throws IOException
	{
		NodeList contents = element.getChildNodes();
		if ( contents.getLength() < 1 ) {
			throw new IOException( "empty value node" );
		}
		Node contentNode = contents.item( 0 );
		if ( contentNode.getNodeType() != Node.ELEMENT_NODE ) {
			throw new IOException( "a value node may contain only an sub-element" );
		}

		Element content = (Element)contentNode;
		String name = content.getNodeName();
		if ( name.equals( "array" ) ) {
			Value currentValue;
			ValueVector vec = value.getChildren( "array" );
			NodeList dataChildren = getFirstElement( content, "data" ).getElementsByTagName( "value" );
			for ( int i = 0; i < dataChildren.getLength(); i++ ) {
				currentValue = Value.create();
				navigateValue( currentValue, (Element)dataChildren.item( i ) );
				vec.add( currentValue );
			}
		} else if ( name.equals( "struct" ) ) {
			NodeList members = content.getElementsByTagName( "member" );
			Element member;
			for ( int i = 0; i < members.getLength(); i++ ) {
				member = (Element)members.item( i );
				Element valueNode = getFirstElement( member, "value" );
				navigateValue( value.getNewChild( getFirstElement( member, "name" ).getNodeValue() ), valueNode );
			}
		} else if ( name.equals( "string" ) ) {
			value.setValue( content.getTextContent() );
		} else if ( name.equals( "int" ) || name.equals( "i4" ) ) {
			try {
				value.setValue( Integer.parseInt( content.getTextContent() ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if ( name.equals( "boolean" ) ) {
			try {
				value.setValue( Integer.parseInt( content.getTextContent() ) != 0);
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		}
	}

	private void documentToValue( Value value, Document document )
		throws IOException
	{
		ValueVector paramsValueVector = value.getChildren( "param" );

		NodeList params = getFirstElement( document.getDocumentElement(), "params" ).getElementsByTagName( "param" );
		Value paramValue;
		for( int i = 0; i < params.getLength(); i++ ) {
			paramValue = Value.create();
			navigateValue( paramValue, getFirstElement( ((Element)params.item( i )), "value" ) );
			paramsValueVector.add( paramValue );
		}
	}

	private void valueToDocument(
		Value value,
		Node node,
		Document doc )
	{
		Element v = doc.createElement( "value" );

		// node value creation in case the contents is a value
		if ( value.isInt() ) {

			Element i = doc.createElement( "int" );
			i.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( i );
			node.appendChild( v );
		} else if ( value.isString() ) {

			Element i = doc.createElement( "string" );
			i.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( i );
			node.appendChild( v );
		} else if ( value.isDouble() ) {

			Element i = doc.createElement( "double" );
			i.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( i );
			node.appendChild( v );

		} else if ( value.isBool() ) {
		
			Element b = doc.createElement( "boolean" );
			b.appendChild( doc.createTextNode( value.boolValue() ? "1" : "0" ) );
			v.appendChild( b );
			node.appendChild( v );
			
		} else if ( value.hasChildren( "array" ) ) {
			// array creation

			Element a = doc.createElement( "array" );
			Element d = doc.createElement( "data" );
			for ( int i = 0; i < value.getChildren( "array" ).size(); i++ ) {
				valueToDocument( value.getChildren( "array" ).get( i ), d, doc );
			}
			a.appendChild( d );
			v.appendChild( a );
			node.appendChild( v );
		} else {
			for ( Entry<String, ValueVector> entry : value.children().entrySet() ) {
				if ( !entry.getKey().startsWith( "@" ) ) {
					Element st = doc.createElement( "struct" );
					Element m = doc.createElement( "member" );
					Element n = doc.createElement( "name" );
					n.appendChild( doc.createTextNode( entry.getKey() ) );
					m.appendChild( n );
					for ( Value val : entry.getValue() ) {
						valueToDocument( val, m, doc );
					}
					st.appendChild( m );
					v.appendChild( st );
				}
			}
		}


	}

	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		Document doc = docBuilder.newDocument();
		// root element <methodCall>
		String rootName = "methodCall";
		if ( received ) {
			// We're responding to a request
			rootName = "methodResponse";
		}

		Element root = doc.createElement( rootName );
		doc.appendChild( root );

		if ( !received ) {
			// element <methodName>
			Element methodName;
			methodName = doc.createElement( "methodName" );
			Value aliases = getParameterFirstValue( "aliases" );
			String alias;
			if ( aliases.hasChildren( message.operationName() ) ) {
				alias = aliases.getFirstChild( message.operationName() ).strValue();
			} else {
				alias = message.operationName();
			}
			methodName.appendChild( doc.createTextNode( alias ) );
			inputId = message.operationName();
			root.appendChild( methodName );
		}

		if ( message.isFault() ) {
			FaultException f = message.fault();
			Element fault = doc.createElement( "fault" );
			Element v = doc.createElement( "value" );
			Element s = doc.createElement( "struct" );
			Element m1 = doc.createElement( "member" );
			Element n1 = doc.createElement( "name" );
			Element v1 = doc.createElement( "value" );
			Element i1 = doc.createElement( "i4" );
			Element m2 = doc.createElement( "member" );
			Element n2 = doc.createElement( "name" );
			Element v2 = doc.createElement( "value" );
			Element i2 = doc.createElement( "string" );
			i1.setTextContent( "0" ); // Jolie generates always zero code faults
			i2.setTextContent( f.faultName() ); // fault name is insertied into faultString tag of XMLRPC message
			v1.appendChild( i1 );
			v2.appendChild( i2 );
			n1.setTextContent( "faultCode" );
			m1.appendChild( v1 );
			m1.appendChild( n1 );
			m2.appendChild( v2 );
			m2.appendChild( n2 );
			s.appendChild( m1 );
			s.appendChild( m2 );
			v.appendChild( s );
			fault.appendChild( v );
			root.appendChild( fault );

		} else if ( message.value().hasChildren( "param" ) ) {
			// params exist
			Element params = doc.createElement( "params" );
			for ( int i = 0; i < message.value().getChildren( "param" ).size(); i++ ) {
				Element p = doc.createElement( "param" );
				Element v = doc.createElement( "value" );
				if ( message.value().getChildren( "param" ).get( i ).isInt() ) {
					Element in = doc.createElement( "int" );
					in.appendChild( doc.createTextNode( message.value().getChildren( "param" ).get( i ).strValue() ) );
					v.appendChild( in );
				} else if ( message.value().getChildren( "param" ).get( i ).isString() ) {
					Element s = doc.createElement( "string" );
					s.appendChild( doc.createTextNode( message.value().getChildren( "param" ).get( i ).strValue() ) );
					v.appendChild( s );
				} else if ( message.value().getChildren( "param" ).get( i ).isDouble() ) {
					Element d = doc.createElement( "double" );
					d.appendChild( doc.createTextNode( message.value().getChildren( "param" ).get( i ).strValue() ) );
					v.appendChild( d );
				} else if ( message.value().getChildren( "param" ).get( i ).isBool() ) {
					Element b = doc.createElement( "boolean" );
					b.appendChild( doc.createTextNode( message.value().getChildren( "param" ).get( i ).boolValue() ? "1" : "0" ) );
					v.appendChild( b );
				} else if ( message.value().getChildren( "param" ).get( i ).hasChildren( "array" ) ) {
					// array creation
					Element a = doc.createElement( "array" );
					Element d = doc.createElement( "data" );
					for ( int x = 0; x < message.value().getChildren( "param" ).get( i ).getChildren( "array" ).size(); x++ ) {
						valueToDocument( message.value().getChildren( "param" ).get( i ).getChildren( "array" ).get( x ), d, doc );
					}
					a.appendChild( d );
					v.appendChild( a );
				} else if ( message.value().getChildren( "param" ).get( i ).hasChildren() ) {
					for ( Entry<String, ValueVector> entry : message.value().getChildren( "param" ).get( i ).children().entrySet() ) {
						if ( !entry.getKey().startsWith( "@" ) ) {
							Element st = doc.createElement( "struct" );
							Element m = doc.createElement( "member" );
							Element n = doc.createElement( "name" );
							n.appendChild( doc.createTextNode( entry.getKey() ) );
							m.appendChild( n );
							for ( Value val : entry.getValue() ) {
								valueToDocument( val, m, doc );
							}
							st.appendChild( m );
							v.appendChild( st );
						}

					}
				}
				p.appendChild( v );
				params.appendChild( p );
			}
			root.appendChild( params );
		}


		inputId = message.operationName();

		Source src = new DOMSource( doc );
		ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
		Result dest = new StreamResult( tmpStream );
		try {
			transformer.transform( src, dest );
		} catch ( TransformerException e ) {
			throw new IOException( e );
		}



		String xmlrpcString = CRLF + "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
			new String( tmpStream.toByteArray() );

		String messageString = "";

		if ( received ) {
			// We're responding to a request
			messageString += "HTTP/1.1 200 OK" + CRLF;

			received = false;
		} else {
			// We're sending a notification or a solicit
			String path = uri.getPath();
			if ( path == null || path.length() == 0 ) {
				path = "*";
			}
			messageString += "POST " + path + " HTTP/1.1" + CRLF;
			messageString += "User-Agent: Jolie" + CRLF;
			messageString += "Host: " + uri.getHost() + CRLF;

		}

		if ( getParameterVector( "keepAlive" ).first().intValue() != 1 ) {
			channel().setToBeClosed( true );
			messageString += "Connection: close" + CRLF;
		}

		//messageString += "Content-Type: application/soap+xml; charset=\"utf-8\"\n";
		messageString += "Content-Type: text/xml; charset=\"utf-8\"" + CRLF;
		messageString += "Content-Length: " + xmlrpcString.length() + CRLF;

		messageString += xmlrpcString + CRLF;

		if ( getParameterVector( "debug" ).first().intValue() > 0 ) {
			interpreter.logInfo( "[XMLRPC debug] Sending:\n" + messageString );
		}

		Writer writer = new OutputStreamWriter( ostream );
		writer.write( messageString );
		writer.flush();
	}

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		HttpParser parser = new HttpParser( istream );
		HttpMessage message = parser.parse();
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		CommMessage retVal = null;
		FaultException fault = null;
		Value value = Value.create();
		Document doc = null;

		if ( getParameterVector( "debug" ).first().intValue() > 0 ) {
			interpreter.logInfo( "[XMLRPC debug] Receiving:\n" + new String( message.content() ) );
		}

		if ( message.content() != null ) {
			try {
				if ( message.size() > 0 ) {
					DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
					InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
					doc = builder.parse( src );
					if ( message.isResponse() ) {
						// test if the message contains a fault
						try {
							Element faultElement = getFirstElement( doc.getDocumentElement(), "fault" );
							Element struct = getFirstElement( getFirstElement( faultElement, "value" ), "struct" );
							NodeList members = struct.getChildNodes();
							if ( members.getLength() != 2 || members.item( 1 ).getNodeType() != Node.ELEMENT_NODE ) {
								throw new IOException( "Malformed fault data" );
							}
							Element faultMember = (Element)members.item( 1 );
							//Element valueElement = getFirstElement( getFirstElement( struct, "value" ), "string" );
							String faultName = getFirstElement( faultMember, "name" ).getTextContent();
							String faultString = getFirstElement( getFirstElement( faultMember, "value" ), "string" ).getTextContent();
							fault = new FaultException( faultName, Value.create( faultString ) );
						} catch( IOException e ) {
							documentToValue( value, doc );
						}
					} else {
						documentToValue( value, doc );
					}
				}
			} catch ( ParserConfigurationException pce ) {
				throw new IOException( pce );
			} catch ( SAXException saxe ) {
				throw new IOException( saxe );
			}

			if ( message.isResponse() ) {
				//fault = new FaultException( "InternalServerError", "" );
				//TODO support resourcePath
				retVal = new CommMessage( CommMessage.GENERIC_ID, inputId, "/", value, fault );
			} else if ( !message.isError() ) {
				//TODO support resourcePath
				String opname = doc.getDocumentElement().getFirstChild().getTextContent();
				retVal = new CommMessage( CommMessage.GENERIC_ID, opname, "/", value, fault );

			}
		}

		received = true;
		return retVal;
	}
}