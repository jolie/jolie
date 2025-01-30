/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package joliex.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@AndJarDeps( { "jolie-xml.jar", "xsom.jar", "relaxngDatatype.jar" } )
public class XmlUtils extends JavaService {
	private final DocumentBuilderFactory documentBuilderFactory;
	private final TransformerFactory transformerFactory;

	public XmlUtils() {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.transformerFactory = TransformerFactory.newInstance();
	}

	public String valueToXml( Value request )
		throws FaultException {
		try {
			Transformer transformer;
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			Value value = request.getFirstChild( "root" );
			String rootNodeName = value.children().keySet().iterator().next();
			if( request.hasChildren( "rootNodeName" ) ) {
				rootNodeName = request.getFirstChild( "rootNodeName" ).strValue();
			} else {
				if( value.children().size() != 1 ) {
					throw new FaultException( "IllegalArgumentException", "Too many root nodes" );
				}
				value = value.getFirstChild( rootNodeName );
			}

			boolean isXmlStore = true;
			if( request.hasChildren( "plain" ) ) {
				isXmlStore = !request.getFirstChild( "plain" ).boolValue();
			}
			if( request.hasChildren( "isXmlStore" ) ) {
				isXmlStore = request.getFirstChild( "isXmlStore" ).boolValue();
			}

			boolean indent = false;
			if( request.hasChildren( "indent" ) ) {
				indent = request.getFirstChild( "indent" ).boolValue();
			}

			boolean isApplySchema = false;
			String schemaFilename = null;
			String encoding = null;
			String doctypeSystem = null;
			if( request.hasChildren( "applySchema" ) ) {
				isApplySchema = true;
				Value applySchema = request.getFirstChild( "applySchema" );
				schemaFilename = applySchema.getFirstChild( "schema" ).strValue();
				if( applySchema.hasChildren( "encoding" ) ) {
					encoding = applySchema.getFirstChild( "encoding" ).strValue();
				}
				if( applySchema.hasChildren( "doctypeSystem" ) ) {
					doctypeSystem = applySchema.getFirstChild( "doctypeSystem" ).strValue();
				}
			}

			if( !isXmlStore ) {
				if( isApplySchema ) {
					transformer = transformerFactory.newTransformer();
					jolie.xml.XmlUtils.configTransformer( transformer, encoding, doctypeSystem, indent );
					jolie.xml.XmlUtils.valueToDocument( value, doc, schemaFilename );
				} else {
					jolie.xml.XmlUtils.valueToDocument(
						value,
						rootNodeName,
						doc );
					transformer = transformerFactory.newTransformer();
					if( indent ) {
						transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
						transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
					} else {
						transformer.setOutputProperty( OutputKeys.INDENT, "no" );
					}
				}
			} else {
				jolie.xml.XmlUtils.valueToStorageDocument(
					value,
					rootNodeName,
					doc );
				transformer = transformerFactory.newTransformer();
				if( indent ) {
					transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
					transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
				} else {
					transformer.setOutputProperty( OutputKeys.INDENT, "no" );
				}
			}


			if( request.getFirstChild( "omitXmlDeclaration" ).boolValue() ) {
				transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
			}

			StringWriter outWriter = new StringWriter();
			StreamResult result = new StreamResult( outWriter );
			transformer.transform( new DOMSource( doc ), result );
			StringBuffer sb = outWriter.getBuffer();
			return sb.toString();

		} catch( ParserConfigurationException | IOException | TransformerException e ) {
			throw new FaultException( e );
		}
	}

	public Value xmlToValue( Value request )
		throws FaultException {
		try {
			Value result = Value.create();
			InputSource src;
			if( request.isByteArray() ) {
				src = new InputSource( new ByteArrayInputStream( request.byteArrayValue().getBytes() ) );
			} else {
				src = new InputSource( new StringReader( request.strValue() ) );
			}

			boolean includeAttributes = false;
			boolean skipMixedText = false;
			boolean includeRoot = false;
			boolean xmlStore = true;
			if( request.hasChildren( "options" ) ) {
				if( request.getFirstChild( "options" ).hasChildren() ) {
					xmlStore = false;
				}
				if( request.getFirstChild( "options" ).hasChildren( "includeRoot" ) ) {
					includeRoot = request.getFirstChild( "options" ).getFirstChild( "includeRoot" ).boolValue();
				}
				if( request.getFirstChild( "options" ).hasChildren( "includeAttributes" ) ) {
					includeAttributes =
						request.getFirstChild( "options" ).getFirstChild( "includeAttributes" ).boolValue();
				}
				if( request.getFirstChild( "options" ).hasChildren( "schemaUrl" ) ) {
					SchemaFactory schemaFactory = SchemaFactory.newInstance(
						request.getFirstChild( "options" ).hasChildren( "schemaLanguage" )
							? request.getFirstChild( "options" ).getFirstChild( "schemaLanguage" ).strValue()
							: XMLConstants.W3C_XML_SCHEMA_NS_URI );
					Schema schema = schemaFactory.newSchema( URI.create(
						request.getFirstChild( "options" ).getFirstChild( "schemaUrl" ).strValue() ).toURL() );
					documentBuilderFactory.setSchema( schema ); // set schema
				}
				if( request.getFirstChild( "options" ).hasChildren( "charset" ) ) {
					src.setEncoding( request.getFirstChild( "options" ).getFirstChild( "charset" ).strValue() );
				}
				if( request.getFirstChild( "options" ).hasChildren( "skipMixedText" ) ) {
					skipMixedText = request.getFirstChild( "options" ).getFirstChild( "skipMixedText" ).boolValue();
				}
			}
			if( request.hasChildren( "isXmlStore" ) ) {
				xmlStore = request.getFirstChild( "isXmlStore" ).boolValue();
			}

			if( !xmlStore ) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				dbFactory.setNamespaceAware( true );
				DocumentBuilder builder = dbFactory.newDocumentBuilder();
				Document doc = builder.parse( src );
				Value value = result;
				if( includeRoot ) {
					value = result.getFirstChild(
						doc.getDocumentElement().getLocalName() == null ? doc.getDocumentElement().getNodeName()
							: doc.getDocumentElement().getLocalName() );
					if( doc.getDocumentElement().getPrefix() != null ) {
						value.getFirstChild( jolie.xml.XmlUtils.PREFIX )
							.setValue( doc.getDocumentElement().getPrefix() );
					}
				}
				jolie.xml.XmlUtils.documentToValue( doc, value, includeAttributes, skipMixedText );
			} else {
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				Document doc = builder.parse( src );
				jolie.xml.XmlUtils.storageDocumentToValue( doc, result );
			}
			return result;
		} catch( ParserConfigurationException | IOException | SAXException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		} finally {
			documentBuilderFactory.setSchema( null ); // reset schema
		}
	}

	public String transform( Value request )
		throws FaultException {
		try {
			StreamSource source = new StreamSource( new StringReader( request.getFirstChild( "source" ).strValue() ) );
			Transformer t = transformerFactory
				.newTransformer( new StreamSource( new StringReader( request.getFirstChild( "xslt" ).strValue() ) ) );
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult( writer );
			t.transform( source, result );
			return writer.toString();
		} catch( TransformerException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		}
	}
}
