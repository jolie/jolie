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
import jolie.lang.parse.ast.SymbolNode;
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
	 * Symbol privacy, PUBLIC means the symbol's AST node is allowed to be imported by external modules
	 * PRIVATE means the symbol's AST node is not allowed to be imported by external modules
	 */
	public enum Privacy {
		PUBLIC, PRIVATE
	}

	/**
	 * name of the symbol
	 */
	private String name;

	/**
	 * scope of this symbol
	 */
	private Scope scope;

	/**
	 * pointer to an AST node
	 */
	private OLSyntaxNode node;

	/**
	 * privacy of this symbol
	 */
	final private Privacy privacy;

	/**
	 * Declaration context of the symbol
	 */
	final private ParsingContext context;

	/**
	 * constructor for SymbolInfo, this constructor is used when it knows the ASTnode to point to
	 * corresponding to this symbol
	 * 
	 * @param name Symbol name
	 * @param scope scope of Symbol
	 * @param node an ASTNode implementing SymbolNode
	 */
	public SymbolInfo( String name, Scope scope, SymbolNode node ) {
		this.context = node.node().context();
		this.name = name;
		this.scope = scope;
		this.privacy = node.privacy();
		this.node = node.node();
	}

	/**
	 * constructor for SymbolInfo, this constructor is used when AST Node is unknown at creating time or
	 * in an external context
	 * 
	 * @param context context of creating Symbol
	 * @param name Symbol name
	 * @param scope scope of Symbol
	 */
	public SymbolInfo( ParsingContext context, String name, Scope scope ) {
		this.context = context;
		this.name = name;
		this.scope = scope;
		this.privacy = Privacy.PRIVATE;
	}

	public String name() {
		return name;
	}

	/**
	 * set a pointer to an AST node. the pointer should be only defined once
	 */
	public void setPointer( OLSyntaxNode pointer ) {
		this.node = pointer;
	}

	public OLSyntaxNode node() {
		return node;
	}

	public Scope scope() {
		return scope;
	}

	public Privacy privacy() {
		return privacy;
	}

	public ParsingContext context() {
		return context;
	}

	@Override
	public String toString() {
		return "SymbolInfo [name=" + name + ", scope=" + scope + "]";
	}


}
