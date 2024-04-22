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
import jolie.lang.parse.module.ModuleException;
import jolie.runtime.Value;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.typing.TypeCheckingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author claudio
 * @author jonas - rewritten to work with jolie2java V2
 */
public class JavaDocumentCreatorTest {

	private static final Boolean DELETE_AFTER_TEST = true;

	@BeforeClass
	public static void setUpClass() throws IOException, ParserException, CodeCheckException, CommandLineException, ModuleException {
		ClassManager.generateClasses();

		final Path packagePath = Path.of( ClassManager.OUTPUTDIRECTORY.toString(), ClassManager.PACKAGENAME.split( "\\." ) );
		assertEquals( 
			"The number of generated package files/directories is wrong", 
			4L, Files.list( packagePath ).count() );
		assertEquals( 
			"The number of generated type files is wrong", 
			26L, Files.list( Path.of( packagePath.toString(), ClassManager.TYPEPACKAGE.split( "\\." ) ) ).count() );
		assertEquals( 
			"The number of generated fault files is wrong", 
			11L, Files.list( Path.of( packagePath.toString(), ClassManager.FAULTPACKAGE.split( "\\." ) ) ).count() );

		ClassManager.compileClasses();
	}

	@AfterClass
	public static void tearDownClass() throws IOException {
		if( DELETE_AFTER_TEST )
			ClassManager.deleteClasses();
	}

	@Before
	public void setUp() {}

	@After
	public void tearDown() {}


	@Test
	public void testInterface() throws ClassNotFoundException {
		final Class< ? > cls = ClassManager.getClass( ".TestInterface" );
		assertEquals(
			"Number of generated methods does not correspond", 34L,
			Arrays.stream( cls.getMethods() ).filter( m -> m.getName().startsWith( "test" ) ).count() );
	}

	@Test
	public void testFlatStructures() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class<?> single_cls = ClassManager.getTypeClass( "FlatStructureType" );
		final Value single_v = DataManager.getFlatStructureType();
		final JolieValue single_j = performFromValueTests( single_cls, single_v );
		performToValueTests( single_cls, single_v, single_j );
		assertEquals( "single: The constructed JolieValue doesn't equal the converted JolieValue.", DataManager.constructFlatStructureType(), single_j );

		final Class<?> vector_cls = ClassManager.getTypeClass( "FlatStructureVectorsType" );
		final Value vector_v = DataManager.getFlatStructureVectorsType();
		final JolieValue vector_j = performFromValueTests( vector_cls, vector_v );
		performToValueTests( vector_cls, vector_v, vector_j );
		assertEquals( "vector: The constructed JolieValue doesn't equal the converted JolieValue.", DataManager.constructFlatStructureVectorsType(), vector_j );

