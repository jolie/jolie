/***************************************************************************
 *   Copyright (C) 2010-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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


package joliex.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import joliex.storage.types.LoadRequest;
import joliex.storage.types.SaveRequest;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps( { "jolie-xml.jar" } )
public class XmlStorage extends AbstractStorageService {
	private File xmlFile = null;
	private Charset charset = null;
	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	public XmlStorage() {
		documentBuilderFactory.setIgnoringElementContentWhitespace( true );
	}

	@RequestResponse
	public void connect( Value request )
		throws FaultException {
		try {
			xmlFile = new File( request.getFirstChild( "filename" ).strValue() );
			if( !xmlFile.exists() ) {
				if( !xmlFile.createNewFile() ) {
					throw new FileAlreadyExistsException( xmlFile.getAbsolutePath() );
				}
				valueToFile( Value.create() );
			}

			if( request.hasChildren( "charset" ) ) {
				charset = Charset.forName( request.getFirstChild( "charset" ).strValue() );
			}
		} catch( Exception e ) {
			throw new FaultException( "StorageFault", e.getMessage() );
		}
	}

	private Value valueFromFile()
		throws FaultException {
		Value value = Value.create();
		try {
			try( InputStream istream = new FileInputStream( xmlFile ) ) {
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				InputSource src = new InputSource( new InputStreamReader( istream ) );
				if( charset != null ) {
					src.setEncoding( charset.name() );
				}
				Document doc = builder.parse( src );
				jolie.xml.XmlUtils.documentToValue( doc, value, false );
			}
		} catch( Exception e ) {
			throw new FaultException( "StorageFault", e.getMessage() );
		}
		return value;
	}

	private void valueToFile( Value value )
		throws FaultException {
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			Document doc = builder.newDocument();
			jolie.xml.XmlUtils.valueToDocument( value, "storage", doc );
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			if( charset != null ) {
				transformer.setOutputProperty( OutputKeys.ENCODING, charset.name() );
			}
			try( Writer writer = new FileWriter( xmlFile ) ) {
				StreamResult result = new StreamResult( writer );
				transformer.transform( new DOMSource( doc ), result );
			}
		} catch( IOException | IllegalArgumentException | ParserConfigurationException | TransformerException e ) {
			throw new FaultException( "StorageFault", e.getMessage() );
		}
	}

	private void checkConnection()
		throws IOException {
		if( xmlFile == null ) {
			throw new IOException( "XML file not specified (maybe you forgot to call connect?)" );
		}
	}

	@RequestResponse
	@Override
	public Value load( LoadRequest request )
		throws FaultException {
		try {
			checkConnection();
			return valueFromFile();
		} catch( IOException e ) {
			throw new FaultException( "StorageFault", e.getMessage() );
		}
	}

	@RequestResponse
	@Override
	public void save( SaveRequest request )
		throws FaultException {
		try {
			checkConnection();
			valueToFile( request.value() );
		} catch( IOException e ) {
			throw new FaultException( "StorageFault", e.getMessage() );
		}
	}
}
