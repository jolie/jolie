/***************************************************************************
 *   Copyright (C) 2009 by Claudio Guidi                                   *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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
import java.net.URI;
import java.util.Map.Entry;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import jolie.Interpreter;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.protocols.SequentialCommProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.w3c.dom.NodeList;

/**
 * Implements the XML-RPC over HTTP protocol.
 * 
 * @author Claudio Guidi 2009 - Fabrizio Montesi: optimizations and refactoring to use the
 *         Element-based API
 * 
 */

/*
 * NOTE about this module: The XML-RPC specification reference considered is
 * http://www.xmlrpc.com/spec I made different assumptions for input and output messages. Output:
 * aliases for operation names are allowed because Jolie does not support operation names with dots
 * (es. mickey.mouse) aliases are expressed as protocol parameters as aliases.opname = "aliasName".
 * In output the request message must contain a subelement param (which could be an array). Each
 * element of the subelement param is translated into a tag <param>. in order to express an array
 * within an element of param, you need to use the keyword array: request.param.array[0]=....;
 * request.param.array[1]=... will be translated into
 * <param><value><array><data><value>...</value><value>...</value>..</data></array></value></param>
 *
 * Output Faults: At the present Jolie always generates zero code faults.
 *
 * Input: All the array in an input XMLRPC message will be translated into Jolie by means of arrays
 * of the keyword array.
 * 
 */
public class XmlRpcProtocol extends SequentialCommProtocol implements HttpUtils.Protocol {
	private String inputId = null;
	final private Transformer transformer;
	final private Interpreter interpreter;
	final private DocumentBuilder docBuilder;
	final private URI uri;
	private final boolean inInputPort;
	private boolean received = false;
	private String encoding;

	/**
	 * In XML-RPC each request or response parameter needs to be contained in a variable with this name,
	 * which is usually an array. Otherwise the parameter gets ignored.
	 */
	private static final String PARAMS_KEY = "param";

	/**
	 * XML-RPC arrays are mapped to Jolie arrays called "ARRAY_KEY" as children of the respective values
	 * (= placeholders). Hence these values should not carry any root value and also not contain any
	 * other children. Eg. <param><array><data><value><int>3</int></value></data></param> in XML-RPC
	 * becomes param[0].array[0] = 3 in Jolie and param[0] should not contain anything beside the array.
	 */
	private static final String ARRAY_KEY = "array";

	@Override
	public String name() {
		return "xmlrpc";
	}

