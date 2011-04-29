/***************************************************************************
 *   Copyright (C) 2008-2010 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.io;


import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.jap.JapURLConnection;
import jolie.runtime.AndJarDeps;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps({"jolie-xml.jar","xsom.jar"})
public class FileService extends JavaService
{
	private final static Pattern fileKeywordPattern = Pattern.compile( "(#+)file\\s+(.*)" );

	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private FileTypeMap fileTypeMap = FileTypeMap.getDefaultFileTypeMap();

	public FileService()
	{
		super();
		documentBuilderFactory.setIgnoringElementContentWhitespace( true );
	}

	@RequestResponse
	public void setMimeTypeFile( String filename )
		throws FaultException
	{
		try {
			fileTypeMap = new MimetypesFileTypeMap( filename );
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	private static void readBase64IntoValue( InputStream istream, long size, Value value )
		throws IOException
	{
		byte[] buffer = new byte[ (int)size ];
		istream.read( buffer );
		sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
		value.setValue( encoder.encode( buffer ) );
	}
	
	private static void readBinaryIntoValue( InputStream istream, long size, Value value )
		throws IOException
	{
		byte[] buffer = new byte[ (int)size ];
		istream.read( buffer );
		value.setValue( new ByteArray( buffer ) );
	}

	private void readXMLIntoValue( InputStream istream, Value value )
		throws IOException
	{
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			Document doc = builder.parse( src );
			value = value.getFirstChild( doc.getDocumentElement().getNodeName() );
			jolie.xml.XmlUtils.documentToValue( doc, value );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			throw new IOException( e );
		}
	}
	
	private void readXMLIntoValueForStoring( InputStream istream, Value value )
		throws IOException
	{
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			Document doc = builder.parse( src );
			value = value.getFirstChild( doc.getDocumentElement().getNodeName() );
			jolie.xml.XmlUtils.storageDocumentToValue( doc, value );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( SAXException e ) {
			throw new IOException( e );
		}
	}
	
	private static void readTextIntoValue( InputStream istream, long size, Value value, Charset charset )
		throws IOException
	{
		byte[] buffer = new byte[ (int)size ];
		istream.read( buffer );
		istream.close();
		if ( charset == null ) {
			value.setValue( new String( buffer ) );
		} else {
			value.setValue( new String( buffer, charset ) );
		}
	}

	private void readPropertiesFile( InputStream istream, Value value )
		throws IOException
	{
		Properties properties = new Properties();
		properties.load( new InputStreamReader( istream ) );
		Enumeration< String > names = (Enumeration< String >) properties.propertyNames();
		String name;
		String propertyValue;
		Matcher matcher;
		while( names.hasMoreElements() ) {
			name = names.nextElement();
			propertyValue = properties.getProperty( name );
			matcher = fileKeywordPattern.matcher( propertyValue );
			if ( matcher.matches() ) {
				if ( matcher.group( 1 ).length() > 1 ) { // The number of #
					propertyValue = propertyValue.substring( 1 );
				} else { // It's a #file directive
					// TODO: this is a bit of a hack. We should have a private
					// method for performing all the lookups of files into
					// JAPs, local directories etc. instead of calling readFile
					// again.
					Value request = Value.create();
					request.getFirstChild( "filename" ).setValue( matcher.group( 2 ) );
					request.getFirstChild( "format" ).setValue( "text" );
					try {
						propertyValue = readFile( request ).strValue();
					} catch( FaultException e ) {
						throw new IOException( e );
					}
				}
			}
			value.getFirstChild( name ).setValue( propertyValue );
		}
	}
	
	public Value readFile( Value request )
		throws FaultException
	{
		Value filenameValue = request.getFirstChild( "filename" );

		Value retValue = Value.create();
		String format = request.getFirstChild( "format" ).strValue();
		File file = new File( filenameValue.strValue() );
		InputStream istream = null;
		long size;
		try {
			if ( file.exists() ) {
				istream = new FileInputStream( file );
				size = file.length();
			} else {
				URL fileURL = interpreter().getClassLoader().findResource( filenameValue.strValue() );
				if ( fileURL != null && fileURL.getProtocol().equals( "jap" ) ) {
					URLConnection conn = fileURL.openConnection();
					if ( conn instanceof JapURLConnection ) {
						JapURLConnection jarConn = (JapURLConnection)conn;
						size = jarConn.getEntrySize();
						if ( size < 0 ) {
							throw new IOException( "File dimension is negative for file " + fileURL.toString() );
						}
						istream = jarConn.getInputStream();
					} else {
						throw new FileNotFoundException( filenameValue.strValue() );
					}
				} else {
					throw new FileNotFoundException( filenameValue.strValue() );
				}
			}
			
			istream = new BufferedInputStream( istream );

			try {
				if ( "base64".equals( format ) ) {
					readBase64IntoValue( istream, size, retValue );
				} else if ( "binary".equals( format ) ) {
					readBinaryIntoValue( istream, size, retValue );
				} else if ( "xml".equals( format ) ) {
					readXMLIntoValue( istream, retValue );
				} else if ( "xml_store".equals( format ) ) {
					readXMLIntoValueForStoring( istream, retValue );
				} else if ( "properties".equals( format ) ) {
					readPropertiesFile( istream, retValue );
				} else {
					Charset charset = null;
					Value formatValue = request.getFirstChild( "format" );
					if ( formatValue.hasChildren( "charset" ) ) {
						charset = Charset.forName( formatValue.getFirstChild( "charset" ).strValue() );
					}
					readTextIntoValue( istream, size, retValue, charset );
				}
			} finally {
				istream.close();
			}
		} catch( FileNotFoundException e ) {
			throw new FaultException( "FileNotFound" );
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}

		return retValue;
	}

	public Integer exists( String filename )
	{
		return ( new File( filename ).exists() ) ? 1 : 0;
	}
	
	public Integer mkdir( String directory )
	{
		return ( new File( directory ).mkdirs() ) ? 1 : 0;
	}

	public String getMimeType( String filename )
		throws FaultException
	{
		File file = new File( filename );
		if ( file.exists() == false ) {
			throw new FaultException( "FileNotFound" );
		}
		return fileTypeMap.getContentType( file );
	}
	
	public String getServiceDirectory()
	{
		String dir = null;
		try {
			dir = interpreter().programDirectory().getCanonicalPath();
		} catch( IOException e ) {
			e.printStackTrace();
		}
		if ( dir == null || dir.isEmpty() ) {
			dir = ".";
		}
		
		return dir;
	}
	
	public String getFileSeparator()
	{
		return jolie.lang.Constants.fileSeparator;
	}

	private void writeXML( File file, Value value, boolean append, String schemaFilename )
		throws IOException
	{
		if ( value.children().isEmpty() ) {
			return; // TODO: perhaps we should erase the content of the file before returning.
		}
		String rootName = value.children().keySet().iterator().next();
		try {
			XSType type = null;
			if ( schemaFilename != null ) {
				try {
					XSOMParser parser = new XSOMParser();
					parser.parse( schemaFilename );
					XSSchemaSet schemaSet = parser.getResult();
					if ( schemaSet != null ) {
						type = schemaSet.getElementDecl( "", rootName ).getType();
					}
				} catch( SAXException e ) {
					throw new IOException( e );
				}
			}
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			
			if ( type == null ) {
				jolie.xml.XmlUtils.valueToDocument(
					value.getFirstChild( rootName ),
					rootName,
					doc
				);
			} else {
				jolie.xml.XmlUtils.valueToDocument(
					value.getFirstChild( rootName ),
					rootName,
					doc,
					type
				);
			}
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			Writer writer = new FileWriter( file, append );
			StreamResult result = new StreamResult( writer );
			transformer.transform( new DOMSource( doc ), result );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException e ) {
			throw new IOException( e );
		}
	}

	private void writeStorageXML( File file, Value value )
		throws IOException
	{
		if ( value.children().isEmpty() ) {
			return; // TODO: perhaps we should erase the content of the file before returning.
		}
		String rootName = value.children().keySet().iterator().next();
		try {
			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			jolie.xml.XmlUtils.valueToStorageDocument(
				value.getFirstChild( rootName ),
				rootName,
				doc
			);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "no" );
			Writer writer = new FileWriter( file, false );
			StreamResult result = new StreamResult( writer );
			transformer.transform( new DOMSource( doc ), result );
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException e ) {
			throw new IOException( e );
		}
	}

	private static void writeBinary( File file, Value value, boolean append )
		throws IOException
	{
		FileOutputStream os = new FileOutputStream( file, append );
		os.write( value.byteArrayValue().getBytes() );
		os.flush();
		os.close();
	}

	private static void writeText( File file, Value value, boolean append )
		throws IOException
	{
		FileWriter writer = new FileWriter( file, append );
		writer.write( value.strValue() );
		writer.flush();
		writer.close();
	}

	@RequestResponse
	public void writeFile( Value request )
		throws FaultException
	{
		boolean append = false;
		Value content = request.getFirstChild( "content" );
		String format = request.getFirstChild( "format" ).strValue();
		File file = new File( request.getFirstChild( "filename" ).strValue() );
		if ( request.getFirstChild( "append" ).intValue() > 0 ) {
			append = true;
		}

		try {
			if ( "text".equals( format ) ) {
				writeText( file, content, append );
			} else if ( "binary".equals( format ) ) {
				writeBinary( file, content, append );
			} else if ( "xml".equals( format ) ) {
				String schemaFilename = null;
				if ( request.getFirstChild( "format" ).hasChildren( "schema" ) ) {
					schemaFilename = request.getFirstChild( "format" ).getFirstChild( "schema" ).strValue();
				}
				writeXML( file, content, append, schemaFilename );
			} else if ( "xml_store".equals( format ) ) {
				writeStorageXML( file, content );
			} else if ( format.isEmpty() ) {
				if ( content.isByteArray() ) {
					writeBinary( file, content, append );
				} else {
					writeText( file, content, append );
				}
			}
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}
	
	public Boolean delete( Value request )
	{
		String filename = request.strValue();
		boolean isRegex = request.getFirstChild( "isRegex" ).intValue() > 0;
		boolean ret = true;
		if ( isRegex ) {
			File dir = new File( filename ).getAbsoluteFile().getParentFile();
			String[] files = dir.list( new ListFilter( filename ) );
			if ( files != null ) {
				for( String file : files ) {
					new File( file ).delete();
				}
			}
		} else {
			if ( new File( filename ).delete() == false ) {
				ret = false;
			}
		}
		return ret;
	}

	@RequestResponse
	public void rename( Value request )
		throws FaultException
	{
		String filename = request.getFirstChild( "filename" ).strValue();
		String toFilename = request.getFirstChild( "to" ).strValue();
		if ( new File( filename ).renameTo( new File( toFilename ) ) == false ) {
			throw new FaultException( "IOException" );
		}
	}
	
	public Value list( Value request )
	{
		File dir = new File( request.getFirstChild( "directory" ).strValue() );
		String[] files = dir.list( new ListFilter( request.getFirstChild( "regex" ).strValue() ) );
		Value response = Value.create();
		if ( files != null ) {
			ValueVector results = response.getChildren( "result" );
			for( String file : files ) {
				results.add( Value.create( file ) );
			}
		}
		return response;
	}
	
	private static class ListFilter implements FilenameFilter
	{
		final private Pattern pattern;
		public ListFilter( String regex )
		{
			this.pattern = Pattern.compile( regex );
		}

		public boolean accept( File file, String name )
		{
			return pattern.matcher( name ).matches();
		}
	}
}
