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

import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.ImportableSymbol;
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.context.ParsingContext;

/**
 * an abstract class of Symbol declaration in Jolie
 */
public abstract class SymbolInfo {

	/**
	 * Scope of symbol, LOCAL means the symbol's AST node is declared within the local execution
	 * environment EXTERNAL means the symbol's AST node is declared in other execution environment
	 */
	public enum Scope {
		LOCAL, EXTERNAL
	}

	/**
	 * name of the symbol
	 */
	private final String name;

	/**
	 * scope of this symbol
	 */
	private final Scope scope;

	/**
	 * Accessibility modifier of this symbol
	 */
	private final AccessModifier accessModifier;

	/**
	 * Declaration context of the symbol
	 */
	private final ParsingContext context;

	/**
	 * pointer to an AST node
	 */
	private OLSyntaxNode node;

	/**
	 * constructor for SymbolInfo
	 * 
	 * @param context context where symbol is declared
	 * @param name Symbol name
	 * @param scope scope of Symbol
	 * @param node an ASTNode implementing SymbolNode
	 */
	protected SymbolInfo( ParsingContext context, String name, Scope scope, AccessModifier accessModifier ) {
		this.context = context;
		this.name = name;
		this.scope = scope;
		this.accessModifier = accessModifier;
	}

	protected String name() {
		return name;
	}

	/**
	 * resolve an external symbol node by set a pointer to an AST node
	 */
	protected void resolve( OLSyntaxNode pointer ) {
		this.node = pointer;
	}

	protected OLSyntaxNode node() {
		return node;
	}

	protected Scope scope() {
		return scope;
	}

	protected AccessModifier accessModifier() {
		return accessModifier;
	}

	protected ParsingContext context() {
		return context;
	}

}
