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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jolie.lang.parse.ast.ImportableSymbol;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.module.SymbolInfo.Scope;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;

/**
 * A class represent the Symbol table of a Jolie module
 */
public class SymbolTable {
	/**
	 * Symbol target source
	 */
	private final URI source;

	/**
	 * Maps of Symbolname and corresponding SymbolInfo object
	 */
	private final Map< String, SymbolInfo > symbols;

	/**
	 * A constructor of SymbolTable
	 * 
	 * @param source source of Jolie module
	 */
	protected SymbolTable( URI source ) {
		this.source = source;
		this.symbols = new HashMap<>();
	}

	protected URI source() {
		return this.source;
	}

	/**
	 * resolve a wildcard symbol by replace it with an array of symbols
	 * 
	 * @param wildCardSymbol a wildcard symbol in symbol table to replace
	 * @param sourceSymbols array of symbols in parsed wildcardSymbol's module
	 * @throws DuplicateSymbolException when adding name duplicate name to the symbol
	 */
	protected void resolveWildcardImport( WildcardImportedSymbolInfo wildCardSymbol,
		SymbolInfo... sourceSymbols ) throws DuplicateSymbolException {
		for( SymbolInfo symbolFromWildcard : sourceSymbols ) {
			if( isDuplicateSymbol( symbolFromWildcard.name() ) ) {
				throw new DuplicateSymbolException( symbolFromWildcard.name() );
			}
			if( symbolFromWildcard.accessModifier() == AccessModifier.PUBLIC ) {
				this.symbols.put( symbolFromWildcard.name(), symbolFromWildcard );
			}
		}
		this.symbols.remove( wildCardSymbol.name(), wildCardSymbol );
	}

	/**
	 * create and add a local Symbol with corresponding ASTNode to the table
	 * 
	 * @param name Symbol name in local execution context
	 * @param node an AST node implementing SymbolNode
	 * 
	 * @throws DuplicateSymbolException when adding name duplicate name to the symbol
	 */
	protected void addSymbol( String name, ImportableSymbol node ) throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			if( node instanceof ServiceNode ) {
				// allows re-declaration of any symbols except ServiceNode.
				throw new DuplicateSymbolException( name );
			}
		}
		this.symbols.put( name, new LocalSymbolInfo( name, node ) );
	}

	/**
	 * create and add an external Symbol with its module target to the table
	 * 
	 * @param context Context where symbol is declare
	 * @param name importing Symbol name
	 * @param importPath a object represent importing path to module where the symbol reside
	 * 
	 * @throws DuplicateSymbolException when adding name duplicate name to the symbol
	 */
	protected void addSymbol( ParsingContext context, String name, ImportPath importPath )
		throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			throw new DuplicateSymbolException( name );
		}
		this.symbols.put( name,
			new ImportedSymbolInfo( context, name, importPath, name ) );
	}

	/**
	 * create and add an external Symbol to the symboltable with an alias for using in local execution
	 * context
	 * 
	 * @param context Context where symbol is declare
	 * @param name Symbol name
	 * @param importPath a object represent importing path to module where the symbol reside
	 * @param originalSymbolName Name for binding result to local environment
	 * 
	 * @throws DuplicateSymbolException when adding name duplicate name to the symbol
	 */
	protected void addSymbolWithAlias( ParsingContext context, String name, ImportPath importPath,
		String originalSymbolName ) throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			throw new DuplicateSymbolException( name );
		}
		this.symbols.put( name,
			new ImportedSymbolInfo( context, name, importPath, originalSymbolName ) );
	}

	/**
	 * add an wildcard Symbol to table
	 * 
	 * @param importPath a object represent importing path to module where the symbol reside
	 * 
	 */
	protected void addWildcardSymbol( ParsingContext context, ImportPath importPath ) {
		this.symbols.put( importPath.toString(),
			new WildcardImportedSymbolInfo( context, importPath ) );
	}

	public SymbolInfo[] symbols() {
		return this.symbols.values().toArray( new SymbolInfo[ 0 ] );
	}

	public LocalSymbolInfo[] localSymbols() {
		return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.LOCAL )
			.toArray( LocalSymbolInfo[]::new );
	}

	public ImportedSymbolInfo[] importedSymbolInfos() {
		return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.EXTERNAL )
			.toArray( ImportedSymbolInfo[]::new );
	}

	public Optional< SymbolInfo > getSymbol( String name ) {
		if( this.symbols.containsKey( name ) ) {
			return Optional.of( this.symbols.get( name ) );
		}
		return Optional.empty();
	}

	private boolean isDuplicateSymbol( String name ) {
		return symbols.containsKey( name );
	}

	@Override
	public String toString() {
		return "SymbolTable [source=" + source + ", symbols=" + symbols + "]";
	}

}
