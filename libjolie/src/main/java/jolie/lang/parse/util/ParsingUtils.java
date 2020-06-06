/***************************************************************************
 *   Copyright 2010 (C) by Fabrizio Montesi <famontesi@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.lang.parse.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.GlobalSymbolReferenceResolver;
import jolie.lang.parse.module.ModuleCrawler;
import jolie.lang.parse.module.ModuleCrawler.ModuleCrawlerResult;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.module.ModuleParser;
import jolie.lang.parse.module.ModuleRecord;
import jolie.lang.parse.util.impl.ProgramInspectorCreatorVisitor;

/**
 * Utility class for accessing the functionalities of the JOLIE parsing library without having to
 * worry about correctly instantiating all the related objects (parser, scanner, etc.).
 * 
 * @author Fabrizio Montesi
 */
public class ParsingUtils {
	private ParsingUtils() {}

	public static Program parseProgram(
		InputStream inputStream,
		URI source,
		String charset,
		String[] includePaths,
		String[] packagesPaths,
		ClassLoader classLoader,
		Map< String, Scanner.Token > definedConstants,
		SemanticVerifier.Configuration configuration,
		boolean includeDocumentation )
		throws IOException, ParserException, SemanticException, ModuleException {

		final ModuleParser parser = new ModuleParser( charset, includePaths, classLoader, includeDocumentation );
		parser.putConstants( definedConstants );
		ModuleRecord mainRecord = parser.parse( source );

		ModuleCrawler crawler = new ModuleCrawler( Paths.get( source ).getParent(), packagesPaths, parser );
		ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );

		GlobalSymbolReferenceResolver symbolResolver =
			new GlobalSymbolReferenceResolver( crawlResult );

		symbolResolver.resolve();

		SemanticVerifier semanticVerifier = new SemanticVerifier( mainRecord.program(),
			symbolResolver.symbolTables(), configuration );
		semanticVerifier.validate();
		return mainRecord.program();
	}

	public static Program parseProgram(
		InputStream inputStream,
		URI source,
		String charset,
		String[] includePaths,
		ClassLoader classLoader,
		Map< String, Scanner.Token > definedConstants,
		SemanticVerifier.Configuration configuration,
		boolean includeDocumentation )
		throws IOException, ParserException, SemanticException, ModuleException {
		return parseProgram(
			inputStream,
			source,
			charset,
			includePaths,
			new String[ 0 ],
			classLoader,
			definedConstants,
			configuration,
			includeDocumentation );
	}

	public static Program parseProgram(
		InputStream inputStream,
		URI source,
		String charset,
		String[] includePaths,
		ClassLoader classLoader,
		Map< String, Scanner.Token > definedConstants,
		boolean includeDocumentation )
		throws IOException, ParserException, SemanticException, ModuleException {
		return parseProgram(
			inputStream,
			source,
			charset,
			includePaths,
			classLoader,
			definedConstants,
			new SemanticVerifier.Configuration(),
			includeDocumentation );
	}

	/**
	 * Creates a {@link ProgramInspector} for the specified {@link jolie.lang.parse.ast.Program}.
	 * 
	 * @param program the {@link jolie.lang.parse.ast.Program} to inspect
	 * @return a {@link ProgramInspector} for the specified {@link jolie.lang.parse.ast.Program}
	 * @see ProgramInspector
	 */
	public static ProgramInspector createInspector( Program program ) {
		return new ProgramInspectorCreatorVisitor( program ).createInspector();
	}
}
