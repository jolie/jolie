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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@AndJarDeps({"jolie-xml.jar"})
public class XmlUtils extends JavaService
{
	private final DocumentBuilderFactory documentBuilderFactory;
	private final TransformerFactory transformerFactory;

	public XmlUtils()
	{
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.transformerFactory = TransformerFactory.newInstance();
	}

	public String valueToXml( Value request )
		throws FaultException
	{
		try {
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			String root = request.getFirstChild( "root" ).strValue();
			jolie.xml.XmlUtils.valueToDocument(
				request.getFirstChild( root ),
				root,
				doc
			);
			Transformer t = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult( writer );
			t.transform( new DOMSource( doc ), result );
			return writer.toString();
		} catch( ParserConfigurationException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		} catch( TransformerConfigurationException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		} catch( TransformerException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		}
	}

	public Value xmlToValue( Value request )
		throws FaultException
	{
		try {
			Value result = Value.create();
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src;
			if ( request.isByteArray() ) {
				src = new InputSource( new ByteArrayInputStream( request.byteArrayValue().getBytes() ) );
			} else {
				src = new InputSource( new StringReader( request.strValue() ) );
			}

			Document doc = builder.parse( src );
			boolean includeAttributes = false;
			if ( request.hasChildren( "options" ) ){
				if ( request.getFirstChild( "options" ).hasChildren( "includeAttributes" ) ){
					includeAttributes = request.getFirstChild( "options" ).getFirstChild( "includeAttributes" ).boolValue();
				}
			}
			jolie.xml.XmlUtils.documentToValue( doc, result, includeAttributes );
            return result;
		} catch( ParserConfigurationException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		} catch( SAXException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		} catch( IOException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		}
	}

	public String transform( Value request )
		throws FaultException
	{
		try {
			StreamSource source = new StreamSource( new StringReader( request.getFirstChild( "source" ).strValue() ) );
			Transformer t = transformerFactory.newTransformer( new StreamSource( new StringReader( request.getFirstChild( "xslt" ).strValue() ) ) );
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
