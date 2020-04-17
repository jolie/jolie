package jolie.lang.parse.module;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.Scanner.Token;
import jolie.lang.parse.ast.ImportStatement;

public class Importer
{

    public static String PACKAGE_FOLDER = "packages";
    public static String JOLIE_HOME = System.getenv( "JOLIE_HOME" );

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

    // private Map< URI, ImportCache > cache;
    private Configuration config;

    public Importer( Configuration c )
    {
        this.config = c;
        // cache = new HashMap<>();
    }

    // private ModuleRecord load( Source s ) throws ModuleParsingException
    // {
    // System.out.println( "[LOADER] loading " + s.source() );
    // SemanticVerifier.Configuration configuration = new
    // SemanticVerifier.Configuration();
    // configuration.setCheckForMain( false );
    // Program program;
    // try {
    // program = ParsingUtils.parseProgram( s.stream(), s.source(),
    // this.config.charset,
    // this.config.includePaths, this.config.classLoader,
    // this.config.definedConstants,
    // configuration, this.config.includeDocumentation, this );
    // } catch (IOException | ParserException | SemanticException e) {
    // throw new ModuleParsingException( e );
    // }
    // if ( program == null ) {
    // throw new ModuleParsingException( "Program from " + s.source() + " is null"
    // );
    // }
    // ProgramInspector pi = ParsingUtils.createInspector( program );
    // return new ModuleRecord( s.source(), pi );
    // }

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

    private void moduleLookUp( String[] target ) throws ModuleException
    {
        Finder finder =
                Finder.getFinderForTarget( this.config.source, this.config.includePaths, target );
        File targetFile = finder.find();
        if ( targetFile == null ) {
            throw new ModuleException( "Unable to locate module " + prettyPrintTarget( target )
                    + ", looked path " + Arrays.toString( finder.lookupedPath() ) );
        }
        System.out.println( targetFile );
        // ImportCache ic = null;
        // Finder[] finders = Finder.getFindersForTargetString(target);

        // for (Finder f : finders) {
        // Source targetSource = f.find(source, target);
        // if (targetSource == null) {
        // continue;
        // }
        // // // perform cache lookup
        // // if (cache.containsKey(targetSource.source())) {
        // // System.out.println("[LOADER] found " + targetSource.source() + " in
        // cache");
        // // ic = cache.get(targetSource.source());
        // // if (ic.s == Status.PENDING) { // check importStatus is finishes
        // // throw new ModuleParsingException(
        // // "cyclic dependency detected between " + source + " and " + targetSource);
        // // }
        // // } else {
        // // ic = new ImportCache(targetSource.source());
        // // cache.put(targetSource.source(), ic);
        // // ModuleRecord rc = load(targetSource);
        // // ic.setModuleRecord(rc);
        // // ic.importFinished();
        // // }
        // break;
        // }
        // if (ic == null) {
        // throw new ModuleNotFoundException("unable to locate " + target);
        // }

        // return ic.rc;
    }

    public void importModule( ImportStatement stmt ) throws ModuleException
    {
        // ModuleRecord rc = moduleLookUp(source, stmt.importTarget());
        moduleLookUp( stmt.importTarget() );
        // if ( rc == null ) {
        // throw new ModuleException("unable to locate from " + source + " with target"
        // + stmt.importTarget());
        // }
        // if ( stmt.isNamespaceImport() ) {
        // return rc.resolveNameSpace( stmt.context() );
        // } else {
        // return rc.resolve( stmt.context(), stmt.pathNodes() );
        // }
    }
}
