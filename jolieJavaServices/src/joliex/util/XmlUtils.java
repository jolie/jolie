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
import jolie.net.CommMessage;
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
	final private DocumentBuilderFactory documentBuilderFactory;

	public XmlUtils()
	{
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
	}

	public CommMessage valueToXml( CommMessage request )
		throws FaultException
	{
		try {
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			String root = request.value().getFirstChild( "root" ).strValue();
			jolie.xml.XmlUtils.valueToDocument(
				request.value().getFirstChild( root ),
				root,
				doc
			);
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer t = tFactory.newTransformer();
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult( writer );
			t.transform( new DOMSource( doc ), result );
			return CommMessage.createResponse( request, Value.create( writer.toString() ) );
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

	public CommMessage xmlToValue( CommMessage request )
		throws FaultException
	{
		try {
			Value result = Value.create();
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src;
			if ( request.value().isByteArray() ) {
				src = new InputSource( new ByteArrayInputStream( request.value().byteArrayValue().getBytes() ) );
			} else {
				src = new InputSource( new StringReader( request.value().strValue() ) );
			}

			Document doc = builder.parse( src );
			jolie.xml.XmlUtils.documentToValue( doc, result );
			return CommMessage.createResponse( request, result );
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

	public CommMessage transform( CommMessage request )
		throws FaultException
	{
		try {
			StreamSource source = new StreamSource( new StringReader( request.value().getFirstChild( "source" ).strValue() ) );
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer t = tFactory.newTransformer( new StreamSource( new StringReader( request.value().getFirstChild( "xslt" ).strValue() ) ) );
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult( writer );
			t.transform( source, result );
			return CommMessage.createResponse( request, Value.create( writer.toString() ) );
		} catch( TransformerException e ) {
			e.printStackTrace();
			throw new FaultException( e );
		}
	}
}
