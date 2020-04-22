package jolie.lang.parse.module;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;

public class ModuleCrawler
{

    public static Map< URI, ModuleRecord > crawl( ModuleRecord parentRecord, ModuleParser parser )
            throws ParserException, IOException, ModuleException
    {
        final Map< URI, ModuleRecord > result = new HashMap< URI, ModuleRecord >();
        result.put( parentRecord.source(), parentRecord );
        List< Source > sourceList = new ArrayList<>( Arrays.asList( parentRecord.dependency() ) );

        for (int i = 0; i < sourceList.size(); i++) {
            final Source s = sourceList.get( i );
            if ( result.containsKey( s.source() ) ) {
                continue;
            }
            final String[] additionalPath;
            if ( s.includePath().isPresent() ) {
                additionalPath = new String[] {s.includePath().get()};
            } else {
                additionalPath = new String[0];
            }
            Program p = parser.parse( s.source(), additionalPath, true );

            SymbolTable st = SymbolTableGenerator.generate( p );
            sourceList.addAll( Arrays.asList( st.dependency() ) );
            result.put( st.source(), new ModuleRecord( s.source(), p, st ) );
        }
        return result;
    }
}
