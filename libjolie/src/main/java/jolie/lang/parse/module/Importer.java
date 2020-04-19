package jolie.lang.parse.module;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.Scanner.Token;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleRecord.Status;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

public class Importer
{

    public static String PACKAGE_FOLDER = "packages";
    public static String JOLIE_HOME = System.getenv( "JOLIE_HOME" );
    private final Map< URI, ModuleRecord > cache;
    private final Configuration config;

    public static class Configuration
    {
        private URI source;
        private String charset;
        private String[] includePaths;
        private ClassLoader classLoader;
        private Map< String, Scanner.Token > definedConstants;

        /**
         * @param source
         * @param charset
         * @param includePaths
         * @param classLoader
         * @param definedConstants
         * @param includeDocumentation
         */
        public Configuration( URI source, String charset, String[] includePaths,
                ClassLoader classLoader, Map< String, Token > definedConstants )
        {
            this.source = source;
            this.charset = charset;
            this.includePaths = includePaths;
            this.classLoader = classLoader;
            this.definedConstants = definedConstants;
        }
    }


    public Importer( Configuration c )
    {
        this.config = c;
        cache = new HashMap<>();
    }

    public String prettyPrintTarget( String[] target )
    {
        String ret = "";
        boolean relativeEnded = false;
        for (String token : target) {
            if ( token.isEmpty() ) {
                ret += ".";
            } else {
                if ( relativeEnded ) {
                    ret += ".";
                }
                relativeEnded = true;
                ret += token;
            }
        }
        return ret;
    }

    private Source moduleLookUp( String[] target ) throws ModuleException
    {
        Finder finder =
                Finder.getFinderForTarget( this.config.source, this.config.includePaths, target );
        Source targetFile = finder.find();
        if ( targetFile == null ) {
            throw new ModuleException(
                    "Unable to locate or read module " + prettyPrintTarget( target )
                            + ", looked path " + Arrays.toString( finder.lookupedPath() ) );
        }

        return targetFile;
    }


    /**
     * load a target source into module record
     * 
     * @param s Source of import target, an implementation of Source interface.
     * @see Source
     * @throws ModuleException
     */
    private ProgramInspector load( Source s ) throws ModuleException
    {
        Program program;
        try {
            switch (s.type()) {
                case FILE:
                    program = parseSource( (FileSource) s );
                    break;
                case JAP:
                    program = parseSource( (JapSource) s );
                    break;
                default:
                    throw new ModuleException( "unknown source type" );
            }
        } catch (IOException | ParserException e) {
            throw new ModuleException( "unable to parse module " + s.source(), e );
        }
        ProgramInspector pi = ParsingUtils.createInspector( program );
        return pi;
    }

    private Program parseSource( FileSource s ) throws IOException, ParserException
    {
        Scanner scanner = new Scanner( s.stream(), s.source(), this.config.charset );
        Program program = ParsingUtils.parseProgram( scanner, this.config.includePaths,
                this.config.classLoader, this.config.definedConstants, this );
        return program;
    }

    private Program parseSource( JapSource s ) throws IOException, ParserException
    {
        System.out.println( s.source().toString() );
        String[] includePaths = new String[this.config.includePaths.length + 1];
        System.arraycopy( config.includePaths, 0, includePaths, 0, config.includePaths.length );
        includePaths[config.includePaths.length] = s.includePath();
        Scanner scanner = new Scanner( s.stream(), s.source(), this.config.charset );
        Program program = ParsingUtils.parseProgram( scanner, includePaths, this.config.classLoader,
                this.config.definedConstants, this );
        return program;

    }


    /**
     * perform import module from a statement
     * this method is null safety
     * 
     * @param stmt
     * @return
     * @throws ModuleException
     */
    public ImportResult importModule( ImportStatement stmt ) throws ModuleException
    {
        Source targetSource = moduleLookUp( stmt.importTarget() );

        // perform cache lookup
        ModuleRecord moduleRecord;
        if ( cache.containsKey( targetSource.source() ) ) {
            moduleRecord = cache.get( targetSource.source() );
            if ( moduleRecord.status() == Status.LOADING ) { // check importStatus is finishes
                moduleRecord.loadPartial();
                return new ImportResult();
            }
        }
        moduleRecord = new ModuleRecord( targetSource.source() );
        cache.put( targetSource.source(), moduleRecord );
        ProgramInspector pi = load( targetSource );
        moduleRecord.setInspector( pi );
        if ( stmt.isNamespaceImport() ) {
            return moduleRecord.resolveNameSpace( stmt.context() );
        } else {
            return moduleRecord.resolve( stmt.context(), stmt.importSymbolTargets() );
        }
    }
}
