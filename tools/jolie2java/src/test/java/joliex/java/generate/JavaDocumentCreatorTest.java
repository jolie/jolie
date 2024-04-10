/**
 * *************************************************************************
 * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
package joliex.java.generate;

import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.java.embedding.util.StructureBuilder;
import joliex.java.embedding.util.StructureListBuilder;
import joliex.java.Jolie2Java;
import joliex.java.Jolie2JavaCommandLineParser;
import joliex.java.embedding.JolieValue;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author claudio
 */
public class JavaDocumentCreatorTest {
	private static ProgramInspector inspector;
	private static final String PACKAGENAME = "com.test";
	private static final String TESTSTRING = "test";
	private static final Integer TESTINTEGER = 1;
	private static final Double TESTDOUBLE = 1.1;
	private static final byte[] TESTRAW =
		new byte[] { (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10, (byte) 0xa2 };
	private static final Boolean TESTBOOL = true;
	private static final Long TESTLONG = 2L;
	private static final Boolean DELETE_AFTER_TEST = true;

	private static URLClassLoader classLoader;
	private static final String OUTPUTDIRECTORY = "./target/generated-test-sources/";

	HashMap< String, Method > setMethodList = new HashMap<>();
	HashMap< String, Method > getMethodList = new HashMap<>();
	HashMap< String, Method > getMethodValueList = new HashMap<>();
	HashMap< String, Method > addMethodList = new HashMap<>();
	HashMap< String, Method > removeMethodList = new HashMap<>();
	HashMap< String, Method > sizeMethodList = new HashMap<>();

	public JavaDocumentCreatorTest() {}

	@BeforeClass
	public static void setUpClass()
		throws IOException, ParserException, CodeCheckException, CommandLineException, ModuleException {
		// clean past generated files if they exist
		File generatedPath = new File( OUTPUTDIRECTORY );
		if( generatedPath.exists() ) {
			TestUtils.deleteFolder( generatedPath );
		}

		String[] args = { "src/test/resources/main.ol" };
		Jolie2JavaCommandLineParser cmdParser =
			Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

		Program program = ParsingUtils.parseProgram(
			cmdParser.getInterpreterConfiguration().inputStream(),
			cmdParser.getInterpreterConfiguration().programFilepath().toURI(),
			cmdParser.getInterpreterConfiguration().charset(),
			cmdParser.getInterpreterConfiguration().includePaths(),
			cmdParser.getInterpreterConfiguration().packagePaths(),
			cmdParser.getInterpreterConfiguration().jolieClassLoader(),
			cmdParser.getInterpreterConfiguration().constants(),
			cmdParser.getInterpreterConfiguration().executionTarget(),
			false );

		// Program program = parser.parse();
		inspector = ParsingUtils.createInspector( program );

		final JavaDocumentCreator jdc =
			new JavaDocumentCreator( PACKAGENAME, null, null, null, OUTPUTDIRECTORY, true, null );

		jdc.generateClasses( inspector );

		assertEquals( "The number of generated files is wrong (generateService=true)", 21,
			new File( OUTPUTDIRECTORY + "com/test/types" ).list().length );
		assertEquals( "The number of generated files is wrong (generateService=true)", 11,
			new File( OUTPUTDIRECTORY + "com/test/faults" ).list().length );
		assertEquals( "The number of generated files is wrong (generateService=true)", 1,
			new File( OUTPUTDIRECTORY + "com/test/interfaces" ).list().length );
		assertEquals( "The number of generated files is wrong (generateService=true)", 4,
			new File( OUTPUTDIRECTORY + "com/test" ).list().length );
		assertEquals( "The number of generated files is wrong (generateService=true)", 1,
			new File( OUTPUTDIRECTORY ).list().length );

		// load classes
		File generated = new File( OUTPUTDIRECTORY );
		classLoader = URLClassLoader.newInstance( new URL[] { generated.toURI().toURL() } );

		// compile files
		File generatedFilesDir = new File( OUTPUTDIRECTORY + "com/test" );
		List< String > files = Arrays.stream( generatedFilesDir.list() )
			.flatMap( fileName -> {
				final File tmpFile = new File( generatedFilesDir.getPath() + File.separator + fileName );
				return tmpFile.isDirectory()
					? Arrays.stream( tmpFile.list() ).map( fn -> tmpFile.getPath() + File.separator + fn )
					: Stream.of( tmpFile.getPath() );
			} )
			.toList();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		OutputStream output = new OutputStream() {
			private final StringBuilder sb = new StringBuilder();

			@Override
			public void write( int b ) throws IOException {
				this.sb.append( (char) b );
			}

			@Override
			public String toString() {
				return this.sb.toString();
			}
		};

		String[] filesToBeCompiled = new String[ files.size() ];
		filesToBeCompiled = files.toArray( filesToBeCompiled );
		compiler.run( null, null, output, filesToBeCompiled );
		System.out.println( output );
	}

	@AfterClass
	public static void tearDownClass() {
		if( DELETE_AFTER_TEST ) {
			File generatedPath = new File( OUTPUTDIRECTORY );
			if( generatedPath.exists() ) {
				TestUtils.deleteFolder( generatedPath );
				generatedPath.delete();
			}
		}
	}

	@Before
	public void setUp() {}

	@After
	public void tearDown() {

	}

	@Test
	public void testInterfaceImpl() throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
		IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// String testName = "testInterfaceImpl";
		Class< ? > TestInterface = getClass( JavaDocumentCreator.DEFAULT_INTERFACE_PACKAGE, "TestInterface" );

		assertEquals(
			"Number of generated methods does not correspond",
			34L,
			Arrays.stream( TestInterface.getMethods() ).filter( m -> m.getName().startsWith( "test" ) ).count() );
	}

	/**
	 * Test of ConvertDocument method, of class JavaDocumentCreator.
	 */
	@Test
	public void testFlatStructure() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testFlatStructure";
		Class< ? > FlatStructureType = getTypeClass( "FlatStructureType" );