	public XmlRpcProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		Transformer transformer,
		DocumentBuilder docBuilder,
		Interpreter interpreter ) {
		super( configurationPath );
		this.uri = uri;
		this.transformer = transformer;
		this.inInputPort = inInputPort;
		this.interpreter = interpreter;
		this.docBuilder = docBuilder;

		transformer.setOutputProperty( OutputKeys.ENCODING, "utf-8" );
		transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
		transformer.setOutputProperty( OutputKeys.INDENT, "no" );
	}

	private static Element getFirstElement( Element element, String name )
		throws IOException {
		NodeList children = element.getElementsByTagName( name );
		if( children.getLength() < 1 ) {
			throw new IOException( "Could not find element " + name );
		}

		return (Element) children.item( 0 );
	}

	private void navigateValue( Value value, Element element )
		throws IOException {
		NodeList contents = element.getChildNodes();
		if( contents.getLength() < 1 ) {
			throw new IOException( "empty value node" );
		}
		Node contentNode = contents.item( 0 );
		if( contentNode.getNodeType() != Node.ELEMENT_NODE ) {
			throw new IOException( "a value node may contain only one sub-element" );
		}

		Element content = (Element) contentNode;
		String name = content.getNodeName();
		switch( name ) {
		case "array":
			Value currentValue;
			ValueVector vec = value.getChildren( ARRAY_KEY );
			Element data = getFirstElement( content, "data" );
			NodeList dataChildren = data.getElementsByTagName( "value" );
			for( int i = 0; i < dataChildren.getLength(); i++ ) {
				Element member = (Element) dataChildren.item( i );
				if( !member.getParentNode().equals( data ) ) {
					continue; // not a direct child
				}
				currentValue = Value.create();
				navigateValue( currentValue, member );
				vec.add( currentValue );
			}
			break;
		case "struct":
			NodeList members = content.getElementsByTagName( "member" );
			for( int i = 0; i < members.getLength(); i++ ) {
				Element member = (Element) members.item( i );
				if( !member.getParentNode().equals( content ) ) {
					continue; // not a direct child
				}
				Element valueNode = getFirstElement( member, "value" );
				navigateValue( value.getNewChild( getFirstElement( member, "name" ).getTextContent() ), valueNode );
			}
			break;
		case "string":
			value.setValue( content.getTextContent() );
			break;
		case "int":
		case "i4":
			try {
				value.setValue( Integer.parseInt( content.getTextContent() ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
			break;
		case "double":
			try {
				value.setValue( Double.parseDouble( content.getTextContent() ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
			break;
		case "boolean":
			try {
				value.setValue( Integer.parseInt( content.getTextContent() ) != 0 );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
			break;
		case "base64":
			value.setValue( new ByteArray( Base64.getDecoder().decode( content.getTextContent() ) ) );
			break;
		default:
			// parse everything else as string (including <dateTime.iso8601>)
			value.setValue( content.getTextContent() );
			break;
		}
	}

	private void documentToValue( Value value, Document document )
		throws IOException {
		NodeList children = document.getDocumentElement().getElementsByTagName( "params" );
		if( children.getLength() < 1 ) {
			return;
		}
		NodeList params = ((Element) children.item( 0 )).getElementsByTagName( "param" );
		ValueVector paramsValueVector = value.getChildren( PARAMS_KEY );
		for( int i = 0; i < params.getLength(); i++ ) {
			Element param = (Element) params.item( i );
			Value paramValue = Value.create();
			navigateValue( paramValue, getFirstElement( param, "value" ) );
			paramsValueVector.add( paramValue );
		}
	}

	private void valueToDocument(
		Value value,
		Node node,
		Document doc ) {
		Element v = doc.createElement( "value" );

		// node value creation in case the contents is a value
		if( value.isInt() ) {

			Element i = doc.createElement( "int" );
			i.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( i );
			node.appendChild( v );
		} else if( value.isString() ) {

			Element s = doc.createElement( "string" );
			s.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( s );
			node.appendChild( v );
		} else if( value.isDouble() ) {

			Element d = doc.createElement( "double" );
			d.appendChild( doc.createTextNode( value.strValue() ) );
			v.appendChild( d );
			node.appendChild( v );
		} else if( value.isBool() ) {

			Element b = doc.createElement( "boolean" );
			b.appendChild( doc.createTextNode( value.boolValue() ? "1" : "0" ) );
			v.appendChild( b );
			node.appendChild( v );
		} else if( value.isByteArray() ) {

			Element b = doc.createElement( "base64" );
			b.appendChild(
				doc.createTextNode( Base64.getEncoder().encodeToString( value.byteArrayValue().getBytes() ) ) );
			v.appendChild( b );
			node.appendChild( v );
		} else if( value.hasChildren( ARRAY_KEY ) ) {
			// array creation

			Element a = doc.createElement( "array" );
			Element d = doc.createElement( "data" );
			for( int i = 0; i < value.getChildren( ARRAY_KEY ).size(); i++ ) {
				valueToDocument( value.getChildren( ARRAY_KEY ).get( i ), d, doc );
			}
			a.appendChild( d );
			v.appendChild( a );
			node.appendChild( v );
		} else if( value.hasChildren() ) {
			Element st = doc.createElement( "struct" );
			for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
				if( !entry.getKey().startsWith( "@" ) ) {
					Element m = doc.createElement( "member" );
					Element n = doc.createElement( "name" );
					n.appendChild( doc.createTextNode( entry.getKey() ) );
					m.appendChild( n );
					for( Value val : entry.getValue() ) {
						valueToDocument( val, m, doc );
					}
					st.appendChild( m );
				}
			}
			v.appendChild( st );
			node.appendChild( v );
		}
	}

	@Override
	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		Document doc = docBuilder.newDocument();
		// root element <methodCall>
		String rootName = "methodCall";
		if( received ) {
			// We're responding to a request
			rootName = "methodResponse";
		}

		Element root = doc.createElement( rootName );
		doc.appendChild( root );

		if( !received ) {
			// element <methodName>
			Element methodName;
			methodName = doc.createElement( "methodName" );
			Value aliases = getParameterFirstValue( "aliases" );
			String alias;
			if( aliases.hasChildren( message.operationName() ) ) {
				alias = aliases.getFirstChild( message.operationName() ).strValue();
			} else {
				alias = message.operationName();
			}
			methodName.appendChild( doc.createTextNode( alias ) );
			inputId = message.operationName();
			root.appendChild( methodName );
		}

		if( message.isFault() ) {
			FaultException f = message.fault();
			Element fault = doc.createElement( "fault" );
			Element v = doc.createElement( "value" );
			Element s = doc.createElement( "struct" );
			Element m1 = doc.createElement( "member" );
			Element n1 = doc.createElement( "name" );
			Element v1 = doc.createElement( "value" );
			Element i1 = doc.createElement( "int" );
			Element m2 = doc.createElement( "member" );
			Element n2 = doc.createElement( "name" );
			Element v2 = doc.createElement( "value" );
			Element i2 = doc.createElement( "string" );
			n1.setTextContent( "faultCode" );
			i1.setTextContent( "0" ); // Jolie generates always zero code faults
			n2.setTextContent( "faultString" );
			// the XML-RPC specification allows us only to set this value
			i2.setTextContent( f.value().strValue() );
			v1.appendChild( i1 );
			v2.appendChild( i2 );
			m1.appendChild( n1 );
			m1.appendChild( v1 );
			m2.appendChild( n2 );
			m2.appendChild( v2 );
			s.appendChild( m1 );
			s.appendChild( m2 );
			v.appendChild( s );
			fault.appendChild( v );
			root.appendChild( fault );

		} else if( message.value().hasChildren( PARAMS_KEY ) ) {
			// params exist
			Element params = doc.createElement( "params" );
			for( int i = 0; i < message.value().getChildren( PARAMS_KEY ).size(); i++ ) {
				Element p = doc.createElement( "param" );
				valueToDocument( message.value().getChildren( PARAMS_KEY ).get( i ), p, doc );
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
		} catch( TransformerException e ) {
			throw new IOException( e );
		}
		ByteArray content = new ByteArray( tmpStream.toByteArray() );

		final StringBuilder httpMessage = new StringBuilder();

		if( received ) {
			// We're responding to a request
			httpMessage.append( "HTTP/1.1 200 OK" ).append( HttpUtils.CRLF )
				.append( "Server: Jolie" ).append( HttpUtils.CRLF );

			received = false;
		} else {
			// We're sending a notification or a solicit
			String path = uri.getRawPath();
			if( path == null || path.length() == 0 ) {
				path = "*";
			}
			httpMessage.append( "POST " ).append( path ).append( " HTTP/1.1" ).append( HttpUtils.CRLF )
				.append( "User-Agent: Jolie" ).append( HttpUtils.CRLF )
				.append( "Host: " ).append( uri.getHost() ).append( HttpUtils.CRLF );

			if( checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true ) ) {
				String requestCompression = getStringParameter( HttpUtils.Parameters.REQUEST_COMPRESSION );
				if( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
					encoding = requestCompression;
					httpMessage.append( "Accept-Encoding: " ).append( encoding ).append( HttpUtils.CRLF );
				} else {
					httpMessage.append( "Accept-Encoding: gzip, deflate" ).append( HttpUtils.CRLF );
				}
			}
		}

		if( !checkBooleanParameter( HttpUtils.Parameters.KEEP_ALIVE, true ) ) {
			if( inInputPort && received )
				channel().setToBeClosed( true ); // we may do this only in input (server) mode
			httpMessage.append( "Connection: close" ).append( HttpUtils.CRLF );
		}

		if( encoding != null && checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true ) ) {
			content = HttpUtils.encode( encoding, content, httpMessage );
		}

		httpMessage.append( "Content-Type: text/xml; charset=utf-8" ).append( HttpUtils.CRLF )
			.append( "Content-Length: " ).append( content.size() ).append( HttpUtils.CRLF ).append( HttpUtils.CRLF );

		if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
			interpreter.logInfo( new StringBuilder( "[XMLRPC debug] Sending:\n" ).append( httpMessage.toString() )
				.append( content.toString( "utf-8" ) ) );
		}

		ostream.write( httpMessage.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		ostream.write( content.getBytes() );
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	@Override
	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException {
		HttpParser parser = new HttpParser( istream );
		HttpMessage message = parser.parse();
		String charset = HttpUtils.getResponseCharset( message );
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		CommMessage retVal = null;
		FaultException fault = null;
		Value value = Value.create();
		Document doc = null;

		if( inInputPort && message.type() != HttpMessage.Type.POST ) {
			throw new UnsupportedMethodException( "Only HTTP method POST allowed!", Method.POST );
		}

		encoding = message.getProperty( "accept-encoding" );

		if( message.size() > 0 ) {
			if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
				interpreter.logInfo( new StringBuilder( "[XMLRPC debug] Receiving:\n" )
					.append( HttpUtils.getHttpBody( message, charset ) ) );
			}

			try {
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				src.setEncoding( charset );
				doc = docBuilder.parse( src );
				if( message.isResponse() ) {
					// test if the message contains a fault
					try {
						Element faultElement = getFirstElement( doc.getDocumentElement(), "fault" );
						Element struct = getFirstElement( getFirstElement( faultElement, "value" ), "struct" );
						NodeList members = struct.getChildNodes();
						if( members.getLength() != 2 || members.item( 1 ).getNodeType() != Node.ELEMENT_NODE ) {
							throw new IOException( "Malformed fault data" );
						}
						Element faultMember = (Element) members.item( 1 );
						// Element valueElement = getFirstElement( getFirstElement( struct, "value" ), "string" );
						String faultName = getFirstElement( faultMember, "name" ).getTextContent();
						String faultString =
							getFirstElement( getFirstElement( faultMember, "value" ), "string" ).getTextContent();
						fault = new FaultException( faultName, Value.create( faultString ) );
					} catch( IOException e ) {
						documentToValue( value, doc );
					}
				} else {
					documentToValue( value, doc );
				}
			} catch( SAXException pce ) {
				throw new IOException( pce );
			}

			if( message.isResponse() ) {
				// fault = new FaultException( "InternalServerError", "" );
				// TODO support resourcePath
				retVal = new CommMessage( CommMessage.GENERIC_REQUEST_ID, inputId, "/", value, fault );
			} else {
				// TODO support resourcePath
				String opname = doc.getDocumentElement().getFirstChild().getTextContent();
				retVal = new CommMessage( CommMessage.GENERIC_REQUEST_ID, opname, "/", value, fault );

			}
		}

		received = true;
		return retVal;
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException {
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}
}
