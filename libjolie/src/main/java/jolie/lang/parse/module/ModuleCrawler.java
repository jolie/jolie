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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

class ModuleCrawler {

	protected static class CrawlerResult {
		private final Map< URI, ModuleRecord > moduleCrawled;

		private CrawlerResult() {
			this.moduleCrawled = new HashMap<>();
		}

		private void addModuleRecord( ModuleRecord mr ) {
			this.moduleCrawled.put( mr.source(), mr );
		}

		private boolean isRecordInResult( URI source ) {
			return this.moduleCrawled.containsKey( source );
		}

		protected Map< URI, ModuleRecord > toMap() {
			return this.moduleCrawled;
		}

		public Map< URI, SymbolTable > symbolTables() {
			Map< URI, SymbolTable > result = new HashMap<>();
			this.moduleCrawled.values().stream().forEach( mr -> result.put( mr.source(), mr.symbolTable() ) );
			return result;
		}
	}

	private static final Map< URI, ModuleRecord > cache = new ConcurrentHashMap<>();

	private static void putToCache( ModuleRecord mc ) {
		ModuleCrawler.cache.put( mc.source(), mc );
	}

	private static boolean inCache( URI source ) {
		return ModuleCrawler.cache.containsKey( source );
	}

	private static ModuleRecord getRecordFromCache( URI source ) {
		return ModuleCrawler.cache.get( source );
	}

	private final ModuleFinder finder;
	private final ModuleParsingConfiguration parserConfiguration;

	private ModuleCrawler( ModuleParsingConfiguration parserConfiguration, ModuleFinder finder ) {
		this.finder = finder;
		this.parserConfiguration = parserConfiguration;
	}

	private ModuleSource findModule( ImportPath importPath, URI parentURI )
		throws ModuleNotFoundException {
		ModuleSource targetFile = finder.find( parentURI, importPath );
		return targetFile;
	}

	private List< ModuleSource > crawlModule( ModuleRecord record ) throws ModuleException {
		List< ModuleSource > modulesToCrawl = new ArrayList<>();
		for( ImportedSymbolInfo externalSymbol : record.symbolTable().externalSymbols() ) {
			try {
				ModuleSource moduleSource =
					this.findModule( externalSymbol.importPath(), record.source() );
				externalSymbol.setModuleSource( moduleSource );
				modulesToCrawl.add( moduleSource );
			} catch( ModuleNotFoundException e ) {
				throw new ModuleException( externalSymbol.context(), e );
			}
		}
		ModuleCrawler.putToCache( record );
		return modulesToCrawl;
	}

	private CrawlerResult crawl( ModuleRecord parentRecord )
		throws ParserException, IOException, ModuleException {
		CrawlerResult result = new CrawlerResult();
		// start with parentRecord
		Queue< ModuleSource > dependencies = new LinkedList<>();
		result.addModuleRecord( parentRecord );
		dependencies.addAll( this.crawlModule( parentRecord ) );

		// walk through dependencies
		while( dependencies.peek() != null ) {
			ModuleSource module = dependencies.poll();

			if( result.isRecordInResult( module.uri() ) ) {
				continue;
			}

			if( ModuleCrawler.inCache( module.uri() ) ) {
				result.addModuleRecord( ModuleCrawler.getRecordFromCache( module.uri() ) );
				continue;
			}

			ModuleParser parser = new ModuleParser( parserConfiguration );
			ModuleRecord p = parser.parse( module );

			result.addModuleRecord( p );
			dependencies.addAll( this.crawlModule( p ) );
		}

		return result;

	}

	/**
	 * crawl module's dependencies required for resolving symbols
	 * 
	 * @param initial a root module
	 * @param component a composite object of Jolie's ModuleFinder and Parser for use during crawl
	 *        process
	 * @throws ParserException
	 * @throws IOException
	 * @throws ModuleException
	 */
	protected static CrawlerResult crawl( ModuleRecord initial, ModuleParsingConfiguration parserConfiguration,
		ModuleFinder finder )
		throws ParserException, IOException, ModuleException {
		ModuleCrawler crawler = new ModuleCrawler( parserConfiguration, finder );
		return crawler.crawl( initial );
	}
}
