package jolie.lang.parse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.util.InstanceCreator;
import jolie.util.jap.JolieURLStreamHandlerFactory;


public class TestParsingImportStatement
{

    static {
        JolieURLStreamHandlerFactory.registerInVM();
    }

    private static String BASE_DIR = "imports/";
    InputStream is;
    static SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
    private static URL baseDir =
            TestParsingImportStatement.class.getClassLoader().getResource( BASE_DIR );


    @Test
    void testImportJAP()
    {
        String code = "from twice import TwiceAPI";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (InterfaceDefinition id : pi.getInterfaces()) {
                if ( id.name().equals( "TwiceAPI" ) && id.operationsMap().containsKey( "twice" ) ) {
                    return;
                }
            }
            throw new Exception(
                    "interface \"TwiceAPI\" not found and operation \"twice\" not found" );
        } );
    }

    @Test
    void testCyclicDependencyValid()
    {
        URL src = getClass().getClassLoader().getResource( "imports/cyclic/A_valid.ol" );
        assertDoesNotThrow( () -> {
            is = src.openStream();
        } );
        InstanceCreator oc = new InstanceCreator( new String[0] );

        Set< String > expectedType = new HashSet<>();
        expectedType.add( "foo" );
        expectedType.add( "bar" );
        expectedType.add( "b" );
        assertDoesNotThrow( () -> {
            URI source = Paths.get( src.toURI() ).getParent().toUri();
            OLParser olParser = oc.createOLParser( new Scanner( is, source, null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( expectedType.contains( td.id() ) ) {
                    expectedType.remove( td.id() );
                    if ( expectedType.size() == 0 ) {
                        return;
                    }
                }
            }
            throw new Exception( "definition incomplete" );
        } );
    }

    @Test
    void testCyclicDependency() throws URISyntaxException, IOException
    {
        String errorMessage = "cyclic dependency detected";

        URL src = getClass().getClassLoader().getResource( BASE_DIR + "cyclic/A.ol" );
        assertDoesNotThrow( () -> {
            is = src.openStream();
        } );
        InstanceCreator oc = new InstanceCreator( new String[0] );

        OLParser olParser = oc.createOLParser( src.toURI(), is );

        Exception exception = assertThrows( ParserException.class, () -> olParser.parse(),
                "Expected parse() to throw, with " + errorMessage + " but it didn't" );
        assertTrue( exception.getMessage().contains( errorMessage ) );
    }

    @Test
    void testImportTestSuite()
    {
        String code = "from .private.imports.point import point";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser =
                    oc.createOLParser( Paths.get( baseDir.getPath(), "import.ol" ).toUri(), is );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( td.id().equals( "point" ) ) {
                    return;
                }
            }
            throw new Exception( "type \"point\" not found" );
        } );
    }

    @Test
    void testImportProcedureDefinition()
    {
        String code = "from procedure import foo";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (DefinitionNode node : pi.getProcedureDefinitions()) {
                if ( node.name().equals( "foo" ) ) {
                    return;
                }
            }
            throw new Exception( "procedure \"foo\" not found" );
        } );
    }

    @Test
    void testImportInterfaceDefinitionRR()
    {
        String code = "from interface import twiceIface";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (InterfaceDefinition id : pi.getInterfaces()) {
                if ( id.name().equals( "twiceIface" )
                        && id.operationsMap().containsKey( "twice" ) ) {
                    return;
                }
            }
            throw new Exception(
                    "interface \"twiceIface\" not found and operation \"twice\" not found" );
        } );
    }

    @Test
    void testImportInterfaceDefinitionOW()
    {
        String code = "from interface import aIface";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (InterfaceDefinition id : pi.getInterfaces()) {
                if ( id.name().equals( "aIface" ) && id.operationsMap().containsKey( "notice" ) ) {
                    return;
                }
            }
            throw new Exception(
                    "interface \"aIface\" not found and operation \"notice\" not found" );
        } );
    }

    @Test
    void testImportInterfaceDefinitionCombine()
    {
        String code = "from interface import bIface";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (InterfaceDefinition id : pi.getInterfaces()) {
                if ( id.name().equals( "bIface" ) && id.operationsMap().containsKey( "notice" )
                        && id.operationsMap().containsKey( "twice" ) ) {
                    return;
                }
            }
            throw new Exception(
                    "interface \"bIface\" not found and operation \"notice\", \"twice\" not found" );
        } );
    }

    @Test
    void testImportInterfaceDefinitionCustomType()
    {
        String code = "from interface import fooIface";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        Set< String > expectedType = new HashSet<>();
        expectedType.add( "foo" );
        expectedType.add( "bar" );
        expectedType.add( "err" );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (InterfaceDefinition id : pi.getInterfaces()) {
                if ( id.name().equals( "fooIface" ) && id.operationsMap().containsKey( "fooOp" ) ) {
                    break;
                }
            }

            for (TypeDefinition td : pi.getTypes()) {
                if ( expectedType.contains( td.id() ) ) {
                    expectedType.remove( td.id() );
                    if ( expectedType.size() == 0 ) {
                        return;
                    }
                }
            }
            throw new Exception( "definition incomplete" );
        } );
    }

    @Test
    void testImportChoiceType()
    {
        String code = "from type import number";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( td.id().equals( "number" ) ) {
                    return;
                }
            }
            throw new Exception( "type \"number\" not found" );
        } );
    }

    @Test
    void testImportCustomChoiceType()
    {
        String code = "from type import baz";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( td.id().equals( "baz" ) ) {
                    return;
                }
            }
            throw new Exception( "type \"baz\" not found" );
        } );
    }

    @Test
    void testImportLinkType()
    {
        String code = "from type import dateFoo";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        Set< String > expectedType = new HashSet<>();
        expectedType.add( "dateFoo" );
        expectedType.add( "date" );
        expectedType.add( "foo" );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( expectedType.contains( td.id() ) ) {
                    expectedType.remove( td.id() );
                    if ( expectedType.size() == 0 ) {
                        return;
                    }
                }
            }
            throw new Exception(
                    "type " + Arrays.toString( expectedType.toArray() ) + " not found" );
        } );
    }

    @Test
    void testImportInlineType()
    {
        String code = "from type import date";
        this.is = new ByteArrayInputStream( code.getBytes() );
        URL source = TestParsingImportStatement.class.getClassLoader().getResource( BASE_DIR );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, source.toURI(), null ) );
            Program p = olParser.parse();
            ProgramInspector pi = ParsingUtils.createInspector( p );

            for (TypeDefinition td : pi.getTypes()) {
                if ( td.id().equals( "date" ) ) {
                    return;
                }
            }
            throw new Exception( "type \"date\" not found" );
        } );
    }

    @ParameterizedTest
    @MethodSource("importStatementTestProvider")
    void testImportStatementParsing( String code )
    {
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[0] );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );
            olParser.parse();
        } );
    }

    private static Stream< Arguments > importStatementTestProvider()
    {
        return Stream.of( Arguments.of( "from A import AA" ),
                Arguments.of( "from A import A as B" ), Arguments.of( "from A import A as B, C" ),
                Arguments.of( "from A import A as B, C as D" ), Arguments.of( "from A import *" ),
                Arguments.of( "from .A import AA" ), Arguments.of( "from A.B import AA " ),
                Arguments.of( "from .A.B import AA " ) );
    }

    @ParameterizedTest
    @MethodSource("importStatementExceptionTestProvider")
    void testImportStatementExceptions( String code, String errorMessage )
            throws RuntimeException, IOException, URISyntaxException
    {
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[] {} );
        OLParser olParser = oc.createOLParser( new Scanner( is, baseDir.toURI(), null ) );

        Exception exception = assertThrows( ParserException.class, () -> olParser.parse(),
                "Expected parse() to throw, with " + errorMessage + " but it didn't" );
        assertTrue( exception.getMessage().contains( errorMessage ) );
    }

    private static Stream< Arguments > importStatementExceptionTestProvider()
    {
        return Stream.of(
                // Arguments.of( "from jolie2/import/simple-import/importstatement-test.ol import
                // AA",
                // "unable to find AA in" ),
                Arguments.of( "from somewhere import AA", "Unable to locate" ),
                Arguments.of( "from somewhere AA ",
                        "expected identifier, dot or import for an import statement" ) );
    }

    @AfterEach
    void closeSteam() throws IOException
    {
        this.is.close();
    }

}
