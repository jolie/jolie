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

import java.util.Optional;
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.context.ParsingContext;

/**
 * A class represent a Symbol defined within external execution environment. Create when consuming
 * an import statement
 */
class ImportedSymbolInfo extends SymbolInfo {

	private final ImportPath importPath;
	private final String originalSymbolName;
	private ModuleSource moduleSource;

	protected ImportedSymbolInfo( ParsingContext context, String name, ImportPath importPath,
		String originalSymbolName ) {
		super( context, name, Scope.EXTERNAL, AccessModifier.PRIVATE );
		this.importPath = importPath;
		this.originalSymbolName = originalSymbolName;
	}

	/**
	 * set a destination source of the symbol
	 */
	protected void setModuleSource( ModuleSource moduleSource ) {
		this.moduleSource = moduleSource;
	}

	protected ImportPath importPath() {
		return this.importPath;
	}

	protected Optional< ModuleSource > moduleSource() {
		return Optional.of( this.moduleSource );
	}

	protected String originalSymbolName() {
		return this.originalSymbolName;
	}

}
