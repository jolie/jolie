package jolie.lang.parse.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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
        OLParser olParser = null;
        URL url = uri.toURL();
        InputStream stream = url.openStream();
        olParser = new OLParser(
                new Scanner( stream, uri, this.charset, this.includeDocumentation ),
                this.includePaths, this.classLoader );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        return program;
    }
}
