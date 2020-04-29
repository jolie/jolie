package jolie.lang.parse.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.util.CheckUtility;
import jolie.util.PortStub;
import jolie.util.TestCasesCreator;
import jolie.util.jap.JolieURLStreamHandlerFactory;

public class TestModuleParser
{

    static {
        JolieURLStreamHandlerFactory.registerInVM();
    }
    private static String BASE_DIR = "imports/";
    private static URL baseDir = TestModuleParser.class.getClassLoader().getResource( BASE_DIR );


    @Test
    void testImportNestedModules() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );

        URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();

        Map.Entry< URI, Set< String > > aOLSymbols = TestCasesCreator.createURISymbolsMap(
                Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(), "A", "from_b" );

        Map.Entry< URI, Set< String > > packageBDotBSymbols = TestCasesCreator.createURISymbolsMap(
                Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(), "C_type",
                "b_type", "b_type" );

        Map.Entry< URI, Set< String > > packageCSymbols =
                TestCasesCreator.createURISymbolsMap( Paths.get( baseDir.toURI() ).resolve( "A" )
                        .resolve( "packages" ).resolve( "C.ol" ).toUri(), "b_type", "c" );

        Map< URI, Set< String > > expectedSourceSymbols =
                Map.ofEntries( aOLSymbols, packageBDotBSymbols, packageCSymbols );

        assertDoesNotThrow( () -> {
            // parse a program
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            // crawl dependencies
            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );

            // check symbols
            Set< URI > visitedURI = new HashSet<>();
            for (ModuleRecord mr : crawlResult) {
                if ( expectedSourceSymbols.containsKey( mr.source() ) ) {
                    Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
                    CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
                    visitedURI.add( mr.source() );
                } else {
                    throw new Exception( "unexpected source " + mr.source().toString() );
                }
            }

            if ( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
                throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
                        + " not found in crawl result" );
            }

            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );

            // resolve symbols
            symbolResolver.resolveExternalSymbols();

            // check if all external symbol is linked
            for (ModuleRecord mr : crawlResult) {
                CheckUtility.checkSymbolNodeLinked( mr.symbolTable() );
            }

        } );

    }

    @Test
    void testImportWildCard() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        URI target = Paths.get( baseDir.toURI() ).resolve( "test_wildcard.ol" ).toUri();
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );

        Map.Entry< URI, Set< String > > expectedSymbolsRoot = TestCasesCreator.createURISymbolsMap(
                target, "date", "number", "foo", "bar", "baz", "dateFoo", "fooIface", "TwiceAPI" );
        Map.Entry< URI, Set< String > > expectedSymbolsExt =
                TestCasesCreator.createURISymbolsMap(
                        Paths.get( baseDir.toURI() ).resolve( "packages" ).resolve( "type.ol" )
                                .toFile().toURI(),
                        "date", "number", "foo", "bar", "baz", "dateFoo" );

        Map< URI, Set< String > > expectedSourceSymbols =
                Map.ofEntries( expectedSymbolsRoot, expectedSymbolsExt );

        Map.Entry< String, Set< String > > ifaceEntry =
                TestCasesCreator.createInterfaceStub( "TwiceAPI", "twice" );

        PortStub opPort = new PortStub( "OP", Map.ofEntries( ifaceEntry ), "twice" );

        Map.Entry< String, Set< String > > ifaceEntry2 =
                TestCasesCreator.createInterfaceStub( "fooIface", "fooOp" );

        PortStub opPort2 = new PortStub( "OP2", Map.ofEntries( ifaceEntry2 ), "fooOp" );

        Map< String, PortStub > expectedOutputPorts =
                TestCasesCreator.createExpectedPortMap( opPort, opPort2 );

        assertDoesNotThrow( () -> {

            // parse a program
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            // check symbols
            Set< URI > visitedURI = new HashSet<>();
            for (ModuleRecord mr : crawlResult) {
                if ( expectedSourceSymbols.containsKey( mr.source() ) ) {
                    Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
                    CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
                    visitedURI.add( mr.source() );
                }
            }

            if ( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
                throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
                        + " not found in crawl result" );
            }

            symbolResolver.resolveLinkedType();
            // check types
            CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );
            // check semantic, all linked type should be set
            CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
                    false );

        } );
    }


    @Test
    void testImportCyclicDependency() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        URI target = Paths.get( baseDir.toURI() ).resolve( "cyclic" ).resolve( "A.ol" ).toUri();
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );

        Map.Entry< URI, Set< String > > expectedSymbolsRoot =
                TestCasesCreator.createURISymbolsMap( target, "foo", "bar" );
        Map.Entry< URI, Set< String > > expectedSymbolsExt =
                TestCasesCreator.createURISymbolsMap( target.resolve( "B.ol" ), "foo", "bar" );

        Map< URI, Set< String > > expectedSourceSymbols =
                Map.ofEntries( expectedSymbolsRoot, expectedSymbolsExt );

        Set< String > expectedType = new HashSet<>( Arrays.asList( "foo" ) );

        assertDoesNotThrow( () -> {

            // parse a program
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            // check symbols
            Set< URI > visitedURI = new HashSet<>();
            for (ModuleRecord mr : crawlResult) {
                if ( expectedSourceSymbols.containsKey( mr.source() ) ) {
                    Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
                    CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
                    visitedURI.add( mr.source() );
                } else {
                    throw new Exception( "unexpected source " + mr.source().toString() );
                }
            }

            if ( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
                throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
                        + " not found in crawl result" );
            }

            symbolResolver.resolveLinkedType();
            // check types
            CheckUtility.checkTypes( mainRecord.program(), expectedType );
            // check semantic, all linked type should be set
            CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
                    false );

        } );
    }


    @Test
    void testImportJap() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        URI target = Paths.get( baseDir.toURI() ).resolve( "test_jap.ol" ).toUri();
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );

        Map.Entry< String, Set< String > > ifaceEntry =
                TestCasesCreator.createInterfaceStub( "TwiceAPI", "twice" );

        PortStub opPort = new PortStub( "OP", Map.ofEntries( ifaceEntry ), "twice" );

        Map< String, PortStub > expectedOutputPorts =
                TestCasesCreator.createExpectedPortMap( opPort );

        assertDoesNotThrow( () -> {

            // parse a program
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            symbolResolver.resolveLinkedType();

            // check interface in outputPort
            CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );

            // check semantic, all linked type should be set
            CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
                    false );

        } );
    }


    @Test
    void testImportInterface() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        URI target = Paths.get( baseDir.toURI() ).resolve( "test_iface.ol" ).toUri();
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );

        Map.Entry< String, Set< String > > ifaceEntry =
                TestCasesCreator.createInterfaceStub( "twiceIface", "twice" );

        PortStub ipPort = new PortStub( "IP", Map.ofEntries( ifaceEntry ), "twice" );
        PortStub opPort = new PortStub( "OP", Map.ofEntries( ifaceEntry ), "twice" );

        Map< String, PortStub > expectedInputPorts =
                TestCasesCreator.createExpectedPortMap( ipPort );
        Map< String, PortStub > expectedOutputPorts =
                TestCasesCreator.createExpectedPortMap( opPort );

        assertDoesNotThrow( () -> {

            // parse a program
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            symbolResolver.resolveLinkedType();
            CheckUtility.checkInputPorts( mainRecord.program(), expectedInputPorts );
            CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );

            // check semantic, all linked type should be set
            CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
                    false );

        } );
    }

    @Test
    void testImportType() throws IOException, ParserException, ModuleException, URISyntaxException
    {
        String[] includePaths = new String[0];
        String code = "from type import date, number, foo, bar, baz, dateFoo";
        InputStream is = new ByteArrayInputStream( code.getBytes() );

        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                this.getClass().getClassLoader() );
        ModuleCrawler crawler = new ModuleCrawler( includePaths );
        Scanner s = new Scanner( is, baseDir.toURI(), null );

        Map.Entry< URI, Set< String > > expectedSymbolsRoot =
                TestCasesCreator.createURISymbolsMap( Paths.get( baseDir.toURI() ).toFile().toURI(),
                        "date", "number", "foo", "bar", "baz", "dateFoo" );
        Map.Entry< URI, Set< String > > expectedSymbolsExt =
                TestCasesCreator.createURISymbolsMap(
                        Paths.get( baseDir.toURI() ).resolve( "packages" ).resolve( "type.ol" )
                                .toFile().toURI(),
                        "date", "number", "foo", "bar", "baz", "dateFoo" );

        Map< URI, Set< String > > expectedSourceSymbols =
                Map.ofEntries( expectedSymbolsRoot, expectedSymbolsExt );

        assertDoesNotThrow( () -> {
            // parse a program
            ModuleRecord mainRecord = parser.parse( s );

            // crawl dependencies
            Set< ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );

            // check symbols
            Set< URI > visitedURI = new HashSet<>();
            for (ModuleRecord mr : crawlResult) {
                if ( expectedSourceSymbols.containsKey( mr.source() ) ) {
                    Set< String > symbols = expectedSourceSymbols.get( mr.source() );
                    CheckUtility.checkSymbols( mr.symbolTable(), symbols );
                    visitedURI.add( mr.source() );
                } else {
                    throw new Exception( "unexpected source " + mr.source().toString()
                            + ", expected " + expectedSourceSymbols.keySet().toString() );
                }
            }

            if ( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
                throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
                        + " not found in crawl result" );
            }

            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );

            // resolve symbols
            symbolResolver.resolveExternalSymbols();

            // check if all external symbol is linked
            for (ModuleRecord mr : crawlResult) {
                CheckUtility.checkSymbolNodeLinked( mr.symbolTable() );
            }

        } );
    }

    @Test
    void testDuplicateSymbolDeclError()
            throws IOException, ParserException, ModuleException, URISyntaxException
    {

        String code = "from A import A\n from B import A";
        InputStream is = new ByteArrayInputStream( code.getBytes() );

        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                this.getClass().getClassLoader() );

        Scanner s = new Scanner( is, baseDir.toURI(), null );

        Exception exception = assertThrows( ModuleException.class, () -> {
            parser.parse( s );
        } );

        String expectedMessage = "detected redeclaration of symbol A";
        String actualMessage = exception.getMessage();

        assertTrue( actualMessage.contains( expectedMessage ) );

    }

    @Test
    void testParser()
    {
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                this.getClass().getClassLoader() );
        Set< String > expectedType = new HashSet<>( Arrays.asList( "A", "from_b" ) );
        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            ModuleRecord p = parser.parse( target );
            CheckUtility.checkTypes( p.program(), expectedType );
        } );
    }

    @ParameterizedTest
    @MethodSource("importStatementExceptionTestProvider")
    void testImportStatementExceptions( String code, String errorMessage )
            throws RuntimeException, IOException, URISyntaxException
    {
        InputStream is = new ByteArrayInputStream( code.getBytes() );
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                this.getClass().getClassLoader() );

        Scanner s = new Scanner( is, baseDir.toURI(), null );

        Exception exception = assertThrows( ParserException.class, () -> {
            parser.parse( s );
        } );

        String expectedMessage = errorMessage;
        String actualMessage = exception.getMessage();

        assertTrue( actualMessage.contains( expectedMessage ) );

    }

    private static Stream< Arguments > importStatementExceptionTestProvider()
    {
        return Stream.of(
                Arguments.of( "from .A import AA as", "error: expected Identifier after as" ),
                Arguments.of( "from A import ", "error: expected Identifier or * after import" ),
                Arguments.of( "from \"somewhere\" import AA ",
                        "error: expected Identifier, dot or import for an import statement. Found token type STRING" ) );
    }
}
