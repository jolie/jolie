package jolie.lang.parse.module;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import jolie.lang.parse.ParserException;
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

	/**
	 * Global module record cache
	 *
	 */
	protected static final ModuleRecordCache CACHE = new ModuleRecordCache();

	/**
	 * parses jolie's code stream to ModuleParsedResult, which contains executable ast and it's
	 * symbolTables.
	 *
	 * Note: this method is meant to be used through the execution of jolie program, as it calls static
	 * crawl method that cache the result
	 *
	 * @param configuration ModuleParsingConfiguration
	 * @param source jolie module source
	 * @return parsed result
	 * @throws ParserException
	 * @throws IOException
	 * @throws ModuleException
	 */
	public static ModuleParsedResult parseModule( ModuleParsingConfiguration configuration, ModuleSource source )
		throws ParserException, IOException, ModuleException {
		ModuleParser parser = new ModuleParser( configuration );
		ModuleRecord mainRecord = parser.parse( source );
		ModuleFinder finder = new ModuleFinderImpl( configuration.packagePaths() );

		ModuleCrawler.CrawlerResult crawlResult = ModuleCrawler.crawl( mainRecord, configuration, finder );

		SymbolReferenceResolver.resolve( crawlResult );

		return new ModuleParsedResult( mainRecord.program(), crawlResult.symbolTables() );
	}

	/**
	 * Clear the module cache entry
	 *
	 * @param source source of module
	 */
	public static void freeCache( URI source ) {
		Modules.CACHE.remove( source );
	}
}