		// invoke fromValue() and toValue()
		JolieValue flatStructureInstance = invokeFromValue( FlatStructureType, getFlatStructuredType() );
		assertTrue(
			compareValues( getFlatStructuredType(), invokeToValue( FlatStructureType, flatStructureInstance ), 0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.setValue( TESTBOOL );
		try {
			invokeFromValue( FlatStructureType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testFlatStructureVectors() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testFlatStructureVectors";
		Class< ? > FlatStructureVectorsType = getTypeClass( "FlatStructureVectorsType" );

		// invoke fromValue() & toValue()
		JolieValue flatStructureVectorsInstance =
			invokeFromValue( FlatStructureVectorsType, getFlatStructuredVectorsType() );
		assertTrue( compareValues( getFlatStructuredVectorsType(),
			invokeToValue( FlatStructureVectorsType, flatStructureVectorsInstance ), 0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );
	}

	@Test
	public void testInLineStructureType() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testInlineStructureType";
		// FileStructureVector
		Class< ? > InLineStructureType = getTypeClass( "InLineStructureType" );

		// invoke fromValue() and toValue()
		JolieValue inLineStructureInstance = invokeFromValue( InLineStructureType, getInlineStructureType() );
		assertTrue( compareValues( getInlineStructureType(),
			invokeToValue( InLineStructureType, inLineStructureInstance ), 0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( InLineStructureType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testInLineStructureVectorsType()
		throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException,
		NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testInLineStructureVectorsType";
		// FileStructureVector
		Class< ? > InLineStructureVectorsType = getTypeClass( "InLineStructureVectorsType" );

		// invoke fromValue() and toValue()
		JolieValue inLineStructureVectorsInstance =
			invokeFromValue( InLineStructureVectorsType, getInlineStructureVectorsType() );
		assertTrue( compareValues( getInlineStructureVectorsType(),
			invokeToValue( InLineStructureVectorsType, inLineStructureVectorsInstance ), 0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( InLineStructureVectorsType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testNewType()
		throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException,
		NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testNewType";
		// FileStructureVector
		Class< ? > NewType = getTypeClass( "NewType" );

		// invoke fromValue() and toValue()
		JolieValue newInstance =
			invokeFromValue( NewType, getNewType() );
		assertTrue( compareValues( getNewType(),
			invokeToValue( NewType, newInstance ), 0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( NewType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testLinkedTypeStructureType()
		throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException,
		NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testLinkedTypeStructureType";
		// FileStructureVector
		Class< ? > LinkedTypeStructureType = getTypeClass( "LinkedTypeStructureType" );

		// invoke fromValue() and toValue()
		JolieValue linkedTypeStructureInstance = invokeFromValue(
			LinkedTypeStructureType, getLinkedTypeStructureType() );
		assertTrue( compareValues(
			getLinkedTypeStructureType(),
			invokeToValue( LinkedTypeStructureType, linkedTypeStructureInstance ),
			0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// invoke methods
		invokingSetAddGetMethodsForLinkedType();
		System.out.println( testName + " invoking methods OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( LinkedTypeStructureType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}


	@Test
	public void testLinkedTypeStructureVectorsType()
		throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException,
		NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testLinkedTypeStructureVectorsType";
		// FileStructureVector
		Class< ? > LinkedTypeStructureVectorsType = getTypeClass( "LinkedTypeStructureVectorsType" );

		// invoke fromValue() and toValue()
		JolieValue linkedTypeStructureVectorsInstance = invokeFromValue(
			LinkedTypeStructureVectorsType, getLinkedTypeStructureVectorsType() );
		assertTrue( compareValues(
			getLinkedTypeStructureVectorsType(),
			invokeToValue( LinkedTypeStructureVectorsType, linkedTypeStructureVectorsInstance ),
			0 ) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// invoke methods
		invokingSetAddGetMethodsForLinkedVectorsType();
		System.out.println( testName + " invoking methods OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( LinkedTypeStructureVectorsType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testChoiceLinkedType() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testChoiceLinkedType";
		Class< ? > ChoiceLinkedType = getTypeClass( "ChoiceLinkedType" );

		// invoke fromValue() and toValue()

		// int
		Value testValue = Value.create( TESTINTEGER );
		JolieValue choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		// InLineStructureType
		choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, getInlineStructureType() );
		assertTrue(
			compareValues( getInlineStructureType(), invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		// void
		testValue = Value.create();
		choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		// FlatStructureType
		choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, getFlatStructuredType() );
		assertTrue(
			compareValues( getFlatStructuredType(), invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		// FlatStructureVectorsType
		choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, getFlatStructuredVectorsType() );
		assertTrue( compareValues( getFlatStructuredVectorsType(),
			invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		// string
		testValue = Value.create( TESTSTRING );
		choiceLinkedInstance = invokeFromValue( ChoiceLinkedType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceLinkedType, choiceLinkedInstance ), 0 ) );

		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create( TESTBOOL );
		try {
			invokeFromValue( ChoiceLinkedType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testChoiceInLineType() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testChoiceInlineType";
		Class< ? > ChoiceInlineType = getTypeClass( "ChoiceInlineType" );

		// invoke fromValue() and toValue()

		// ChoiceInlineType1
		JolieValue choiceInlineInstance = invokeFromValue( ChoiceInlineType, getChoiceInlineType1() );
		assertTrue(
			compareValues( getChoiceInlineType1(), invokeToValue( ChoiceInlineType, choiceInlineInstance ), 0 ) );

		// int
		Value testValue = Value.create( TESTINTEGER );
		choiceInlineInstance = invokeFromValue( ChoiceInlineType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceInlineType, choiceInlineInstance ), 0 ) );

		// ChoiceInlineType2
		choiceInlineInstance = invokeFromValue( ChoiceInlineType, getChoiceInlineType2() );
		assertTrue(
			compareValues( getChoiceInlineType2(), invokeToValue( ChoiceInlineType, choiceInlineInstance ), 0 ) );

		// ChoiceInlineType3
		choiceInlineInstance = invokeFromValue( ChoiceInlineType, getChoiceInlineType3() );
		assertTrue(
			compareValues( getChoiceInlineType3(), invokeToValue( ChoiceInlineType, choiceInlineInstance ), 0 ) );

		// string
		testValue = Value.create( TESTSTRING );
		choiceInlineInstance = invokeFromValue( ChoiceInlineType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceInlineType, choiceInlineInstance ), 0 ) );

		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create( TESTBOOL );
		try {
			invokeFromValue( ChoiceInlineType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testChoiceSimpleType() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		String testName = "testChoiceSimpleType";
		Class< ? > ChoiceSimpleType = getTypeClass( "ChoiceSimpleType" );

		// invoke fromValue() and toValue()

		// string
		Value testValue = Value.create( TESTSTRING );
		JolieValue choiceSimpleInstance = invokeFromValue( ChoiceSimpleType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceSimpleType, choiceSimpleInstance ), 0 ) );

		// int
		testValue = Value.create( TESTINTEGER );
		choiceSimpleInstance = invokeFromValue( ChoiceSimpleType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceSimpleType, choiceSimpleInstance ), 0 ) );

		// double
		testValue = Value.create( TESTDOUBLE );
		choiceSimpleInstance = invokeFromValue( ChoiceSimpleType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceSimpleType, choiceSimpleInstance ), 0 ) );

		// void
		testValue = Value.create();
		choiceSimpleInstance = invokeFromValue( ChoiceSimpleType, testValue );
		assertTrue( compareValues( testValue, invokeToValue( ChoiceSimpleType, choiceSimpleInstance ), 0 ) );

		System.out.println( testName + " fromValue() & toValue() OK" );

		// checking TypeCheckingException
		try {
			testValue.setValue( TESTBOOL );
			invokeFromValue( ChoiceSimpleType, testValue );
			assertTrue( "TypeCheckingException not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testLinkedChoiceStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalArgumentException, InstantiationException {
		String testName = "testLinkedChoiceStructureType";
		// FileStructureVector
		Class< ? > LinkedChoiceStructureType = getTypeClass( "LinkedChoiceStructureType" );

		// invoke fromValue() and toValue()
		JolieValue linkedChoiceStructureInstance = invokeFromValue(
			LinkedChoiceStructureType, getLinkedChoiceStructureType() );
		assertTrue( testName + " fromValue() & toValue() FAILED", compareValues(
			getLinkedChoiceStructureType(),
			invokeToValue( LinkedChoiceStructureType, linkedChoiceStructureInstance ),
			0 
		) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// invoke methods
		invokingSetAddGetMethodsForLinkedChoiceStructureType();
		System.out.println( testName + " invoking methods OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( LinkedChoiceStructureType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testInlineChoiceStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, 
	SecurityException, ClassNotFoundException, IllegalArgumentException, InstantiationException {

		String testName = "testInlineChoiceStructureType";
		// FileStructureVector
		Class< ? > InlineChoiceStructureType = getTypeClass( "InlineChoiceStructureType" );

		// invoke fromValue() and toValue()
		JolieValue inlineChoiceStructureInstance = invokeFromValue(
			InlineChoiceStructureType, getInlineChoiceStructureType() );

		assertTrue( testName + " fromValue() & toValue() FAILED", compareValues(
			getInlineChoiceStructureType(),
			invokeToValue( InlineChoiceStructureType, inlineChoiceStructureInstance ),
			0 
		) );
		System.out.println( testName + " fromValue() & toValue() OK" );

		// invoke methods
		invokingSetAddGetMethodsForInlineChoiceStructureType();
		System.out.println( testName + " invoking methods OK" );

		// checking TypeCheckingException
		Value exceptionValue = Value.create();
		exceptionValue.getFirstChild( "zzzzzz" ).setValue( TESTBOOL );
		try {
			invokeFromValue( InlineChoiceStructureType, exceptionValue );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {
		}
		System.out.println( testName + " Exception raised OK" );
	}

	@Test
	public void testRootValuesType() throws MalformedURLException, ClassNotFoundException, InstantiationException,
		IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		int numberOfRootValueTypeSamples = 7;
		for( int i = 1; i <= numberOfRootValueTypeSamples; i++ ) {
			String testName = "testRootValue" + i + "Type";
			Class< ? > RootValueType = getTypeClass( "RootValue" + i + "Type" );

			// invoke fromValue() & toValue()
			JolieValue rootValueInstance = invokeFromValue( RootValueType, getRootValue( i ) );
			assertTrue( compareValues( getRootValue( i ), invokeToValue( RootValueType, rootValueInstance ), 0 ) );
			System.out.println( testName + " fromValue() & toValue() OK" );
		}
	}

	private Class< ? > getClass( String folder, String name ) throws ClassNotFoundException {
		return Class.forName( PACKAGENAME + "." + folder + "." + name, true, classLoader );
	}

	private Class< ? > getTypeClass( String name ) throws ClassNotFoundException {
		return getClass( JavaDocumentCreator.DEFAULT_TYPE_PACKAGE, name );
	}

	private JolieValue invokeFromValue( Class< ? > cls, Value value )
		throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return JolieValue.class.cast( cls.getMethod( "fromValue", Value.class ).invoke( null, value ) );
	}

	private Value invokeToValue( Class< ? > cls, JolieValue t )
		throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return Value.class.cast( cls.getMethod( "toValue", cls ).invoke( null, t ) );
	}

	private Object invokeGet( Class< ? > cls, JolieValue ins, String name )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			return cls.getDeclaredMethod( name ).invoke( ins );
		} catch( NoSuchMethodException e ) {
			assertTrue( "get method for field " + name + " not found, class " + cls.getName(), false );
			return null;
		}
	}

	private StructureBuilder< ?, ? > invokeConstruct( Class< ? > cls )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			return StructureBuilder.class.cast( cls.getDeclaredMethod( "construct" ).invoke( null ) );
		} catch( NoSuchMethodException e ) {
			assertTrue( "construct method not found, class " + cls.getName(), false );
			return null;
		}
	}

	private void invokeSet( StructureBuilder< ?, ? > ins, Object arg, String name ) throws IllegalAccessException, InvocationTargetException, SecurityException { 
		invokeSet( ins, arg, name, arg.getClass() ); 
	}
	private void invokeSet( StructureBuilder< ?, ? > ins, Object arg, String name, Class< ? > cls )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		
		final String methodName = "set" + StringUtils.capitalize( name );
		try {
			ins.getClass().getDeclaredMethod( methodName, cls ).invoke( ins, arg );
		} catch( NoSuchMethodException e ) {
			assertTrue( methodName + " method of type " + arg.getClass().getName() + " not found, class "
				+ ins.getClass().getName(), false );
		}
	}

	private void invokeListSet( StructureBuilder< ?, ? > ins, Object arg, String name ) throws IllegalAccessException, InvocationTargetException, SecurityException {
		final String methodName = "set" + StringUtils.capitalize( name );
		try {
			ins.getClass().getDeclaredMethod( methodName, SequencedCollection.class ).invoke( ins, arg );
		} catch( NoSuchMethodException e ) {
			assertTrue( methodName + " method of type " + arg.getClass().getName() + " not found, class "
				+ ins.getClass().getName(), false );
		}
	}

	private void invokeAdd( StructureListBuilder< ?, ? > ins, Object arg, Integer choiceNr ) throws IllegalAccessException, InvocationTargetException, SecurityException {
		if ( choiceNr == null )
			invokeAdd( ins, arg, "add", Object.class );
		else
			invokeAdd( ins, arg, "add" + choiceNr, arg.getClass() );
	}
	private void invokeAdd( StructureListBuilder< ?, ? > ins, Object arg, String name, Class< ? > cls )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			ins.getClass().getMethod( name, cls ).invoke( ins, arg );
		} catch( NoSuchMethodException e ) {
			assertTrue(
				name + " method of type " + arg.getClass().getName() + " not found, class "
					+ ins.getClass().getName() + "\n" + "Instead found: " + Arrays.stream( ins.getClass().getMethods() ).map( s -> "\n\t" + s ).reduce( (s1, s2) -> s1 + s2 ).orElse( "" ),
				false );
		}
	}

	private StructureListBuilder< ?, ? > invokeConstructListSet( StructureBuilder< ?, ? > ins, String name )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			return StructureListBuilder.class
				.cast( ins.getClass().getDeclaredMethod( "construct" + StringUtils.capitalize( name ) ).invoke( ins ) );
		} catch( NoSuchMethodException e ) {
			assertTrue( "construct method for field " + name + " not found, class " + ins.getClass().getName(), false );
			return null;
		}
	}

	private void invokeDone( StructureListBuilder< ?, ? > ins )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			ins.getClass().getDeclaredMethod( "done" ).invoke( ins );
		} catch( NoSuchMethodException e ) {
			assertTrue( "build method not found, class " + ins.getClass().getName(), false );
		}
	}

	private JolieValue invokeBuild( StructureBuilder< ?, ? > ins )
		throws IllegalAccessException, InvocationTargetException, SecurityException {
		try {
			return JolieValue.class.cast( ins.getClass().getDeclaredMethod( "build" ).invoke( ins ) );
		} catch( NoSuchMethodException e ) {
			assertTrue( "build method not found, class " + ins.getClass().getName(), false );
			return null;
		}
	}

	private void invokingSetAddGetMethodsForLinkedType()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
		InstantiationException, ClassNotFoundException {

		Class< ? > LinkedTypeStructureType = getTypeClass( "LinkedTypeStructureType" );
		StructureBuilder< ?, ? > builder = invokeConstruct( LinkedTypeStructureType );

		// invoking methods

		// setA()
		Class< ? > InLineStructureType = getTypeClass( "InLineStructureType" );
		JolieValue inLineStructureInstance = invokeFromValue( InLineStructureType, getInlineStructureType() );
		invokeSet( builder, inLineStructureInstance, "a" );

		// setB()
		Class< ? > InLineStructureVectorsType = getTypeClass( "InLineStructureVectorsType" );
		JolieValue inLineStructureVectorsInstance =
			invokeFromValue( InLineStructureVectorsType, getInlineStructureVectorsType() );
		invokeSet( builder, inLineStructureVectorsInstance, "b" );

		// setC()
		Class< ? > FlatStructureType = getTypeClass( "FlatStructureType" );
		JolieValue flatStructureInstance = invokeFromValue( FlatStructureType, getFlatStructuredType() );
		invokeSet( builder, flatStructureInstance, "c" );

		// setD()
		Class< ? > NewType = getTypeClass( "NewType" );
		JolieValue newInstance = invokeFromValue( NewType, getNewType() );
		invokeSet( builder, newInstance, "d" );

		// build()
		JolieValue linkedTypeStructureInstance = invokeBuild( builder );

		// check get methods
		assertEquals( "Linked type a does not correspond.", inLineStructureInstance,
			invokeGet( LinkedTypeStructureType, linkedTypeStructureInstance, "a" ) );
		assertEquals( "Linked type b does not correspond.", inLineStructureVectorsInstance,
			invokeGet( LinkedTypeStructureType, linkedTypeStructureInstance, "b" ) );
		assertEquals( "Linked type c does not correspond.", flatStructureInstance,
			invokeGet( LinkedTypeStructureType, linkedTypeStructureInstance, "c" ) );
		assertEquals( "Linked type d does not correspond.", newInstance,
			invokeGet( LinkedTypeStructureType, linkedTypeStructureInstance, "d" ) );
	}

	private void invokingSetAddGetMethodsForLinkedVectorsType()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
		InstantiationException, ClassNotFoundException {

		Class< ? > LinkedVectorsType = getTypeClass( "LinkedTypeStructureVectorsType" );
		StructureBuilder< ?, ? > builder = invokeConstruct( LinkedVectorsType );

		// invoking methods

		// constructA().add()
		StructureListBuilder< ?, ? > aBuilder = invokeConstructListSet( builder, "a" );
		Class< ? > InLineStructureType = getTypeClass( "InLineStructureType" );
		int aElements = 10;
		for( int i = 0; i < aElements; i++ ) {
			JolieValue inLineStructureInstance = invokeFromValue( InLineStructureType, getInlineStructureType() );
			invokeAdd( aBuilder, inLineStructureInstance, null );
		}
		invokeDone( aBuilder );

		// constructC().add()
		StructureListBuilder< ?, ? > cBuilder = invokeConstructListSet( builder, "c" );
		Class< ? > FlatStructureType = getTypeClass( "FlatStructureType" );
		int cElements = 7;
		for( int i = 0; i < cElements; i++ ) {
			JolieValue flatStructureInstance = invokeFromValue( FlatStructureType, getFlatStructuredType() );
			invokeAdd( cBuilder, flatStructureInstance, null );
		}
		invokeDone( cBuilder );

		// build()
		JolieValue linkedVectorsInstance = invokeBuild( builder );

		// check get methods
		assertEquals( "Linked type a does not correspond.", aElements,
			List.class.cast( invokeGet( LinkedVectorsType, linkedVectorsInstance, "a" ) ).size() );
		assertEquals( "Linked type b does not correspond.", Optional.empty(),
			Optional.class.cast( invokeGet( LinkedVectorsType, linkedVectorsInstance, "b" ) ) );
		assertEquals( "Linked type c does not correspond.", cElements,
			List.class.cast( invokeGet( LinkedVectorsType, linkedVectorsInstance, "c" ) ).size() );
		assertEquals( "Linked type d does not correspond.", 0,
			List.class.cast( invokeGet( LinkedVectorsType, linkedVectorsInstance, "d" ) ).size() );
	}

	private void invokingSetAddGetMethodsForLinkedChoiceStructureType()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
		InstantiationException, ClassNotFoundException {

		Class< ? > LinkedChoiceStructureType = getTypeClass( "LinkedChoiceStructureType" );
		StructureBuilder< ?, ? > builder = invokeConstruct( LinkedChoiceStructureType );

		// invoking methods

		// setA()
		Class< ? > ChoiceSimpleType = getTypeClass( "ChoiceSimpleType" );
		List< ? > choiceSimpleList = List.of(
			invokeFromValue( ChoiceSimpleType, Value.create( TESTINTEGER ) )
		);
		invokeListSet( builder, choiceSimpleList, "a" );

		// constructB()
		StructureListBuilder< ?, ? > bBuilder = invokeConstructListSet( builder, "b" );

		// b.add1()
		Class< ? > ChoiceInlineType1 = getTypeClass( "ChoiceInlineType$S1" );
		JolieValue b1 = invokeFromValue( ChoiceInlineType1, getChoiceInlineType1() );
		invokeAdd( bBuilder, b1, 1 );
		
		// b.add4()
		Class< ? > ChoiceInlineType3 = getTypeClass( "ChoiceInlineType$S3" );
		JolieValue b2 = invokeFromValue( ChoiceInlineType3, getChoiceInlineType3() );
		invokeAdd( bBuilder, b2, 4 );

		// b.add2()
		Class< ? > ChoiceInlineType = getTypeClass( "ChoiceInlineType" );
		JolieValue b3 = invokeFromValue( ChoiceInlineType, Value.create( TESTINTEGER ) );
		invokeAdd( bBuilder, b3, null );

		// b.done()
		invokeDone( bBuilder );

		// setC()
		Class< ? > ChoiceLinkedType = getTypeClass( "ChoiceLinkedType" );
		List< ? > choiceLinkedList = List.of(
			invokeFromValue( ChoiceLinkedType, getFlatStructuredType() ),
			invokeFromValue( ChoiceLinkedType, Value.create( TESTSTRING ) ),
			invokeFromValue( ChoiceLinkedType, getLinkedTypeStructureType() ),
			invokeFromValue( ChoiceLinkedType, Value.create( TESTINTEGER ) ),
			invokeFromValue( ChoiceLinkedType, getFlatStructuredType() ),
			invokeFromValue( ChoiceLinkedType, Value.create() )
		);
		invokeListSet( builder, choiceLinkedList, "c" );

		// build()
		JolieValue linkedTypeStructureInstance = invokeBuild( builder );

		// check get methods
		assertEquals( "Linked type a does not correspond.", choiceSimpleList,
			invokeGet( LinkedChoiceStructureType, linkedTypeStructureInstance, "a" ) );

		List< ? > choiceInlineList = List.class.cast( invokeGet( LinkedChoiceStructureType, linkedTypeStructureInstance, "b" ) );
		assertEquals( "Linked type b1 does not correspond.", b1, choiceInlineList.get( 0 ) );
		assertEquals( "Linked type b2 does not correspond.", b2, choiceInlineList.get( 1 ) );
		assertTrue( "Linked type b3 does not correspond.", b3.equals( choiceInlineList.get( 2 ) ) );

		assertEquals( "Linked type c does not correspond.", choiceLinkedList,
			invokeGet( LinkedChoiceStructureType, linkedTypeStructureInstance, "c" ) );
	}

	private void invokingSetAddGetMethodsForInlineChoiceStructureType()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
		InstantiationException, ClassNotFoundException {

		Class< ? > InlineChoiceStructureType = getTypeClass( "InlineChoiceStructureType" );
		StructureBuilder< ?, ? > builder = invokeConstruct( InlineChoiceStructureType );

		// invoking methods

		// setA()
		Class< ? > AType = getTypeClass( "InlineChoiceStructureType$AType" );
		JolieValue aInstance = invokeFromValue( AType, Value.create( TESTDOUBLE ) );
		invokeSet( builder, aInstance.content().value(), "a3" );

		// setB()
		Class< ? > InLineStructureType = getTypeClass( "InLineStructureType" );
		JolieValue bInstance =
			invokeFromValue( InLineStructureType, getInlineStructureType() );
		invokeSet( builder, bInstance, "b2" );

		// setC()
		Class< ? > ChoiceInlineType = getTypeClass( "ChoiceInlineType" );
		JolieValue cInstance = invokeFromValue( ChoiceInlineType, Value.create( TESTINTEGER ) );
		invokeSet( builder, cInstance, "c2", ChoiceInlineType );

		// setD()
		JolieValue dInstance = invokeFromValue( InlineChoiceStructureType, getInlineChoiceStructureType() );
		invokeSet( builder, dInstance, "d1" );

		// build()
		JolieValue inlineChoiceStructureInstance = invokeBuild( builder );

		// check get methods
		assertEquals( "Linked type a does not correspond.", aInstance,
			invokeGet( InlineChoiceStructureType, inlineChoiceStructureInstance, "a" ) );
		assertEquals( "Linked type b does not correspond.", bInstance,
			invokeGet( InlineChoiceStructureType, inlineChoiceStructureInstance, "b" ) );
		assertEquals( "Linked type c does not correspond.", cInstance,
			invokeGet( InlineChoiceStructureType, inlineChoiceStructureInstance, "c" ) );
		assertEquals( "Linked type d does not correspond.", dInstance,
			invokeGet( InlineChoiceStructureType, inlineChoiceStructureInstance, "d" ) );
	}

	private Value getRootValue( int index ) {
		Value returnValue = Value.create();
		Value field = returnValue.getFirstChild( "field" );
		switch( index ) {
		case 1:
			returnValue.setValue( TESTSTRING );
			field.setValue( TESTSTRING );
			break;
		case 2:
			returnValue.setValue( TESTINTEGER );
			field.setValue( TESTINTEGER );
			break;
		case 3:
			returnValue.setValue( TESTDOUBLE );
			field.setValue( TESTDOUBLE );
			break;
		case 4:
			returnValue.setValue( new ByteArray( TESTRAW ) );
			field.setValue( new ByteArray( TESTRAW ) );
			break;
		case 5:
			returnValue.setValue( TESTLONG );
			field.setValue( TESTLONG );
			break;
		case 6:
			returnValue.setValue( TESTBOOL );
			field.setValue( TESTBOOL );
			break;
		case 7: // any TODO: explore all the possibilties
			returnValue.setValue( TESTBOOL );
			field.setValue( TESTBOOL );
			break;
		}
		return returnValue;
	}

	private Value getNewType() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.setValue( TESTSTRING );
		ValueVector b = testValue.getChildren( "b" );
		for( int i = 0; i < 100; i++ ) {
			b.get( i ).setValue( TESTINTEGER );
			Value c = b.get( i ).getFirstChild( "c" );
			c.setValue( TESTLONG );
			Value d = c.getFirstChild( "d" );
			Value e = c.getFirstChild( "e" );
			d.setValue( new ByteArray( TESTRAW ) );
			e.deepCopy( getFlatStructuredType() );
		}

		return testValue;

	}

	private Value getLinkedChoiceStructureType() {
		final Value testValue = Value.create();
		ValueVector vv;
		
		vv = testValue.getChildren( "a" );
		vv.add( Value.create( TESTDOUBLE ) );
		vv.add( Value.create( TESTSTRING ) );

		vv = testValue.getChildren( "b" );
		vv.add( getChoiceInlineType2() );
		vv.add( getChoiceInlineType1() );
		vv.add( Value.create( TESTINTEGER ) );
		vv.add( getChoiceInlineType3() );

		vv = testValue.getChildren( "c" );
		vv.add( Value.create( TESTINTEGER ) );
		vv.add( getInlineStructureType() );
		vv.add( getFlatStructuredVectorsType() );

		return testValue;
	}

	private Value getInlineChoiceStructureType() {
		final Value testValue = Value.create( new ByteArray( TESTRAW ) );
		testValue.setFirstChild( "a", TESTLONG );
		testValue.getFirstChild( "b" ).deepCopy( getFlatStructuredType() );
		testValue.getFirstChild( "c" ).deepCopy( getChoiceInlineType3() );
		
		final Value d = testValue.getFirstChild( "d" );
		d.setValue( TESTBOOL );
		d.setFirstChild( "a", TESTDOUBLE );
		d.setFirstChild( "b", TESTBOOL );
		d.getFirstChild( "c" );
		d.getFirstChild( "d" );

		return testValue;
	}

	private Value getChoiceInlineType1() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.setValue( TESTSTRING );
		Value b = a.getFirstChild( "b" );
		b.setValue( TESTSTRING );
		Value c = b.getFirstChild( "c" );
		c.setValue( TESTSTRING );

		return testValue;

	}

	private Value getChoiceInlineType2() {
		Value testValue = Value.create();
		testValue.setValue( TESTSTRING );
		ValueVector d = testValue.getChildren( "d" );
		for( int i = 0; i < 20; i++ ) {
			d.get( i ).setValue( TESTINTEGER );
			ValueVector e = d.get( i ).getChildren( "e" );
			for( int y = 0; y < 5; y++ ) {
				e.get( y ).setValue( TESTDOUBLE );
				e.get( y ).getFirstChild( "f" ).setValue( new ByteArray( TESTRAW ) );
			}
		}
		return testValue;
	}

	private Value getChoiceInlineType3() {
		Value testValue = Value.create();
		testValue.getFirstChild( "g" ).setValue( TESTSTRING );
		testValue.getFirstChild( "m" ).setValue( TESTINTEGER );

		return testValue;

	}

	private Value getLinkedTypeStructureType() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.deepCopy( getInlineStructureType() );
		Value b = testValue.getFirstChild( "b" );
		b.deepCopy( getInlineStructureVectorsType() );
		Value c = testValue.getFirstChild( "c" );
		c.deepCopy( getFlatStructuredType() );
		Value d = testValue.getFirstChild( "d" );
		d.deepCopy( getNewType() );

		return testValue;
	}

	private Value getLinkedTypeStructureVectorsType() {
		Value testValue = Value.create();
		ValueVector a = testValue.getChildren( "a" );
		for( int i = 0; i < 50; i++ ) {
			a.add( getInlineStructureType() );
		}
		Value b = testValue.getFirstChild( "b" );
		ValueVector bb = b.getChildren( "bb" );
		for( int i = 0; i < 10; i++ ) {
			bb.add( getInlineStructureVectorsType() );
		}
		ValueVector c = testValue.getChildren( "c" );
		for( int i = 0; i < 7; i++ ) {
			c.add( getFlatStructuredType() );
		}
		ValueVector d = testValue.getChildren( "d" );
		for( int i = 0; i < 50; i++ ) {
			d.add( getNewType() );
		}

		return testValue;
	}

	private Value getInlineStructureType() {
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.getFirstChild( "b" ).setValue( TESTSTRING );
		a.getFirstChild( "c" ).setValue( TESTINTEGER );
		a.getFirstChild( "f" ).setValue( TESTDOUBLE );
		Value e = a.getFirstChild( "e" );
		e.setValue( TESTSTRING );
		e.getFirstChild( "ab" ).setValue( new ByteArray( TESTRAW ) );
		e.getFirstChild( "bc" ).setValue( TESTSTRING );
		Value fh = e.getFirstChild( "fh" );
		fh.setValue( TESTSTRING );
		fh.getFirstChild( "abc" ).setValue( TESTSTRING );
		fh.getFirstChild( "def" ).setValue( TESTLONG );
		Value aa = testValue.getFirstChild( "aa" );
		aa.setValue( TESTSTRING );
		aa.getFirstChild( "z" ).setValue( TESTINTEGER );
		aa.getFirstChild( "c" ).setValue( TESTDOUBLE );
		aa.getFirstChild( "f" ).getFirstChild( "rm" ).setValue( TESTSTRING );
		return testValue;
	}

	private Value getInlineStructureVectorsType() {
		Value testValue = Value.create();
		ValueVector a = testValue.getChildren( "a" );
		for( int x = 0; x < 10; x++ ) {
			for( int i = 0; i < 10; i++ ) {
				a.get( x ).getNewChild( "b" ).setValue( TESTSTRING );
			}
			a.get( x ).getFirstChild( "c" ).setValue( TESTINTEGER );
			for( int i = 0; i < 9; i++ ) {
				a.get( x ).getNewChild( "f" ).setValue( TESTDOUBLE );
			}
			ValueVector e = a.get( x ).getChildren( "e" );
			for( int i = 0; i < 8; i++ ) {
				e.get( i ).setValue( TESTSTRING );
				e.get( i ).getFirstChild( "ab" ).setValue( new ByteArray( TESTRAW ) );
				for( int y = 0; y < 4; y++ ) {
					e.get( i ).getNewChild( "bc" ).setValue( TESTSTRING );
				}
				ValueVector fh = e.get( i ).getChildren( "fh" );
				for( int y = 0; y < 100; y++ ) {
					fh.get( y ).setValue( TESTSTRING );
					ValueVector abc = fh.get( y ).getChildren( "abc" );
					for( int z = 0; z < 2; z++ ) {
						abc.get( z ).setValue( TESTSTRING );
					}
					fh.get( y ).getFirstChild( "def" ).setValue( TESTLONG );
				}
			}
		}
		Value aa = testValue.getFirstChild( "aa" );
		aa.setValue( TESTSTRING );
		for( int i = 0; i < 5; i++ ) {
			aa.getNewChild( "z" ).setValue( TESTINTEGER );
		}
		for( int i = 0; i < 3; i++ ) {
			aa.getNewChild( "c" ).setValue( TESTDOUBLE );
		}
		ValueVector f = aa.getChildren( "f" );
		for( int i = 0; i < 100; i++ ) {
			f.get( i ).getFirstChild( "rm" ).setValue( TESTSTRING );
		}
		return testValue;
	}

	private Value getFlatStructuredType() {
		Value testValue = Value.create();
		testValue.setValue( TESTSTRING );
		testValue.getFirstChild( "afield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "bfield" ).setValue( TESTINTEGER );
		testValue.getFirstChild( "cfield" ).setValue( TESTDOUBLE );
		testValue.getFirstChild( "dfield" ).setValue( new ByteArray( TESTRAW ) );
		testValue.getFirstChild( "efield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "ffield" ).setValue( TESTBOOL );
		testValue.getFirstChild( "gfield" ).deepCopy( getTestUndefined() );
		testValue.getFirstChild( "hfield" ).setValue( TESTLONG );
		return testValue;
	}

	private Value getFlatStructuredVectorsType() {
		Value testValue = Value.create();
		for( int i = 0; i < 50; i++ ) {
			testValue.getNewChild( "afield" ).setValue( TESTSTRING );
		}
		// bfield is missing for testing 0 occurencies
		for( int i = 0; i < 10; i++ ) {
			testValue.getNewChild( "cfield" ).setValue( TESTDOUBLE );
		}
		for( int i = 0; i < 100; i++ ) {
			testValue.getNewChild( "dfield" ).setValue( new ByteArray( TESTRAW ) );
		}
		// efield is missing for testing 0 occurencies
		for( int i = 0; i < 10; i++ ) {
			testValue.getNewChild( "ffield" ).setValue( TESTBOOL );
		}
		for( int i = 0; i < 4; i++ ) {
			testValue.getNewChild( "gfield" ).deepCopy( getTestUndefined() );
		}
		for( int i = 0; i < 2; i++ ) {
			testValue.getNewChild( "hfield" ).setValue( TESTLONG );
		}
		return testValue;
	}

	private Value getTestUndefined() {
		Value returnValue = Value.create();
		returnValue.getFirstChild( "a" ).setValue( TESTBOOL );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).setValue( TESTSTRING );
		returnValue.getFirstChild( "a" ).getFirstChild( "b" ).getFirstChild( "c" ).setValue( TESTDOUBLE );

		return returnValue;
	}

	private boolean compareValues( Value v1, Value v2, int level ) {

		boolean resp = true;
		if( !checkRootValue( v1, v2 ) ) {
			System.out.println( "level: " + level + " Root values are different" );
			System.out.println( v1.toString() + "," + v2.toString() );
			return false;
		}
		// from v1 -> v2
		for( Entry< String, ValueVector > entry : v1.children().entrySet() ) {
			if( !v2.hasChildren( entry.getKey() ) ) {
				System.out.println( "level: " + level + " from v1 -> v2: field " + entry.getKey() + " not present" );
				return false;
			} else {
				if( entry.getValue().size() != v2.getChildren( entry.getKey() ).size() ) {
					System.out.println( "level: " + level + " Node name " + entry.getKey() );
					System.out.println( "level: " + level + " from v1 -> v2: The number of subnodes is different: "
						+ entry.getValue().size() + "," + v2.getChildren( entry.getKey() ).size() );
					return false;
				}
				for( int i = 0; i < entry.getValue().size(); i++ ) {
					resp = compareValues( entry.getValue().get( i ), v2.getChildren( entry.getKey() ).get( i ),
						level + 1 );
					if( !resp ) {
						System.out.println(
							"level: " + level + " Error found in subnode " + entry.getKey() + ", index:" + i );
						return false;
					}
				}
			}
		}

		// from v2 -> v1
		for( Entry< String, ValueVector > entry : v2.children().entrySet() ) {
			if( !v1.hasChildren( entry.getKey() ) ) {
				System.out.println( "level: " + level + " from v2 -> v1: field " + entry.getKey() + " not present" );
				return false;
			} else {
				if( entry.getValue().size() != v1.getChildren( entry.getKey() ).size() ) {
					System.out.println( "level: " + level + " from v2 -> v1: The number of subnodes is different: "
						+ entry.getValue().size() + "," + v1.getChildren( entry.getKey() ).size() );
					return false;
				}
				for( int i = 0; i < entry.getValue().size(); i++ ) {
					resp = compareValues( entry.getValue().get( i ), v1.getChildren( entry.getKey() ).get( i ),
						level + 1 );
					if( !resp ) {
						System.out.println(
							"level: " + level + " Error found in subnode " + entry.getKey() + ", index:" + i );
						return false;
					}
				}
			}
		}
		return resp;
	}

	private boolean checkRootValue( Value v1, Value v2 ) {
		boolean resp = true;
		if( v1.isString() && !v2.isString() ) {
			resp = false;
		} else if( v1.isString() && v2.isString() && !(v1.strValue().equals( v2.strValue() )) ) {
			resp = false;
		}
		if( v1.isBool() && !v2.isBool() ) {
			resp = false;
		} else if( v1.isBool() && v2.isBool() && (v1.boolValue() != v2.boolValue()) ) {
			resp = false;
		}
		if( v1.isByteArray() && !v2.isByteArray() ) {
			resp = false;
		} else if( v1.isByteArray() && v2.isByteArray() ) {
			resp = compareByteArrays( v1.byteArrayValue(), v2.byteArrayValue() );
		}
		if( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if( v1.isDouble() && !v2.isDouble() && (v1.doubleValue() != v2.doubleValue()) ) {
			resp = false;
		}
		if( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if( v1.isDouble() && v2.isDouble() && (v1.intValue() != v2.intValue()) ) {
			resp = false;
		}
		if( v1.isLong() && !v2.isLong() ) {
			resp = false;
		} else if( v1.isLong() && v2.isLong() && (v1.longValue() != v2.longValue()) ) {
			resp = false;
		}

		if( !resp ) {
			System.out.println( "v1:" + v1.strValue() + ",isBool:" + v1.isBool() + ",isInt:" + v1.isInt() + ",isLong:"
				+ v1.isLong() + ",isDouble:" + v1.isDouble() + ",isByteArray:" + v1.isByteArray() );
			System.out.println( "v2:" + v2.strValue() + ",isBool:" + v2.isBool() + ",isInt:" + v2.isInt() + ",isLong:"
				+ v2.isLong() + ",isDouble:" + v2.isDouble() + ",isByteArray:" + v2.isByteArray() );
		}
		return resp;

	}

	private boolean compareByteArrays( ByteArray b1, ByteArray b2 ) {
		if( b1.getBytes().length != b2.getBytes().length ) {
			System.out.println( "ByteArray sizes are different: " + b1.getBytes().length + "," + b2.getBytes().length );
			return false;
		} else {
			for( int i = 0; i < b1.getBytes().length; i++ ) {
				if( b1.getBytes()[ i ] != b2.getBytes()[ i ] ) {
					System.out.println(
						"Bytes at index " + i + " are different: " + b1.getBytes()[ i ] + "," + b2.getBytes()[ i ] );
					return false;
				}
			}
		}
		return true;
	}

}
