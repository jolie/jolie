/**
 * *************************************************************************
 * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
package joliex.java.impl;

import jolie.CommandLineException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import joliex.java.Jolie2Java;
import joliex.java.Jolie2JavaCommandLineParser;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class OutputDirectoryTest {

	private static ProgramInspector inspector;
	private static final String outputDirectory = "./target/jolie2java-generated-sources/";

	@AfterClass
	public static void tearDownClass() {
		File generatedPath = new File( JavaDocumentCreator.DEFAULT_OUTPUT_DIRECTORY );
		if( generatedPath.exists() ) {
			TestUtils.deleteFolder( generatedPath );
			generatedPath.delete();
		}
	}


	@Test
	public void checkEmptyOutputDirecotry()
		throws IOException, ParserException, SemanticException, CommandLineException {
		String[] args = { "src/test/resources/main.ol" };
		Jolie2JavaCommandLineParser cmdParser =
			Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

		Program program = ParsingUtils.parseProgram(
			cmdParser.getInterpreterParameters().inputStream(),
			cmdParser.getInterpreterParameters().programFilepath().toURI(),
			cmdParser.getInterpreterParameters().charset(),
			cmdParser.getInterpreterParameters().includePaths(),
			cmdParser.getInterpreterParameters().jolieClassLoader(),
			cmdParser.getInterpreterParameters().constants(),
			false );

		// Program program = parser.parse();
		inspector = ParsingUtils.createInspector( program );
		JavaDocumentCreator instance = new JavaDocumentCreator( inspector, "com.test", null, false, null, true, false );
		instance.ConvertDocument();
	}

	@Test
	public void checkJavaserviceTrue() throws IOException, ParserException, SemanticException, CommandLineException {
		String[] args = { "src/test/resources/main.ol" };
		Jolie2JavaCommandLineParser cmdParser =
			Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

		Program program = ParsingUtils.parseProgram(
			cmdParser.getInterpreterParameters().inputStream(),
			cmdParser.getInterpreterParameters().programFilepath().toURI(),
			cmdParser.getInterpreterParameters().charset(),
			cmdParser.getInterpreterParameters().includePaths(),
			cmdParser.getInterpreterParameters().jolieClassLoader(),
			cmdParser.getInterpreterParameters().constants(),
			false );

		// Program program = parser.parse();
		inspector = ParsingUtils.createInspector( program );
		JavaDocumentCreator instance =
			new JavaDocumentCreator( inspector, "com.test", null, false, outputDirectory, true, true );
		instance.ConvertDocument();

		assertEquals( "The number of generated files is wrong (interfaceOnly=true)", 43,
			new File( outputDirectory + "com/test/types" ).list().length );
		assertEquals( "The number of generated files is wrong (interfaceOnly=true)", 2,
			new File( outputDirectory + "com/test" ).list().length );
		assertEquals( "The number of generated files is wrong (interfaceOnly=true)", 2,
			new File( outputDirectory ).list().length );
	}

}
