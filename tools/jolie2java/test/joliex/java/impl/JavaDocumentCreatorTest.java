/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.java.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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
	private static final String TESTSTRING = "test";
	private static final Integer TESTINTEGER = 1;
	private static final Double TESTDOUBLE = 1.1;
	private static final byte[] TESTRAW = new byte[]{ (byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea, 0x3a, 0x69, 0x10, (byte) 0xa2 };
	private static final Boolean TESTBOOL = true;
	private static final Long TESTLONG = 2L;

	private static URLClassLoader classLoader;

	HashMap<String, Method> setMethodList = new HashMap<>();
	HashMap<String, Method> getMethodList = new HashMap<>();
	HashMap<String, Method> addMethodList = new HashMap<>();
	HashMap<String, Method> removeMethodList = new HashMap<>();
	HashMap<String, Method> sizeMethodList = new HashMap<>();

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
			cmdParser.programFilepath().toURI(),
			cmdParser.charset(),
			cmdParser.includePaths(),
			cmdParser.jolieClassLoader(),
			cmdParser.definedConstants(),
			false );

		//Program program = parser.parse();
		inspector = ParsingUtils.createInspector( program );

		JavaDocumentCreator instance = new JavaDocumentCreator( inspector, "com.test", null, false );
		instance.ConvertDocument();

		assertEquals( "The number of generated files is wrong", 16, new File( "./generated/com/test" ).list().length );

		// load classes
		File generated = new File( "./generated" );
		classLoader = URLClassLoader.newInstance( new URL[]{ generated.toURI().toURL() } );

		// compile files
		if ( generatedPath.exists() ) {
			String files[] = generatedPath.list();
			for( int i = 0; i < files.length; i++ ) {
				files[ i ] = generatedPath.getPath() + "/" + files[ i ];
			}

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			OutputStream output = new OutputStream()
			{
				private StringBuilder sb = new StringBuilder();

				@Override
				public void write( int b ) throws IOException
				{
					this.sb.append( (char) b );
				}

				@Override
				public String toString()
				{
					return this.sb.toString();
				}
			};

			compiler.run( null, null, output, files );
			System.out.println( output );

		}
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
	public void testFlatStructure() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testFlatStructure";
		// FileStructure
		Class<?> FlatStructureType = Class.forName( "com.test.FlatStructureType", true, classLoader );
		Constructor flatStructureTypeConstructor = FlatStructureType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface flatStructureType = (Jolie2JavaInterface) flatStructureTypeConstructor.newInstance( getFlatStructuredType() );
		// check constructor and getValues
		assertTrue( compareValues( getFlatStructuredType(), flatStructureType.getValue(), 0 ) );
		Jolie2JavaInterface flatStructureTypeEmpty = (Jolie2JavaInterface) FlatStructureType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );
		// check methods
		checkMethods( FlatStructureType, getFlatStructuredType() );
		System.out.println( testName + " checking methods OK" );
		checkRootMethods( FlatStructureType, flatStructureTypeEmpty, getFlatStructuredType() );
		invokingSetAddGetMethods( FlatStructureType, flatStructureTypeEmpty, getFlatStructuredType() );
		invokingRemoveSizetMethods( flatStructureTypeEmpty, getFlatStructuredType() );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testFlatStructureVectors() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testFlatStructureVectors";
		// FileStructureVector
		Class<?> FlatStructureVectorsType = Class.forName( "com.test.FlatStructureVectorsType", true, classLoader );
		Constructor flatStructureVectorsTypeConstructor = FlatStructureVectorsType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface flatStructureVectorsType = (Jolie2JavaInterface) flatStructureVectorsTypeConstructor.newInstance( getFlatStructuredVectorsType() );
		// check constructor and getValues
		assertTrue( compareValues( getFlatStructuredVectorsType(), flatStructureVectorsType.getValue(), 0 ) );
		Jolie2JavaInterface flatStructureVectorsTypeEmpty = (Jolie2JavaInterface) FlatStructureVectorsType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );

		// check methods
		checkMethods( FlatStructureVectorsType, getFlatStructuredVectorsType() );
		System.out.println( testName + " checking methods OK" );
		checkRootMethods( FlatStructureVectorsType, flatStructureVectorsTypeEmpty, getFlatStructuredVectorsType() );
		invokingSetAddGetMethods( FlatStructureVectorsType, flatStructureVectorsTypeEmpty, getFlatStructuredVectorsType() );
		invokingRemoveSizetMethods( flatStructureVectorsTypeEmpty, getFlatStructuredVectorsType() );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testInLineStructureType() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testInLineStructureType";
		// FileStructureVector
		Class<?> InLineStructureType = Class.forName( "com.test.InLineStructureType", true, classLoader );
		Constructor inLineStructureTypeConstructor = InLineStructureType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface inLineStructureType = (Jolie2JavaInterface) inLineStructureTypeConstructor.newInstance( getInlineStructureType() );
		// check constructor and getValues
		assertTrue( compareValues( getInlineStructureType(), inLineStructureType.getValue(), 0 ) );
		Jolie2JavaInterface inLineStructureTypeEmpty = (Jolie2JavaInterface) InLineStructureType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );

		// check methods
		checkMethods( InLineStructureType, getInlineStructureType() );
		System.out.println( testName + " checking methods OK" );
		checkRootMethods( InLineStructureType, inLineStructureTypeEmpty, getInlineStructureType() );
		invokingSetAddGetMethods( InLineStructureType, inLineStructureTypeEmpty, getInlineStructureType() );
		invokingRemoveSizetMethods( inLineStructureTypeEmpty, getInlineStructureType() );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testInLineStructureVectorsType() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testInLineStructureVectorsType";
		// FileStructureVector
		Class<?> InLineStructureVectorsType = Class.forName( "com.test.InLineStructureVectorsType", true, classLoader );
		Constructor inLineStructureVectorsTypeConstructor = InLineStructureVectorsType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface inLineStructureVectorsType = (Jolie2JavaInterface) inLineStructureVectorsTypeConstructor.newInstance( getInlineStructureVectorsType() );
		// check constructor and getValues
		assertTrue( compareValues( getInlineStructureVectorsType(), inLineStructureVectorsType.getValue(), 0 ) );
		Jolie2JavaInterface inLineStructureVectorsTypeEmpty = (Jolie2JavaInterface) InLineStructureVectorsType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );

		// check methods
		checkMethods( InLineStructureVectorsType, getInlineStructureVectorsType() );
		System.out.println( testName + " checking methods OK" );
		checkRootMethods( InLineStructureVectorsType, inLineStructureVectorsTypeEmpty, getInlineStructureVectorsType() );
		invokingSetAddGetMethods( InLineStructureVectorsType, inLineStructureVectorsTypeEmpty, getInlineStructureVectorsType() );
		invokingRemoveSizetMethods( inLineStructureVectorsTypeEmpty, getInlineStructureVectorsType() );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testLinkedTypeStructureType() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testLinkedTypeStructureType";
		// FileStructureVector
		Class<?> LinkedTypeStructureType = Class.forName( "com.test.LinkedTypeStructureType", true, classLoader );
		Constructor linkedTypeStructureTypeConstructor = LinkedTypeStructureType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface linkedTypeStructureType = (Jolie2JavaInterface) linkedTypeStructureTypeConstructor.newInstance( getLinkedTypeStructureType() );
		// check constructor and getValues
		assertTrue( compareValues( getLinkedTypeStructureType(), linkedTypeStructureType.getValue(), 0 ) );
		Jolie2JavaInterface linkedTypeStructureTypeEmpty = (Jolie2JavaInterface) LinkedTypeStructureType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );

		// check methods
		checkMethods( LinkedTypeStructureType, getLinkedTypeStructureType() );
		System.out.println( testName + " checking methods OK" );
		invokingSetAddGetMethodsForLinkedType( linkedTypeStructureTypeEmpty );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testLinkedTypeStructureVectorsType() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		String testName = "testLinkedTypeStructureVectorsType";
		// FileStructureVector
		Class<?> LinkedTypeStructureVectorsType = Class.forName( "com.test.LinkedTypeStructureVectorsType", true, classLoader );
		Constructor linkedTypeStructureVecotrsTypeConstructor = LinkedTypeStructureVectorsType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface linkedTypeStructureVectorsType = (Jolie2JavaInterface) linkedTypeStructureVecotrsTypeConstructor.newInstance( getLinkedTypeStructureVectorsType() );
		// check constructor and getValues
		assertTrue( compareValues( getLinkedTypeStructureVectorsType(), linkedTypeStructureVectorsType.getValue(), 0 ) );
		Jolie2JavaInterface linkedTypeStructureVectorsTypeEmpty = (Jolie2JavaInterface) LinkedTypeStructureVectorsType.newInstance();
		System.out.println( testName + " contructors and getValue() OK" );

		// check methods
		checkMethods( LinkedTypeStructureVectorsType, getLinkedTypeStructureVectorsType() );
		System.out.println( testName + " checking methods OK" );
		invokingSetAddGetMethodsForLinkedVectorsType( linkedTypeStructureVectorsTypeEmpty );
		System.out.println( testName + " invoking methods OK" );
	}

	@Test
	public void testRootValuesType() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{

		int numberOfRootValueTypeSamples = 7;
		for( int i = 1; i <= numberOfRootValueTypeSamples; i++ ) {
			String testName = "testRootValue" + i + "Type";
			Class<?> RootValueType = Class.forName( "com.test.RootValue" + i + "Type", true, classLoader );
			Constructor rootValueConstructotr = RootValueType.getConstructor( new Class[]{ Value.class } );
			Jolie2JavaInterface rootValueTypeInstance = (Jolie2JavaInterface) rootValueConstructotr.newInstance( getRootValue( i ) );
			assertTrue( compareValues( getRootValue( i ), rootValueTypeInstance.getValue(), 0 ) );
			Jolie2JavaInterface rootValueTypeInstanceEmpty = (Jolie2JavaInterface) RootValueType.newInstance();
			System.out.println( testName + " contructors and getValue() OK" );
			// check methods
			checkMethods( RootValueType, getRootValue( i ) );
			System.out.println( testName + " checking methods OK" );
			if ( i == 7 ) {
				checkRootMethodsAnyValue( RootValueType, rootValueTypeInstanceEmpty, getRootValue( i ) );
			} else {
				checkRootMethods( RootValueType, rootValueTypeInstanceEmpty, getRootValue( i ) );
			}
			invokingSetAddGetMethods( RootValueType, rootValueTypeInstanceEmpty, getRootValue( i ) );
			System.out.println( testName + " invoking methods OK" );
		}
	}

	private void checkMethods( Class cls, Value value )
	{
		setMethodList = new HashMap<>();
		getMethodList = new HashMap<>();
		addMethodList = new HashMap<>();
		removeMethodList = new HashMap<>();
		sizeMethodList = new HashMap<>();
		for( Entry<String, ValueVector> vv : value.children().entrySet() ) {
			if ( vv.getValue().size() == 1 ) {
				boolean foundGet = false;
				boolean foundSet = false;
				for( Method method : cls.getDeclaredMethods() ) {
					String mNameTmp = vv.getKey().substring( 0, 1 ).toUpperCase() + vv.getKey().substring( 1 );
					if ( method.getName().equals( "get" + mNameTmp ) ) {
						foundGet = true;
						getMethodList.put( mNameTmp, method );
					}
					if ( method.getName().equals( "set" + mNameTmp ) ) {
						foundSet = true;
						setMethodList.put( mNameTmp, method );
					}
				}
				assertTrue( "get method for field " + vv.getKey() + "not found, class " + cls.getName(), foundGet );
				assertTrue( "set method for field " + vv.getKey() + "not found, class " + cls.getName(), foundSet );
			} else {
				boolean foundAdd = false;
				boolean foundRemove = false;
				boolean foundSize = false;
				boolean foundGet = false;
				for( Method method : cls.getDeclaredMethods() ) {
					String mNameTmp = vv.getKey().substring( 0, 1 ).toUpperCase() + vv.getKey().substring( 1 );
					if ( method.getName().equals( "get" + mNameTmp ) || method.getName().equals( "get" + mNameTmp + "Value" ) ) {
						foundGet = true;
						getMethodList.put( mNameTmp, method );
					}
					if ( method.getName().equals( "add" + mNameTmp + "Value" ) ) {
						foundAdd = true;
						addMethodList.put( mNameTmp, method );
					}
					if ( method.getName().equals( "remove" + mNameTmp + "Value" ) ) {
						foundRemove = true;
						removeMethodList.put( mNameTmp, method );
					}
					if ( method.getName().equals( "get" + mNameTmp + "Size" ) ) {
						foundSize = true;
						sizeMethodList.put( mNameTmp, method );
					}
				}
				assertTrue( "get method for field " + vv.getKey() + "not found, class " + cls.getName(), foundGet );
				assertTrue( "add method for field " + vv.getKey() + "not found, class " + cls.getName(), foundAdd );
				assertTrue( "size method for field " + vv.getKey() + "not found, class " + cls.getName(), foundSize );
				assertTrue( "remove method for field " + vv.getKey() + "not found, class " + cls.getName(), foundRemove );
			}
		}
	}

	private void invokingRemoveSizetMethods( Object obj, Value v ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// invoking methods 
		for( Entry<String, ValueVector> vv : v.children().entrySet() ) {
			String mNameTmp = vv.getKey().substring( 0, 1 ).toUpperCase() + vv.getKey().substring( 1 );
			if ( vv.getValue().size() > 1 ) {
				Integer size = (Integer) sizeMethodList.get( mNameTmp ).invoke( obj );
				assertEquals( "size of vector of element " + vv.getKey() + " does not correspond", size, new Integer( vv.getValue().size() ) );
				for( int i = vv.getValue().size(); i > 0; i-- ) {
					removeMethodList.get( mNameTmp ).invoke( obj, (i - 1) );
				}
				size = (Integer) sizeMethodList.get( mNameTmp ).invoke( obj );
				assertEquals( "size of vector of element " + vv.getKey() + " does not correspond", size, new Integer( 0 ) );

			}
		}
	}

	private void invokingSetAddGetMethodsForLinkedType( Object obj ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
	{
		// invoking methods 
		Class<?> InLineStructureType = Class.forName( "com.test.InLineStructureType", true, classLoader );
		Constructor inLineStructureTypeConstructor = InLineStructureType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface inLineStructureType = (Jolie2JavaInterface) inLineStructureTypeConstructor.newInstance( getInlineStructureType() );
		setMethodList.get( "A" ).invoke( obj, inLineStructureType );
		Object a = getMethodList.get( "A" ).invoke( obj );
		assertEquals( "Linked type a does not correspond ", InLineStructureType.cast( a ), inLineStructureType );

		Class<?> InLineStructureVectorsType = Class.forName( "com.test.InLineStructureVectorsType", true, classLoader );
		Constructor inLineStructureVectorsTypeConstructor = InLineStructureVectorsType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface inLineStructureVectorsType = (Jolie2JavaInterface) inLineStructureVectorsTypeConstructor.newInstance( getInlineStructureVectorsType() );
		setMethodList.get( "B" ).invoke( obj, inLineStructureVectorsType );
		Object b = getMethodList.get( "B" ).invoke( obj );
		assertEquals( "Linked type b does not correspond ", InLineStructureVectorsType.cast( b ), inLineStructureVectorsType );

		Class<?> FlatStructureType = Class.forName( "com.test.FlatStructureType", true, classLoader );
		Constructor flatStructureTypeConstructor = FlatStructureType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface flatStructureType = (Jolie2JavaInterface) flatStructureTypeConstructor.newInstance( getFlatStructuredType() );
		setMethodList.get( "C" ).invoke( obj, flatStructureType );
		Object c = getMethodList.get( "C" ).invoke( obj );
		assertEquals( "Linked type c does not correspond ", FlatStructureType.cast( c ), flatStructureType );

		Class<?> NewType = Class.forName( "com.test.NewType", true, classLoader );
		Constructor newTypeConstructor = NewType.getConstructor( new Class[]{ Value.class } );
		Jolie2JavaInterface newType = (Jolie2JavaInterface) newTypeConstructor.newInstance( getNewType() );
		setMethodList.get( "D" ).invoke( obj, newType );
		Object d = getMethodList.get( "D" ).invoke( obj );
		assertEquals( "Linked type a does not correspond ", NewType.cast( d ), newType );
	}

	private void invokingSetAddGetMethodsForLinkedVectorsType( Object obj ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
	{
		// invoking methods 
		Class<?> InLineStructureType = Class.forName( "com.test.InLineStructureType", true, classLoader );
		Constructor inLineStructureTypeConstructor = InLineStructureType.getConstructor( new Class[]{ Value.class } );
		Integer aElements = 10;
		for( int i = 0; i < aElements; i++ ) {
			Jolie2JavaInterface inLineStructureType = (Jolie2JavaInterface) inLineStructureTypeConstructor.newInstance( getInlineStructureType() );
			addMethodList.get( "A" ).invoke( obj, inLineStructureType );
			Object a = getMethodList.get( "A" ).invoke( obj, i );
			assertEquals( "Linked type a does not correspond ", InLineStructureType.cast( a ), inLineStructureType );
		};
		Integer sizeA = (Integer) sizeMethodList.get( "A" ).invoke( obj );
		assertEquals( "LinkedVectorsType field a, wrong number of elements", sizeA, aElements );
		for( int i = aElements; i > 0; i-- ) {
			removeMethodList.get( "A" ).invoke( obj, i - 1 );
		};
		sizeA = (Integer) sizeMethodList.get( "A" ).invoke( obj );
		assertEquals( "LinkedVectorsType field a remove test, elemenst number should be 0", sizeA, new Integer( 0 ) );

		// TODO test field b
		Class<?> FlatStructureType = Class.forName( "com.test.FlatStructureType", true, classLoader );
		Constructor flatStructureTypeConstructor = FlatStructureType.getConstructor( new Class[]{ Value.class } );
		Integer cElements = 7;
		for( int i = 0; i < cElements; i++ ) {
			Jolie2JavaInterface flatStructureType = (Jolie2JavaInterface) flatStructureTypeConstructor.newInstance( getFlatStructuredType() );
			addMethodList.get( "C" ).invoke( obj, flatStructureType );
			Object c = getMethodList.get( "C" ).invoke( obj, i );
			assertEquals( "Linked type c does not correspond ", FlatStructureType.cast( c ), flatStructureType );
		};
		Integer sizeC = (Integer) sizeMethodList.get( "C" ).invoke( obj );
		assertEquals( "LinkedVectorsType field c, wrong number of elements", sizeC, cElements );
		for( int i = cElements; i > 0; i-- ) {
			removeMethodList.get( "C" ).invoke( obj, i - 1 );
		};
		sizeC = (Integer) sizeMethodList.get( "C" ).invoke( obj );
		assertEquals( "LinkedVectorsType field c remove test, elemenst number should be 0", sizeC, new Integer( 0 ) );

		Class<?> NewType = Class.forName( "com.test.NewType", true, classLoader );
		Constructor newTypeConstructor = NewType.getConstructor( new Class[]{ Value.class } );
		Integer dElements = 10;
		for( int i = 0; i < dElements; i++ ) {
			Jolie2JavaInterface newType = (Jolie2JavaInterface) newTypeConstructor.newInstance( getNewType() );
			addMethodList.get( "D" ).invoke( obj, newType );
			Object d = getMethodList.get( "D" ).invoke( obj, i );
			assertEquals( "Linked type c does not correspond ", NewType.cast( d ), newType );
		};
		Integer sizeD = (Integer) sizeMethodList.get( "D" ).invoke( obj );
		assertEquals( "LinkedVectorsType field d, wrong number of elements", sizeD, dElements );
		for( int i = dElements; i > 0; i-- ) {
			removeMethodList.get( "D" ).invoke( obj, i - 1 );
		};
		sizeD = (Integer) sizeMethodList.get( "D" ).invoke( obj );
		assertEquals( "LinkedVectorsType field d remove test, elemenst number should be 0", sizeD, new Integer( 0 ) );
	}

	private void checkRootMethods( Class cls, Object obj, Value v ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException
	{
		if ( v.isBool() || v.isByteArray() || v.isString() || v.isDouble() || v.isInt() || v.isLong() ) {
			Method getRootValue = cls.getDeclaredMethod( "getRootValue" );
			if ( v.isBool() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", Boolean.class );
				setRootValue.invoke( obj, v.boolValue() );
				assertEquals( "Root values in methods do not correspond", v.boolValue(), (Boolean) getRootValue.invoke( obj ) );
			}
			if ( v.isByteArray() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", ByteArray.class );
				setRootValue.invoke( obj, v.byteArrayValue() );
				assertTrue( "Root values in methods do not correspond", compareByteArrays( v.byteArrayValue(), (ByteArray) getRootValue.invoke( obj ) ) );
			}
			if ( v.isString() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", String.class );
				setRootValue.invoke( obj, v.strValue() );
				assertEquals( "Root values in methods do not correspond", v.strValue(), (String) getRootValue.invoke( obj ) );
			}
			if ( v.isDouble() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", Double.class );
				setRootValue.invoke( obj, v.doubleValue() );
				assertEquals( "Root values in methods do not correspond", new Double( v.doubleValue() ), (Double) getRootValue.invoke( obj ) );
			}
			if ( v.isInt() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", Integer.class );
				setRootValue.invoke( obj, v.intValue() );
				assertEquals( "Root values in methods do not correspond", new Integer( v.intValue() ), (Integer) getRootValue.invoke( obj ) );
			}
			if ( v.isLong() ) {
				Method setRootValue = cls.getDeclaredMethod( "setRootValue", Long.class );
				setRootValue.invoke( obj, v.longValue() );
				assertEquals( "Root values in methods do not correspond", new Long( v.longValue() ), (Long) getRootValue.invoke( obj ) );
			}
		}
	}

	private void checkRootMethodsAnyValue( Class cls, Object obj, Value v ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException
	{
		if ( v.isBool() || v.isByteArray() || v.isString() || v.isDouble() || v.isInt() || v.isLong() ) {
			Method getRootValue = cls.getDeclaredMethod( "getRootValue" );
			Method setRootValue = cls.getDeclaredMethod( "setRootValue", Object.class );
			if ( v.isBool() ) {
				setRootValue.invoke( obj, v.boolValue() );
				assertEquals( "Root values in methods do not correspond", v.boolValue(), (Boolean) getRootValue.invoke( obj ) );
			}
			if ( v.isByteArray() ) {
				setRootValue.invoke( obj, v.byteArrayValue() );
				assertTrue( "Root values in methods do not correspond", compareByteArrays( v.byteArrayValue(), (ByteArray) getRootValue.invoke( obj ) ) );
			}
			if ( v.isString() ) {
				setRootValue.invoke( obj, v.strValue() );
				assertEquals( "Root values in methods do not correspond", v.strValue(), (String) getRootValue.invoke( obj ) );
			}
			if ( v.isDouble() ) {
				setRootValue.invoke( obj, v.doubleValue() );
				assertEquals( "Root values in methods do not correspond", new Double( v.doubleValue() ), (Double) getRootValue.invoke( obj ) );
			}
			if ( v.isInt() ) {
				setRootValue.invoke( obj, v.intValue() );
				assertEquals( "Root values in methods do not correspond", new Integer( v.intValue() ), (Integer) getRootValue.invoke( obj ) );
			}
			if ( v.isLong() ) {
				setRootValue.invoke( obj, v.longValue() );
				assertEquals( "Root values in methods do not correspond", new Long( v.longValue() ), (Long) getRootValue.invoke( obj ) );
			}
		}
	}

	private void invokingSetAddGetMethods( Class cls, Object obj, Value v ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException
	{

		// invoking methods 
		for( Entry<String, ValueVector> vv : v.children().entrySet() ) {
			if ( vv.getValue().size() > 1 ) {
				for( int i = 0; i < vv.getValue().size(); i++ ) {
					Value value = vv.getValue().get( i );
					if ( value.hasChildren() ) {
						String mNameTmp = vv.getKey().substring( 0, 1 ).toUpperCase() + vv.getKey().substring( 1 );
						for( int y = 0; y < addMethodList.get( mNameTmp ).getParameterTypes().length; y++ ) {
							Class SubClass = addMethodList.get( mNameTmp ).getParameterTypes()[ y ];
							Constructor subClassConstructor = SubClass.getConstructors()[ 0 ];
							Object subClassInstance = subClassConstructor.newInstance( obj, value );
							addMethodList.get( mNameTmp ).invoke( obj, subClassInstance );
							Object getSubClassInstance = getMethodList.get( mNameTmp ).invoke( obj, i );
							assertTrue( "SubClasses (" + SubClass.getName() + ") are not equal", subClassInstance.equals( getSubClassInstance ) );
						}
					} else {
						invokingAddMethodForVector( value, vv.getKey(), obj, i );
					}
				}
			} else {
				Value value = vv.getValue().get( 0 );
				if ( value.hasChildren() ) {
					String mNameTmp = vv.getKey().substring( 0, 1 ).toUpperCase() + vv.getKey().substring( 1 );
					for( int i = 0; i < setMethodList.get( mNameTmp ).getParameterTypes().length; i++ ) {
						Class SubClass = setMethodList.get( mNameTmp ).getParameterTypes()[ i ];

						Constructor subClassConstructor = null;
						for( int c = 0; c < SubClass.getConstructors().length; c++ ) {
							if ( SubClass.getConstructors()[ c ].getParameterTypes().length > 1 ) {
								subClassConstructor = SubClass.getConstructors()[ c ];
							}
						}
						Object subClassInstance = subClassConstructor.newInstance( obj, value );
						setMethodList.get( mNameTmp ).invoke( obj, subClassInstance );
						Object getSubClassInstance = getMethodList.get( mNameTmp ).invoke( obj );
						assertTrue( "SubClasses (" + SubClass.getName() + ") are not equal", subClassInstance.equals( getSubClassInstance ) );
					}
				} else {
					invokingSetGetMethodsForFlatType( value, vv.getKey(), obj );
				}
			}
		}
	}

	private void invokingSetGetMethodsForFlatType( Value v, String fieldName, Object obj ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{

		String mNameTmp = fieldName.substring( 0, 1 ).toUpperCase() + fieldName.substring( 1 );
		if ( v.isBool() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.boolValue() );
			Boolean returnValue = (Boolean) getMethodList.get( mNameTmp ).invoke( obj );
			assertEquals( "check methods for field " + fieldName + " failed", v.boolValue(), returnValue );
		} else if ( v.isInt() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.intValue() );
			Integer returnValue = (Integer) getMethodList.get( mNameTmp ).invoke( obj );
			assertEquals( "check methods for field " + fieldName + " failed", v.intValue(), returnValue.intValue() );
		} else if ( v.isString() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.strValue() );
			String returnValue = (String) getMethodList.get( mNameTmp ).invoke( obj );
			assertEquals( "check methods for field " + fieldName + " failed", v.strValue(), returnValue );
		} else if ( v.isDouble() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.doubleValue() );
			Double returnValue = (Double) getMethodList.get( mNameTmp ).invoke( obj );
			assertEquals( "check methods for field " + fieldName + " failed", new Double( v.doubleValue() ), returnValue );
		} else if ( v.isLong() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.longValue() );
			Long returnValue = (Long) getMethodList.get( mNameTmp ).invoke( obj );
			assertEquals( "check methods for field " + fieldName + " failed", v.longValue(), returnValue.longValue() );
		} else if ( v.isByteArray() ) {
			setMethodList.get( mNameTmp ).invoke( obj, v.byteArrayValue() );
			ByteArray returnValue = (ByteArray) getMethodList.get( mNameTmp ).invoke( obj );
			assertTrue( "check methods for field " + fieldName + " failed", compareByteArrays( returnValue, v.byteArrayValue() ) );
		} else {
			setMethodList.get( mNameTmp ).invoke( obj, v );
			Value returnValue = (Value) getMethodList.get( mNameTmp ).invoke( obj );
			assertTrue( "check methods for field " + fieldName + " failed", compareValues( returnValue, v, 0 ) );
		}
	}

	private void invokingAddMethodForVector( Value v, String fieldName, Object obj, int index ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		String mNameTmp = fieldName.substring( 0, 1 ).toUpperCase() + fieldName.substring( 1 );
		if ( v.isBool() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.boolValue() );
			Boolean returnValue = (Boolean) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertEquals( "check methods for field " + fieldName + ", index " + index + " failed", v.boolValue(), returnValue );
		} else if ( v.isInt() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.intValue() );
			Integer returnValue = (Integer) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertEquals( "check methods for field " + fieldName + ", index " + index + " failed", v.intValue(), returnValue.intValue() );
		} else if ( v.isString() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.strValue() );
			String returnValue = (String) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertEquals( "check methods for field " + fieldName + ", index " + index + " failed", v.strValue(), returnValue );
		} else if ( v.isDouble() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.doubleValue() );
			Double returnValue = (Double) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertEquals( "check methods for field " + fieldName + ", index " + index + " failed",
				new Double( v.doubleValue() ), returnValue );
		} else if ( v.isLong() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.longValue() );
			Long returnValue = (Long) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertEquals( "check methods for field " + fieldName + ", index " + index + " failed", v.longValue(), returnValue.longValue() );
		} else if ( v.isByteArray() ) {
			addMethodList.get( mNameTmp ).invoke( obj, v.byteArrayValue() );
			ByteArray returnValue = (ByteArray) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertTrue( "check methods for field " + fieldName + ", index " + index + " failed", compareByteArrays( returnValue, v.byteArrayValue() ) );
		} else {
			addMethodList.get( mNameTmp ).invoke( obj, v );
			Value returnValue = (Value) getMethodList.get( mNameTmp ).invoke( obj, index );
			assertTrue( "check methods for field " + fieldName + ", index " + index + " failed", compareValues( returnValue, v, 0 ) );
		}
	}

	private Value getRootValue( int index )
	{
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

	private Value getNewType()
	{
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
			e.add( getFlatStructuredType() );
		}

		return testValue;

	}

	private Value getLinkedTypeStructureType()
	{
		Value testValue = Value.create();
		Value a = testValue.getFirstChild( "a" );
		a.add( getInlineStructureType() );
		Value b = testValue.getFirstChild( "b" );
		b.add( getInlineStructureVectorsType() );
		Value c = testValue.getFirstChild( "c" );
		c.add( getFlatStructuredType() );
		Value d = testValue.getFirstChild( "d" );
		d.add( getNewType() );

		return testValue;
	}

	private Value getLinkedTypeStructureVectorsType()
	{
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

	private Value getInlineStructureType()
	{
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

	private Value getInlineStructureVectorsType()
	{
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
					fh.get( y ).getFirstChild( "abc" ).setValue( TESTSTRING );
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

	private Value getFlatStructuredType()
	{
		Value testValue = Value.create();
		testValue.setValue( TESTSTRING );
		testValue.getFirstChild( "afield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "bfield" ).setValue( TESTINTEGER );
		testValue.getFirstChild( "cfield" ).setValue( TESTDOUBLE );
		testValue.getFirstChild( "dfield" ).setValue( new ByteArray( TESTRAW ) );
		testValue.getFirstChild( "efield" ).setValue( TESTSTRING );
		testValue.getFirstChild( "ffield" ).setValue( TESTBOOL );
		testValue.getFirstChild( "gfield" ).setValue( getTestUndefined() );
		testValue.getFirstChild( "hfield" ).setValue( TESTLONG );
		return testValue;
	}

	private Value getFlatStructuredVectorsType()
	{
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
			testValue.getNewChild( "gfield" ).setValue( getTestUndefined() );
		}
		for( int i = 0; i < 2; i++ ) {
			testValue.getNewChild( "hfield" ).setValue( TESTLONG );
		}
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

	private boolean compareValues( Value v1, Value v2, int level )
	{
		boolean resp = true;
		if ( !checkRootValue( v1, v2 ) ) {
			System.out.println( "level: " + level + " Root values are different" );
			System.out.println( v1.toString() + "," + v2.toString() );
			return false;
		}
		// from v1 -> v2
		for( Entry<String, ValueVector> entry : v1.children().entrySet() ) {
			if ( !v2.hasChildren( entry.getKey() ) ) {
				System.out.println( "level: " + level + " from v1 -> v2: field " + entry.getKey() + " not present" );
				return false;
			} else {
				if ( entry.getValue().size() != v2.getChildren( entry.getKey() ).size() ) {
					System.out.println( "level: " + level + " Node name " + entry.getKey() );
					System.out.println( "level: " + level + " from v1 -> v2: The number of subnodes is different: " + entry.getValue().size() + "," + v2.getChildren( entry.getKey() ).size() );
					return false;
				}
				for( int i = 0; i < entry.getValue().size(); i++ ) {
					resp = compareValues( entry.getValue().get( i ), v2.getChildren( entry.getKey() ).get( i ), level + 1 );
					if ( !resp ) {
						System.out.println( "level: " + level + " Error found in subnode " + entry.getKey() + ", index:" + i );
						return false;
					}
				}
			}
		}

		// from v2 -> v1
		for( Entry<String, ValueVector> entry : v2.children().entrySet() ) {
			if ( !v1.hasChildren( entry.getKey() ) ) {
				System.out.println( "level: " + level + " from v2 -> v1: field " + entry.getKey() + " not present" );
				return false;
			} else {
				if ( entry.getValue().size() != v1.getChildren( entry.getKey() ).size() ) {
					System.out.println( "level: " + level + " from v2 -> v1: The number of subnodes is different: " + entry.getValue().size() + "," + v1.getChildren( entry.getKey() ).size() );
					return false;
				}
				for( int i = 0; i < entry.getValue().size(); i++ ) {
					resp = compareValues( entry.getValue().get( i ), v1.getChildren( entry.getKey() ).get( i ), level + 1 );
					if ( !resp ) {
						System.out.println( "level: " + level + " Error found in subnode " + entry.getKey() + ", index:" + i );
						return false;
					}
				}
			}
		}
		return resp;
	}

	private boolean checkRootValue( Value v1, Value v2 )
	{
		boolean resp = true;
		if ( v1.isString() && !v2.isString() ) {
			resp = false;
		} else if ( v1.isString() && v2.isString() && (v1.strValue() != v2.strValue()) ) {
			resp = false;
		}
		if ( v1.isBool() && !v2.isBool() ) {
			resp = false;
		} else if ( v1.isBool() && v2.isBool() && (v1.boolValue() != v2.boolValue()) ) {
			resp = false;
		}
		if ( v1.isByteArray() && !v2.isByteArray() ) {
			resp = false;
		} else if ( v1.isByteArray() && v2.isByteArray() ) {
			resp = compareByteArrays( v1.byteArrayValue(), v2.byteArrayValue() );
		}
		if ( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if ( v1.isDouble() && !v2.isDouble() && (v1.doubleValue() != v2.doubleValue()) ) {
			resp = false;
		}
		if ( v1.isDouble() && !v2.isDouble() ) {
			resp = false;
		} else if ( v1.isDouble() && v2.isDouble() && (v1.intValue() != v2.intValue()) ) {
			resp = false;
		}
		if ( v1.isLong() && !v2.isLong() ) {
			resp = false;
		} else if ( v1.isLong() && v2.isLong() && (v1.longValue() != v2.longValue()) ) {
			resp = false;
		}

		if ( !resp ) {
			System.out.println( "v1:" + v1.strValue() + ",isBool:" + v1.isBool() + ",isInt:" + v1.isInt() + ",isLong:" + v1.isLong() + ",isDouble:" + v1.isDouble() + ",isByteArray:" + v1.isByteArray() );
			System.out.println( "v2:" + v2.strValue() + ",isBool:" + v2.isBool() + ",isInt:" + v2.isInt() + ",isLong:" + v2.isLong() + ",isDouble:" + v2.isDouble() + ",isByteArray:" + v2.isByteArray() );
		}
		return resp;

	}

	private boolean compareByteArrays( ByteArray b1, ByteArray b2 )
	{
		if ( b1.getBytes().length != b2.getBytes().length ) {
			System.out.println( "ByteArray sizes are different: " + b1.getBytes().length + "," + b2.getBytes().length );
			return false;
		} else {
			for( int i = 0; i < b1.getBytes().length; i++ ) {
				if ( b1.getBytes()[ i ] != b2.getBytes()[ i ] ) {
					System.out.println( "Bytes at index " + i + " are different: " + b1.getBytes()[ i ] + "," + b2.getBytes()[ i ] );
					return false;
				}
			}
		}
		return true;
	}
}
