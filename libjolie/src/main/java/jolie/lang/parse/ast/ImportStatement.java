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

import java.util.Arrays;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

/**
 * A class for holding the information of an import statement in jolie.
 */
public class ImportStatement extends OLSyntaxNode {

	private static final long serialVersionUID = Constants.serialVersionUID();
	private final ImportSymbolTarget[] importSymbolTargets;
	private final List< String > importTarget;
	private final boolean isNamespaceImport;

	/**
	 * Constructor for namespace import
	 *
	 * @param context parsing context
	 * @param importTarget tokenized import target, empty denote a dot token ( import target for .A.B
	 *        should give this field ["", "A", "", "B"])
	 */
	public ImportStatement( ParsingContext context, List< String > importTarget ) {
		this( context, importTarget, true, null );
	}

	/**
	 * Constructor for qualified import
	 *
	 * @param context a parsing context
	 * @param importTarget tokenized import target, empty denote a dot token ( import target for .A.B
	 *        should give this field ["", "A", "", "B"])
	 * @param pathNodes list of parsed Symbol token in target Symbol each element in list refer to pair
	 *        of (moduleSymbolName, localSymbolName)
	 */
	public ImportStatement( ParsingContext context, List< String > importTarget,
		List< Pair< String, String > > pathNodes ) {
		this( context, importTarget, false, pathNodes );
	}


	/**
	 * Constructor for qualified import
	 *
	 * @param context a parsing context
	 * @param importTarget tokenized import target, empty denote a dot token ( import target for .A.B
	 *        should give this field ["", "A", "", "B"])
	 * @param isNamespaceImport a boolean flag if the
	 * @param pathNodes list of parsed Symbol token in target Symbol each element in list refer to pair
	 *        of (moduleSymbolName, localSymbolName)
	 */
	private ImportStatement( ParsingContext context, List< String > importTarget,
		boolean isNamespaceImport, List< Pair< String, String > > pathNodes ) {
		super( context );
		this.importTarget = importTarget;
		this.isNamespaceImport = isNamespaceImport;
		if( pathNodes != null ) {
			importSymbolTargets = new ImportSymbolTarget[ pathNodes.size() ];
			for( int i = 0; i < pathNodes.size(); i++ ) {
				importSymbolTargets[ i ] = new ImportSymbolTarget( pathNodes.get( i ).key(),
					pathNodes.get( i ).value() );
			}
		} else {
			importSymbolTargets = null;
		}
	}

	public List< String > importTarget() {
		return importTarget;
	}

	public boolean isNamespaceImport() {
		return isNamespaceImport;
	}

	public ImportSymbolTarget[] importSymbolTargets() {
		return importSymbolTargets;
	}

	public String prettyPrintTarget() {
		String ret = "";
		boolean relativeEnded = false;
		for( String token : this.importTarget ) {
			if( token.isEmpty() ) {
				ret += ".";
			} else {
				if( relativeEnded ) {
					ret += ".";
				}
				relativeEnded = true;
				ret += token;
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		String target = (this.isNamespaceImport) ? "*" : prettyPrintTarget();
		return "from " + target + " import " + Arrays.toString( this.importSymbolTargets );
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
