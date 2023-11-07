/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.similarity.LevenshteinDistance;

import jolie.lang.CodeCheckMessage;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

class ModuleCrawler {

	protected static class CrawlerResult {
		private final Map< URI, ModuleRecord > moduleCrawled;

		private CrawlerResult() {
			this.moduleCrawled = new HashMap<>();
		}

		private void addModuleRecord( ModuleRecord mr ) {
			this.moduleCrawled.put( mr.uri(), mr );
		}

		private boolean isRecordInResult( URI source ) {
			return this.moduleCrawled.containsKey( source );
		}

		protected Map< URI, ModuleRecord > toMap() {
			return this.moduleCrawled;
		}

		public Map< URI, SymbolTable > symbolTables() {
			Map< URI, SymbolTable > result = new HashMap<>();
			this.moduleCrawled.values().stream().forEach( mr -> result.put( mr.uri(), mr.symbolTable() ) );
			return result;
		}
	}

	private final ModuleFinder finder;
	private final ModuleParsingConfiguration parserConfiguration;
	private final ModuleRecordCache cache;

	private ModuleCrawler( ModuleParsingConfiguration parserConfiguration, ModuleFinder finder ) {
		this.finder = finder;
		this.parserConfiguration = parserConfiguration;
		if( parserConfiguration.useGlobalCache() ) {
			this.cache = Modules.CACHE;
		} else {
			this.cache = new ModuleRecordCache();
		}
	}

	private ModuleSource findModule( ImportPath importPath, URI source )
		throws ModuleNotFoundException {
		return finder.find( source, importPath );
	}

	private List< ModuleSource > crawlModule( ModuleRecord record ) throws ModuleException {
		List< ModuleSource > modulesToCrawl = new ArrayList<>();
		for( ImportedSymbolInfo importedSymbol : record.symbolTable().importedSymbolInfos() ) {
			try {
				ModuleSource moduleSource =
					this.findModule( importedSymbol.importPath(), record.uri() );
				importedSymbol.setModuleSource( moduleSource );
				modulesToCrawl.add( moduleSource );
			} catch( ModuleNotFoundException e ) {
				// Add the importpath and name to the line of code, as the rest of the line cannot be gotten from
				// the context, since the getContextDuringError cannot be used in here, and the context is not
				// updated after the error is found.
				String codeLine = importedSymbol.context().enclosingCode().get( 0 ).replace( "\n", "" );
				String CodeLineWithPath = codeLine + importedSymbol.importPath() + " import " + importedSymbol.name();
				int column = codeLine.length();
				ParsingContext context = new URIParsingContext( importedSymbol.context().source(),
					importedSymbol.context().endLine(), importedSymbol.context().endLine(), column,
					column + e.importPath().toString().length(),
					List.of( CodeLineWithPath ) );
				CodeCheckMessage message = CodeCheckMessage.withHelp( context, e.getMessage(), getHelp( e ) );
				throw new ModuleException( message );
			}
		}
		cache.put( record, modulesToCrawl
			.stream()
			.map( ModuleSource::uri )
			.collect( Collectors.toList() ) );
		return modulesToCrawl;
	}

	private CrawlerResult crawl( ModuleRecord mainRecord )
		throws ParserException, IOException, ModuleException {
		CrawlerResult result = new CrawlerResult();
		// start with main module record
		Queue< ModuleSource > dependencies = new LinkedList<>();
		result.addModuleRecord( mainRecord );
		dependencies.addAll( this.crawlModule( mainRecord ) );

		// walk through dependencies
		while( dependencies.peek() != null ) {
			ModuleSource module = dependencies.poll();

			if( result.isRecordInResult( module.uri() ) ) {
				continue;
			}

			if( cache.contains( module.uri() ) ) {
				ModuleRecord cached = cache.get( module.uri() );
				for( ImportedSymbolInfo importedSymbol : cached.symbolTable().importedSymbolInfos() ) {
					if( importedSymbol.moduleSource().isPresent() ) {
						dependencies.add( importedSymbol.moduleSource().get() );
					} else {
						ModuleRecord record = new ModuleParser( parserConfiguration ).parse( module );
						result.addModuleRecord( record );
						dependencies.addAll( crawlModule( record ) );
					}
				}
				result.addModuleRecord( cached );
			} else {
				ModuleRecord record = new ModuleParser( parserConfiguration ).parse( module );
				result.addModuleRecord( record );
				dependencies.addAll( crawlModule( record ) );
			}
		}

		return result;
	}

	/**
	 * crawl module's dependencies required for resolving symbols
	 *
	 * @param mainRecord root ModuleRecord object to begin the dependency crawling
	 * @param parsingConfiguration configuration for parsing Jolie module
	 * @param finder Jolie module finder
	 */
	protected static CrawlerResult crawl( ModuleRecord mainRecord, ModuleParsingConfiguration parsingConfiguration,
		ModuleFinder finder )
		throws ParserException, IOException, ModuleException {
		ModuleCrawler crawler = new ModuleCrawler( parsingConfiguration, finder );
		return crawler.crawl( mainRecord );
	}

	/**
	 * Creates help message for exceptions occuring while crawling through modules
	 */
	private String getHelp( ModuleNotFoundException exception ) {
		StringBuilder message = new StringBuilder();
		Set< String > fileNames = new HashSet<>();
		Stream< Path > stream;
		try { // get all file names in current directory
			stream = Files.list( Paths.get( ".\\" ) );
			fileNames.addAll( stream.filter( file -> !Files.isDirectory( file ) ).map( Path::getFileName )
				.map( Path::toString ).collect( Collectors.toSet() ) );
			stream.close();
		} catch( IOException e ) {
		}
		// Get file names from lookedPaths parent directory
		for( Path path : exception.lookedPaths() ) {
			Stream< Path > forloopStream;
			try {
				if( !Files.isDirectory( path ) ) {
					continue;
				}
				forloopStream = Files.list( path );
				fileNames.addAll(
					forloopStream.filter( file -> Files.isDirectory( file ) || file.toString().endsWith( ".ol" ) )
						.map( Path::getFileName )
						.map( Path::toString ).collect( Collectors.toSet() ) );
				forloopStream.close();

			} catch( IOException e ) {
			}
		}
		// Calculate distance between module and file names
		LevenshteinDistance dist = new LevenshteinDistance();
		ArrayList< String > proposedModules = new ArrayList<>();
		for( String correctModule : fileNames ) {
			String moduleName;
			if( correctModule.contains( "." ) ) {
				int column = correctModule.indexOf( "." );
				moduleName = correctModule.substring( 0, column );
			} else {
				moduleName = correctModule;
			}
			if( dist.apply( exception.importPath().toString(), moduleName ) <= 2 ) {
				proposedModules.add( moduleName );
			}

		}
		// Create help from matching file names
		if( !proposedModules.isEmpty() ) {
			message.append( "Maybe you meant one of these modules: " ).append( String.join( ", ", proposedModules ) );
		} else {
			// If no matching file names were found, report the file names that could be used instead
			message.append( "Could not find modules matching \"" ).append( exception.importPath() )
				.append( "\". Here are some modules that can be imported:\n" );
			for( String module : fileNames ) {
				String temp;
				// Correctly add the module names to the string
				if( module.contains( "." ) ) {
					int column = module.indexOf( "." );
					temp = module.substring( 0, column );
				} else {
					temp = module;
				}
				message.append( temp ).append( ", " );
			}
		}
		if( message.charAt( message.length() - 2 ) == ',' ) {
			message.delete( message.length() - 2, message.length() );
		}
		// message.append( "\n" );
		return message.toString();
	}
}
