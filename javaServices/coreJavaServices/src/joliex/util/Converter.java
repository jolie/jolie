/**
 * *************************************************************************
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
 **************************************************************************
 */
package joliex.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.runtime.AndJarDeps;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@AndJarDeps({"jolie-xml.jar"})
public class Converter extends JavaService
{
	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	public Converter()
	{
		super();
		documentBuilderFactory.setIgnoringElementContentWhitespace( true );
	}

	@RequestResponse
	public String rawToBase64( Value value )
	{
		byte[] buffer = value.byteArrayValue().getBytes();
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString( buffer );
	}

	@RequestResponse
	public ByteArray base64ToRaw( Value value )
		throws FaultException
	{
		String stringValue = value.strValue();
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] supportArray = decoder.decode( stringValue );
		return new ByteArray( supportArray );
	}

	@RequestResponse
	public String rawToString( Value value )
		throws FaultException
	{
		byte[] buffer = value.byteArrayValue().getBytes();

		String charset = null;
		if ( value.hasChildren( "charset" ) ) {
			charset = value.getFirstChild( "charset" ).strValue();
		}
		try {
			if ( charset != null ) {
				return new String( buffer, charset );
			} else {
				return new String( buffer );
			}
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public ByteArray stringToRaw( Value value )
		throws FaultException
	{
		String str = value.strValue();

		String charset = null;
		if ( value.hasChildren( "charset" ) ) {
			charset = value.getFirstChild( "charset" ).strValue();
		}
		try {
			if ( charset != null ) {
				return new ByteArray( str.getBytes( charset ) );
			} else {
				return new ByteArray( str.getBytes() );
			}
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public String valueToXml( Value request ) throws FaultException
	{
		String schemaFilename = null;
		Value value = request.getFirstChild( "value" );
		if ( request.hasChildren( "schema" ) ) {
			schemaFilename = request.getFirstChild( "schema" ).strValue();
		}
		boolean indent = false;
		if ( request.hasChildren( "indent" ) ) {
			indent = request.getFirstChild( "indent" ).boolValue();
		}

		String doctypeSystem = null;
		if ( request.hasChildren( "doctype_system" ) ) {
			doctypeSystem = request.getFirstChild( "doctype_system" ).strValue();
		}

		String encoding = null;
		if ( request.hasChildren( "encoding" ) ) {
			encoding = request.getFirstChild( "encoding" ).strValue();
		}
		
		boolean xmlStore = true;
		if ( request.hasChildren( "xml_store" ) && !request.getFirstChild( "xml_store" ).boolValue() ) {
			xmlStore = false;
		}

		if ( value.children().isEmpty() ) {
			return new String(); // TODO: perhaps we should erase the content of the file before returning.
		}

		try {
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			Transformer transformer = null;
			if ( xmlStore ) {
				String rootName = value.children().keySet().iterator().next();
				jolie.xml.XmlUtils.valueToStorageDocument( value.getFirstChild( rootName ), rootName, doc );
				transformer = transformerFactory.newTransformer();
			} else {
				transformer = jolie.xml.XmlUtils.valueToDocument( value, doc, schemaFilename, indent, doctypeSystem, encoding );
			}
			

			StringWriter outWriter = new StringWriter();
			StreamResult result = new StreamResult( outWriter );
			transformer.transform( new DOMSource( doc ), result );
			StringBuffer sb = outWriter.getBuffer();
			return sb.toString();

		} catch( IOException | ParserConfigurationException | TransformerException e ) {
			throw new FaultException( "ConversionError", e.getMessage() );
		}

	}

	@RequestResponse
	public Value xmlStringToValue( Value request ) throws FaultException
	{
		String xmlString = request.getFirstChild( "xmlString" ).strValue();
		boolean xmlStore = true;
		if ( request.hasChildren( "xml_store" ) && !request.getFirstChild( "xml_store" ).boolValue() ) {
			xmlStore = false;
		}
		Charset charset = null;
		Value response = Value.create();
		if ( request.hasChildren( "charset" ) ) {
			charset = Charset.forName( request.getFirstChild( "charset" ).strValue() );
		}

		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			StringReader srd = new StringReader( xmlString );
			InputSource src = new InputSource( srd );
			if ( charset != null ) {
				src.setEncoding( charset.name() );
			}

			
			Document doc = builder.parse( src );
			Value value = response.getFirstChild( doc.getDocumentElement().getNodeName());
			if ( xmlStore ) {
				jolie.xml.XmlUtils.storageDocumentToValue( doc, value ); 
			} else {
				jolie.xml.XmlUtils.documentToValue( doc, value );
			}
			return response;
		} catch( ParserConfigurationException | SAXException | IOException e ) {
			throw new FaultException( "ConversionError", e.getMessage() );
		}
	}
}
