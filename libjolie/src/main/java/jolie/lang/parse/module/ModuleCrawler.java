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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

public class ModuleCrawler {

	public static class ModuleCrawlerResult {
		private final Map< URI, ModuleRecord > moduleCrawled;

		public ModuleCrawlerResult() {
			this.moduleCrawled = new HashMap<>();
		}

		public void addModuleRecord( ModuleRecord mr ) {
			this.moduleCrawled.put( mr.source(), mr );
		}

		public boolean isRecordInResult( URI source ) {
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

	private final Map< URI, ModuleRecord > cache = new HashMap<>();
	private final Queue< Source > modulesToCrawl;
	private final ModuleCrawlerResult result;
	private final Finder finder;
	private final ModuleParser parser;

	public ModuleCrawler( String[] packagePaths, ModuleParser parser ) throws FileNotFoundException {
		this( new FinderImpl( packagePaths ), parser );
	}

	public ModuleCrawler( Finder finder, ModuleParser parser ) throws FileNotFoundException {
		this.modulesToCrawl = new LinkedList<>();
		this.result = new ModuleCrawlerResult();
		this.finder = finder;
		this.parser = parser;
	}

	public ModuleCrawler( Path workingDirectory, String[] packagePaths, ModuleParser parser )
		throws FileNotFoundException {
		this( new FinderImpl( workingDirectory, packagePaths ), parser );
	}

	private Source findModule( URI parentURI, List< String > importTargetStrings )
		throws ModuleNotFoundException {
		Source targetFile = finder.find( importTargetStrings, parentURI );
		return targetFile;
	}

	private void crawlModule( ModuleRecord record ) throws ModuleException {
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
		this.result.addModuleRecord( record );
	}

	public ModuleCrawlerResult crawl( ModuleRecord parentRecord )
		throws ParserException, IOException, ModuleException {
		// start with parentRecord
		crawlModule( parentRecord );
		this.cache.put( parentRecord.source(), parentRecord );

		// walk through dependencies
		while( modulesToCrawl.peek() != null ) {
			Source module = modulesToCrawl.poll();

			if( this.result.isRecordInResult( module.source() ) ) {
				continue;
			}

			if( this.cache.containsKey( module.source() ) ) {
				this.result.addModuleRecord( this.cache.get( module.source() ) );
				continue;
			}

			ModuleRecord p = parser.parse( module );
			this.cache.put( p.source(), p );

			crawlModule( p );
		}

		return this.result;
	}
}
