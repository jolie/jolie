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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jolie.lang.parse.ast.SymbolNode;
import jolie.lang.parse.ast.SymbolNode.Privacy;
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
	 * Maps of Symbolname an corresponding SymbolInfo object
	 */
	private final Map< String, SymbolInfo > symbols;

	/**
	 * A constructor of SymbolTable
	 * 
	 * @param source source of Jolie module
	 */
	public SymbolTable( URI source ) {
		this.source = source;
		this.symbols = new HashMap<>();
	}

	public URI source() {
		return this.source;
	}

	/**
	 * resolve the wildcard symbol by replace with array of symbol rely in it's symbolTable
	 * 
	 * @param wildCardSymbol a wildcard symbol in symbol table to replace
	 * @param symbolsFromWildcard array of symbols in parsed wildcardSymbol's module
	 * @throws ModuleException when adding name duplicate name to the symbol
	 */
	public void replaceWildCardSymbol( SymbolWildCard wildCardSymbol,
		SymbolInfo... symbolsFromWildcard ) throws DuplicateSymbolException {
		for( SymbolInfo symbolFromWildcard : symbolsFromWildcard ) {
			if( isDuplicateSymbol( symbolFromWildcard.name() ) ) {
				throw new DuplicateSymbolException( symbolFromWildcard.name() );
			}
			if( symbolFromWildcard.privacy() == Privacy.PUBLIC ) {
				this.symbols.put( symbolFromWildcard.name(), symbolFromWildcard );
			}
		}
		this.symbols.remove( wildCardSymbol.name(), wildCardSymbol );
	}

	/**
	 * create and add a local Symbol with it's ASTNode to the table
	 * 
	 * @param name Symbol name in local execution context
	 * @param node an AST node implementing SymbolNode
	 * 
	 * @throws ModuleException when adding name duplicate name to the symbol
	 */
	public void addSymbol( String name, SymbolNode node ) throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			throw new DuplicateSymbolException( name );
		}
		this.symbols.put( name, new SymbolInfoLocal( name, node ) );
	}

	/**
	 * create and add an external Symbol with it's module target to the table
	 * 
	 * @param context Context where symbol is declare
	 * @param name Symbol name
	 * @param moduleTargetStrings String array defining target module location defined at import
	 *        statement
	 * 
	 * @throws ModuleException when adding name duplicate name to the symbol
	 */
	public void addSymbol( ParsingContext context, String name, List< String > moduleTargetStrings )
		throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			throw new DuplicateSymbolException( name );
		}
		this.symbols.put( name,
			new SymbolInfoExternal( context, name, moduleTargetStrings, name ) );
	}

	/**
	 * create and add an external Symbol
	 * 
	 * @param context Context where symbol is declare
	 * @param name Symbol name
	 * @param moduleTargetStrings String array defining target module location defined at import
	 *        statement
	 * @param moduleSymbol Name for binding result to local environment
	 * 
	 * @throws ModuleException when adding name duplicate name to the symbol
	 */
	public void addSymbol( ParsingContext context, String name, List< String > moduleTargetStrings,
		String moduleSymbol ) throws DuplicateSymbolException {
		if( isDuplicateSymbol( name ) ) {
			throw new DuplicateSymbolException( name );
		}
		this.symbols.put( name,
			new SymbolInfoExternal( context, name, moduleTargetStrings, moduleSymbol ) );
	}

	/**
	 * add an wildcard Symbol to table
	 * 
	 * @param moduleTargetStrings an array of defining target module locationString defined at import
	 *        statement
	 */
	public void addWildCardSymbol( ParsingContext context, List< String > moduleTargetStrings ) {
		this.symbols.put( moduleTargetStrings.toString(),
			new SymbolWildCard( context, moduleTargetStrings ) );
	}

	public SymbolInfo[] symbols() {
		return this.symbols.values().toArray( new SymbolInfo[ 0 ] );
	}

	public SymbolInfoLocal[] localSymbols() {
		return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.LOCAL )
			.toArray( SymbolInfoLocal[]::new );
	}

	public SymbolInfoExternal[] externalSymbols() {
		return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.EXTERNAL )
			.toArray( SymbolInfoExternal[]::new );
	}

	public Optional< SymbolInfo > symbol( String name ) {
		if( this.symbols.containsKey( name ) ) {
			return Optional.of( this.symbols.get( name ) );
		}
		return Optional.empty();
	}

	private boolean isDuplicateSymbol( String name ) {
		if( symbols.containsKey( name ) && symbols.get( name ).scope() != Scope.LOCAL ) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "SymbolTable [source=" + source + ", symbols=" + symbols + "]";
	}

}
