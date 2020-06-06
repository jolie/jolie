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

package jolie.lang.parse.ast;

import jolie.lang.parse.module.SymbolInfo.Privacy;

/**
 * An interface of Symbol defines an importable symbols in Jolie
 */
public interface SymbolNode {
	/**
	 * returns the privacy of the symbol, can be either PRIVATE or PUBLIC
	 */
	public Privacy privacy();

	/**
	 * set Privacy of the symbol
	 * 
	 * @param isPrivate a boolean defines the private status of the symbol
	 */
	public void setPrivate( boolean isPrivate );

	/**
	 * returns qualify name of the symbol in local execution
	 */
	public String name();

	/**
	 * returns linking AST node of the symbol.
	 */
	public OLSyntaxNode node();
}
