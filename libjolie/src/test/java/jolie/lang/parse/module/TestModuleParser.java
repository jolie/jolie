package jolie.lang.parse.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.module.SymbolInfo.Scope;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.util.InstanceCreator;
import jolie.util.jap.JolieURLStreamHandlerFactory;

public class TestModuleParser
{

    static {
        JolieURLStreamHandlerFactory.registerInVM();
    }
    private static String BASE_DIR = "imports/";
    private static URL baseDir = TestModuleParser.class.getClassLoader().getResource( BASE_DIR );

    void checkSymbols( Set< String > expectedSymbols, SymbolTable st ) throws Exception
    {
        for (SymbolInfo symbolInfo : st.symbols()) {
            expectedSymbols.remove( symbolInfo.name() );
        }
        if ( !expectedSymbols.isEmpty() ) {
            throw new Exception( "Symbols " + Arrays.toString( expectedSymbols.toArray() )
                    + " not found in table " + st.source().toString() );
        }
    }
    @Test
    void testInterfaceReferenceResolver() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                InstanceCreator.class.getClassLoader() );

        Map< URI, Set< String > > expectedSymbols = new HashMap<>();

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "test_iface.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "twiceIface" ) ) );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "test_iface.ol" ).toUri();
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            for (ModuleRecord mr : crawlResult.values()) {
                for (SymbolInfo si : mr.symbolTable().symbols()) {
                    if ( si.scope() == Scope.EXTERNAL && si.node() == null ) {
                        throw new Exception(
                                "external symbolinfo " + si.name() + " has no node reference" );
                    }
                }
            }

            symbolResolver.resolveLinkedType();

            SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
            conf.setCheckForMain( false );
            SemanticVerifier semanticVerifier = new SemanticVerifier( mainRecord.program(),conf );

            semanticVerifier.validate();

        } );

    }

    @Test
    void testReferenceResolver() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                InstanceCreator.class.getClassLoader() );

        Map< URI, Set< String > > expectedSymbols = new HashMap<>();

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "A", "from_b" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "C_type", "b_type", "b_type" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B" )
                .resolve( "packages" ).toUri(),
                new HashSet< String >( Arrays.asList( "b_type", "c" ) ) );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver =
                    new GlobalSymbolReferenceResolver( crawlResult );
            symbolResolver.resolveExternalSymbols();

            for (ModuleRecord mr : crawlResult.values()) {
                for (SymbolInfo si : mr.symbolTable().symbols()) {
                    if ( si.scope() == Scope.EXTERNAL && si.node() == null ) {
                        throw new Exception(
                                "external symbolinfo " + si.name() + " has no node reference" );
                    }
                }
            }

            symbolResolver.resolveLinkedType();

            SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
            conf.setCheckForMain( false );
            SemanticVerifier semanticVerifier = new SemanticVerifier( mainRecord.program(),conf );

            semanticVerifier.validate();

        } );

    }


    @Test
    void testGenerateSymbolTable() throws URISyntaxException
    {
        String[] includePaths = new String[0];
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
                InstanceCreator.class.getClassLoader() );

        Map< URI, Set< String > > expectedSymbols = new HashMap<>();

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "A", "from_b" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "C_type", "b_type" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B" )
                .resolve( "packages" ).toUri(),
                new HashSet< String >( Arrays.asList( "b_type", "c" ) ) );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            ModuleRecord mainRecord = parser.parse( target, includePaths );

            ModuleCrawler crawler = new ModuleCrawler( includePaths );
            Map< URI, ModuleRecord > crawlResult = crawler.crawl( mainRecord, parser );
            for (ModuleRecord mr : crawlResult.values()) {
                if ( expectedSymbols.containsKey( mr.source() ) ) {
                    checkSymbols( expectedSymbols.get( mr.source() ), mr.symbolTable() );
                }
            }
        } );

    }

    @Test
    void testParse()
    {
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                InstanceCreator.class.getClassLoader() );
        Set< String > expectedType = new HashSet<>();
        expectedType.add( "A" );
        expectedType.add( "from_b" );
        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            ModuleRecord p = parser.parse( target );

            ProgramInspector pi = ParsingUtils.createInspector( p.program() );

            for (TypeDefinition td : pi.getTypes()) {
                if ( expectedType.contains( td.id() ) ) {
                    expectedType.remove( td.id() );
                }
            }
            if ( !expectedType.isEmpty() ) {
                throw new Exception(
                        "type " + Arrays.toString( expectedType.toArray() ) + " not found" );
            }
        } );
    }
}
