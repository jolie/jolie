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
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

/**
 * A class represent parser for the parser of Jolie module.
 */
public class ModuleParser {

	private final ModuleParsingConfiguration parserConfiguration;

	public ModuleParser( ModuleParsingConfiguration parserConfiguration ) {
		this.parserConfiguration = parserConfiguration;
	}

	public ModuleRecord parse( ModuleSource module ) throws ParserException, IOException, ModuleException {
		URI[] additionalPath;
		if( module.includePath().isPresent() ) {
			additionalPath = new URI[] { module.includePath().get() };
		} else {
			additionalPath = new URI[ 0 ];
		}
		try( Scanner scanner =
			new Scanner( module, parserConfiguration.charset(), parserConfiguration.includeDocumentation() ) ) {
			return this.parse( scanner, additionalPath );
		}
	}

	public ModuleRecord parse( Scanner scanner, URI[] additionalIncludePaths )
		throws ParserException, IOException, ModuleException {
		String[] includePaths = Stream.concat( Arrays.stream( parserConfiguration.includePaths() ),
			Arrays.stream( additionalIncludePaths ).map( URI::toString ) )
			.distinct().toArray( String[]::new );
		OLParser olParser = new OLParser( scanner, includePaths, parserConfiguration.classLoader() );
		olParser.putConstants( parserConfiguration.constantsMap() );
		Program program = olParser.parse();
		program = OLParseTreeOptimizer.optimize( program );
		SymbolTable st = SymbolTableGenerator.generate( program );
		return new ModuleRecord( scanner.source(), program, st );
	}
}
