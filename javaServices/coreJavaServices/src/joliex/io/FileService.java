/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Base64;
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
import jolie.Interpreter;
import jolie.jap.JapURLConnection;
import jolie.js.JsUtils;
import jolie.runtime.AndJarDeps;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.typing.Type;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps( { "jolie-xml.jar", "xsom.jar", "jolie-js.jar", "json_simple.jar" } )
public class FileService extends JavaService
{
	private final static Pattern FILE_KEYWORD_PATTERN = Pattern.compile( "(#+)file\\s+(.*)" );
	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private FileTypeMap fileTypeMap = FileTypeMap.getDefaultFileTypeMap();

	public FileService()
	{
		super();
		documentBuilderFactory.setIgnoringElementContentWhitespace( true );
	}

	@RequestResponse
	public String convertFromBinaryToBase64Value( Value value )
	{
		Interpreter.getInstance().logWarning( "convertFromBinaryToBase64Value@FileService()() became rawToBase64@Converter()()" );
		byte[] buffer = value.byteArrayValue().getBytes();
		
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString( buffer );
	}

	@RequestResponse
	public ByteArray convertFromBase64ToBinaryValue( Value value )
		throws FaultException
	{
		Interpreter.getInstance().logWarning( "convertFromBase64ToBinaryValue@FileService()() became base64ToRaw@Converter()()" );
		String stringValue = value.strValue();
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] supportArray = decoder.decode( stringValue );
		return new ByteArray( supportArray );
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
		byte[] buffer = new byte[ (int) size ];
		istream.read( buffer );
		Base64.Encoder encoder = Base64.getEncoder();
		value.setValue( encoder.encodeToString( buffer ) );
	}

	private static void readBinaryIntoValue( InputStream istream, long size, Value value )
		throws IOException
	{
		byte[] buffer = new byte[ (int) size ];
		istream.read( buffer );
		value.setValue( new ByteArray( buffer ) );
	}

	private static void readJsonIntoValue( InputStream istream, Value value, Charset charset, boolean strictEncoding )
		throws IOException
	{
		InputStreamReader isr;
		if ( charset == null ) {
			// UTF-8 is JSON's default charset: https://tools.ietf.org/html/rfc7159#section-8.1
			isr = new InputStreamReader( istream, "UTF-8" );
		} else {
			isr = new InputStreamReader( istream, charset );
		}
		JsUtils.parseJsonIntoValue( isr, value, strictEncoding );
	}

	private void readXMLIntoValue( InputStream istream, Value value, Charset charset )
		throws IOException
	{
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			if ( charset != null ) {
				src.setEncoding( charset.name() );
			}
			Document doc = builder.parse( src );
			value = value.getFirstChild( doc.getDocumentElement().getNodeName() );
			jolie.xml.XmlUtils.documentToValue( doc, value );
		} catch( ParserConfigurationException | SAXException e ) {
			throw new IOException( e );
		}
	}

	private void readXMLIntoValueForStoring( InputStream istream, Value value, Charset charset )
		throws IOException
	{
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			if ( charset != null ) {
				src.setEncoding( charset.name() );
			}
			Document doc = builder.parse( src );
			value = value.getFirstChild( doc.getDocumentElement().getNodeName() );
			jolie.xml.XmlUtils.storageDocumentToValue( doc, value );
		} catch( ParserConfigurationException | SAXException e ) {
			throw new IOException( e );
		}
	}

	private static void readTextIntoValue( InputStream istream, long size, Value value, Charset charset )
		throws IOException
	{
		byte[] buffer = new byte[ (int) size ];
		int len = istream.read( buffer );
		if ( len < 0 ) {
			len = 0; // EOF handled as empty string
		}
		if ( charset == null ) {
			value.setValue( new String( buffer, 0, len ) );
		} else {
			value.setValue( new String( buffer, 0, len, charset ) );
		}
	}

	private void readPropertiesFile( InputStream istream, Value value, Charset charset )
		throws IOException
	{
		Properties properties = new Properties();
		if ( charset == null ) {
			properties.load( new InputStreamReader( istream ) );
		} else {
			properties.load( new InputStreamReader( istream, charset ) );
		}
		Enumeration< String > names = (Enumeration< String >) properties.propertyNames();
		String name;
		String propertyValue;
		Matcher matcher;
		while( names.hasMoreElements() ) {
			name = names.nextElement();
			propertyValue = properties.getProperty( name );
			matcher = FILE_KEYWORD_PATTERN.matcher( propertyValue );
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

	private static void __copyDir( File src, File dest ) throws FileNotFoundException, IOException
	{
		if ( src.isDirectory() ) {
			if ( !dest.exists() ) {
				dest.mkdir();
			}
			String[] files = src.list();
			for( String file : files ) {
				File fileSrc = new File( src, file );
				File fileDest = new File( dest, file );
				__copyDir( fileSrc, fileDest );
			}
		} else {
			try ( // copy files
				FileInputStream inStream = new FileInputStream( src );
				FileOutputStream outStream = new FileOutputStream( dest );
			) {
				byte[] buffer = new byte[ 4096 ];
				int length;
				while( (length = inStream.read( buffer )) > 0 ) {
					outStream.write( buffer, 0, length );
				}
			}
		}
	}

	@RequestResponse
	public Value copyDir( Value request ) throws FaultException
	{
		Value retValue = Value.create();
		retValue.setValue( true );
		String fromDirName = request.getFirstChild( "from" ).strValue();
		String toDirName = request.getFirstChild( "to" ).strValue();
		File fromDir = new File( fromDirName );
		File toDir = new File( toDirName );
		try {
			__copyDir( fromDir, toDir );
		} catch( FileNotFoundException e ) {
			throw new FaultException( "FileNotFound" );
		} catch( IOException e ) {
			throw new FaultException( "IOException" );
		}
		return retValue;
	}

	@RequestResponse
	public Value readFile( Value request )
		throws FaultException
	{
		Value filenameValue = request.getFirstChild( "filename" );

		Value retValue = Value.create();
		String format = request.getFirstChild( "format" ).strValue();
		Charset charset = null;
		Value formatValue = request.getFirstChild( "format" );
		if ( formatValue.hasChildren( "charset" ) ) {
			charset = Charset.forName( formatValue.getFirstChild( "charset" ).strValue() );
		}

		final File file = new File( filenameValue.strValue() );
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
						JapURLConnection jarConn = (JapURLConnection) conn;
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

			// istream = new BufferedInputStream( istream );

			try {
				switch( format ) {
					case "base64":
						readBase64IntoValue( istream, size, retValue );
						break;
					case "binary":
						readBinaryIntoValue( istream, size, retValue );
						break;
					case "xml":
						istream = new BufferedInputStream( istream );
						readXMLIntoValue( istream, retValue, charset );
						break;
					case "xml_store":
						istream = new BufferedInputStream( istream );
						readXMLIntoValueForStoring( istream, retValue, charset );
						break;
					case "properties":
						istream = new BufferedInputStream( istream );
						readPropertiesFile( istream, retValue, charset );
						break;
					case "json":
						istream = new BufferedInputStream( istream );
						boolean strictEncoding = false;
						if ( request.getFirstChild( "format" ).hasChildren( "json_encoding" ) ) {
							if ( request.getFirstChild( "format" ).getFirstChild( "json_encoding" ).strValue().equals( "strict" ) ) {
								strictEncoding = true;
							}
						}	readJsonIntoValue( istream, retValue, charset, strictEncoding );
						break;
					default:
						readTextIntoValue( istream, size, retValue, charset );
						break;
				}
			} finally {
				istream.close();
			}
		} catch( FileNotFoundException e ) {
			throw new FaultException( "FileNotFound", e );
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}

		return retValue;
	}

	@RequestResponse
	public Boolean exists( String filename )
	{
		return new File( filename ).exists();
	}

	@RequestResponse
	public Boolean mkdir( String directory )
	{
		return new File( directory ).mkdirs();
	}

	@RequestResponse
	public String getMimeType( String filename )
		throws FaultException
	{
		File file = new File( filename );
		if ( file.exists() == false ) {
			throw new FaultException( "FileNotFound", filename );
		}
		return fileTypeMap.getContentType( file );
	}

	@RequestResponse
	public String getServiceDirectory()
		throws FaultException
	{
		String dir = null;
		try {
			dir = interpreter().programDirectory().getCanonicalPath();
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
		if ( dir == null || dir.isEmpty() ) {
			dir = ".";
		}

		return dir;
	}

	@RequestResponse
	public String getFileSeparator()
	{
		return jolie.lang.Constants.fileSeparator;
	}

	private void writeXML(
		File file, Value value,
		boolean append,
		String schemaFilename,
		String doctypeSystem,
		String encoding,
		boolean indent )
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
					doc );
			} else {
				jolie.xml.XmlUtils.valueToDocument(
					value.getFirstChild( rootName ),
					rootName,
					doc,
					type );
			}
			Transformer transformer = transformerFactory.newTransformer();
			if ( indent ) {
				transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			} else {
				transformer.setOutputProperty( OutputKeys.INDENT, "no" );
			}

			if ( doctypeSystem != null ) {
				transformer.setOutputProperty( "doctype-system", doctypeSystem );
			}

			if ( encoding != null ) {
				transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
			}

			try( Writer writer = new FileWriter( file, append ) ) {
				StreamResult result = new StreamResult( writer );
				transformer.transform( new DOMSource( doc ), result );
			}
		} catch( ParserConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerConfigurationException e ) {
			throw new IOException( e );
		} catch( TransformerException e ) {
			throw new IOException( e );
		}
	}

	private void writeStorageXML( File file, Value value, String encoding, boolean indent )
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
				doc );
			Transformer transformer = transformerFactory.newTransformer();
			if ( indent ) {
				transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			} else {
				transformer.setOutputProperty( OutputKeys.INDENT, "no" );
			}
			if ( encoding != null ) {
				transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
			}
			try( Writer writer = new FileWriter( file, false ) ) {
				StreamResult result = new StreamResult( writer );
				transformer.transform( new DOMSource( doc ), result );
			}
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
		try( FileOutputStream os = new FileOutputStream( file, append ) ) {
			os.write( value.byteArrayValue().getBytes() );
			os.flush();
		}
	}

	private static void writeText( File file, Value value, boolean append, String encoding )
		throws IOException
	{
		OutputStreamWriter writer;
		if ( encoding != null ) {
			writer = new OutputStreamWriter( new FileOutputStream( file, append ), encoding );
		} else {
			writer = new FileWriter( file, append );
		}
		writer.write( value.strValue() );
		writer.flush();
		writer.close();
	}

	private static void writeJson( File file, Value value, boolean append, String encoding )
		throws IOException
	{
		StringBuilder json = new StringBuilder();
		JsUtils.valueToJsonString( value, true, Type.UNDEFINED, json );

		OutputStreamWriter writer;
		if ( encoding != null ) {
			writer = new OutputStreamWriter( new FileOutputStream( file, append ), encoding );
		} else {
			// UTF-8 is JSON's default charset: https://tools.ietf.org/html/rfc7159#section-8.1
			writer = new OutputStreamWriter( new FileOutputStream( file, append ), "UTF-8" );
		}
		writer.write( json.toString() );
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
		String encoding = null;
		if ( request.getFirstChild( "format" ).hasChildren( "encoding" ) ) {
			encoding = request.getFirstChild( "format" ).getFirstChild( "encoding" ).strValue();
		}

		try {
			if ( "text".equals( format ) ) {
				writeText( file, content, append, encoding );
			} else if ( "binary".equals( format ) ) {
				writeBinary( file, content, append );
			} else if ( "xml".equals( format ) ) {
				String schemaFilename = null;
				if ( request.getFirstChild( "format" ).hasChildren( "schema" ) ) {
					schemaFilename = request.getFirstChild( "format" ).getFirstChild( "schema" ).strValue();
				}
				boolean indent = false;
				if ( request.getFirstChild( "format" ).hasChildren( "indent" ) ) {
					indent = request.getFirstChild( "format" ).getFirstChild( "indent" ).boolValue();
				}

				String doctypePublic = null;
				if ( request.getFirstChild( "format" ).hasChildren( "doctype_system" ) ) {
					doctypePublic = request.getFirstChild( "format" ).getFirstChild( "doctype_system" ).strValue();
				}
				writeXML( file, content, append, schemaFilename, doctypePublic, encoding, indent );
			} else if ( "xml_store".equals( format ) ) {
				boolean indent = false;
				if ( request.getFirstChild( "format" ).hasChildren( "indent" ) ) {
					indent = request.getFirstChild( "format" ).getFirstChild( "indent" ).boolValue();
				}
				writeStorageXML( file, content, encoding, indent );
			} else if ( "json".equals( format ) ) {
				writeJson( file, content, append, encoding );
			} else if ( format.isEmpty() ) {
				if ( content.isByteArray() ) {
					writeBinary( file, content, append );
				} else {
					writeText( file, content, append, encoding );
				}
			}
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public Boolean delete( Value request )
	{
		String filename = request.strValue();
		boolean isRegex = request.getFirstChild( "isRegex" ).intValue() > 0;
		boolean ret = true;
		if ( isRegex ) {
			File dir = new File( filename ).getAbsoluteFile().getParentFile();
			String[] files = dir.list( new ListFilter( filename, false ) );
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
	public Boolean deleteDir( Value request )
	{
		return __deleteDir( new File( request.strValue() ) );
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

	@RequestResponse
	public Value getSize( Value request )
	{
		Value retValue = Value.create();

		retValue.setValue( request.byteArrayValue().size() );

		return retValue;
	}

	@RequestResponse
	public Value list( Value request )
	{
		final File dir = new File( request.getFirstChild( "directory" ).strValue() );
                boolean fileInfo = false;
                if ( request.getFirstChild( "info" ).isDefined() ) {
                    fileInfo = request.getFirstChild( "info" ).boolValue();
                }
		final String regex;
		if ( request.hasChildren( "regex" ) ) {
			regex = request.getFirstChild( "regex" ).strValue();
		} else {
			regex = ".*";
		}

		boolean dirsOnly;
		if ( request.hasChildren( "dirsOnly" ) ) {
			dirsOnly = request.getFirstChild( "dirsOnly" ).boolValue();
		} else {
			dirsOnly = false;
		}

		final String[] files = dir.list( new ListFilter( regex, dirsOnly ) );
		
		if ( request.hasChildren( "order" ) ) {
			Value order = request.getFirstChild( "order" );
			
			if ( files != null && order.hasChildren( "byname" ) && order.getFirstChild( "byname" ).boolValue() ) {
				Arrays.sort( files );
			}
		}

		Value response = Value.create();
		if ( files != null ) {
			ValueVector results = response.getChildren( "result" );
			for( String file : files ) {
                                Value fileValue = Value.create( file );
                                if( fileInfo ) {
                                    Value info = fileValue.getFirstChild( "info" );
                                    File currFile = new File ( dir + File.separator + file );
                                    info.getFirstChild( "lastModified" ).setValue( currFile.lastModified() );
                                    info.getFirstChild( "size" ).setValue( currFile.length() );
                                    info.getFirstChild( "absolutePath" ).setValue( currFile.getAbsolutePath() );
                                    info.getFirstChild( "isHidden" ).setValue( currFile.isHidden() );
                                    info.getFirstChild( "isDirectory" ).setValue( currFile.isDirectory() );
                                    
                                }
				results.add( fileValue );
			}
		}
		return response;
	}

	@RequestResponse
	public Value isDirectory( Value request )
	{
		File dir = new File( request.strValue() );
		Value response = Value.create();
		response.setValue( dir.isDirectory() );
		return response;

	}

	private static boolean __deleteDir( File file )
	{
		if ( file.isDirectory() ) {
			String[] children = file.list();
			for( String children1 : children ) {
				__deleteDir( new File( file, children1 ) );
			}
		}
		return file.delete();
	}

	private static class ListFilter implements FilenameFilter
	{
		private final Pattern pattern;
		private final boolean dirsOnly;

		public ListFilter( String regex, boolean dirsOnly )
		{
			this.pattern = Pattern.compile( regex );
			this.dirsOnly = dirsOnly;
		}

		@Override
		public boolean accept( File directory, String filename )
		{
			File file = new File( directory.getAbsolutePath() + File.separator + filename );
			return pattern.matcher( filename ).matches() && (!dirsOnly || file.isDirectory());
		}
	}

	@RequestResponse
	public Value toAbsolutePath( Value request ) throws FaultException
	{
		Value response = Value.create();
		String fileName = request.strValue();

		Path absolutePath = null;
		
		try {
			absolutePath = Paths.get( fileName ).toAbsolutePath().normalize();
		} catch ( InvalidPathException invalidPathException ) {
			throw new FaultException( invalidPathException );
		}

		response.setValue( absolutePath.toString() );

		return response;
	}

	@RequestResponse
	public Value getParentPath( Value request ) throws FaultException
	{
		Value response = Value.create();
		String fileName = request.strValue();

		Path parent = null;

		try {
			parent = Paths.get( fileName ).getParent();
		} catch ( InvalidPathException invalidPathException ) {
			throw new FaultException( invalidPathException );
		}

		if ( parent == null ) {
			throw new FaultException( new InvalidPathException( fileName, "Path has no parent" ) );
		}
		
		response.setValue( parent.toString() );

		return response;
	}
}
