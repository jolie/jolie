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

public class ModuleCrawler {

	public static class CrawlerResult {
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

		public Map< URI, ModuleRecord > toMap() {
			return this.moduleCrawled;
		}

		public Map< URI, SymbolTable > symbolTables() {
			Map< URI, SymbolTable > result = new HashMap<>();
			for( ModuleRecord mr : this.moduleCrawled.values() ) {
				result.put( mr.source(), mr.symbolTable() );
			}
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

	public static void clearCache() {
		ModuleCrawler.cache.clear();
	}

	private final Finder finder;
	private final ModuleParser parser;

	private ModuleCrawler( ModuleCrawlerComponent component ) {
		this( component.parser(), component.finder() );
	}

	private ModuleCrawler( ModuleParser parser, Finder finder ) {
		this.finder = finder;
		this.parser = parser;
	}

	private Source findModule( URI parentURI, List< String > importTargetStrings )
		throws ModuleNotFoundException {
		Source targetFile = finder.find( importTargetStrings, parentURI );
		return targetFile;
	}

	private List< Source > crawlModule( ModuleRecord record ) throws ModuleException {
		List< Source > modulesToCrawl = new ArrayList<>();
		for( SymbolInfoExternal externalSymbol : record.symbolTable().externalSymbols() ) {
			try {
				Source moduleSource =
					this.findModule( record.source(), externalSymbol.moduleTargets() );
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
		Queue< Source > dependencies = new LinkedList<>();
		result.addModuleRecord( parentRecord );
		dependencies.addAll( this.crawlModule( parentRecord ) );

		// walk through dependencies
		while( dependencies.peek() != null ) {
			Source module = dependencies.poll();

			if( result.isRecordInResult( module.source() ) ) {
				continue;
			}

			if( ModuleCrawler.inCache( module.source() ) ) {
				result.addModuleRecord( ModuleCrawler.getRecordFromCache( module.source() ) );
				continue;
			}

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
	 * @param component a composite object of Jolie's Finder and Parser for use during crawl process
	 * @throws ParserException
	 * @throws IOException
	 * @throws ModuleException
	 */
	public static CrawlerResult crawl( ModuleRecord initial, ModuleCrawlerComponent component )
		throws ParserException, IOException, ModuleException {
		ModuleCrawler crawler = new ModuleCrawler( component );
		return crawler.crawl( initial );
	}
}
