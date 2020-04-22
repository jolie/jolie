package jolie.lang.parse.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

public class ModuleParser
{

    private String charset;
    private String[] includePaths;
    private ClassLoader classLoader;
    private boolean includeDocumentation;

    public ModuleParser( String charset, String[] includePaths, ClassLoader classLoader,
            boolean includeDocumentation )
    {
        this.charset = charset;
        this.includePaths = includePaths;
        this.classLoader = classLoader;
        this.includeDocumentation = includeDocumentation;
    }

    public Program parse( URI uri ) throws ParserException, IOException
    {
        URL url = uri.toURL();
        InputStream stream = url.openStream();
        OLParser olParser =
                new OLParser( new Scanner( stream, uri, this.charset, this.includeDocumentation ),
                        this.includePaths, this.classLoader );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        return program;
    }

    public Program parse( URI uri, String[] includePaths ) throws ParserException, IOException
    {
        return parse( uri, includePaths, false );
    }

    public Program parse( URI uri, String[] additionalIncludePaths, boolean joinPaths )
            throws ParserException, IOException
    {
        URL url = uri.toURL();
        InputStream stream = url.openStream();
        String[] inculdePaths = includePaths;
        if ( joinPaths ) {
            inculdePaths = Stream
                    .concat( Arrays.stream( this.includePaths ), Arrays.stream( additionalIncludePaths ) )
                    .distinct().toArray( String[]::new );
        }
        OLParser olParser =
                new OLParser( new Scanner( stream, uri, this.charset, this.includeDocumentation ),
                        inculdePaths, this.classLoader );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        return program;
    }
}
