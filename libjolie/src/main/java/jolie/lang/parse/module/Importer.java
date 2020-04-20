/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.Scanner.Token;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleRecord.Status;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

public class Importer
{

    public static String JOLIE_HOME = System.getenv( "JOLIE_HOME" );
    private final Map< URI, ModuleRecord > cache;
    private final Configuration config;

    public static class Configuration
    {
        private String charset;
        private String[] includePaths;
        private ClassLoader classLoader;
        private Map< String, Scanner.Token > definedConstants;

        /**
         * @param charset
         * @param includePaths
         * @param classLoader
         * @param definedConstants
         * @param includeDocumentation
         */
        public Configuration( String charset, String[] includePaths, ClassLoader classLoader,
                Map< String, Token > definedConstants )
        {
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

    private Source moduleLookUp( URI source, String[] target ) throws ModuleException
    {
        Finder finder = Finder.getFinderForTarget( source, this.config.includePaths, target );
        try {
            Source targetFile = finder.find();
            return targetFile;
        } catch (ModuleException e) {
            throw new ModuleException(
                    "Unable to locate or read module " + prettyPrintTarget( target )
                            + ", looked path " + Arrays.toString( finder.lookupedPath() ) );
        }
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
            if ( s instanceof FileSource ) {
                program = parseSource( (FileSource) s );
            } else if ( s instanceof JapSource ) {
                program = parseSource( (JapSource) s );
            } else {
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
        try (InputStream stream = s.stream().get()) {
            Scanner scanner = new Scanner( stream, s.source(), this.config.charset );
            Program program = ParsingUtils.parseProgram( scanner, this.config.includePaths,
                    this.config.classLoader, this.config.definedConstants, this );
            return program;
        }
    }

    private Program parseSource( JapSource s ) throws IOException, ParserException
    {
        String[] includePaths = new String[this.config.includePaths.length + 1];
        System.arraycopy( config.includePaths, 0, includePaths, 0, config.includePaths.length );
        includePaths[config.includePaths.length] = s.includePath();

        try (InputStream stream = s.stream().get()) {
            Scanner scanner = new Scanner( stream, s.source(), this.config.charset );
            Program program = ParsingUtils.parseProgram( scanner, includePaths,
                    this.config.classLoader, this.config.definedConstants, this );
            return program;
        }
    }

    private ImportResult resolveImportStatement( ImportStatement stmt, ModuleRecord mc )
            throws ModuleException
    {
        if ( stmt.isNamespaceImport() ) {
            return mc.resolveNameSpace( stmt.context() );
        } else {
            return mc.resolve( stmt.context(), stmt.importSymbolTargets() );
        }
    }

    /**
     * perform import module from a statement
     * this method is null safety
     * 
     * @param stmt an import statement
     * @return
     * @throws ModuleException
     */
    public ImportResult importModule( ImportStatement stmt ) throws ModuleException
    {
        Source targetSource = moduleLookUp( stmt.context().source(), stmt.importTarget() );
        // perform cache lookup
        ModuleRecord moduleRecord;
        if ( cache.containsKey( targetSource.source() ) ) {
            moduleRecord = cache.get( targetSource.source() );
            if ( moduleRecord.status() == Status.LOADING ) { // check importStatus is finished
                moduleRecord.loadPartial();
                return new ImportResult();
            }
            return resolveImportStatement( stmt, moduleRecord );
        }
        moduleRecord = new ModuleRecord( targetSource.source() );
        cache.put( targetSource.source(), moduleRecord );
        ProgramInspector pi = load( targetSource );
        moduleRecord.setInspector( pi );
        return resolveImportStatement( stmt, moduleRecord );
    }
}
