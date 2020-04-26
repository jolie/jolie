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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

/**
 * A class represent parser for the parser of Jolie module.
 */
public class ModuleParser
{

    /**
     * an array of string for lookup path of include statement in Module
     */
    private final String[] includePaths;

    private final String charset;
    private final ClassLoader classLoader;
    private final boolean includeDocumentation;

    private final Map< String, Scanner.Token > constantsMap = new HashMap<>();

    public ModuleParser( String charset, String[] includePaths, ClassLoader classLoader )
    {
        this( charset, includePaths, classLoader, false );
    }

    public ModuleParser( String charset, String[] includePaths, ClassLoader classLoader,
            boolean includeDocumentation )
    {
        this.charset = charset;
        this.includePaths = includePaths;
        this.classLoader = classLoader;
        this.includeDocumentation = includeDocumentation;
    }

    public void putConstants( Map< String, Scanner.Token > constantsToPut )
    {
        constantsMap.putAll( constantsToPut );
    }

    public ModuleRecord parse( URI uri ) throws ParserException, IOException, ModuleException
    {
        return parse( uri, this.includePaths, false );
    }


    public ModuleRecord parse( File file ) throws ParserException, IOException, ModuleException
    {
        return parse( file.toURI(), this.includePaths, false );
    }

    public ModuleRecord parse( Source module ) throws ParserException, IOException, ModuleException
    {
        String[] additionalPath;
        if ( module.includePath().isPresent() ) {
            additionalPath = new String[] {module.includePath().get()};
        } else {
            additionalPath = new String[0];
        }
        String[] inculdePaths =
                Stream.concat( Arrays.stream( this.includePaths ), Arrays.stream( additionalPath ) )
                        .distinct().toArray( String[]::new );

        OLParser olParser = new OLParser( new Scanner( module.stream().get(), module.source(),
                this.charset, includeDocumentation ), inculdePaths, this.classLoader );
        olParser.putConstants( constantsMap );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        SymbolTable st = SymbolTableGenerator.generate( program );
        return new ModuleRecord( module.source(), program, st );
    }

    public ModuleRecord parse( URI uri, String[] includePaths )
            throws ParserException, IOException, ModuleException
    {
        return parse( uri, includePaths, false );
    }

    public ModuleRecord parse( Scanner scanner )
            throws ParserException, IOException, ModuleException
    {
        return parse( scanner, includePaths, false );
    }

    public ModuleRecord parse( Scanner scanner, String[] additionalIncludePaths, boolean joinPaths )
            throws ParserException, IOException, ModuleException
    {
        String[] inculdePaths = includePaths;
        if ( joinPaths ) {
            inculdePaths = Stream
                    .concat( Arrays.stream( this.includePaths ),
                            Arrays.stream( additionalIncludePaths ) )
                    .distinct().toArray( String[]::new );
        }
        OLParser olParser = new OLParser( scanner, inculdePaths, this.classLoader );
        olParser.putConstants( constantsMap );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        SymbolTable st = SymbolTableGenerator.generate( program );
        return new ModuleRecord( scanner.source(), program, st );
    }

    public ModuleRecord parse( URI uri, String[] additionalIncludePaths, boolean joinPaths )
            throws ParserException, IOException, ModuleException
    {
        URL url = uri.toURL();
        InputStream stream = url.openStream();
        String[] inculdePaths = includePaths;
        if ( joinPaths ) {
            inculdePaths = Stream
                    .concat( Arrays.stream( this.includePaths ),
                            Arrays.stream( additionalIncludePaths ) )
                    .distinct().toArray( String[]::new );
        }
        OLParser olParser =
                new OLParser( new Scanner( stream, uri, this.charset, includeDocumentation ),
                        inculdePaths, this.classLoader );
        olParser.putConstants( constantsMap );
        Program program = olParser.parse();
        program = OLParseTreeOptimizer.optimize( program );
        SymbolTable st = SymbolTableGenerator.generate( program );
        return new ModuleRecord( uri, program, st );
    }
}
