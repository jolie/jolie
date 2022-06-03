/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.runtime;


import java.util.Arrays;

import jolie.Interpreter;
import jolie.process.TransformationReason;
import jolie.runtime.expression.Expression;
import jolie.util.Pair;

/**
 * @author Fabrizio Montesi
 */
public class GlobalVariablePath extends VariablePath {
	public GlobalVariablePath( Pair< Expression, Expression >[] path ) {
		super( path );
	}

	@Override
	public boolean isGlobal() {
		return true;
	}

	@Override
	protected VariablePath _createVariablePath( Pair< Expression, Expression >[] path ) {
		return new GlobalVariablePath( path );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		Pair< Expression, Expression >[] clonedPath = cloneExpressionHelper( path(), reason );
		return new GlobalVariablePath( clonedPath );
	}

	@Override
	protected Value getRootValue() {
		return Interpreter.getInstance().globalValue();
	}

	@Override
	public VariablePath copy() {
		return new GlobalVariablePath( Arrays.copyOf( path(), path().length ) );
	}
}
