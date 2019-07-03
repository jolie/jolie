/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.java.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Map.Entry;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import jolie.CommandLineException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.Jolie2JavaInterface;
import joliex.java.Jolie2Java;
import joliex.java.Jolie2JavaCommandLineParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author claudio
 */
public class JavaDocumentCreatorTest
{
	private static ProgramInspector inspector;
	private static String TESTSTRING = "test";
	private static Integer TESTINTEGER = 1;
	private static Double TESTDOUBLE = 1.1;
	private byte[] TESTRAW = new byte[]{ (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10, (byte) 0xa2 };
	private Boolean TESTBOOL = true;
	private Long TESTLONG = 1L;

	public JavaDocumentCreatorTest()
	{
	}

	@BeforeClass
	public static void setUpClass() throws IOException, ParserException, SemanticException, CommandLineException
	{
		// clean past generated files if they exist
		File generatedPath = new File( "./generated/com/test" );
		if ( generatedPath.exists() ) {
			String files[] = generatedPath.list();
			for( String temp : files ) {
				File fileDelete = new File( generatedPath, temp );
				fileDelete.delete();
			}
		}

		String[] args = { "./resources/main.ol" };
		Jolie2JavaCommandLineParser cmdParser = Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

		Program program = ParsingUtils.parseProgram(
			cmdParser.programStream(),
			cmdParser.programFilepath().toURI(), cmdParser.charset(),
			cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants() );

		//Program program = parser.parse();
		inspector = ParsingUtils.createInspector( program );
	}

	@AfterClass
	public static void tearDownClass()
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	/**
	 * Test of ConvertDocument method, of class JavaDocumentCreator.
	 */
	@Test
	public void testConvertDocument() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		System.out.println( "ConvertDocument" );
		JavaDocumentCreator instance = new JavaDocumentCreator( inspector, "com.test", null, false );
		instance.ConvertDocument();

		assertEquals( "The number of generated files is wrong", 2, new File( "./generated/com/test" ).list().length );

		// compile files
		File generatedPath = new File( "./generated/com/test" );
		if ( generatedPath.exists() ) {
			String files[] = generatedPath.list();
			for( String temp : files ) {
				File sourceFile = new File( generatedPath, temp );
				JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				compiler.run( null, null, null, sourceFile.getPath() );
			}
		}

		// load classes
		File pippo = new File( "./generated" );
		URLClassLoader classLoader = URLClassLoader.newInstance( new URL[]{ pippo.toURI().toURL() } );
		Class<?> cls = Class.forName( "com.test.FlatStructureType", true, classLoader ); // Should print "hello".
		Constructor flatStructureTypeConstructor = cls.getConstructor( new Class[]{ Value.class } );

		// FileStructure
		Jolie2JavaInterface flatStructureType = (Jolie2JavaInterface) flatStructureTypeConstructor.newInstance( getFlatStructuredType() );
		assertTrue( compareValues( getFlatStructuredType(), flatStructureType.getValue() ));

	}

	private Value getFlatStructuredType()
	{
		Value testValue = Value.create();
		testValue.getFirstChild( "afield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "bfield" ).setValue( TESTINTEGER );
		testValue.getFirstChild( "cfield" ).setValue( TESTDOUBLE );
		testValue.getFirstChild( "dfield" ).setValue( TESTRAW );
		testValue.getFirstChild( "efield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "ffield" ).setValue( TESTBOOL );
		testValue.getFirstChild( "gfield" ).setValue( getTestUndefined() );
		testValue.getFirstChild( "hfield" ).setValue( TESTLONG );
		return testValue;
	}

	private Value getTestUndefined()
	{
		Value returnValue = Value.create();
		returnValue.getFirstChild( "a" ).setValue( TESTBOOL );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).setValue( TESTSTRING );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).getFirstChild( "c" ).setValue( TESTDOUBLE );

		return returnValue;
	}

	private boolean compareValues( Value v1, Value v2 )
	{
		boolean resp = true;
		if ( !checkRootValue( v1, v2 ) ) {
			return false;
		}
		// from v1 -> v2
		for( Entry<String, ValueVector> entry : v1.children().entrySet() ) {
			if ( !v2.hasChildren( entry.getKey() ) ) {
				return false;
			} else {
				if ( entry.getValue().size() != v2.getChildren( entry.getKey() ).size() ) {
					return false;
				} 
				for( int i = 0; i < entry.getValue().size(); i++ ) {
					resp = compareValues( entry.getValue().get( i ), v2.getChildren( entry.getKey() ).get( i ) );
				}
			}
		}
		return resp;
	}
	
	private boolean checkRootValue ( Value v1, Value v2 ) {
		boolean resp = true;
		if ( v1.isBool() && !v2.isBool() || v1.boolValue() != v2.boolValue() ) {
			resp = false;
		}
		if ( v1.isByteArray()&& !v2.isByteArray()|| v1.byteArrayValue()!= v2.byteArrayValue()) {
			resp = false;
		}
		if ( v1.isDouble()&& !v2.isDouble()|| v1.doubleValue()!= v2.doubleValue()) {
			resp = false;
		}
		if ( v1.isInt()&& !v2.isInt()|| v1.intValue()!= v2.intValue()) {
			resp = false;
		}
		if ( v1.isLong()&& !v2.isLong()|| v1.longValue()!= v2.longValue()) {
			resp = false;
		}
		return resp;
		
	}
}
