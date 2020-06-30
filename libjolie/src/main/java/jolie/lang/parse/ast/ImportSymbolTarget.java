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

/**
 * A class for holding information of symbol tar
 */
public class ImportSymbolTarget {
	private final String originalSymbolName;
	private final String localSymbolName;

	/**
	 * @param originalSymbolName a symbol of taring module
	 * @param localSymbolName a symbol of to place in local execution
	 */
	public ImportSymbolTarget( String originalSymbolName, String localSymbolName ) {
		this.originalSymbolName = originalSymbolName;
		this.localSymbolName = localSymbolName;
	}

	@Override
	public String toString() {
		if( this.originalSymbolName.equals( this.localSymbolName ) ) {
			return this.originalSymbolName;
		}
		return this.originalSymbolName + " as " + this.localSymbolName;
	}

	public String originalSymbolName() {
		return originalSymbolName;
	}

	public String localSymbolName() {
		return localSymbolName;
	}
}
