package jolie.lang.parse.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.types.TypeDefinition;
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

            SymbolTable st = SymbolTableGenerator.generate( p );
            System.out.println(st);
        } );
    }
}
