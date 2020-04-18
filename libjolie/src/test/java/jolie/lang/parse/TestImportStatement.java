package jolie.lang.parse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jolie.lang.Constants;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

public class TestImportStatement
{

    InputStream is;
    static SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
    private static Path packageDir =
            Paths.get( TestImportStatement.class.getClassLoader().getResource( "." ).getPath(),
                    Constants.PACKAGES_DIR );

    @Test
    void testImportTestSuite()
    {
        String code = "from .private.imports.point import point";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
    void testImportChoiceType()
    {
        String code = "from type import number";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        Set< String > expectedType = new HashSet<>();
        expectedType.add( "dateFoo" );
        expectedType.add( "date" );
        expectedType.add( "foo" );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
            throw new Exception( "type \"dateFoo\" not found" );
        } );
    }

    @Test
    void testImportInlineType()
    {
        String code = "from type import date";
        this.is = new ByteArrayInputStream( code.getBytes() );
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
        InstanceCreator oc = new InstanceCreator( new String[] {packageDir.toString()} );
        assertDoesNotThrow( () -> {
            OLParser olParser = oc.createOLParser( is );
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
        OLParser olParser = oc.createOLParser( is );

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
