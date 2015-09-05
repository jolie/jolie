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
import java.util.Map;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.impl.ProgramInspectorCreatorVisitor;

/**
 * Utility class for accessing the functionalities of the JOLIE parsing
 * library without having to worry about correctly instantiating all
 * the related objects (parser, scanner, etc.).
 * @author Fabrizio Montesi
 */
public class ParsingUtils
{
	private ParsingUtils()
	{}

	public static Program parseProgram(
		InputStream inputStream,
		URI source,
		String charset,
		String[] includePaths,
		ClassLoader classLoader,
		Map< String, Scanner.Token > definedConstants,
		SemanticVerifier.Configuration configuration
	)
		throws IOException, ParserException, SemanticException
	{
		OLParser olParser = new OLParser( new Scanner( inputStream, source, charset ), includePaths, classLoader );
		olParser.putConstants( definedConstants );
		Program program = olParser.parse();
		program = OLParseTreeOptimizer.optimize( program );
		SemanticVerifier semanticVerifier = new SemanticVerifier( program, configuration );
		semanticVerifier.validate();

		return program;
	}
	
	public static Program parseProgram(
		InputStream inputStream,
		URI source,
		String charset,
		String[] includePaths,
		ClassLoader classLoader,
		Map< String, Scanner.Token > definedConstants
	)
		throws IOException, ParserException, SemanticException
	{
		OLParser olParser = new OLParser( new Scanner( inputStream, source, charset ), includePaths, classLoader );
		olParser.putConstants( definedConstants );
		Program program = olParser.parse();
		program = OLParseTreeOptimizer.optimize( program );
		SemanticVerifier semanticVerifier = new SemanticVerifier( program );
		semanticVerifier.validate();

		return program;
	}

	/**
	 * Creates a {@link ProgramInspector} for the specified {@link jolie.lang.parse.ast.Program}.
	 * @param program the {@link jolie.lang.parse.ast.Program} to inspect
	 * @return a {@link ProgramInspector} for the specified {@link jolie.lang.parse.ast.Program}
	 * @see ProgramInspector
	 */
	public static ProgramInspector createInspector( Program program )
	{
		return new ProgramInspectorCreatorVisitor( program ).createInspector();
	}
}
