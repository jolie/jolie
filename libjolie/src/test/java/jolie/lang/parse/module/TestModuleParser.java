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
import jolie.lang.parse.SemanticVerifier;
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
    void testInterfaceReferenceResolver() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );

        Map.Entry< String, Set< String > > ifaceEntry =
                TestCasesCreator.createInterfaceStub( "twiceIface", "twice" );

        PortStub ipPort = new PortStub( "IP", Map.ofEntries( ifaceEntry ), "twice" );
        PortStub opPort = new PortStub( "OP", Map.ofEntries( ifaceEntry ), "twice" );

        Map< String, PortStub > expectedInputPorts = TestCasesCreator.createExpectedPortMap( ipPort );
        Map< String, PortStub > expectedOutputPorts = TestCasesCreator.createExpectedPortMap( opPort );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "test_iface.ol" ).toUri();
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            symbolResolver.resolveLinkedType();
            CheckUtility.checkInputPorts( mainRecord.program(),expectedInputPorts  );
            CheckUtility.checkOutputPorts( mainRecord.program(),expectedOutputPorts  );

            SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
            conf.setCheckForMain( false );
            SemanticVerifier semanticVerifier = new SemanticVerifier( mainRecord.program(), conf );

            semanticVerifier.validate();

        } );

    }

    @Test
    void testReferenceResolver() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
        URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();

        assertDoesNotThrow( () -> {
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );

            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            for (ModuleRecord mr : crawlResult.values()) {
                CheckUtility.checkSymbolNodeLinked( mr.symbolTable() );
            }

            symbolResolver.resolveLinkedType();

            SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
            conf.setCheckForMain( false );
            SemanticVerifier semanticVerifier = new SemanticVerifier( mainRecord.program(), conf );

            semanticVerifier.validate();

        } );

    }


    @Test
    void testGenerateSymbolTable() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                this.getClass().getClassLoader() );
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
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            Set< URI > visitedURI = new HashSet<>();
            for (ModuleRecord mr : crawlResult.values()) {
                if ( expectedSourceSymbols.containsKey( mr.source() ) ) {
                    Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
                    CheckUtility.checkSymbols( mr.symbolTable(),expectedSymbols );
                    visitedURI.add( mr.source() );
                } else {
                    throw new Exception( "unexpected source " + mr.source().toString() );
                }
            }

            if ( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
                throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
                        + " not found in crawl result" );
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
            CheckUtility.checkTypes(  p.program(), expectedType );
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
