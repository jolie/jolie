package jolie.lang.parse.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

public class Modules {
	public static class ModuleParsedResult {

		/**
		 * An ast program of root program
		 */
		private final Program mainProgram;

		/**
		 * Collection of symbol table reference for main program execution
		 */
		private final Map< URI, SymbolTable > symbolTables;

		private ModuleParsedResult( Program mainProgram, Map< URI, SymbolTable > symbolTables ) {
			this.mainProgram = mainProgram;
			this.symbolTables = symbolTables;
		}

		public Program mainProgram() {
			return mainProgram;
		}

		public Map< URI, SymbolTable > symbolTables() {
			return symbolTables;
		}
	}

	public static ModuleParsedResult parseModule( ModuleParsingConfiguration configuration, InputStream stream,
		URI programURI )
		throws ParserException, IOException, ModuleException {
		ModuleParser parser = new ModuleParser( configuration );
		Path programDirectory = Paths.get( programURI ).toFile().isFile() ? Paths.get( programURI ).getParent()
			: Paths.get( programURI );
		ModuleFinder finder = new ModuleFinderImpl( programDirectory, configuration.packagePaths() );

		ModuleRecord mainRecord = parser.parse(
			new Scanner( stream, programURI, configuration.charset(), configuration.includeDocumentation() ) );

		ModuleCrawler.CrawlerResult crawlResult = ModuleCrawler.crawl( mainRecord, configuration, finder );

		SymbolReferenceResolver.resolve( crawlResult );

		return new ModuleParsedResult( mainRecord.program(), crawlResult.symbolTables() );
	}
}
