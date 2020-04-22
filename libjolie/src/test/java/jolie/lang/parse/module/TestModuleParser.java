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
    void testReferenceResolver() throws URISyntaxException
    {
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                InstanceCreator.class.getClassLoader(), false );

        Map< URI, Set< String > > expectedSymbols = new HashMap<>();

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "A", "from_b" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "C_type", "b_type", "b_type" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B" ).resolve( "packages" ).toUri(),
        new HashSet< String >( Arrays.asList( "b_type", "c" ) ) );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            Program p = parser.parse( target );

            SymbolTable st = SymbolTableGenerator.generate( p );
            ModuleRecord mainRecord = new ModuleRecord( target, p, st );
            Map< URI, ModuleRecord > crawlResult = ModuleCrawler.crawl( mainRecord, parser );
            GlobalSymbolReferenceResolver symbolResolver = new GlobalSymbolReferenceResolver(crawlResult);
            symbolResolver.resolveExternalSymbols();

            for (ModuleRecord mr : crawlResult.values()){
                for (SymbolInfo si : mr.symbolTable().symbols()){
                    if (si.scope() == Scope.EXTERNAL && si.node() == null){
                        throw new Exception("external symbolinfo " + si.name() + " has no node reference");
                    }
                }
            }

            symbolResolver.resolveLinkedType();

            for (ModuleRecord mr : crawlResult.values()){
                Program pro = mr.program();
                System.out.println(pro);
            }
            System.out.println(crawlResult);
        } );

    }


    @Test
    void testGenerateSymbolTable() throws URISyntaxException
    {
        ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[0],
                InstanceCreator.class.getClassLoader(), false );

        Map< URI, Set< String > > expectedSymbols = new HashMap<>();

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "A", "from_b" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(),
                new HashSet< String >( Arrays.asList( "C_type", "b_type" ) ) );

        expectedSymbols.put( Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B" ).resolve( "packages" ).toUri(),
        new HashSet< String >( Arrays.asList( "b_type", "c" ) ) );

        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            Program p = parser.parse( target );

            SymbolTable st = SymbolTableGenerator.generate( p );
            ModuleRecord parentRecord = new ModuleRecord( target, p, st );
            Map< URI, ModuleRecord > crawlResult = ModuleCrawler.crawl( parentRecord, parser );
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
                InstanceCreator.class.getClassLoader(), false );
        Set< String > expectedType = new HashSet<>();
        expectedType.add( "A" );
        expectedType.add( "from_b" );
        assertDoesNotThrow( () -> {
            URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();
            Program p = parser.parse( target );

            ProgramInspector pi = ParsingUtils.createInspector( p );

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