		final Object builder = ValueUtils.invokeConstructFrom( single_cls, vector_j );
		ValueUtils.invokeSetter( builder, "contentValue", DataManager.TESTSTRING );
		ValueUtils.invokeSetter( builder, "bfield", DataManager.TESTINT );
		ValueUtils.invokeSetter( builder, "efield", DataManager.TESTSTRING );
		assertEquals( single_j, ValueUtils.invokeBuild( builder ) );
	}

	@Test
	public void testInlineStructures() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class<?> single_cls = ClassManager.getTypeClass( "InLineStructureType" );
		final Value single_v = DataManager.getInlineStructureType();
		final JolieValue single_j = performFromValueTests( single_cls, single_v );
		performToValueTests( single_cls, single_v, single_j );
		assertEquals( "single: The constructed JolieValue doesn't equal the converted JolieValue.", DataManager.constructInlineStructureType(), single_j );

		final Class<?> vector_cls = ClassManager.getTypeClass( "InLineStructureVectorsType" );
		final Value vector_v = DataManager.getInlineStructureVectorsType();
		final JolieValue vector_j = performFromValueTests( vector_cls, vector_v );
		performToValueTests( vector_cls, vector_v, vector_j );
		assertEquals( "vector: The constructed JolieValue doesn't equal the converted JolieValue.", DataManager.constructInlineStructureVectorsType(), vector_j );

		assertEquals( single_j, ValueUtils.invokeCreateFrom( single_cls, vector_j ) );
	}

	@Test
	public void testNewType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "NewType" ), 
			DataManager.getNewType() );
	}

	@Test
	public void testLinkedTypeStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "LinkedTypeStructureType" ), 
			DataManager.getLinkedTypeStructureType() );
	}

	@Test
	public void testLinkedTypeStructureVectorsType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "LinkedTypeStructureVectorsType" ), 
			DataManager.getLinkedTypeStructureVectorsType() );
	}

	@Test
	public void testChoiceLinkedType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class< ? > cls = ClassManager.getTypeClass( "ChoiceLinkedType" );

		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C1" ), DataManager.getLinkedTypeStructureType() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C2" ), DataManager.getIntValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C3" ), DataManager.getInlineStructureType() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C4" ), DataManager.getVoidValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C5" ), DataManager.getFlatStructureType() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C6" ), DataManager.getFlatStructureVectorsType() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceLinkedType$C7" ), DataManager.getStringValue() );
		performFromExceptionValueTest( cls );
	}

	@Test
	public void testChoiceInLineType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class< ? > cls = ClassManager.getTypeClass( "ChoiceInlineType" );
		
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceInlineType$C1" ), DataManager.getChoiceInlineType1() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceInlineType$C2" ), DataManager.getIntValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceInlineType$C3" ), DataManager.getChoiceInlineType2() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceInlineType$C4" ), DataManager.getChoiceInlineType3() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceInlineType$C5" ), DataManager.getStringValue() );
		performFromExceptionValueTest( cls );
	}

	@Test
	public void testChoiceSimpleType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class< ? > cls = ClassManager.getTypeClass( "ChoiceSimpleType" );

		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceSimpleType$C1" ), DataManager.getStringValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceSimpleType$C2" ), DataManager.getIntValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceSimpleType$C3" ), DataManager.getDoubleValue() );
		performChoiceValueTests( cls, ClassManager.getTypeClass( "ChoiceSimpleType$C4" ), DataManager.getVoidValue() );
		performFromExceptionValueTest( cls );
	}

	@Test
	public void testLinkedChoiceStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "LinkedChoiceStructureType" ), 
			DataManager.getLinkedChoiceStructureType() );
	}

	@Test
	public void testInlineChoiceStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "InlineChoiceStructureType" ), 
			DataManager.getInlineChoiceStructureType() );
	}

	@Test
	public void testInlinedLinkStructureType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		performValueTests(
			ClassManager.getTypeClass( "InlinedLinkStructureType" ), 
			DataManager.getInlinedLinkStructureType() );
	}
	
	@Test 
	public void testLinkInlining() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		try {
			ClassManager.getTypeClass( "InlinedLinkType" );
			assertTrue( "InlinedLinkType had a class generated for it.", false );
		} catch ( ClassNotFoundException e) {}

		/** commenting on choice options doesn't currently work
		 * try {
		 *	ClassManager.getTypeClass( "UnbuildableInlinedChoice$C3" );
		 * } catch ( ClassNotFoundException e ) {
		 *	assertTrue( "UnbuildableInlinedChoice didn't inline its choice option.", false );
		 * }
		 */

		final Class<?> cls = ClassManager.getTypeClass( "InlinedLinkStructureType" );
		assertEquals( "The field \"inlinedString\" of InlinedLinkStructureType wasn't of type Boolean",
			String.class, cls.getDeclaredField( "inlinedString" ).getType() );
		assertEquals( "The field \"inlinedLink\" of InlinedLinkStructureType wasn't of type RefinedStringType",
			ClassManager.getTypeClass( "RefinedStringType" ), cls.getDeclaredField( "inlinedLink" ).getType() );
		assertEquals( "The field \"inlinedLinkString\" of InlinedLinkStructureType wasn't of type Boolean",
			String.class, cls.getDeclaredField( "inlinedLinkString" ).getType() );
	}

	@Test
	public void testGenerateBuilder() {
		try {
			ClassManager.getTypeClass( "UnbuildableInlinedChoice$ListBuilder" );
			assertTrue( "UnbuildableInlinedChoice had a ListBuilder class generated for it.", false );
		} catch ( ClassNotFoundException e ) {}
		try {
			ClassManager.getTypeClass( "RootValue8Type$Builder" );
			assertTrue( "RootValue8Type had a Builder class generated for it.", false );
		} catch ( ClassNotFoundException e ) {}
		try {
			ClassManager.getTypeClass( "RootValue4Type$ListBuilder" );
			assertTrue( "RootValue4Type had a ListBuilder class generated for it.", false );
		} catch ( ClassNotFoundException e ) {}
		try {
			ClassManager.getTypeClass( "UnbuildableLinkStructureType$Dfield$Builder" );
			assertTrue( "UnbuildableLinkStructureType.Dfield had a Builder class generated for it.", false );
		} catch ( ClassNotFoundException e ) {}
	}

	@Test
	public void testRootValuesType() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, TypeCheckingException {
		final Class<?> anycls = ClassManager.getTypeClass( "RootValue8Type" );
		for( int i = 1; i <= 7; i++ ) {
			final Value v = DataManager.getRootValue( i );
			performValueTests( ClassManager.getTypeClass( "RootValue" + i + "Type" ), v );
			performValueTests( anycls, v );
		}
	}

	private static void performFromExceptionValueTest( final Class< ? > cls ) throws IllegalAccessException, NoSuchMethodException, SecurityException, TypeCheckingException {
		try {
			ValueUtils.invokeFromValue( cls, DataManager.getExceptionValue() );
			assertTrue( "Exception not thrown", false );
		} catch( java.lang.reflect.InvocationTargetException e ) {}
	}

	private static JolieValue performFromValueTests( final Class< ? > cls, final Value value ) throws IllegalAccessException, NoSuchMethodException, SecurityException, TypeCheckingException, InvocationTargetException {
		performFromExceptionValueTest( cls );
		return ValueUtils.invokeFromValue( cls, value );
	}

	private static void performToValueTests( final Class< ? > cls, final Value value, final JolieValue t ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, AssertionError {
		ValueUtils.compareValues( value, ValueUtils.invokeToValue( cls, t ) );
		ValueUtils.compareValues( value, JolieValue.toValue( t ) ); // checks that the children map is correct
	}

	private static void performValueTests( final Class< ? > cls, final Value value ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, TypeCheckingException { 
		performToValueTests( cls, value, performFromValueTests( cls, value ) );
	}

	private static void performChoiceValueTests( final Class< ? > cls, final Class< ? > subcls, final Value value ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, TypeCheckingException {
		final JolieValue instance = ValueUtils.invokeFromValue( cls, value );
		performToValueTests( cls, value, instance );
		assertEquals( "The result of calling fromValue on choice type wasn't an instance of the expected subclass.",
			subcls, instance.getClass() );
	}
}
