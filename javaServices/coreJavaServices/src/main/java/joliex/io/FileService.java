/*
 * Copyright (C) 2008-2024 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package joliex.io;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;
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

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps( { "jolie-xml.jar", "xsom.jar", "jolie-js.jar", "json-simple.jar", "javax.activation.jar",
	"snakeyaml-engine.jar" } )
public class FileService extends JavaService {
	private final static Pattern FILE_KEYWORD_PATTERN = Pattern.compile( "(#+)file\\s+(.*)" );
	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private FileTypeMap fileTypeMap = FileTypeMap.getDefaultFileTypeMap();

	public FileService()
		throws ParserConfigurationException {
		super();
		documentBuilderFactory.setIgnoringElementContentWhitespace( true );
		documentBuilderFactory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
	}

	@RequestResponse
	public String convertFromBinaryToBase64Value( Value value ) {
		Interpreter.getInstance()
			.logWarning( "convertFromBinaryToBase64Value@FileService()() became rawToBase64@Converter()()" );
		byte[] buffer = value.byteArrayValue().getBytes();

		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString( buffer );
	}

	@RequestResponse
	public ByteArray convertFromBase64ToBinaryValue( Value value )
		throws FaultException {
		Interpreter.getInstance()
			.logWarning( "convertFromBase64ToBinaryValue@FileService()() became base64ToRaw@Converter()()" );
		String stringValue = value.strValue();
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] supportArray = decoder.decode( stringValue );
		return new ByteArray( supportArray );
	}

	@RequestResponse
	public void setMimeTypeFile( String path )
		throws FaultException {
		try {
			final String url;
			if( path.startsWith( "jap:" ) || path.startsWith( "jar:" ) ) {
				url = path.substring( 0, 4 ) +
					new URI( path.substring( 4 ) ).normalize().toString();
			} else {
				url = path;
			}

			if( Files.exists( Paths.get( url ) ) ) {
				fileTypeMap = new MimetypesFileTypeMap( url );
			} else {
				try( InputStream mimeIS = new URL( url ).openStream() ) {
					fileTypeMap = new MimetypesFileTypeMap( mimeIS );
				}
			}
		} catch( IOException | URISyntaxException | InvalidPathException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	private static void readBase64IntoValue( InputStream istream, long size, Value value )
		throws IOException {
		byte[] buffer = new byte[ (int) size ];
		if( istream.read( buffer ) < 0 )
			throw new EOFException();
		Base64.Encoder encoder = Base64.getEncoder();
		value.setValue( encoder.encodeToString( buffer ) );
	}

	private static void readBinaryIntoValue( InputStream istream, long size, Value value )
		throws IOException {
		byte[] buffer = new byte[ (int) size ];
		if( istream.read( buffer ) < 0 )
			throw new EOFException();
		value.setValue( new ByteArray( buffer ) );
	}

	private static void readJsonIntoValue( InputStream istream, Value value, Charset charset, boolean strictEncoding )
		throws IOException {
		InputStreamReader isr;
		if( charset == null ) {
			// UTF-8 is JSON's default charset: https://tools.ietf.org/html/rfc7159#section-8.1
			isr = new InputStreamReader( istream, StandardCharsets.UTF_8 );
		} else {
			isr = new InputStreamReader( istream, charset );
		}
		JsUtils.parseJsonIntoValue( isr, value, strictEncoding );
	}

	private static void readYaml( InputStream istream, Value value, boolean isStream )
		throws IOException {
		try {
			Load load = new Load( LoadSettings.builder().build() );

			if( isStream ) {
				for( Object yamlDoc : load.loadAllFromInputStream( istream ) ) {
					yamlObjectToValue( yamlDoc, value.getNewChild( "documents" ) );
				}
			} else {
				yamlObjectToValue( load.loadFromInputStream( istream ), value );
			}
		} catch( YamlEngineException e ) {
			throw new IOException( e );
		}
	}

	private static void yamlObjectToValue( Object o, Value v ) {
		switch( o ) {
		case Boolean x -> v.setValue( x );
		case Integer x -> v.setValue( x );
		case Long x -> v.setValue( x );
		case Double x -> v.setValue( x );
		case String x -> v.setValue( x );
		case Iterable< ? > x -> yamlObjectToValueVector( x, v.getChildren( "_" ) );
		case Map< ?, ? > x -> {
			x.forEach( ( k, nestedObj ) -> {
				yamlObjectToValueVector( nestedObj, v.getChildren( k.toString() ) );
			} );
		}
		default -> {
		}
		}
	}

	private static void yamlObjectToValueVector( Object o, ValueVector vec ) {
		switch( o ) {
		case Iterable< ? > x -> {
			x.forEach( element -> yamlObjectToValue( element, vec.get( vec.size() ) ) );
		}
		case Boolean x -> yamlObjectToValue( x, vec.first() );
		case Integer x -> yamlObjectToValue( x, vec.first() );
		case Long x -> yamlObjectToValue( x, vec.first() );
		case Double x -> yamlObjectToValue( x, vec.first() );
		case String x -> yamlObjectToValue( x, vec.first() );
		case Map< ?, ? > x -> yamlObjectToValue( x, vec.first() );
		default -> {
		}
		}
	}

	private void readXMLIntoValue( InputStream istream, Value value, Charset charset, boolean skipMixedElement )
		throws IOException {
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			if( charset != null ) {
				src.setEncoding( charset.name() );
			}
			Document doc = builder.parse( src );
			jolie.xml.XmlUtils.documentToValue(
				doc,
				value.getFirstChild( doc.getDocumentElement().getNodeName() ),
				skipMixedElement );
		} catch( ParserConfigurationException | SAXException e ) {
			throw new IOException( e );
		}
	}

	private void readXMLIntoValueForStoring( InputStream istream, Value value, Charset charset )
		throws IOException {
		try {
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			InputSource src = new InputSource( new InputStreamReader( istream ) );
			if( charset != null ) {
				src.setEncoding( charset.name() );
			}
			Document doc = builder.parse( src );
			jolie.xml.XmlUtils.storageDocumentToValue(
				doc,
				value.getFirstChild( doc.getDocumentElement().getNodeName() ) );
		} catch( ParserConfigurationException | SAXException e ) {
			throw new IOException( e );
		}
	}

	private static void readTextIntoValue( InputStream istream, long size, Value value, Charset charset )
		throws IOException {
		byte[] buffer = new byte[ (int) size ];
		int len = istream.read( buffer );
		if( len < 0 ) {
			len = 0; // EOF handled as empty string
		}
		if( charset == null ) {
			value.setValue( new String( buffer, 0, len ) );
		} else {
			value.setValue( new String( buffer, 0, len, charset ) );
		}
	}

	private void readPropertiesFile( InputStream istream, Value value, Charset charset )
		throws IOException {
		Properties properties = new Properties();
		if( charset == null ) {
			properties.load( new InputStreamReader( istream ) );
		} else {
			properties.load( new InputStreamReader( istream, charset ) );
		}
		for( String name : properties.stringPropertyNames() ) {
			String propertyValue = properties.getProperty( name );
			Matcher matcher = FILE_KEYWORD_PATTERN.matcher( propertyValue );
			if( matcher.matches() ) {
				if( matcher.group( 1 ).length() > 1 ) { // The number of #
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

	private static void __copyDir( File src, File dest ) throws IOException {
		if( src.isDirectory() ) {
			if( !dest.exists() ) {
				if( !dest.mkdir() ) {
					throw new FileAlreadyExistsException( dest.getAbsolutePath() );
				}
			}
			String[] files = src.list();
			for( String file : files ) {
				File fileSrc = new File( src, file );
				File fileDest = new File( dest, file );
				__copyDir( fileSrc, fileDest );
			}
		} else {
			try( // copy files
				FileInputStream inStream = new FileInputStream( src );
				FileOutputStream outStream = new FileOutputStream( dest ) ) {
				byte[] buffer = new byte[ 4096 ];
				int length;
				while( (length = inStream.read( buffer )) > 0 ) {
					outStream.write( buffer, 0, length );
				}
			}
		}
	}

	@RequestResponse
	public Value copyDir( Value request ) throws FaultException {
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

	private void navigateTree( File file, ValueVector result ) {
		if( file.isDirectory() ) {
			File[] files = file.listFiles();
			for( File f : files ) {
				navigateTree( f, result );
			}
		}
		Value path = Value.create();
		path.setValue( file.getAbsolutePath() );
		result.add( path );

	}


	@RequestResponse
	public Value fileTree( Value request ) {
		Value retValue = Value.create();
		ValueVector result = retValue.getChildren( "result" );
		File startingDirectory = new File( request.strValue() );
		navigateTree( startingDirectory, result );
		return retValue;
	}

	@RequestResponse
	public Value readFile( Value request )
		throws FaultException {
		Value filenameValue = request.getFirstChild( "filename" );
		boolean skipMixedText = false;

		Value retValue = Value.create();
		String format = request.getFirstChild( "format" ).strValue();
		Charset charset = null;
		Value formatValue = request.getFirstChild( "format" );
		if( formatValue.hasChildren( "charset" ) ) {
			charset = Charset.forName( formatValue.getFirstChild( "charset" ).strValue() );
		}

		if( formatValue.hasChildren( "skipMixedText" ) ) {
			skipMixedText = formatValue.getFirstChild( "skipMixedText" ).boolValue();
		}
		final File file = new File( filenameValue.strValue() );
		InputStream istream = null;
		long size;
		try {
			if( file.exists() ) {
				istream = new FileInputStream( file );
				size = file.length();
			} else {
				URL fileURL = interpreter().getClassLoader().findResource( filenameValue.strValue() );
				if( fileURL != null && fileURL.getProtocol().equals( "jap" ) ) {
					URLConnection conn = fileURL.openConnection();
					if( conn instanceof JapURLConnection ) {
						JapURLConnection jarConn = (JapURLConnection) conn;
						size = jarConn.getEntrySize();
						if( size < 0 ) {
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
					readXMLIntoValue( istream, retValue, charset, skipMixedText );
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
					if( request.getFirstChild( "format" ).hasChildren( "json_encoding" ) ) {
						if( request.getFirstChild( "format" ).getFirstChild( "json_encoding" ).strValue()
							.equals( "strict" ) ) {
							strictEncoding = true;
						}
					}
					readJsonIntoValue( istream, retValue, charset, strictEncoding );
					break;
				case "yaml":
					istream = new BufferedInputStream( istream );
					readYaml( istream, retValue,
						request.getFirstChild( "format" ).firstChildOrDefault( "stream", Value::boolValue, false ) );
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
	public Boolean exists( String filename ) {
		return new File( filename ).exists();
	}

	@RequestResponse
	public Boolean mkdir( String directory ) {
		return new File( directory ).mkdirs();
	}

	@RequestResponse
	public String getMimeType( String filename )
		throws FaultException {
		File file = new File( filename );
		if( file.exists() == false ) {
			throw new FaultException( "FileNotFound", filename );
		}
		return fileTypeMap.getContentType( file );
	}

	@RequestResponse
	public String getServiceParentPath() {
		String filepath = interpreter().programFilepath();
		return filepath.substring( 0, filepath.lastIndexOf( "/" ) );
	}

	@RequestResponse
	public String getServiceDirectory()
		throws FaultException {
		String dir = null;
		try {
			dir = interpreter().programDirectory().getCanonicalPath();
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
		if( dir == null || dir.isEmpty() ) {
			dir = ".";
		}

		return dir;
	}

	@RequestResponse
	public String getRealServiceDirectory()
		throws FaultException {
		try {
			return Paths.get( interpreter().programFilepath() ).toRealPath().getParent().toString();
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public String getServiceFileName() {
		return Paths.get( interpreter().programFilepath() ).getFileName().toString();
	}

	@RequestResponse
	public String getRealServiceFileName()
		throws FaultException {
		try {
			return Paths.get( interpreter().programFilepath() ).toRealPath().getFileName().toString();
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public String getFileSeparator() {
		return jolie.lang.Constants.FILE_SEPARATOR;
	}

	private final static String NAMESPACE_ATTRIBUTE_NAME = "@NameSpace";

	private void writeXML(
		File file, Value value,
		boolean append,
		String schemaFilename,
		String doctypeSystem,
		String encoding,
		boolean indent )
		throws IOException {
		if( value.children().isEmpty() ) {
			return; // TODO: perhaps we should erase the content of the file before returning.
		}

		String rootName = value.children().keySet().iterator().next();
		Value root = value.children().get( rootName ).get( 0 );
		String rootNameSpace = "";
		if( root.hasChildren( NAMESPACE_ATTRIBUTE_NAME ) ) {
			rootNameSpace = root.getFirstChild( NAMESPACE_ATTRIBUTE_NAME ).strValue();
		}

		try {
			// XSType type = null;
			if( schemaFilename != null ) {
				try {
					XSOMParser parser = new XSOMParser();
					parser.parse( schemaFilename );
					XSSchemaSet schemaSet = parser.getResult();
					// if( schemaSet != null && schemaSet.getElementDecl( rootNameSpace, rootName ) != null ) {
					// type = schemaSet.getElementDecl( rootNameSpace, rootName ).getType();
					// } else
					if( schemaSet == null || schemaSet.getElementDecl( rootNameSpace, rootName ) == null ) {
						throw new IOException( "Root element " + rootName + " with namespace " + rootNameSpace
							+ " not found in the schema " + schemaFilename );
						// System.out.println( "Root element " + rootName + " with namespace " + rootNameSpace
						// + " not found in the schema " + schemaFilename );
					}
				} catch( SAXException e ) {
					throw new IOException( e );
				}
			}

			Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
			Transformer transformer = transformerFactory.newTransformer();
			jolie.xml.XmlUtils.configTransformer( transformer, encoding, doctypeSystem, indent );
			jolie.xml.XmlUtils.valueToDocument( value, doc, schemaFilename );

			try( Writer writer = new FileWriter( file, append ) ) {
				StreamResult result = new StreamResult( writer );
				transformer.transform( new DOMSource( doc ), result );
			}
		} catch( ParserConfigurationException | TransformerException e ) {
			throw new IOException( e );
		}
	}

	private void writeStorageXML( File file, Value value, String encoding, boolean indent )
		throws IOException {
		if( value.children().isEmpty() ) {
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
			if( indent ) {
				transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
				transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
			} else {
				transformer.setOutputProperty( OutputKeys.INDENT, "no" );
			}
			if( encoding != null ) {
				transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
			}
			try( Writer writer = new FileWriter( file, false ) ) {
				StreamResult result = new StreamResult( writer );
				transformer.transform( new DOMSource( doc ), result );
			}
		} catch( ParserConfigurationException | TransformerException e ) {
			throw new IOException( e );
		}
	}

	private static void writeBinary( File file, Value value, boolean append )
		throws IOException {
		try( FileOutputStream os = new FileOutputStream( file, append ) ) {
			os.write( value.byteArrayValue().getBytes() );
			os.flush();
		}
	}

	private static void writeText( File file, Value value, boolean append, String encoding )
		throws IOException {
		try( OutputStream fos = new FileOutputStream( file, append ) ) {
			OutputStreamWriter writer =
				(encoding != null)
					? new OutputStreamWriter( fos, encoding )
					: new OutputStreamWriter( fos );
			writer.write( value.strValue() );
			writer.flush();
		}
	}

	private static void writeJson( File file, Value value, boolean append, String encoding )
		throws IOException {
		StringBuilder json = new StringBuilder();
		JsUtils.valueToJsonString( value, true, Type.UNDEFINED, json );

		try( OutputStream fos = new FileOutputStream( file, append ) ) {
			OutputStreamWriter writer = new OutputStreamWriter( fos, encoding != null ? encoding : "UTF-8" );
			writer.write( json.toString() );
			writer.flush();
		}
	}

	@RequestResponse
	public void writeFile( Value request )
		throws FaultException {
		boolean append = false;
		Value content = request.getFirstChild( "content" );
		String format = request.getFirstChild( "format" ).strValue();
		File file = new File( request.getFirstChild( "filename" ).strValue() );
		if( request.getFirstChild( "append" ).intValue() > 0 ) {
			append = true;
		}
		String encoding = null;
		if( request.getFirstChild( "format" ).hasChildren( "encoding" ) ) {
			encoding = request.getFirstChild( "format" ).getFirstChild( "encoding" ).strValue();
		}

		try {
			if( "text".equals( format ) ) {
				writeText( file, content, append, encoding );
			} else if( "binary".equals( format ) ) {
				writeBinary( file, content, append );
			} else if( "xml".equals( format ) ) {
				String schemaFilename = null;
				if( request.getFirstChild( "format" ).hasChildren( "schema" ) ) {
					schemaFilename = request.getFirstChild( "format" ).getFirstChild( "schema" ).strValue();
				}
				boolean indent = false;
				if( request.getFirstChild( "format" ).hasChildren( "indent" ) ) {
					indent = request.getFirstChild( "format" ).getFirstChild( "indent" ).boolValue();
				}

				String doctypePublic = null;
				if( request.getFirstChild( "format" ).hasChildren( "doctype_system" ) ) {
					doctypePublic = request.getFirstChild( "format" ).getFirstChild( "doctype_system" ).strValue();
				}
				writeXML( file, content, append, schemaFilename, doctypePublic, encoding, indent );
			} else if( "xml_store".equals( format ) ) {
				boolean indent = false;
				if( request.getFirstChild( "format" ).hasChildren( "indent" ) ) {
					indent = request.getFirstChild( "format" ).getFirstChild( "indent" ).boolValue();
				}
				writeStorageXML( file, content, encoding, indent );
			} else if( "json".equals( format ) ) {
				writeJson( file, content, append, encoding );
			} else if( format.isEmpty() ) {
				if( content.isByteArray() ) {
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
	public Boolean delete( Value request ) {
		String filename = request.strValue();
		boolean isRegex = request.getFirstChild( "isRegex" ).intValue() > 0;
		boolean ret = true;
		if( isRegex ) {
			File dir = new File( filename ).getAbsoluteFile().getParentFile();
			String[] files = dir.list( new ListFilter( filename, false ) );
			if( files != null ) {
				for( String file : files ) {
					new File( file ).delete();
				}
			}
		} else {
			if( new File( filename ).delete() == false ) {
				ret = false;
			}
		}
		return ret;
	}

	@RequestResponse
	public Boolean deleteDir( Value request ) {
		return __deleteDir( new File( request.strValue() ) );
	}

	@RequestResponse
	public void rename( Value request )
		throws FaultException {
		String filename = request.getFirstChild( "filename" ).strValue();
		String toFilename = request.getFirstChild( "to" ).strValue();
		if( new File( filename ).renameTo( new File( toFilename ) ) == false ) {
			Value fault = Value.create();
			fault.setValue( "Error renaming file " + filename );
			fault.getFirstChild( "stackTrace" ).setValue( "" );
			throw new FaultException( "IOException", fault );
		}
	}

	@RequestResponse
	public Value getSize( Value request ) {
		Value retValue = Value.create();

		retValue.setValue( request.byteArrayValue().size() );

		return retValue;
	}

	@RequestResponse
	public Value list( Value request )
		throws FaultException {
		final Path dir;
		try {
			dir = Paths.get( request.getFirstChild( "directory" ).strValue() );
		} catch( InvalidPathException e ) {
			throw new FaultException( e );
		}
		final boolean fileInfo = request.firstChildOrDefault( "info", Value::boolValue, false );

		final String regex = request.firstChildOrDefault(
			"regex",
			Value::strValue,
			".*" );

		final boolean dirsOnly = request.firstChildOrDefault(
			"dirsOnly",
			Value::boolValue,
			false );

		final Pattern pattern = Pattern.compile( regex );

		final BiPredicate< Path, BasicFileAttributes > matcher =
			( path, attrs ) -> pattern.matcher( path.toString() ).matches() && (!dirsOnly || Files.isDirectory( path ));

		final Stream< Path > dirStream;
		try {
			if( request.hasChildren( "recursive" ) && request.getFirstChild( "recursive" ).boolValue() ) {
				dirStream = Files.find( dir, Integer.MAX_VALUE, matcher );
			} else {
				dirStream = Files.find( dir, 1, matcher );
			}
		} catch( IOException e ) {
			throw new FaultException( e );
		}

		final ArrayList< Value > results = new ArrayList<>();
		dirStream.forEach( path -> {
			if( !path.equals( dir ) ) {
				final Path p = dir.relativize( path );
				Value fileValue = Value.create( p.toString() );
				if( fileInfo ) {
					Value info = fileValue.getFirstChild( "info" );
					File currFile = new File( path.toString() );
					info.getFirstChild( "lastModified" ).setValue( currFile.lastModified() );
					info.getFirstChild( "size" ).setValue( currFile.length() );
					info.getFirstChild( "absolutePath" ).setValue( currFile.getAbsolutePath() );
					info.getFirstChild( "isHidden" ).setValue( currFile.isHidden() );
					info.getFirstChild( "isDirectory" ).setValue( currFile.isDirectory() );
				}
				results.add( fileValue );
			}
		} );

		dirStream.close();

		if( request.hasChildren( "order" ) ) {
			Value order = request.getFirstChild( "order" );

			if( order.hasChildren( "byname" ) && order.getFirstChild( "byname" ).boolValue() ) {
				results.sort( Comparator.comparing( Value::strValue ) );
			}
		}

		Value response = Value.create();
		ValueVector responseResults = response.getChildren( "result" );
		results.forEach( responseResults::add );
		return response;
	}

	@SuppressWarnings( "PMD" )
	@RequestResponse
	public Value isDirectory( Value request ) {
		File dir = new File( request.strValue() );
		return Value.create( dir.isDirectory() );
	}

	private static boolean __deleteDir( File file ) {
		if( file.isDirectory() ) {
			String[] children = file.list();
			for( String children1 : children ) {
				__deleteDir( new File( file, children1 ) );
			}
		}
		return file.delete();
	}

	private static class ListFilter implements FilenameFilter {
		private final Pattern pattern;
		private final boolean dirsOnly;

		public ListFilter( String regex, boolean dirsOnly ) {
			this.pattern = Pattern.compile( regex );
			this.dirsOnly = dirsOnly;
		}

		@Override
		public boolean accept( File directory, String filename ) {
			File file = new File( directory.getAbsolutePath() + File.separator + filename );
			return pattern.matcher( filename ).matches() && (!dirsOnly || file.isDirectory());
		}
	}

	@RequestResponse
	public Value toAbsolutePath( Value request ) throws FaultException {
		Value response = Value.create();
		String fileName = request.strValue();

		Path absolutePath = null;

		try {
			absolutePath = Paths.get( fileName ).toAbsolutePath().normalize();
		} catch( InvalidPathException invalidPathException ) {
			throw new FaultException( invalidPathException );
		}

		response.setValue( absolutePath.toString() );

		return response;
	}

	@RequestResponse
	public Value getParentPath( Value request ) throws FaultException {
		Value response = Value.create();
		String fileName = request.strValue();
		Path parent = null;

		try {
			parent = Paths.get( fileName ).getParent();
		} catch( InvalidPathException e ) {
			try {
				parent = Paths.get( new URL( fileName ).toURI() ).getParent();
			} catch( InvalidPathException | URISyntaxException | MalformedURLException invalidPathException ) {
				throw new FaultException( invalidPathException );
			}
		}

		if( parent == null ) {
			throw new FaultException( new InvalidPathException( fileName, "Path has no parent" ) );
		}

		response.setValue( parent.toString() );

		return response;
	}
}
